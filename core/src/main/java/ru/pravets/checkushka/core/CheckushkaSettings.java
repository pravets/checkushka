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
package ru.pravets.checkushka.core;

import java.nio.file.Path;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.source.SourceFormat;

/**
 * Настройки запуска анализа.
 *
 * <p>Поля со значением {@code null} берутся из конфигурационного файла проекта.
 *
 * @param configPath путь к {@code checkushka.json} или {@code null} для поиска в корне проекта
 * @param format     формат выгрузки или {@code null} для автоопределения
 * @param checks     настройки проверок или {@code null}, чтобы использовать файл конфигурации
 */
public record CheckushkaSettings(Path configPath, SourceFormat format, CheckSettings checks) {

  /**
   * Настройки по умолчанию: всё берётся из проекта.
   *
   * @return настройки
   */
  public static CheckushkaSettings defaults() {
    return new CheckushkaSettings(null, null, null);
  }

  /**
   * Настройки с явным путём к конфигурационному файлу.
   *
   * @param configPath путь к файлу
   * @return настройки
   */
  public static CheckushkaSettings withConfig(Path configPath) {
    return new CheckushkaSettings(configPath, null, null);
  }
}
