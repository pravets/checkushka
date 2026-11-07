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
package ru.pravets.checkushka.core.engine;

/**
 * Режим включения проверок, совпадающий по смыслу с режимом диагностик BSL LS.
 */
public enum ChecksMode {

  /** Все проверки, включённые по умолчанию, плюс явно включённые в конфигурации. */
  ON,

  /** Все проверки выключены. */
  OFF,

  /** Все, кроме перечисленных. */
  EXCEPT,

  /** Только перечисленные. */
  ONLY,

  /** Все проверки, включая выключенные по умолчанию. */
  ALL;

  /**
   * Разбирает режим из конфигурации без учёта регистра.
   *
   * @param value значение из файла
   * @param defaultValue режим по умолчанию
   * @return режим
   */
  public static ChecksMode parse(String value, ChecksMode defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    for (var mode : values()) {
      if (mode.name().equalsIgnoreCase(value.trim())) {
        return mode;
      }
    }
    return defaultValue;
  }
}
