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
package ru.pravets.checkushka.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import ru.pravets.checkushka.core.Checkushka;

/**
 * MCP-сервер checkushka.
 *
 * <p>Транспорт {@code stdio} — основной: клиент сам запускает процесс и общается по стандартным
 * потокам. Streamable HTTP появится отдельным этапом.
 */
public final class CheckushkaMcp {

  /** Имя сервера в протоколе MCP. */
  public static final String SERVER_NAME = "checkushka";

  private static final String STDIO = "stdio";

  private CheckushkaMcp() {
  }

  /**
   * Точка входа.
   *
   * @param args аргументы: {@code --protocol stdio}
   * @throws InterruptedException если поток прерван
   */
  public static void main(String[] args) throws InterruptedException {
    var protocol = protocol(args);
    if (!STDIO.equals(protocol)) {
      System.err.println("Поддерживается только --protocol stdio; " + protocol + " появится позже");
      System.exit(2);
    }

    var transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper());
    var server = create(transport);
    Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));

    System.err.println(SERVER_NAME + " " + Checkushka.version() + " слушает stdio");
    Thread.currentThread().join();
  }

  /**
   * Создаёт сервер с инструментами.
   *
   * @param transport транспорт протокола
   * @return запущенный сервер
   */
  public static McpSyncServer create(McpServerTransportProvider transport) {
    var tools = new ToolFactory(new WorkspaceRegistry()).tools();
    return McpServer.sync(transport)
      .serverInfo(SERVER_NAME, Checkushka.version())
      .instructions("Проверка метаданных и форм 1С. Зарегистрируйте рабочую папку "
        + "инструментом register_workspace_folder, затем вызовите analyze_configuration.")
      .tools(tools)
      .build();
  }

  private static String protocol(String[] args) {
    for (var index = 0; index < args.length - 1; index++) {
      if ("--protocol".equals(args[index])) {
        return args[index + 1];
      }
    }
    return STDIO;
  }
}
