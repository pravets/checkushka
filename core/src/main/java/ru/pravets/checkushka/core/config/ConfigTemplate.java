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
package ru.pravets.checkushka.core.config;

/**
 * Шаблон конфигурационного файла для команды {@code config init}.
 */
public final class ConfigTemplate {

  /** Имя конфигурационного файла в корне проекта. */
  public static final String FILE_NAME = "checkushka.json";

  private static final String JSON = """
    {
      "$schema": "https://raw.githubusercontent.com/pravets/checkushka/master/schema/checkushka.schema.json",
      "language": "ru",
      "source": {
        "format": "auto"
      },
      "checks": {
        "mode": "ON",
        "parameters": {
          "MetadataOrder": {
            "mode": "none",
            "prefixes": []
          }
        },
        "severity": {}
      },
      "ignore": []
    }
    """;

  private ConfigTemplate() {
  }

  /**
   * Содержимое шаблона.
   *
   * @return JSON с настройками по умолчанию
   */
  public static String render() {
    return JSON;
  }
}
