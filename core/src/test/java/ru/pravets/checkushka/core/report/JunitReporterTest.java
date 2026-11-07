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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

class JunitReporterTest {

  private static final Path ROOT = Path.of("C:/project");

  @Test
  void writesTestsuiteWithFailures() throws Exception {
    var issue = new CheckResult("FormWithoutTitle", "Нет заголовка", Severity.MINOR,
      SourceLocation.of(ROOT.resolve("Catalogs/Товары/Forms/Форма.xml"), null, "Форма"), 1);

    var bytes = render(new AnalysisResult(List.of(issue),
      new AnalysisResult.Statistics(1, 1, Duration.ofMillis(250))));

    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new ByteArrayInputStream(bytes));
    var suite = document.getDocumentElement();
    assertThat(suite.getTagName()).isEqualTo("testsuite");
    assertThat(suite.getAttribute("name")).isEqualTo("checkushka");
    assertThat(suite.getAttribute("tests")).isEqualTo("1");
    assertThat(suite.getAttribute("failures")).isEqualTo("1");
    assertThat(suite.getAttribute("time")).isEqualTo("0.25");

    var failures = document.getElementsByTagName("failure");
    assertThat(failures.getLength()).isEqualTo(1);
    var failure = (org.w3c.dom.Element) failures.item(0);
    assertThat(failure.getAttribute("message")).isEqualTo("Нет заголовка");
    assertThat(failure.getAttribute("type")).isEqualTo("MINOR");
  }

  @Test
  void writesEmptySuiteForCleanResult() throws Exception {
    var bytes = render(new AnalysisResult(List.of(),
      new AnalysisResult.Statistics(1, 1, Duration.ZERO)));

    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new ByteArrayInputStream(bytes));
    assertThat(document.getDocumentElement().getAttribute("tests")).isEqualTo("0");
  }

  private static byte[] render(AnalysisResult result) {
    var buffer = new ByteArrayOutputStream();
    new JunitReporter().report(result,
      new ReportContext(ROOT, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
    return buffer.toByteArray();
  }
}
