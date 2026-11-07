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
package ru.pravets.checkushka.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

class ReportersTest {

  @TempDir
  private Path root;

  private AnalysisResult result() {
    var issue = new CheckResult(
      "MetadataOrder",
      "Объект «МодульА» должен располагаться раньше «МодульБ»",
      Severity.MINOR,
      SourceLocation.of(root.resolve("Configuration.xml"), 12, "CommonModule.МодульА"),
      1);
    return new AnalysisResult(List.of(issue),
      new AnalysisResult.Statistics(3, 4, Duration.ofMillis(25)));
  }

  @Test
  void consoleReporterPrintsIssueAndSummary() {
    var buffer = new ByteArrayOutputStream();
    var out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

    new ConsoleReporter().report(result(), new ReportContext(root, out));

    var text = buffer.toString(StandardCharsets.UTF_8);
    assertThat(text)
      .contains("Configuration.xml:12: MINOR [MetadataOrder]")
      .contains("Замечаний: 1")
      .contains("MINOR: 1");
  }

  @Test
  void consoleReporterReportsCleanResult() {
    var buffer = new ByteArrayOutputStream();
    var out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

    new ConsoleReporter().report(
      new AnalysisResult(List.of(), new AnalysisResult.Statistics(1, 1, Duration.ZERO)),
      new ReportContext(root, out));

    assertThat(buffer.toString(StandardCharsets.UTF_8)).contains("Замечаний не найдено");
  }

  @Test
  void jsonReporterWritesSchemaVersionAndIssues() throws Exception {
    var buffer = new ByteArrayOutputStream();
    var out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

    new JsonReporter().report(result(), new ReportContext(root, out));

    var node = new ObjectMapper().readTree(buffer.toString(StandardCharsets.UTF_8));
    assertThat(node.path("version").asInt()).isEqualTo(JsonReporter.SCHEMA_VERSION);
    assertThat(node.path("summary").path("issues").asInt()).isEqualTo(1);
    assertThat(node.path("issues")).hasSize(1);
    assertThat(node.path("issues").get(0).path("key").asText()).isEqualTo("MetadataOrder");
    assertThat(node.path("issues").get(0).path("line").asInt()).isEqualTo(12);
    assertThat(node.path("issues").get(0).path("mdoRef").asText()).isEqualTo("CommonModule.МодульА");
  }
}
