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

/**
 * Фасад библиотеки checkushka.
 *
 * <p>Точка входа для встраивания проверок в другие инструменты: CLI, MCP-сервер, плагин SonarQube,
 * LSP-адаптер. На этапе M0 содержит только сведения о версии; построение анализа появится в M1.
 */
public final class Checkushka {

  private Checkushka() {
  }

  /**
   * Возвращает версию библиотеки.
   *
   * @return версия из манифеста, либо {@code dev}, если манифест недоступен
   */
  public static String version() {
    return Version.current();
  }
}
