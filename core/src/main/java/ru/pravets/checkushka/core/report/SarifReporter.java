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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Set;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;

/**
 * Отчёт в формате SARIF 2.1.0: подходит для GitHub/GitLab и импорта в SonarQube.
 */
public final class SarifReporter implements Reporter {

  private static final String SARIF_SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json";
  private static final String SARIF_VERSION = "2.1.0";
  private static final String TOOL_NAME = "checkushka";
  private static final String INFORMATION_URI = "https://github.com/checkushka/checkushka";

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void report(AnalysisResult result, ReportContext context) {
    var root = mapper.createObjectNode();
    root.put("$schema", SARIF_SCHEMA);
    root.put("version", SARIF_VERSION);

    var runs = root.putArray("runs");
    var run = runs.addObject();
    var driver = run.putObject("tool").putObject("driver");
    driver.put("name", TOOL_NAME);
    driver.put("informationUri", INFORMATION_URI);
    driver.put("version", version());
    fillRules(driver.putArray("rules"), result);

    var results = run.putArray("results");
    for (var issue : result.issues()) {
      results.add(toResult(issue, context));
    }

    write(root, context);
  }

  private static void fillRules(ArrayNode rules, AnalysisResult result) {
    Set<String> keys = new LinkedHashSet<>();
    result.issues().forEach(issue -> keys.add(issue.key()));
    for (var key : keys) {
      var rule = rules.addObject();
      rule.put("id", key);
      rule.put("name", CheckCatalog.nameOf(key));
      rule.putObject("shortDescription").put("text", CheckCatalog.nameOf(key));
      rule.putObject("defaultConfiguration").put("level", toLevel(defaultSeverity(key)));
    }
  }

  private static Severity defaultSeverity(String key) {
    return CheckCatalog.metadataOf(key).map(CheckMetadata::severity).orElse(Severity.MINOR);
  }

  private ObjectNode toResult(CheckResult issue, ReportContext context) {
    var node = mapper.createObjectNode();
    node.put("ruleId", issue.key());
    node.put("level", toLevel(issue.severity()));
    node.putObject("message").put("text", issue.message());

    var locations = node.putArray("locations");
    var physical = locations.addObject().putObject("physicalLocation");
    var artifact = physical.putObject("artifactLocation");
    artifact.put("uri", relativeUri(context, issue));
    artifact.put("uriBaseId", "%SRCROOT%");
    if (issue.location().line() != null) {
      physical.putObject("region").put("startLine", issue.location().line());
    }
    return node;
  }

  private static String relativeUri(ReportContext context, CheckResult issue) {
    String path;
    try {
      path = context.projectRoot().relativize(issue.location().file().toAbsolutePath()).toString();
    } catch (IllegalArgumentException e) {
      path = issue.location().file().toString();
    }
    if (path.isEmpty()) {
      path = issue.location().file().getFileName().toString();
    }
    return path.replace('\\', '/');
  }

  private static String toLevel(Severity severity) {
    return switch (severity) {
      case BLOCKER, CRITICAL -> "error";
      case MAJOR -> "warning";
      case MINOR, INFO -> "note";
    };
  }

  private static String version() {
    return ru.pravets.checkushka.core.Checkushka.version();
  }

  private void write(ObjectNode root, ReportContext context) {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(context.out(), root);
      context.out().println();
      context.out().flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось сформировать SARIF-отчёт", e);
    }
  }
}
