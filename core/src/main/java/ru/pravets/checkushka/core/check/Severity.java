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
package ru.pravets.checkushka.core.check;

/**
 * Важность замечания.
 */
public enum Severity {

  BLOCKER,
  CRITICAL,
  MAJOR,
  MINOR,
  INFO;

  /**
   * Сравнивает важности по «строгости»: {@code BLOCKER} строже {@code INFO}.
   *
   * @param other важность для сравнения
   * @return {@code true}, если текущая важность не ниже указанной
   */
  public boolean atLeast(Severity other) {
    return ordinal() <= other.ordinal();
  }

  /**
   * Разбирает важность из конфигурации без учёта регистра.
   *
   * @param value        значение из файла
   * @param defaultValue важность по умолчанию
   * @return важность
   */
  public static Severity parse(String value, Severity defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    for (var severity : values()) {
      if (severity.name().equalsIgnoreCase(value.trim())) {
        return severity;
      }
    }
    return defaultValue;
  }
}
