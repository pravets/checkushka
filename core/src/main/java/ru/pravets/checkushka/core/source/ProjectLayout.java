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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Соответствие объектов метаданных и форм их файлам в проекте.
 *
 * <p>Индекс строится один раз обходом дерева. Раскладка различается по форматам:
 * <ul>
 *   <li>объект Designer и EDT — файл с именем, равным имени каталога
 *       ({@code Catalogs/Товары/Товары.xml}, {@code CommonModules/Общий/Общий.mdo});</li>
 *   <li>объект формы Designer — {@code <Объект>/Forms/<Форма>.xml};</li>
 *   <li>форма EDT — {@code <Объект>/Forms/<Форма>/Form.form}.</li>
 * </ul>
 *
 * <p>Имена форм уникальны только внутри объекта: у разных документов штатно встречаются формы
 * с одинаковым именем ({@code ФормаДокумента}). Поэтому формы индексируются по паре
 * «владелец + имя», а поиск без владельца отдаёт файл только при уникальном имени.
 */
public final class ProjectLayout {

  private static final int MAX_DEPTH = 12;
  private static final List<String> METADATA_EXTENSIONS = List.of(".xml", ".mdo");
  private static final char KEY_SEPARATOR = '\u0001';

  private final Map<String, Path> objectFiles;
  private final Map<String, Path> formsByOwner;
  private final Map<String, Path> formsByName;
  private final Map<String, AtomicInteger> formNameCounts;

  private ProjectLayout(Map<String, Path> objectFiles, Map<String, Path> formsByOwner,
                        Map<String, Path> formsByName, Map<String, AtomicInteger> formNameCounts) {
    this.objectFiles = Map.copyOf(objectFiles);
    this.formsByOwner = Map.copyOf(formsByOwner);
    this.formsByName = Map.copyOf(formsByName);
    this.formNameCounts = Map.copyOf(formNameCounts);
  }

  /**
   * Строит индекс файлов проекта.
   *
   * @param root корень проекта
   * @return индекс
   */
  public static ProjectLayout scan(Path root) {
    var objects = new HashMap<String, Path>();
    var byOwner = new HashMap<String, Path>();
    var byName = new HashMap<String, Path>();
    var nameCounts = new HashMap<String, AtomicInteger>();
    try (var stream = Files.walk(root, MAX_DEPTH)) {
      var projectRoot = root.toAbsolutePath().normalize();
      stream.filter(Files::isRegularFile)
        .forEach(path -> index(projectRoot, path, objects, byOwner, byName, nameCounts));
    } catch (IOException e) {
      throw new ProjectException("Не удалось прочитать структуру проекта: " + root, e);
    }
    return new ProjectLayout(objects, byOwner, byName, nameCounts);
  }

  /**
   * Возвращает файл объекта метаданных.
   *
   * @param mdoRef ссылка вида {@code Catalog.Товары}
   * @return файл или пустой результат, если объект не найден
   */
  public Optional<Path> fileOf(String mdoRef) {
    return Optional.ofNullable(mdoRef).map(ProjectLayout::objectName).map(objectFiles::get);
  }

  /**
   * Возвращает файл формы.
   *
   * @param ownerMdoRef ссылка на объект-владелец формы; {@code null} — владелец неизвестен
   * @param formName    имя формы
   * @return файл формы или пустой результат
   */
  public Optional<Path> formFile(String ownerMdoRef, String formName) {
    if (formName == null || formName.isBlank()) {
      return Optional.empty();
    }
    if (ownerMdoRef != null && !ownerMdoRef.isBlank()) {
      var ownerName = objectName(ownerMdoRef);
      var indexed = formsByOwner.get(ownerName + KEY_SEPARATOR + formName);
      if (indexed != null) {
        return Optional.of(indexed);
      }
      return formFileNextToOwner(ownerName, formName);
    }

    // Без владельца отдаём файл только при уникальном имени формы: иначе легко вернуть
    // чужую форму и напечатать одно замечание вместо нескольких разных
    var count = formNameCounts.get(formName);
    if (count == null || count.get() != 1) {
      return Optional.empty();
    }
    return Optional.ofNullable(formsByName.get(formName));
  }

  private Optional<Path> formFileNextToOwner(String ownerName, String formName) {
    var ownerFile = objectFiles.get(ownerName);
    if (ownerFile == null || ownerFile.getParent() == null) {
      return Optional.empty();
    }
    var formsDir = ownerFile.getParent().resolve("Forms");
    for (var extension : METADATA_EXTENSIONS) {
      var candidate = formsDir.resolve(formName + extension);
      if (Files.isRegularFile(candidate)) {
        return Optional.of(candidate);
      }
    }
    var edtCandidate = formsDir.resolve(formName).resolve("Form.form");
    return Files.isRegularFile(edtCandidate) ? Optional.of(edtCandidate) : Optional.empty();
  }

  private static void index(Path root, Path path, Map<String, Path> objects,
                            Map<String, Path> formsByOwner, Map<String, Path> formsByName,
                            Map<String, AtomicInteger> formNameCounts) {
    var fileName = path.getFileName().toString();
    var dot = fileName.lastIndexOf('.');
    if (dot <= 0) {
      return;
    }
    var base = fileName.substring(0, dot);
    var extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    var parent = path.getParent();
    var parentName = parent == null || parent.getFileName() == null
      ? "" : parent.getFileName().toString();

    // EDT: <Объект>/Forms/<Форма>/Form.form
    if ("form".equalsIgnoreCase(base) && "form".equals(extension)) {
      registerForm(parent == null ? null : parent.getParent(), parentName, path,
        formsByOwner, formsByName, formNameCounts);
      return;
    }
    // Designer: <Объект>/Forms/<Форма>.xml
    if ("forms".equalsIgnoreCase(parentName) && "xml".equals(extension)) {
      registerForm(parent, base, path, formsByOwner, formsByName, formNameCounts);
      return;
    }
    if (base.equalsIgnoreCase(parentName)) {
      objects.putIfAbsent(base, path);
      return;
    }
    // Часть объектов Designer выгружается одним файлом в каталоге группы
    // (например, Roles/Администратор.xml): файл лежит на втором уровне от корня.
    var group = parent == null ? null : parent.getParent();
    if ("xml".equals(extension) && group != null && group.equals(root)) {
      objects.putIfAbsent(base, path);
    }
  }

  private static void registerForm(Path formsDir, String formName, Path path,
                                   Map<String, Path> formsByOwner, Map<String, Path> formsByName,
                                   Map<String, AtomicInteger> formNameCounts) {
    var ownerDir = formsDir == null ? null : formsDir.getParent();
    var ownerName = ownerDir == null || ownerDir.getFileName() == null
      ? "" : ownerDir.getFileName().toString();
    formsByOwner.putIfAbsent(ownerName + KEY_SEPARATOR + formName, path);
    formsByName.putIfAbsent(formName, path);
    formNameCounts.computeIfAbsent(formName, name -> new AtomicInteger()).incrementAndGet();
  }

  private static String objectName(String mdoRef) {
    var dot = mdoRef.indexOf('.');
    return dot >= 0 && dot < mdoRef.length() - 1 ? mdoRef.substring(dot + 1) : mdoRef;
  }
}
