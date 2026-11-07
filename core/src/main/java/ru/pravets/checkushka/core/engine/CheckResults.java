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

import java.util.LinkedHashSet;
import java.util.List;
import ru.pravets.checkushka.core.check.CheckResult;

/**
 * Операции над набором замечаний.
 */
public final class CheckResults {

  private CheckResults() {
  }

  /**
   * Убирает полные дубликаты замечаний, сохраняя порядок первого вхождения.
   *
   * <p>Дубликатом считается совпадение ключа проверки, текста и расположения. Это страховка
   * от повторного обхода одной и той же сущности (например, после слияния конфигурации
   * с расширениями).
   *
   * @param results замечания
   * @return замечания без дублей
   */
  public static List<CheckResult> distinct(List<CheckResult> results) {
    var seen = new LinkedHashSet<String>();
    return results.stream()
      .filter(result -> seen.add(identity(result)))
      .toList();
  }

  private static String identity(CheckResult result) {
    var location = result.location();
    return result.key() + '\u0000' + result.message() + '\u0000' + location.file()
      + '\u0000' + location.line() + '\u0000' + location.mdoRef();
  }
}
