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

import java.util.regex.Pattern;

/**
 * Правило исключения замечания из отчёта.
 *
 * @param check  ключ проверки или {@code null}, если правило применяется ко всем проверкам
 * @param mdo    шаблон ссылки на объект метаданных ({@code *} — любой набор символов)
 * @param reason причина исключения, попадает в отчёт
 */
public record IgnoreRule(String check, String mdo, String reason) {

  /**
   * Проверяет, подходит ли правило под замечание.
   *
   * @param checkKey ключ проверки
   * @param mdoRef   ссылка на объект метаданных или {@code null}
   * @return {@code true}, если замечание нужно исключить
   */
  public boolean matches(String checkKey, String mdoRef) {
    return matchesCheck(checkKey) && matchesMdo(mdoRef);
  }

  private boolean matchesCheck(String checkKey) {
    return check == null || check.isBlank() || check.equalsIgnoreCase(checkKey);
  }

  private boolean matchesMdo(String mdoRef) {
    if (mdo == null || mdo.isBlank()) {
      return true;
    }
    if (mdoRef == null) {
      return false;
    }
    return Pattern.compile(toRegex(mdo), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
      .matcher(mdoRef)
      .matches();
  }

  private static String toRegex(String pattern) {
    var builder = new StringBuilder();
    for (var symbol : pattern.toCharArray()) {
      if (symbol == '*') {
        builder.append(".*");
      } else {
        builder.append(Pattern.quote(String.valueOf(symbol)));
      }
    }
    return builder.toString();
  }
}
