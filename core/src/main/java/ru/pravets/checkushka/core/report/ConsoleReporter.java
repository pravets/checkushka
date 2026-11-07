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

import java.util.EnumMap;
import java.util.Map;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.Severity;

/**
 * Текстовый отчёт для терминала.
 */
public final class ConsoleReporter implements Reporter {

  @Override
  public void report(AnalysisResult result, ReportContext context) {
    var out = context.out();
    var root = context.projectRoot();

    for (var issue : result.issues()) {
      out.printf("%s%s: %s [%s] %s%n",
        relative(root, issue.location().file()),
        issue.location().line() == null ? "" : ":" + issue.location().line(),
        issue.severity(),
        issue.key(),
        issue.message());
    }

    if (result.isEmpty()) {
      out.println("Замечаний не найдено");
    } else {
      out.println();
      out.printf("Замечаний: %d%s%n", result.issues().size(), bySeverity(result));
    }
    out.flush();
  }

  private static String relative(java.nio.file.Path root, java.nio.file.Path file) {
    try {
      var relative = root.relativize(file.toAbsolutePath()).toString();
      return relative.isEmpty() ? file.getFileName().toString() : relative;
    } catch (IllegalArgumentException e) {
      return file.toString();
    }
  }

  private static String bySeverity(AnalysisResult result) {
    Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
    result.issues().forEach(issue -> counts.merge(issue.severity(), 1, Integer::sum));
    if (counts.isEmpty()) {
      return "";
    }
    var builder = new StringBuilder(" (");
    var first = true;
    for (var entry : counts.entrySet()) {
      if (!first) {
        builder.append(", ");
      }
      builder.append(entry.getKey()).append(": ").append(entry.getValue());
      first = false;
    }
    return builder.append(')').toString();
  }
}
