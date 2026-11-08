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
import java.util.LinkedHashMap;
import java.util.List;
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
import ru.pravets.checkushka.core.engine.NameComparator;
import ru.pravets.checkushka.core.engine.PrefixMode;
import ru.pravets.checkushka.core.source.SourceLocation;
import ru.pravets.checkushka.core.source.SourceOrder;

/**
 * Проверка порядка объектов метаданных в конфигурации.
 *
 * <p>Объекты одного типа должны идти в порядке имён. Режим учёта префиксов задаётся параметром
 * {@code mode} ({@code none}, {@code stripPrefix}, {@code groupByPrefix}), список префиксов —
 * параметром {@code prefixes}. Если список пуст, префикс распознаётся по шаблону
 * «2–5 букв и разделитель».
 *
 * <p>По умолчанию отчёт агрегируется: одно замечание на тип метаданных со счётчиком нарушений.
 * Параметр {@code detail} со значением {@code object} возвращает замечание на каждый объект.
 */
@CheckMetadata(
  key = MetadataOrderCheck.KEY,
  name = "Порядок объектов метаданных",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.STANDARD, CheckTag.DESIGN},
  targets = CheckTarget.CONFIGURATION,
  minutesToFix = 1)
public final class MetadataOrderCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "MetadataOrder";

  /** Значение параметра {@code detail}: одно замечание на тип метаданных. */
  static final String DETAIL_TYPE = "type";

  /** Значение параметра {@code detail}: замечание на каждый объект. */
  static final String DETAIL_OBJECT = "object";

  private static final CheckMetadata META = MetadataOrderCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    var parameters = context.parameters();
    return analyze(
      context.project().order(),
      PrefixMode.parse(parameters.string("mode", "none"), PrefixMode.NONE),
      parameters.stringList("prefixes", List.of()),
      parameters.string("detail", DETAIL_TYPE));
  }

  /**
   * Проверяет порядок объектов с агрегированием по типу.
   *
   * @param order    порядок объектов из файла конфигурации
   * @param mode     режим учёта префиксов
   * @param prefixes настроенные префиксы
   * @return замечания, одно на тип метаданных
   */
  static List<CheckResult> analyze(SourceOrder order, PrefixMode mode, List<String> prefixes) {
    return analyze(order, mode, prefixes, DETAIL_TYPE);
  }

  /**
   * Проверяет порядок объектов.
   *
   * @param order    порядок объектов из файла конфигурации
   * @param mode     режим учёта префиксов
   * @param prefixes настроенные префиксы
   * @param detail   {@code type} — агрегировать по типу, {@code object} — по замечанию на объект
   * @return замечания
   */
  static List<CheckResult> analyze(SourceOrder order, PrefixMode mode, List<String> prefixes,
                                   String detail) {
    if (order.entries().isEmpty()) {
      return List.of();
    }
    var comparator = NameComparator.instance();
    var violations = new ArrayList<Violation>();
    var previousKeys = new HashMap<String, String>();
    var previousNames = new HashMap<String, String>();

    for (var entry : order.entries()) {
      var sortKey = mode.sortKey(entry.name(), prefixes);
      var previousKey = previousKeys.get(entry.type());
      if (previousKey != null && comparator.compare(previousKey, sortKey) > 0) {
        violations.add(new Violation(entry.type(), entry.name(), previousNames.get(entry.type()),
          entry.line()));
      }
      previousKeys.put(entry.type(), sortKey);
      previousNames.put(entry.type(), entry.name());
    }

    return DETAIL_OBJECT.equals(detail) ? detailed(order, violations) : aggregated(order, violations);
  }

  private static List<CheckResult> detailed(SourceOrder order, List<Violation> violations) {
    var results = new ArrayList<CheckResult>(violations.size());
    for (var violation : violations) {
      results.add(new CheckResult(
        KEY,
        "Объект \"" + violation.name() + "\" должен располагаться раньше \""
          + violation.previousName() + "\"",
        META.severity(),
        SourceLocation.of(order.file(), violation.line(),
          violation.type() + "." + violation.name()),
        META.minutesToFix()));
    }
    return results;
  }

  private static List<CheckResult> aggregated(SourceOrder order, List<Violation> violations) {
    var byType = new LinkedHashMap<String, TypeStat>();
    for (var violation : violations) {
      byType.computeIfAbsent(violation.type(), type -> new TypeStat(violation.line())).increment();
    }
    var results = new ArrayList<CheckResult>(byType.size());
    for (var entry : byType.entrySet()) {
      results.add(new CheckResult(
        KEY,
        MetadataTypeNames.plural(entry.getKey()) + " не отсортированы по имени: нарушений "
          + entry.getValue().count(),
        META.severity(),
        SourceLocation.of(order.file(), entry.getValue().firstLine(), entry.getKey()),
        META.minutesToFix()));
    }
    return results;
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(
      new CheckParameter(
        "mode", "string", "none",
        "Режим учёта префиксов: none, stripPrefix, groupByPrefix"),
      new CheckParameter(
        "prefixes", "string[]", "[]",
        "Префиксы имён объектов; пустой список включает автоопределение"),
      new CheckParameter(
        "detail", "string", DETAIL_TYPE,
        "Что попадает в отчёт: type - одно замечание на тип метаданных, object - на каждый объект"));
  }

  private record Violation(String type, String name, String previousName, int line) {
  }

  private static final class TypeStat {

    private final int firstLine;
    private int count;

    private TypeStat(int firstLine) {
      this.firstLine = firstLine;
    }

    private void increment() {
      count++;
    }

    private int firstLine() {
      return firstLine;
    }

    private int count() {
      return count;
    }
  }
}
