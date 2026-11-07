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

import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckType;

/**
 * Отчёт в формате SonarQube Generic Issue Data.
 *
 * <p>Импортируется сканером Sonar без плагина: путь к файлу передаётся параметром
 * {@code sonar.externalIssuesReportPaths}.
 */
public final class GenericIssueReporter implements Reporter {

  private static final String ENGINE_ID = "checkushka";

  @Override
  public void report(AnalysisResult result, ReportContext context) {
    var out = context.out();
    try {
      var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(out, "UTF-8");
      writer.writeStartDocument("UTF-8", "1.0");
      writer.writeStartElement("runs");
      writer.writeStartElement("issues");

      for (var issue : result.issues()) {
        writeIssue(writer, issue, context);
      }

      writer.writeEndElement();
      writer.writeEndElement();
      writer.writeEndDocument();
      writer.close();
      out.println();
      out.flush();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Не удалось сформировать отчёт Generic Issue", e);
    }
  }

  private static void writeIssue(XMLStreamWriter writer, CheckResult issue, ReportContext context)
    throws XMLStreamException {
    writer.writeStartElement("issue");
    element(writer, "engineId", ENGINE_ID);
    element(writer, "ruleId", issue.key());
    element(writer, "type", sonarType(CheckCatalog.typeOf(issue.key())));
    element(writer, "severity", issue.severity().name());
    if (issue.minutesToFix() > 0) {
      element(writer, "effortMinutes", String.valueOf(issue.minutesToFix()));
    }

    writer.writeStartElement("primaryLocation");
    element(writer, "message", issue.message());
    element(writer, "filePath", relativePath(context, issue));
    if (issue.location().line() != null) {
      writer.writeStartElement("textRange");
      element(writer, "startLine", String.valueOf(issue.location().line()));
      writer.writeEndElement();
    }
    writer.writeEndElement();

    writer.writeEndElement();
  }

  private static void element(XMLStreamWriter writer, String name, String value)
    throws XMLStreamException {
    writer.writeStartElement(name);
    writer.writeCharacters(value);
    writer.writeEndElement();
  }

  private static String relativePath(ReportContext context, CheckResult issue) {
    try {
      var path = context.projectRoot().relativize(issue.location().file().toAbsolutePath()).toString();
      if (!path.isEmpty()) {
        return path.replace('\\', '/');
      }
    } catch (IllegalArgumentException e) {
      // Путь вне проекта — оставляем как есть.
    }
    return issue.location().file().toString().replace('\\', '/');
  }

  private static String sonarType(CheckType type) {
    return switch (type) {
      case ERROR -> "BUG";
      case CODE_SMELL -> "CODE_SMELL";
      case SECURITY -> "SECURITY_HOTSPOT";
      case VULNERABILITY -> "VULNERABILITY";
    };
  }
}
