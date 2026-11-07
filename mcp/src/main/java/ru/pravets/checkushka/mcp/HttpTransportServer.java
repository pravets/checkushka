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
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * MCP поверх Streamable HTTP на встроенном Jetty.
 *
 * <p>Клиент подключается к {@code http://<host>:<port>/mcp}. Сервер stateless по отношению
 * к файловой системе: проекты регистрируются инструментами и живут в памяти процесса.
 */
public final class HttpTransportServer {

  /** Путь MCP-эндпоинта по умолчанию. */
  public static final String DEFAULT_PATH = "/mcp";

  private HttpTransportServer() {
  }

  /**
   * Поднимает HTTP-сервер с MCP-эндпоинтом.
   *
   * @param host интерфейс прослушивания
   * @param port порт; {@code 0} — выбрать свободный
   * @param path путь эндпоинта
   * @return запущенный сервер; порт можно узнать через {@link #port(Server)}
   * @throws Exception если сервер не запустился
   */
  public static Server start(String host, int port, String path) throws Exception {
    var transport = HttpServletStreamableServerTransportProvider.builder()
      .jsonMapper(McpJsonDefaults.getMapper())
      .mcpEndpoint(path)
      .build();

    CheckushkaMcp.create(transport);

    var server = new Server();
    var connector = new ServerConnector(server);
    connector.setHost(host);
    connector.setPort(port);
    server.addConnector(connector);

    var context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(transport), path);
    server.setHandler(context);
    server.start();
    return server;
  }

  /**
   * Фактический порт запущенного сервера.
   *
   * @param server сервер
   * @return порт
   */
  public static int port(Server server) {
    return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }
}
