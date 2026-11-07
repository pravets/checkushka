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

import java.util.List;
import org.junit.jupiter.api.Test;

class PrefixModeTest {

  private static final List<String> PREFIXES = List.of("об_", "мс_");

  @Test
  void noneKeepsFullName() {
    assertThat(PrefixMode.NONE.sortKey("об_Модуль", PREFIXES)).isEqualTo("об_Модуль");
  }

  @Test
  void stripPrefixRemovesConfiguredPrefix() {
    assertThat(PrefixMode.STRIP_PREFIX.sortKey("об_Модуль", PREFIXES)).isEqualTo("Модуль");
    assertThat(PrefixMode.STRIP_PREFIX.sortKey("Модуль", PREFIXES)).isEqualTo("Модуль");
  }

  @Test
  void stripPrefixDetectsPrefixWhenNotConfigured() {
    assertThat(PrefixMode.STRIP_PREFIX.sortKey("аб_Модуль", List.of())).isEqualTo("Модуль");
    assertThat(PrefixMode.STRIP_PREFIX.sortKey("Модуль", List.of())).isEqualTo("Модуль");
  }

  @Test
  void groupByPrefixPutsSamePrefixTogether() {
    var first = PrefixMode.GROUP_BY_PREFIX.sortKey("об_Модуль", PREFIXES);
    var second = PrefixMode.GROUP_BY_PREFIX.sortKey("Модуль", PREFIXES);

    assertThat(first).startsWith("об_");
    assertThat(second).startsWith("\u0001");
    assertThat(NameComparator.instance().compare(second, first)).isNegative();
  }

  @Test
  void choosesLongestPrefix() {
    var mode = PrefixMode.STRIP_PREFIX;

    assertThat(mode.prefixOf("об_Модуль", List.of("об", "об_"))).isEqualTo("об_");
  }

  @Test
  void parsesModeByNameIgnoringCase() {
    assertThat(PrefixMode.parse("groupByPrefix", PrefixMode.NONE)).isEqualTo(PrefixMode.GROUP_BY_PREFIX);
    assertThat(PrefixMode.parse("strip-prefix", PrefixMode.NONE)).isEqualTo(PrefixMode.STRIP_PREFIX);
    assertThat(PrefixMode.parse("неизвестно", PrefixMode.NONE)).isEqualTo(PrefixMode.NONE);
  }
}
