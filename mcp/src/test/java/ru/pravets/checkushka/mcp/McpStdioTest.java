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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Проверка stdio-транспорта: сервер запускается отдельным процессом, клиент говорит JSON-RPC.
 */
class McpStdioTest {

  private static final String INITIALIZE = """
    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
    "protocolVersion":"2025-06-18","capabilities":{},\
    "clientInfo":{"name":"checkushka-test","version":"1.0"}}}""";

  private static final String INITIALIZED = """
    {"jsonrpc":"2.0","method":"notifications/initialized"}""";

  private static final String TOOLS_LIST = """
    {"jsonrpc":"2.0","id":2,"method":"tools/list"}""";

  private static final String CALL_LIST_CHECKS = """
    {"jsonrpc":"2.0","id":3,"method":"tools/call",\
    "params":{"name":"list_checks","arguments":{}}}""";

  @Test
  void respondsToInitializeAndListsTools() throws Exception {
    var java = Path.of(System.getProperty("java.home"), "bin",
      System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();

    var process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
      CheckushkaMcp.class.getName())
      .redirectError(ProcessBuilder.Redirect.DISCARD)
      .start();

    var executor = Executors.newSingleThreadExecutor();
    try (var writer = new BufferedWriter(
      new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
         var reader = new BufferedReader(
           new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

      writer.write(INITIALIZE);
      writer.newLine();
      writer.flush();
      var initialize = readLine(executor, reader);
      assertThat(initialize).contains("\"result\"").contains("checkushka");

      writer.write(INITIALIZED);
      writer.newLine();
      writer.flush();

      writer.write(TOOLS_LIST);
      writer.newLine();
      writer.flush();
      var tools = readLine(executor, reader);
      assertThat(tools)
        .contains("register_workspace_folder")
        .contains("analyze_configuration")
        .contains("describe_check");

      writer.write(CALL_LIST_CHECKS);
      writer.newLine();
      writer.flush();
      var checks = readLine(executor, reader);
      assertThat(checks)
        .contains("\"result\"")
        .contains("MetadataOrder")
        .contains("RoleWithoutRights");
    } finally {
      executor.shutdownNow();
      process.destroyForcibly();
      process.waitFor(5, TimeUnit.SECONDS);
    }
  }

  private static String readLine(java.util.concurrent.ExecutorService executor, BufferedReader reader)
    throws Exception {
    var future = executor.submit(reader::readLine);
    var line = future.get(30, TimeUnit.SECONDS);
    assertThat(line).as("ответ сервера").isNotNull();
    return line;
  }
}
