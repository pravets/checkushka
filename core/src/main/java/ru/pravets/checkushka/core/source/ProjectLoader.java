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

import com.github._1c_syntax.bsl.mdclasses.MDClasses;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Загрузка проекта: поиск файла конфигурации, разбор модели через mdclasses,
 * построение индекса файлов и чтение порядка объектов.
 */
public final class ProjectLoader {

  private static final int MAX_SEARCH_DEPTH = 4;

  /**
   * Загружает проект, определяя формат автоматически.
   *
   * @param root каталог проекта или каталог, содержащий проект
   * @return разобранный проект
   * @throws ProjectException если конфигурация не найдена или не разобрана
   */
  public Project load(Path root) {
    return load(root, SourceFormat.AUTO);
  }

  /**
   * Загружает проект.
   *
   * @param root            каталог проекта или каталог, содержащий проект
   * @param requestedFormat формат выгрузки; {@link SourceFormat#AUTO} — определить по файлам
   * @return разобранный проект
   * @throws ProjectException если конфигурация не найдена или не разобрана
   */
  public Project load(Path root, SourceFormat requestedFormat) {
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(requestedFormat, "requestedFormat");

    var path = root.toAbsolutePath().normalize();
    if (!Files.isDirectory(path)) {
      throw new ProjectException("Каталог не найден: " + path);
    }

    var configurationFile = findConfigurationFile(path).orElseThrow(() -> new ProjectException(
      "Не найдена конфигурация 1С (Configuration.xml или Configuration.mdo) в " + path
        + " и подкаталогах глубиной до " + MAX_SEARCH_DEPTH));

    var format = requestedFormat == SourceFormat.AUTO ? detectFormat(configurationFile) : requestedFormat;
    var solution = MDClasses.createSolution(path);
    if (solution.getMergedConfiguration().isEmpty()) {
      throw new ProjectException("Не удалось разобрать конфигурацию: " + configurationFile);
    }

    return new Project(path, configurationFile, format, solution,
      ProjectLayout.scan(path), SourceOrderReader.read(configurationFile, format));
  }

  private Optional<Path> findConfigurationFile(Path root) {
    try (var stream = Files.walk(root, MAX_SEARCH_DEPTH)) {
      return stream
        .filter(Files::isRegularFile)
        .filter(path -> isConfigurationFile(path.getFileName().toString()))
        .min(Comparator.comparingInt(path -> root.relativize(path).getNameCount()));
    } catch (IOException e) {
      throw new ProjectException("Не удалось прочитать каталог проекта: " + root, e);
    }
  }

  private static boolean isConfigurationFile(String fileName) {
    var normalized = fileName.toLowerCase(Locale.ROOT);
    return "configuration.xml".equals(normalized) || "configuration.mdo".equals(normalized);
  }

  private static SourceFormat detectFormat(Path configurationFile) {
    var name = configurationFile.getFileName().toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".mdo") ? SourceFormat.EDT : SourceFormat.DESIGNER;
  }
}
