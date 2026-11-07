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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceOrderReaderTest {

  private static final String DESIGNER = """
    <?xml version="1.0" encoding="UTF-8"?>
    <MetaDataObject xmlns="http://v8.1c.ru/8.3/MDClasses" version="2.17">
      <Configuration uuid="00000000-0000-0000-0000-000000000001">
        <Properties>
          <Name>Мини</Name>
        </Properties>
        <ChildObjects>
          <CommonModule>МодульБ</CommonModule>
          <CommonModule>МодульА</CommonModule>
          <Catalog>Товары</Catalog>
        </ChildObjects>
      </Configuration>
    </MetaDataObject>
    """;

  private static final String EDT = """
    <?xml version="1.0" encoding="UTF-8"?>
    <mdclass:Configuration xmlns:mdclass="http://g5.1c.ru/v8/dt/metadata/mdclass" uuid="00000000-0000-0000-0000-000000000001">
      <name>Мини</name>
      <commonModules>CommonModule.МодульБ</commonModules>
      <commonModules>CommonModule.МодульА</commonModules>
      <catalogs>Catalog.Товары</catalogs>
    </mdclass:Configuration>
    """;

  @TempDir
  private Path tempDir;

  @Test
  void readsDesignerChildObjectsInDocumentOrder() throws IOException {
    var file = write("Configuration.xml", DESIGNER);

    var order = SourceOrderReader.read(file, SourceFormat.DESIGNER);

    assertThat(order.entries()).hasSize(3);
    assertThat(order.entries()).extracting(SourceOrder.Entry::type)
      .containsExactly("CommonModule", "CommonModule", "Catalog");
    assertThat(order.entries()).extracting(SourceOrder.Entry::name)
      .containsExactly("МодульБ", "МодульА", "Товары");
    assertThat(order.entries()).allSatisfy(entry -> assertThat(entry.line()).isPositive());
  }

  @Test
  void readsEdtChildObjectsInDocumentOrder() throws IOException {
    var file = write("Configuration.mdo", EDT);

    var order = SourceOrderReader.read(file, SourceFormat.EDT);

    assertThat(order.entries()).hasSize(3);
    assertThat(order.entries()).extracting(SourceOrder.Entry::type)
      .containsExactly("CommonModule", "CommonModule", "Catalog");
    assertThat(order.entries()).extracting(SourceOrder.Entry::name)
      .containsExactly("МодульБ", "МодульА", "Товары");
  }

  @Test
  void returnsEmptyOrderForMissingFile() {
    var order = SourceOrderReader.read(tempDir.resolve("нет-такого.xml"), SourceFormat.DESIGNER);

    assertThat(order.entries()).isEmpty();
  }

  @Test
  void returnsEmptyOrderForBrokenXml() throws IOException {
    var file = write("Configuration.xml", "<MetaDataObject><Configuration>");

    var order = SourceOrderReader.read(file, SourceFormat.DESIGNER);

    assertThat(order.entries()).isEmpty();
  }

  private Path write(String name, String content) throws IOException {
    var file = tempDir.resolve(name);
    Files.writeString(file, content);
    return file;
  }
}
