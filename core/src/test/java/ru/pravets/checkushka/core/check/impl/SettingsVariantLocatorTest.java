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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsVariantLocatorTest {

  @TempDir
  private Path dir;

  @Test
  void findsNameLineOfEachVariant() throws IOException {
    var file = write("""
      <DataCompositionSchema>
        <settingsVariant>
          <dcsset:name>Детальный</dcsset:name>
          <dcsset:settings/>
        </settingsVariant>
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
          <dcsset:settings/>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.nameLine(file, 0)).isEqualTo(3);
    assertThat(SettingsVariantLocator.nameLine(file, 1)).isEqualTo(7);
  }

  @Test
  void findsLinesInSingleLineXml() throws IOException {
    var file = write("<DataCompositionSchema><settingsVariant><dcsset:name>Основной</dcsset:name>"
      + "</settingsVariant></DataCompositionSchema>");

    assertThat(SettingsVariantLocator.nameLine(file, 0)).isEqualTo(1);
  }

  @Test
  void fallsBackToVariantLineWithoutNameNode() throws IOException {
    var file = write("""
      <DataCompositionSchema>
        <settingsVariant>
          <dcsset:settings/>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.nameLine(file, 0)).isEqualTo(2);
  }

  @Test
  void returnsNullForUnknownIndex() throws IOException {
    var file = write("""
      <DataCompositionSchema>
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.nameLine(file, 3)).isNull();
  }

  @Test
  void returnsNullWhenFileIsMissing() {
    assertThat(SettingsVariantLocator.nameLine(dir.resolve("нет-файла.xml"), 0)).isNull();
    assertThat(SettingsVariantLocator.nameLine(null, 0)).isNull();
  }

  @Test
  void returnsNullForNegativeIndex() throws IOException {
    var file = write("<DataCompositionSchema/>");

    assertThat(SettingsVariantLocator.nameLine(file, -1)).isNull();
  }

  private Path write(String content) throws IOException {
    var file = dir.resolve("Template.xml");
    Files.writeString(file, content);
    return file;
  }
}
