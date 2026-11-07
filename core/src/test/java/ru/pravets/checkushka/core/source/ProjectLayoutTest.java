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

class ProjectLayoutTest {

  @TempDir
  private Path root;

  @Test
  void findsDesignerObjectFile() throws IOException {
    var file = touch(root.resolve("Catalogs/Товары/Товары.xml"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.fileOf("Catalog.Товары")).contains(file);
  }

  @Test
  void findsEdtObjectFile() throws IOException {
    var file = touch(root.resolve("src/CommonModules/Общий/Общий.mdo"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.fileOf("CommonModule.Общий")).contains(file);
  }

  @Test
  void findsDesignerFormFile() throws IOException {
    var file = touch(root.resolve("Catalogs/Товары/Forms/ФормаЭлемента.xml"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.formFile(null, "ФормаЭлемента")).contains(file);
  }

  @Test
  void findsEdtFormFile() throws IOException {
    var file = touch(root.resolve("src/Catalogs/Товары/Forms/ItemForm/Form.form"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.formFile(null, "ItemForm")).contains(file);
  }

  @Test
  void findsDesignerSingleFileObject() throws IOException {
    var file = touch(root.resolve("Roles/Администратор.xml"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.fileOf("Role.Администратор")).contains(file);
  }

  @Test
  void findsFormByOwnerWhenNamesRepeat() throws IOException {
    var first = touch(root.resolve("Documents/НачислениеЗарплаты/Forms/ФормаДокумента.xml"));
    var second = touch(root.resolve("Documents/Операция/Forms/ФормаДокумента.xml"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.formFile("Document.НачислениеЗарплаты", "ФормаДокумента")).contains(first);
    assertThat(layout.formFile("Document.Операция", "ФормаДокумента")).contains(second);
  }

  @Test
  void doesNotGuessFormByAmbiguousName() throws IOException {
    touch(root.resolve("Documents/НачислениеЗарплаты/Forms/ФормаДокумента.xml"));
    touch(root.resolve("Documents/Операция/Forms/ФормаДокумента.xml"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.formFile(null, "ФормаДокумента")).isEmpty();
  }

  @Test
  void findsEdtFormByOwner() throws IOException {
    var first = touch(root.resolve("src/Catalogs/Товары/Forms/ItemForm/Form.form"));
    var second = touch(root.resolve("src/Catalogs/Услуги/Forms/ItemForm/Form.form"));

    var layout = ProjectLayout.scan(root);

    assertThat(layout.formFile("Catalog.Товары", "ItemForm")).contains(first);
    assertThat(layout.formFile("Catalog.Услуги", "ItemForm")).contains(second);
  }

  @Test
  void returnsEmptyForUnknownObject() {
    var layout = ProjectLayout.scan(root);

    assertThat(layout.fileOf("Catalog.Нет")).isEmpty();
    assertThat(layout.formFile(null, "Нет")).isEmpty();
  }

  private static Path touch(Path path) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, "<xml/>");
    return path;
  }
}
