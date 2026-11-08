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
package ru.pravets.checkushka.core.check.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

class MetadataTypeNamesTest {

  @Test
  void translatesKnownTypes() {
    assertThat(MetadataTypeNames.plural("CommonModule")).isEqualTo("Общие модули");
    assertThat(MetadataTypeNames.plural("Catalog")).isEqualTo("Справочники");
    assertThat(MetadataTypeNames.plural("Document")).isEqualTo("Документы");
    assertThat(MetadataTypeNames.plural("Role")).isEqualTo("Роли");
  }

  @Test
  void fallsBackToPlatformTypeName() {
    assertThat(MetadataTypeNames.plural("UnknownThing"))
      .isEqualTo("Метаданные типа \"UnknownThing\"");
  }

  @Test
  void namesAreEncodableInConsoleCodePage() {
    var cp866 = Charset.forName("Cp866");

    assertThat(cp866.newEncoder().canEncode(MetadataTypeNames.plural("CommonModule"))).isTrue();
    assertThat(cp866.newEncoder().canEncode("не отсортированы по имени")).isTrue();
  }
}
