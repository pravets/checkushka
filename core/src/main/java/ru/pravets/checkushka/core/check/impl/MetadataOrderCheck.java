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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.NameComparator;
import ru.pravets.checkushka.core.engine.PrefixMode;
import ru.pravets.checkushka.core.source.SourceLocation;

/**
 * Проверка порядка объектов метаданных в конфигурации.
 *
 * <p>Объекты одного типа должны идти в порядке имён. Режим учёта префиксов задаётся параметром
 * {@code mode} ({@code none}, {@code stripPrefix}, {@code groupByPrefix}), список префиксов —
 * параметром {@code prefixes}. Если список пуст, префикс распознаётся по шаблону
 * «2–5 букв и разделитель».
 */
@CheckMetadata(
  key = MetadataOrderCheck.KEY,
  name = "Порядок объектов метаданных",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.STANDARD, CheckTag.DESIGN},
  targets = CheckTarget.CONFIGURATION,
  minutesToFix = 1)
public final class MetadataOrderCheck implements MetadataCheck {

  /** Ключ проверки. */
  public static final String KEY = "MetadataOrder";

  private static final CheckMetadata META = MetadataOrderCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    var parameters = context.parameters();
    return analyze(
      context.project().order(),
      PrefixMode.parse(parameters.string("mode", "none"), PrefixMode.NONE),
      parameters.stringList("prefixes", List.of()));
  }

  /**
   * Проверяет порядок объектов.
   *
   * @param order    порядок объектов из файла конфигурации
   * @param mode     режим учёта префиксов
   * @param prefixes настроенные префиксы
   * @return замечания
   */
  static List<CheckResult> analyze(ru.pravets.checkushka.core.source.SourceOrder order,
                                   PrefixMode mode, List<String> prefixes) {
    if (order.entries().isEmpty()) {
      return List.of();
    }
    var comparator = NameComparator.instance();

    var results = new ArrayList<CheckResult>();
    var previousKeys = new HashMap<String, String>();
    var previousNames = new HashMap<String, String>();

    for (var entry : order.entries()) {
      var sortKey = mode.sortKey(entry.name(), prefixes);
      var previousKey = previousKeys.get(entry.type());
      if (previousKey != null && comparator.compare(previousKey, sortKey) > 0) {
        results.add(new CheckResult(
          KEY,
          "Объект «" + entry.name() + "» должен располагаться раньше «"
            + previousNames.get(entry.type()) + "»",
          META.severity(),
          SourceLocation.of(order.file(), entry.line(), entry.type() + "." + entry.name()),
          META.minutesToFix()));
      }
      previousKeys.put(entry.type(), sortKey);
      previousNames.put(entry.type(), entry.name());
    }
    return results;
  }
}
