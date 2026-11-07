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
import java.io.IOException;
import java.io.UncheckedIOException;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.Severity;

/**
 * Машиночитаемый отчёт. Схема версионируется полем {@code version}: изменение структуры
 * означает новую версию.
 */
public final class JsonReporter implements Reporter {

  /** Версия схемы отчёта. */
  public static final int SCHEMA_VERSION = 1;

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void report(AnalysisResult result, ReportContext context) {
    var root = mapper.createObjectNode();
    root.put("version", SCHEMA_VERSION);
    root.put("project", context.projectRoot().toString());

    var statistics = result.statistics();
    var summary = root.putObject("summary");
    summary.put("issues", result.issues().size());
    summary.put("checksRun", statistics.checksRun());
    summary.put("objectsAnalyzed", statistics.objectsAnalyzed());
    summary.put("durationMs", statistics.duration().toMillis());
    var bySeverity = summary.putObject("bySeverity");
    for (var severity : Severity.values()) {
      var count = result.issues().stream().filter(issue -> issue.severity() == severity).count();
      if (count > 0) {
        bySeverity.put(severity.name(), count);
      }
    }

    var issues = root.putArray("issues");
    for (var issue : result.issues()) {
      var node = issues.addObject();
      node.put("key", issue.key());
      node.put("message", issue.message());
      node.put("severity", issue.severity().name());
      node.put("file", relative(context, issue.location().file()));
      if (issue.location().line() != null) {
        node.put("line", issue.location().line());
      }
      if (issue.location().mdoRef() != null) {
        node.put("mdoRef", issue.location().mdoRef());
      }
      node.put("minutesToFix", issue.minutesToFix());
    }

    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(context.out(), root);
      context.out().println();
      context.out().flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось сформировать JSON-отчёт", e);
    }
  }

  private static String relative(ReportContext context, java.nio.file.Path file) {
    try {
      var relative = context.projectRoot().relativize(file.toAbsolutePath()).toString();
      return relative.isEmpty() ? file.getFileName().toString() : relative;
    } catch (IllegalArgumentException e) {
      return file.toString();
    }
  }
}
