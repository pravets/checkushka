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
package ru.pravets.checkushka.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConsoleStreamsTest {

  private static final Charset CP866 = Charset.forName("Cp866");
  private static final Charset CP1251 = Charset.forName("Cp1251");

  @AfterEach
  void clearOverride() {
    System.clearProperty(ConsoleStreams.ENCODING_PROPERTY);
  }

  @Test
  void usesUtf8WithoutConsoleAndWithoutPipe() {
    assertThat(ConsoleStreams.choose(null, null, null)).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void usesConsoleCharsetWhenOutputGoesToConsole() {
    assertThat(ConsoleStreams.choose(null, CP866, CP1251)).isEqualTo(CP866);
  }

  @Test
  void usesConsoleCodePageForPipes() {
    assertThat(ConsoleStreams.choose(null, null, CP866)).isEqualTo(CP866);
  }

  @Test
  void explicitEncodingWins() {
    assertThat(ConsoleStreams.choose(CP1251, CP866, CP866)).isEqualTo(CP1251);
  }

  @Test
  void readsExplicitEncodingFromProperty() {
    System.setProperty(ConsoleStreams.ENCODING_PROPERTY, "Cp1251");

    assertThat(ConsoleStreams.requested()).isEqualTo(CP1251);
  }

  @Test
  void unknownEncodingIsIgnored() {
    System.setProperty(ConsoleStreams.ENCODING_PROPERTY, "НетТакой");

    assertThat(ConsoleStreams.requested()).isNull();
  }
}
