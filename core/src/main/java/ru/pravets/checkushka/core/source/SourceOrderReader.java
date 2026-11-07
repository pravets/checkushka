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
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * Чтение порядка дочерних объектов конфигурации из исходного файла.
 *
 * <p>Поддерживаются оба формата выгрузки:
 * <ul>
 *   <li>Designer: {@code <ChildObjects><CommonModule>Имя</CommonModule>...</ChildObjects>};</li>
 *   <li>EDT: прямые дети корня вида {@code <commonModules>CommonModule.Имя</commonModules>}.</li>
 * </ul>
 */
public final class SourceOrderReader {

  private static final String CHILD_OBJECTS = "ChildObjects";

  private SourceOrderReader() {
  }

  /**
   * Читает порядок объектов.
   *
   * @param file   файл конфигурации
   * @param format формат выгрузки
   * @return порядок объектов; пустой, если разобрать файл не удалось
   */
  public static SourceOrder read(Path file, SourceFormat format) {
    if (file == null || !Files.isRegularFile(file)) {
      return SourceOrder.empty(file, format);
    }
    try {
      return format == SourceFormat.EDT ? readEdt(file) : readDesigner(file);
    } catch (IOException | XMLStreamException e) {
      return SourceOrder.empty(file, format);
    }
  }

  private static SourceOrder readDesigner(Path file) throws IOException, XMLStreamException {
    var entries = new ArrayList<SourceOrder.Entry>();
    var factory = newFactory();
    try (var input = Files.newInputStream(file)) {
      var reader = factory.createXMLStreamReader(input);
      try {
        var depth = 0;
        var childObjectsDepth = -1;
        String currentType = null;
        var currentLine = 0;
        var text = new StringBuilder();

        while (reader.hasNext()) {
          var event = reader.next();
          switch (event) {
            case XMLStreamConstants.START_ELEMENT -> {
              depth++;
              var localName = reader.getLocalName();
              if (childObjectsDepth < 0 && CHILD_OBJECTS.equals(localName)) {
                childObjectsDepth = depth;
              } else if (childObjectsDepth >= 0 && depth == childObjectsDepth + 1) {
                currentType = localName;
                currentLine = reader.getLocation().getLineNumber();
                text.setLength(0);
              }
            }
            case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
              if (currentType != null) {
                text.append(reader.getText());
              }
            }
            case XMLStreamConstants.END_ELEMENT -> {
              var localName = reader.getLocalName();
              if (currentType != null && depth == childObjectsDepth + 1) {
                var name = text.toString().trim();
                if (!name.isEmpty()) {
                  entries.add(new SourceOrder.Entry(currentType, name, currentLine));
                }
                currentType = null;
              } else if (CHILD_OBJECTS.equals(localName) && depth == childObjectsDepth) {
                childObjectsDepth = -1;
              }
              depth--;
            }
            default -> {
              // Остальные события не влияют на порядок объектов.
            }
          }
        }
      } finally {
        reader.close();
      }
    }
    return new SourceOrder(file, SourceFormat.DESIGNER, entries);
  }

  private static SourceOrder readEdt(Path file) throws IOException, XMLStreamException {
    var entries = new ArrayList<SourceOrder.Entry>();
    var factory = newFactory();
    try (var input = Files.newInputStream(file)) {
      var reader = factory.createXMLStreamReader(input);
      try {
        var depth = 0;
        String currentElement = null;
        var currentLine = 0;
        var text = new StringBuilder();

        while (reader.hasNext()) {
          var event = reader.next();
          switch (event) {
            case XMLStreamConstants.START_ELEMENT -> {
              depth++;
              if (depth == 2) {
                currentElement = reader.getLocalName();
                currentLine = reader.getLocation().getLineNumber();
                text.setLength(0);
              }
            }
            case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
              if (depth >= 2 && currentElement != null) {
                text.append(reader.getText());
              }
            }
            case XMLStreamConstants.END_ELEMENT -> {
              if (depth == 2 && currentElement != null) {
                addEdtEntry(entries, text.toString().trim(), currentLine);
                currentElement = null;
              }
              depth--;
            }
            default -> {
              // Остальные события не влияют на порядок объектов.
            }
          }
        }
      } finally {
        reader.close();
      }
    }
    return new SourceOrder(file, SourceFormat.EDT, entries);
  }

  private static void addEdtEntry(List<SourceOrder.Entry> entries, String text, int line) {
    var dot = text.indexOf('.');
    if (dot <= 0 || dot == text.length() - 1) {
      return;
    }
    var type = text.substring(0, dot);
    var name = text.substring(dot + 1);
    if (Character.isUpperCase(type.charAt(0))) {
      entries.add(new SourceOrder.Entry(type, name, line));
    }
  }

  private static XMLInputFactory newFactory() {
    var factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
