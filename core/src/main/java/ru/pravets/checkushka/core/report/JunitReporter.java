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

/**
 * Отчёт в формате JUnit XML: каждая проверка — класс теста, каждое замечание — падение.
 */
public final class JunitReporter implements Reporter {

  @Override
  public void report(AnalysisResult result, ReportContext context) {
    var out = context.out();
    try {
      var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(out, "UTF-8");
      writer.writeStartDocument("UTF-8", "1.0");
      writer.writeStartElement("testsuite");
      writer.writeAttribute("name", "checkushka");
      writer.writeAttribute("tests", String.valueOf(result.issues().size()));
      writer.writeAttribute("failures", String.valueOf(result.issues().size()));
      writer.writeAttribute("errors", "0");
      writer.writeAttribute("time", seconds(result));

      for (var issue : result.issues()) {
        writeCase(writer, issue);
      }

      writer.writeEndElement();
      writer.writeEndDocument();
      writer.close();
      out.println();
      out.flush();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Не удалось сформировать JUnit-отчёт", e);
    }
  }

  private static void writeCase(XMLStreamWriter writer, CheckResult issue) throws XMLStreamException {
    writer.writeStartElement("testcase");
    writer.writeAttribute("classname", "checkushka." + issue.key());
    writer.writeAttribute("name", caseName(issue));
    writer.writeAttribute("time", "0");

    writer.writeStartElement("failure");
    writer.writeAttribute("message", issue.message());
    writer.writeAttribute("type", issue.severity().name());
    writer.writeCharacters(issue.location().file() + " " + issue.message());
    writer.writeEndElement();

    writer.writeEndElement();
  }

  private static String caseName(CheckResult issue) {
    var mdoRef = issue.location().mdoRef();
    return mdoRef != null ? mdoRef : issue.location().file().toString();
  }

  private static String seconds(AnalysisResult result) {
    var millis = result.statistics().duration().toMillis();
    return String.valueOf(millis / 1000.0);
  }
}
