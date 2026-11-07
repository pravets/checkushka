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

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Проверка Streamable HTTP транспорта: сервер на Jetty, клиент — штатный клиент SDK.
 */
class StreamableHttpTest {

  @Test
  void servesToolsOverStreamableHttp() throws Exception {
    var server = HttpTransportServer.start("127.0.0.1", 0, HttpTransportServer.DEFAULT_PATH);
    try {
      var url = "http://127.0.0.1:" + HttpTransportServer.port(server);
      var transport = HttpClientStreamableHttpTransport.builder(url)
        .endpoint(HttpTransportServer.DEFAULT_PATH)
        .build();

      try (var client = McpClient.sync(transport).build()) {
        client.initialize();

        var tools = client.listTools();
        assertThat(tools.tools()).extracting(McpSchema.Tool::name)
          .contains("analyze_configuration", "register_workspace_folder", "describe_check");

        var result = client.callTool(new McpSchema.CallToolRequest("list_checks", Map.of()));
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content()).isNotEmpty();
      }
    } finally {
      server.stop();
    }
  }
}
