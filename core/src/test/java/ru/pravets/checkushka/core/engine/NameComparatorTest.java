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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameComparatorTest {

  private final NameComparator comparator = NameComparator.instance();

  @Test
  void comparesNumbersAsNumbers() {
    assertThat(comparator.compare("Модуль2", "Модуль10")).isNegative();
    assertThat(comparator.compare("Модуль10", "Модуль2")).isPositive();
    assertThat(comparator.compare("Модуль2", "Модуль02")).isZero();
  }

  @Test
  void ignoresCase() {
    assertThat(comparator.compare("Модуль", "модуль")).isZero();
    assertThat(comparator.compare("Альфа", "Бета")).isNegative();
  }

  @Test
  void comparesCyrillicAndLatin() {
    assertThat(comparator.compare("Alpha", "Бета")).isNotZero();
    assertThat(comparator.compare("Бета", "Бета")).isZero();
  }

  @Test
  void emptyNamesAreEqual() {
    assertThat(comparator.compare("", "")).isZero();
  }
}
