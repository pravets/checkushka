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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Поиск строки варианта отчёта в файле макета схемы компоновки данных.
 *
 * <p>Модель mdclasses не хранит номера строк, поэтому позиция ищется по тексту файла: блоки
 * {@code <settingsVariant>} перебираются в порядке документа (он совпадает с порядком вариантов
 * в модели), внутри блока берётся строка элемента {@code <dcsset:name>}. Разбор нужен только
 * для расположения замечания: решение о нарушении принимается по модели.
 */
final class SettingsVariantLocator {

  private static final String VARIANT_NODE = "<settingsVariant";
  private static final String VARIANT_END_NODE = "</settingsVariant";
  private static final String NAME_NODE = "<dcsset:name";

  private SettingsVariantLocator() {
  }

  /**
   * Возвращает номер строки варианта отчёта.
   *
   * @param file  файл макета
   * @param index порядковый номер варианта в макете, с нуля
   * @return номер строки с единицы или {@code null}, если позицию определить не удалось
   */
  static Integer nameLine(Path file, int index) {
    if (file == null || index < 0) {
      return null;
    }
    List<String> lines;
    try {
      lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }

    var current = -1;
    for (var lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
      if (!lines.get(lineNumber).contains(VARIANT_NODE)) {
        continue;
      }
      current++;
      if (current != index) {
        continue;
      }
      var inside = nameLineInside(lines, lineNumber);
      return inside == null ? lineNumber + 1 : inside;
    }
    return null;
  }

  private static Integer nameLineInside(List<String> lines, int variantLine) {
    for (var lineNumber = variantLine; lineNumber < lines.size(); lineNumber++) {
      var line = lines.get(lineNumber);
      if (lineNumber > variantLine && line.contains(VARIANT_NODE)) {
        return null;
      }
      if (line.contains(VARIANT_END_NODE)) {
        return null;
      }
      if (line.contains(NAME_NODE)) {
        return lineNumber + 1;
      }
    }
    return null;
  }
}
