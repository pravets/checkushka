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
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsVariantLocatorTest {

  private static final String SCHEMA_NS = "http://v8.1c.ru/8.1/data-composition-system/schema";
  private static final String SETTINGS_NS = "http://v8.1c.ru/8.1/data-composition-system/settings";

  @TempDir
  private Path dir;

  @Test
  void findsNameAndLineOfEachVariant() throws IOException {
    var file = write(root() + """
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

    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple("Детальный", 3), tuple("Основной", 7));
  }

  @Test
  void findsVariantsInSingleLineXml() throws IOException {
    var file = write(root().replace("\n", "")
      + "<settingsVariant><dcsset:name>Основной</dcsset:name></settingsVariant>"
      + "</DataCompositionSchema>");

    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple("Основной", 1));
  }

  @Test
  void fallsBackToVariantLineWithoutNameNode() throws IOException {
    var file = write(root() + """
        <settingsVariant>
          <dcsset:settings/>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple(null, 2));
  }

  @Test
  void returnsEmptyListForMissingFile() {
    assertThat(SettingsVariantLocator.variants(dir.resolve("нет-файла.xml"))).isEmpty();
    assertThat(SettingsVariantLocator.variants(null)).isEmpty();
  }

  @Test
  void returnsEmptyListForBrokenXml() throws IOException {
    var file = write("<DataCompositionSchema><settingsVariant>");

    assertThat(SettingsVariantLocator.variants(file)).isEmpty();
  }

  @Test
  void ignoresVariantsOfNestedSchemas() throws IOException {
    var file = write(root() + """
        <nestedSchema>
          <schema>
            <settingsVariant>
              <dcsset:name>Основной</dcsset:name>
            </settingsVariant>
          </schema>
        </nestedSchema>
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    // вариант верхнего уровня один, вложенный в результат не попадает
    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple("Основной", 10));
  }

  @Test
  void findsPrefixedVariantElement() throws IOException {
    var file = write(root() + """
        <dcsset:settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </dcsset:settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple("Основной", 3));
  }

  @Test
  void ignoresVariantsOutsideDataCompositionSchema() throws IOException {
    var file = write("""
      <Something xmlns:dcsset="http://v8.1c.ru/8.1/data-composition-system/settings">
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
      </Something>
      """);

    assertThat(SettingsVariantLocator.variants(file)).isEmpty();
  }

  @Test
  void keepsFileOrderOfDuplicates() throws IOException {
    var file = write(root() + """
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple("Основной", 3), tuple("Основной", 6));
  }

  @Test
  void respectsDeclaredNamespaces() throws IOException {
    var file = write(root() + """
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
      </DataCompositionSchema>
      """);

    assertThat(SettingsVariantLocator.variants(file))
      .extracting(SettingsVariantLocator.Occurrence::name, SettingsVariantLocator.Occurrence::line)
      .containsExactly(tuple("Основной", 3));
  }

  private static String root() {
    return "<DataCompositionSchema xmlns=\"" + SCHEMA_NS + "\" xmlns:dcsset=\"" + SETTINGS_NS
      + "\">\n";
  }

  private Path write(String content) throws IOException {
    var file = dir.resolve("Template.xml");
    Files.writeString(file, content);
    return file;
  }
}
