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
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Поиск строки варианта отчёта в файле макета схемы компоновки данных.
 *
 * <p>Модель mdclasses не хранит номера строк, поэтому позиция читается из файла потоковым
 * разбором. Учитываются только варианты верхнего уровня — прямые дети
 * {@code DataCompositionSchema}, как их и читает mdclasses: варианты вложенных схем
 * ({@code nestedSchema/schema}) относятся к вложенному отчёту и индекс не сдвигают.
 * Внутри варианта берётся строка элемента {@code dcsset:name}. Разбор нужен только для
 * расположения замечания: решение о нарушении принимается по модели.
 */
final class SettingsVariantLocator {

  private static final String VARIANT = "settingsVariant";
  private static final String NAME = "name";

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

    var factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

    try (var stream = Files.newInputStream(file)) {
      var reader = factory.createXMLStreamReader(stream);
      try {
        return nameLine(reader, index);
      } finally {
        reader.close();
      }
    } catch (IOException | XMLStreamException | RuntimeException e) {
      return null;
    }
  }

  private static Integer nameLine(XMLStreamReader reader, int index)
    throws XMLStreamException {
    var depth = 0;
    var current = -1;

    while (reader.hasNext()) {
      var event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
        if (depth == 2 && VARIANT.equalsIgnoreCase(reader.getLocalName())) {
          current++;
          if (current == index) {
            var variantLine = reader.getLocation().getLineNumber();
            var nameLine = nameLineInside(reader);
            return nameLine == null ? variantLine : nameLine;
          }
        }
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        depth--;
      }
    }
    return null;
  }

  /**
   * Ищет строку {@code name} среди прямых детей текущего варианта.
   *
   * @param reader потоковый читатель, стоящий на {@code settingsVariant}
   * @return строка элемента {@code name} или {@code null}, если его нет
   */
  private static Integer nameLineInside(XMLStreamReader reader) throws XMLStreamException {
    var depth = 0;
    while (reader.hasNext()) {
      var event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
        if (depth == 1 && NAME.equalsIgnoreCase(localName(reader.getLocalName()))) {
          return reader.getLocation().getLineNumber();
        }
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        if (depth == 0) {
          return null;
        }
        depth--;
      }
    }
    return null;
  }

  private static String localName(String name) {
    var colon = name.indexOf(':');
    return colon >= 0 ? name.substring(colon + 1) : name;
  }
}
