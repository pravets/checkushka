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
import java.util.Objects;

/**
 * Расположение замечания в исходниках.
 *
 * <p>Строка может отсутствовать: часть правил формулируется на уровне файла или объекта
 * метаданных, и позиция внутри файла для них не определена.
 *
 * @param file   файл исходников
 * @param line   номер строки (с единицы) или {@code null}, если позиция не определена
 * @param mdoRef ссылка на объект метаданных вида {@code Catalog.Товары} или {@code null}
 */
public record SourceLocation(Path file, Integer line, String mdoRef) {

  public SourceLocation {
    Objects.requireNonNull(file, "file");
  }

  /**
   * Создаёт локацию на файл без указания строки.
   *
   * @param file файл исходников
   * @return локация
   */
  public static SourceLocation of(Path file) {
    return new SourceLocation(file, null, null);
  }

  /**
   * Создаёт локацию на строку файла.
   *
   * @param file файл исходников
   * @param line номер строки с единицы
   * @return локация
   */
  public static SourceLocation of(Path file, int line) {
    return new SourceLocation(file, line, null);
  }

  /**
   * Создаёт локацию на строку файла с привязкой к объекту метаданных.
   *
   * @param file   файл исходников
   * @param line   номер строки с единицы или {@code null}
   * @param mdoRef ссылка на объект метаданных
   * @return локация
   */
  public static SourceLocation of(Path file, Integer line, String mdoRef) {
    return new SourceLocation(file, line, mdoRef);
  }

  /**
   * Возвращает локацию с тем же файлом и строкой, но с указанной привязкой к объекту.
   *
   * @param ref ссылка на объект метаданных
   * @return новая локация
   */
  public SourceLocation withMdoRef(String ref) {
    return new SourceLocation(file, line, ref);
  }
}
