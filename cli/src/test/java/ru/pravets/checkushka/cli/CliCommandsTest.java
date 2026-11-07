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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.config.ConfigLoader;
import ru.pravets.checkushka.core.engine.ChecksMode;

class CliCommandsTest {

  @TempDir
  private Path tempDir;

  @Test
  void listChecksPrintsAllChecks() {
    var output = run("list-checks");

    assertThat(output).contains("MetadataOrder", "RoleWithoutRights", "FormWithoutTitle");
  }

  @Test
  void listChecksPrintsJson() throws Exception {
    var output = run("list-checks", "--json");

    var node = new ObjectMapper().readTree(output);
    assertThat(node).hasSize(3);
    assertThat(node.get(0).path("key").asText()).isNotBlank();
    assertThat(node.get(0).path("targets").isArray()).isTrue();
  }

  @Test
  void describeCheckShowsParameters() {
    var output = run("describe-check", "MetadataOrder");

    assertThat(output)
      .contains("MetadataOrder")
      .contains("groupByPrefix")
      .contains("prefixes");
  }

  @Test
  void describeCheckReturnsErrorForUnknownKey() {
    var exitCode = Main.createCommandLine().execute("describe-check", "НетТакой");

    assertThat(exitCode).isEqualTo(2);
  }

  @Test
  void configInitCreatesLoadableFile() throws Exception {
    var file = tempDir.resolve("checkushka.json");

    var exitCode = Main.createCommandLine().execute("config", "init", "-o", file.toString());

    assertThat(exitCode).isZero();
    assertThat(Files.exists(file)).isTrue();

    var config = new ConfigLoader().load(tempDir, file);
    assertThat(config.checks().mode()).isEqualTo(ChecksMode.ON);
    assertThat(config.checks().parametersFor("MetadataOrder").string("mode", "none"))
      .isEqualTo("none");
    assertThat(config.checks().severityFor("MetadataOrder", Severity.MAJOR))
      .isEqualTo(Severity.MAJOR);
  }

  @Test
  void configInitRefusesToOverwriteWithoutForce() throws Exception {
    var file = tempDir.resolve("checkushka.json");
    Files.writeString(file, "{}");

    assertThat(Main.createCommandLine().execute("config", "init", "-o", file.toString()))
      .isEqualTo(2);
    assertThat(Main.createCommandLine().execute("config", "init", "-o", file.toString(), "--force"))
      .isZero();
  }

  @Test
  void checkCommandIsRegistered() {
    var commandLine = Main.createCommandLine();

    assertThat(commandLine.getSubcommands()).containsKey("check");
    assertThat(commandLine.getSubcommands()).containsKey("mcp");
  }

  @Test
  void mcpRejectsUnknownProtocol() {
    var exitCode = Main.createCommandLine().execute("mcp", "--protocol", "foo");

    assertThat(exitCode).isEqualTo(2);
  }

  private static String run(String... args) {
    var buffer = new ByteArrayOutputStream();
    var original = System.out;
    System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
    try {
      var exitCode = Main.createCommandLine().execute(List.of(args).toArray(String[]::new));
      assertThat(exitCode).isZero();
      return buffer.toString(StandardCharsets.UTF_8);
    } finally {
      System.setOut(original);
    }
  }
}
