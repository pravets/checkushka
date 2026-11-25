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
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Поиск вариантов отчёта в файле макета схемы компоновки данных.
 *
 * <p>Модель mdclasses не хранит номера строк, поэтому позиция читается из файла потоковым
 * разбором. Учитываются только варианты верхнего уровня — те, чей родительский элемент
 * {@code DataCompositionSchema}, как их и читает mdclasses: варианты вложенных схем
 * ({@code nestedSchema/schema}) относятся к вложенному отчёту и в результат не попадают.
 *
 * <p>Для каждого варианта возвращаются имя и строка элемента {@code name} (или строка самого
 * {@code settingsVariant}, если узла {@code name} нет). Имя нужно, чтобы замечание получало
 * строку по совпадению имени, а не по порядковому номеру: порядок вариантов в модели и в файле
 * — неявный контракт, который может разойтись. Разбор нужен только для расположения замечания:
 * решение о нарушении принимается по модели.
 */
final class SettingsVariantLocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsVariantLocator.class);

  private static final String ROOT = "DataCompositionSchema";
  private static final String VARIANT = "settingsVariant";
  private static final String NAME = "name";

  private SettingsVariantLocator() {
  }

  /**
   * Вариант отчёта из файла макета.
   *
   * @param name имя варианта или {@code null}, если в файле нет узла {@code name}
   * @param line строка элемента {@code name}, а при его отсутствии — строка {@code settingsVariant}
   */
  record Occurrence(String name, int line) {
  }

  /**
   * Читает варианты отчёта из файла макета.
   *
   * @param file файл макета; {@code null} допустим
   * @return варианты в порядке файла; пустой список, если файл не прочитан или не разобран
   */
  static List<Occurrence> variants(Path file) {
    if (file == null) {
      return List.of();
    }

    var factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

    try (var stream = Files.newInputStream(file)) {
      var reader = factory.createXMLStreamReader(stream);
      try {
        return variants(reader);
      } finally {
        reader.close();
      }
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOGGER.debug("Не удалось прочитать варианты отчёта из {}", file, e);
      return List.of();
    }
  }

  private static List<Occurrence> variants(XMLStreamReader reader) throws XMLStreamException {
    var result = new ArrayList<Occurrence>();
    var path = new ArrayList<String>();
    var variantDepth = 0;
    var variantLine = 0;
    var nameLine = -1;
    var inName = false;
    var name = new StringBuilder();

    while (reader.hasNext()) {
      var event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        path.add(reader.getLocalName());
        if (variantDepth == 0 && isTopLevelVariant(path)) {
          variantDepth = path.size();
          variantLine = reader.getLocation().getLineNumber();
          nameLine = -1;
        } else if (variantDepth > 0 && nameLine < 0 && !inName
          && path.size() == variantDepth + 1 && NAME.equals(reader.getLocalName())) {
          nameLine = reader.getLocation().getLineNumber();
          inName = true;
          name.setLength(0);
        }
      } else if (event == XMLStreamConstants.CHARACTERS && inName
        && path.size() == variantDepth + 1) {
        name.append(reader.getText());
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        if (inName && path.size() == variantDepth + 1) {
          inName = false;
        }
        if (variantDepth > 0 && path.size() == variantDepth) {
          result.add(new Occurrence(nameLine < 0 ? null : name.toString().trim(),
            nameLine < 0 ? variantLine : nameLine));
          variantDepth = 0;
        }
        path.remove(path.size() - 1);
      }
    }
    return result;
  }

  private static boolean isTopLevelVariant(List<String> path) {
    return path.size() >= 2
      && VARIANT.equals(path.get(path.size() - 1))
      && ROOT.equals(path.get(path.size() - 2));
  }
}
