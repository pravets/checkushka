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
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import ru.pravets.checkushka.core.Checkushka;

/**
 * MCP-сервер checkushka.
 *
 * <p>Транспорты: {@code stdio} (по умолчанию) и {@code streamable} — HTTP-эндпоинт
 * на встроенном Jetty.
 */
public final class CheckushkaMcp {

  /** Имя сервера в протоколе MCP. */
  public static final String SERVER_NAME = "checkushka";

  /** Порт HTTP-транспорта по умолчанию. */
  public static final int DEFAULT_PORT = 8080;

  private static final String INSTRUCTIONS = "Проверка метаданных и форм 1С. Зарегистрируйте рабочую папку "
    + "инструментом register_workspace_folder, затем вызовите analyze_configuration.";

  private CheckushkaMcp() {
  }

  /**
   * Точка входа.
   *
   * @param args аргументы: {@code --protocol stdio|streamable}, {@code --server.port}
   * @throws Exception если транспорт не удалось запустить
   */
  public static void main(String[] args) throws Exception {
    var code = run(
      option(args, "--protocol", "stdio"),
      Integer.parseInt(option(args, "--server.port", String.valueOf(DEFAULT_PORT))),
      option(args, "--mcp-path", HttpTransportServer.DEFAULT_PATH));
    System.exit(code);
  }

  /**
   * Запускает сервер и блокирует вызывающий поток до остановки процесса.
   *
   * @param protocol {@code stdio} или {@code streamable}
   * @param port     порт HTTP-транспорта
   * @param path     путь MCP-эндпоинта
   * @return код возврата, если запуск не удался; при успешном запуске метод не возвращает управление
   * @throws Exception если транспорт не удалось запустить
   */
  public static int run(String protocol, int port, String path) throws Exception {
    var tools = new ToolFactory(new WorkspaceRegistry()).tools();

    switch (protocol) {
      case "stdio" -> {
        var transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper());
        var server = McpServer.sync(transport)
          .serverInfo(SERVER_NAME, Checkushka.version())
          .instructions(INSTRUCTIONS)
          .tools(tools)
          .build();
        Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));
        System.err.println(SERVER_NAME + " " + Checkushka.version() + " слушает stdio");
        Thread.currentThread().join();
      }
      case "streamable" -> {
        var jetty = HttpTransportServer.start("0.0.0.0", port, path);
        System.err.println(SERVER_NAME + " " + Checkushka.version()
          + " слушает http://localhost:" + HttpTransportServer.port(jetty) + path);
        jetty.join();
      }
      default -> {
        System.err.println("Неизвестный транспорт: " + protocol + "; поддерживаются stdio и streamable");
        return 2;
      }
    }
    return 0;
  }

  /**
   * Создаёт сервер с инструментами поверх произвольного транспорта.
   *
   * @param transport транспорт протокола
   * @return запущенный сервер
   */
  public static McpSyncServer create(McpServerTransportProvider transport) {
    return McpServer.sync(transport)
      .serverInfo(SERVER_NAME, Checkushka.version())
      .instructions(INSTRUCTIONS)
      .tools(new ToolFactory(new WorkspaceRegistry()).tools())
      .build();
  }

  /**
   * Создаёт сервер поверх Streamable HTTP транспорта.
   *
   * @param transport транспорт протокола
   * @return запущенный сервер
   */
  public static McpSyncServer create(McpStreamableServerTransportProvider transport) {
    return McpServer.sync(transport)
      .serverInfo(SERVER_NAME, Checkushka.version())
      .instructions(INSTRUCTIONS)
      .tools(new ToolFactory(new WorkspaceRegistry()).tools())
      .build();
  }

  private static String option(String[] args, String name, String defaultValue) {
    for (var index = 0; index < args.length - 1; index++) {
      if (name.equals(args[index])) {
        return args[index + 1];
      }
    }
    return defaultValue;
  }
}
