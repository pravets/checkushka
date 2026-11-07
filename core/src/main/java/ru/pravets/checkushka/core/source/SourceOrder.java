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

import java.nio.file.Path;
import java.util.List;

/**
 * Порядок дочерних объектов конфигурации в исходном файле.
 *
 * <p>Порядок читается из {@code Configuration.xml} (элемент {@code ChildObjects}) или из
 * {@code Configuration.mdo} (повторяющиеся элементы вида {@code <commonModules>}) именно в том
 * виде, в котором он записан на диске: модель mdclasses такой порядок не гарантирует.
 *
 * @param file    файл конфигурации
 * @param format  формат выгрузки
 * @param entries дочерние объекты в порядке следования
 */
public record SourceOrder(Path file, SourceFormat format, List<Entry> entries) {

  public SourceOrder {
    entries = List.copyOf(entries);
  }

  /**
   * Пустой порядок, если файл не удалось разобрать.
   *
   * @param file   файл конфигурации
   * @param format формат выгрузки
   * @return порядок без элементов
   */
  public static SourceOrder empty(Path file, SourceFormat format) {
    return new SourceOrder(file, format, List.of());
  }

  /**
   * Один дочерний объект конфигурации.
   *
   * @param type тип метаданных в нотации платформы, например {@code Catalog}
   * @param name имя объекта
   * @param line номер строки в файле конфигурации (с единицы)
   */
  public record Entry(String type, String name, int line) {
  }
}
