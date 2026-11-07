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
package ru.pravets.checkushka.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Сериализация результатов инструментов MCP в JSON-текст.
 */
final class JsonText {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonText() {
  }

  static String pretty(Object value) {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Не удалось сериализовать результат", e);
    }
  }
}
