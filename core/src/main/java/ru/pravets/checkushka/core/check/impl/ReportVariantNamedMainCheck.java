/*
 * This file is a part of Checkushka.
 *
 * Copyright (c) 2026 Checkushka authors and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * Checkushka is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Checkushka is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Checkushka. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.pravets.checkushka.core.check.impl;

import com.github._1c_syntax.bsl.mdo.Report;
import com.github._1c_syntax.bsl.mdo.storage.DataCompositionSchema;
import com.github._1c_syntax.bsl.mdo.support.TemplateType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckParameter;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.ParameterizedCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

/**
 * Проверка вариантов отчёта, названных шаблонно.
 *
 * <p>Имя варианта отчёта видит пользователь в списке вариантов. Имя {@code Основной} не несёт
 * смысла и повторяется у разных отчётов, поэтому вариант должен называться по назначению.
 *
 * <p>Проверка работает на модели mdclasses 0.22: варианты читаются из макетов схемы компоновки
 * данных ({@code Reports/<Отчёт>/Templates/<Макет>/Template.dcs} в проекте EDT и
 * {@code .../Ext/Template.xml} в выгрузке конфигуратора). Список запрещённых имён задаётся
 * параметром {@code names}, сравнение — без учёта регистра.
 */
@CheckMetadata(
  key = ReportVariantNamedMainCheck.KEY,
  name = "Вариант отчёта с именем Основной",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.STANDARD, CheckTag.DESIGN},
  targets = CheckTarget.MDO,
  minutesToFix = 1)
public final class ReportVariantNamedMainCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "ReportVariantNamedMain";

  /** Имена вариантов, которые считаются шаблонными по умолчанию. */
  static final List<String> DEFAULT_NAMES = List.of("Основной");

  private static final CheckMetadata META =
    ReportVariantNamedMainCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    if (!(context.mdo() instanceof Report report)) {
      return List.of();
    }
    var fallback = context.project().layout().fileOf(report.getMdoRef())
      .orElseGet(() -> context.project().root());
    return analyze(report, context.parameters().stringList("names", DEFAULT_NAMES), fallback);
  }

  /**
   * Проверяет варианты отчёта.
   *
   * @param report       отчёт
   * @param names        имена вариантов, которые считаются шаблонными
   * @param fallbackFile файл для замечания, если расположение макета определить не удалось
   * @return замечания по вариантам
   */
  static List<CheckResult> analyze(Report report, List<String> names, Path fallbackFile) {
    var forbidden = normalize(names);
    if (forbidden.isEmpty()) {
      return List.of();
    }

    var results = new ArrayList<CheckResult>();
    for (var template : report.getTemplates()) {
      if (template.getTemplateType() != TemplateType.DATA_COMPOSITION_SCHEME
        || !(template.getData() instanceof DataCompositionSchema schema)) {
        continue;
      }
      var variants = schema.getSettingsVariants();
      if (variants == null) {
        continue;
      }

      var file = templateFile(schema, fallbackFile);
      for (var index = 0; index < variants.size(); index++) {
        var variant = variants.get(index);
        if (variant == null || variant.name() == null
          || !forbidden.contains(variant.name().trim().toLowerCase(Locale.ROOT))) {
          continue;
        }
        results.add(new CheckResult(
          KEY,
          message(variant.name(), template.getName()),
          META.severity(),
          SourceLocation.of(file, SettingsVariantLocator.nameLine(file, index), report.getMdoRef()),
          META.minutesToFix()));
      }
    }
    return results;
  }

  private static Set<String> normalize(List<String> names) {
    var normalized = new HashSet<String>();
    if (names == null) {
      return normalized;
    }
    names.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(name -> !name.isEmpty())
      .map(name -> name.toLowerCase(Locale.ROOT))
      .forEach(normalized::add);
    return normalized;
  }

  private static String message(String name, String templateName) {
    return "Вариант отчёта \"" + name + "\" в макете \"" + templateName
      + "\" назван шаблонно: имя варианта должно отражать его назначение";
  }

  private static Path templateFile(DataCompositionSchema schema, Path fallbackFile) {
    var dataPath = schema.getDataPath();
    if (dataPath != null && "file".equalsIgnoreCase(dataPath.getScheme())) {
      try {
        return Path.of(dataPath);
      } catch (RuntimeException e) {
        // путь из другого файлового провайдера — используем запасной вариант
        return fallbackFile;
      }
    }
    return fallbackFile;
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(new CheckParameter(
      "names", "string[]", "[\"Основной\"]",
      "Имена вариантов, которые считаются шаблонными; сравнение без учёта регистра"));
  }
}
