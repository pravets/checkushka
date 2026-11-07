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
package ru.pravets.checkushka.core.source;

/**
 * Формат выгрузки конфигурации.
 */
public enum SourceFormat {

  /** Определить по структуре каталога. */
  AUTO,

  /** Выгрузка конфигуратором: {@code Configuration.xml} и каталоги групп объектов. */
  DESIGNER,

  /** Проект 1С:EDT: {@code Configuration.mdo} внутри {@code src/Configuration}. */
  EDT;

  /**
   * Разбирает формат по имени без учёта регистра.
   *
   * @param value        значение
   * @param defaultValue формат по умолчанию
   * @return формат
   */
  public static SourceFormat parse(String value, SourceFormat defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    for (var format : values()) {
      if (format.name().equalsIgnoreCase(value.trim())) {
        return format;
      }
    }
    return defaultValue;
  }
}
