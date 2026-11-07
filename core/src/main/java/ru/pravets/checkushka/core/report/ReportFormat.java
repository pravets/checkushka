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
package ru.pravets.checkushka.core.report;

import java.util.Locale;

/**
 * Формат отчёта.
 */
public enum ReportFormat {

  CONSOLE,
  JSON,
  SARIF,
  JUNIT,
  GENERIC_ISSUE;

  /**
   * Разбирает формат по имени; регистр, дефисы и подчёркивания не важны.
   *
   * @param value        имя формата
   * @param defaultValue формат по умолчанию
   * @return формат
   */
  public static ReportFormat parse(String value, ReportFormat defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    for (var format : values()) {
      if (normalize(format.name()).equals(normalize(value))) {
        return format;
      }
    }
    throw new IllegalArgumentException("Неизвестный формат отчёта: " + value
      + "; допустимые: " + String.join(", ", names()));
  }

  private static String normalize(String value) {
    return value.replace("_", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
  }

  private static String[] names() {
    var values = values();
    var result = new String[values.length];
    for (var index = 0; index < values.length; index++) {
      result[index] = values[index].name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
    return result;
  }
}
