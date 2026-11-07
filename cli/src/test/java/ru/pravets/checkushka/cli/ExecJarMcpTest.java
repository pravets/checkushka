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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Проверка готовой поставки одним файлом: fat-jar запускается как отдельный процесс,
 * MCP-сервер из него проходит handshake и возвращает кириллицу в UTF-8.
 */
class ExecJarMcpTest {

  private static final String INITIALIZE = """
    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
    "protocolVersion":"2025-06-18","capabilities":{},\
    "clientInfo":{"name":"checkushka-execjar-test","version":"1.0"}}}""";

  private static final String INITIALIZED = """
    {"jsonrpc":"2.0","method":"notifications/initialized"}""";

  private static final String TOOLS_LIST = """
    {"jsonrpc":"2.0","id":2,"method":"tools/list"}""";

  private static final String DESCRIBE_CHECK = """
    {"jsonrpc":"2.0","id":3,"method":"tools/call",\
    "params":{"name":"describe_check","arguments":{"key":"MetadataOrder"}}}""";

  @Test
  void mcpServerStartsFromExecJar() throws Exception {
    var jar = execJar();
    var java = Path.of(System.getProperty("java.home"), "bin",
      System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();

    var process = new ProcessBuilder(java, "-jar", jar.toString(), "mcp")
      .redirectError(ProcessBuilder.Redirect.DISCARD)
      .start();

    var executor = Executors.newSingleThreadExecutor();
    try (var writer = new BufferedWriter(
      new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
         var reader = new BufferedReader(
           new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

      write(writer, INITIALIZE);
      assertThat(readLine(executor, reader)).contains("\"result\"").contains("checkushka");

      write(writer, INITIALIZED);
      write(writer, TOOLS_LIST);
      assertThat(readLine(executor, reader))
        .contains("register_workspace_folder")
        .contains("analyze_configuration");

      write(writer, DESCRIBE_CHECK);
      assertThat(readLine(executor, reader))
        .contains("Порядок объектов метаданных")
        .contains("groupByPrefix");
    } finally {
      executor.shutdownNow();
      process.destroyForcibly();
      process.waitFor(5, TimeUnit.SECONDS);
    }
  }

  private static void write(BufferedWriter writer, String message) throws Exception {
    writer.write(message);
    writer.newLine();
    writer.flush();
  }

  private static String readLine(java.util.concurrent.ExecutorService executor, BufferedReader reader)
    throws Exception {
    var line = executor.submit(reader::readLine).get(30, TimeUnit.SECONDS);
    assertThat(line).as("ответ MCP-сервера из fat-jar").isNotNull();
    return line;
  }

  private static Path execJar() throws Exception {
    var libs = Path.of(System.getProperty("user.dir")).resolve("build").resolve("libs");
    try (var stream = Files.list(libs)) {
      return stream
        .filter(path -> path.getFileName().toString().endsWith("-exec.jar"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
          "Не найден -exec.jar в " + libs + "; запустите :checkushka-cli:shadowJar"));
    }
  }
}
