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
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

class SarifReporterTest {

  private static final Path ROOT = Path.of("C:/project");

  @Test
  void writesSarifWithRulesAndResults() throws Exception {
    var buffer = new ByteArrayOutputStream();
    var issue = new CheckResult("MetadataOrder", "Нарушен порядок", Severity.MINOR,
      SourceLocation.of(ROOT.resolve("src/Configuration/Configuration.mdo"), 34, "CommonModule.Locked"), 1);

    new SarifReporter().report(
      new AnalysisResult(List.of(issue), new AnalysisResult.Statistics(3, 1, Duration.ofMillis(5))),
      new ReportContext(ROOT, new PrintStream(buffer, true, StandardCharsets.UTF_8)));

    var node = new ObjectMapper().readTree(buffer.toString(StandardCharsets.UTF_8));
    assertThat(node.path("version").asText()).isEqualTo("2.1.0");

    var run = node.path("runs").get(0);
    assertThat(run.path("tool").path("driver").path("name").asText()).isEqualTo("checkushka");
    assertThat(run.path("tool").path("driver").path("rules").get(0).path("id").asText())
      .isEqualTo("MetadataOrder");

    var result = run.path("results").get(0);
    assertThat(result.path("ruleId").asText()).isEqualTo("MetadataOrder");
    assertThat(result.path("level").asText()).isEqualTo("note");
    assertThat(result.path("message").path("text").asText()).isEqualTo("Нарушен порядок");

    var physical = result.path("locations").get(0).path("physicalLocation");
    assertThat(physical.path("artifactLocation").path("uri").asText())
      .isEqualTo("src/Configuration/Configuration.mdo");
    assertThat(physical.path("region").path("startLine").asInt()).isEqualTo(34);
  }

  @Test
  void mapsSeverityToSarifLevel() throws Exception {
    var buffer = new ByteArrayOutputStream();
    var issue = new CheckResult("RoleWithoutRights", "Нет прав", Severity.CRITICAL,
      SourceLocation.of(ROOT.resolve("Roles/Роль.xml")), 1);

    new SarifReporter().report(
      new AnalysisResult(List.of(issue), new AnalysisResult.Statistics(1, 1, Duration.ZERO)),
      new ReportContext(ROOT, new PrintStream(buffer, true, StandardCharsets.UTF_8)));

    var node = new ObjectMapper().readTree(buffer.toString(StandardCharsets.UTF_8));
    assertThat(node.path("runs").get(0).path("results").get(0).path("level").asText())
      .isEqualTo("error");
  }
}
