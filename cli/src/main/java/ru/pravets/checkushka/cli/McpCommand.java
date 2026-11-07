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
package ru.pravets.checkushka.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import ru.pravets.checkushka.mcp.CheckushkaMcp;

/**
 * Запуск MCP-сервера из основной утилиты.
 *
 * <p>Команда блокирует процесс: сервер работает до его остановки клиентом или оператором.
 */
@CommandLine.Command(
  name = "mcp",
  description = "Запустить MCP-сервер (stdio или streamable HTTP).")
public final class McpCommand implements Callable<Integer> {

  @CommandLine.Option(
    names = "--protocol",
    paramLabel = "<stdio|streamable>",
    defaultValue = "stdio",
    description = "Транспорт: stdio (по умолчанию) или streamable.")
  private String protocol;

  @CommandLine.Option(
    names = "--server.port",
    paramLabel = "<порт>",
    defaultValue = "8080",
    description = "Порт для streamable HTTP.")
  private int port;

  @CommandLine.Option(
    names = "--mcp-path",
    paramLabel = "<путь>",
    defaultValue = "/mcp",
    description = "Путь MCP-эндпоинта для streamable HTTP.")
  private String path;

  @Override
  public Integer call() throws Exception {
    return CheckushkaMcp.run(protocol, port, path);
  }
}
