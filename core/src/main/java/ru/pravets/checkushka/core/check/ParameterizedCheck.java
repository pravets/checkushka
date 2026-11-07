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
package ru.pravets.checkushka.core.check;

import java.util.List;

/**
 * Проверка с настраиваемыми параметрами.
 *
 * <p>Описание параметров используется командой {@code describe-check} и MCP-инструментом
 * {@code describe_check}, чтобы клиент мог собрать корректную конфигурацию.
 */
public interface ParameterizedCheck {

  /**
   * Параметры проверки.
   *
   * @return описания параметров
   */
  List<CheckParameter> parameters();
}
