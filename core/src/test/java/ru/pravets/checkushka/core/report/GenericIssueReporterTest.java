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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

class GenericIssueReporterTest {

  private static final Path ROOT = Path.of("C:/project");

  @Test
  void writesSonarIssueFormat() throws Exception {
    var issue = new CheckResult("MetadataOrder", "Нарушен порядок", Severity.MINOR,
      SourceLocation.of(ROOT.resolve("src/Configuration/Configuration.mdo"), 34, "CommonModule.Locked"), 1);

    var document = render(new AnalysisResult(List.of(issue),
      new AnalysisResult.Statistics(1, 1, Duration.ZERO)));

    var issues = document.getElementsByTagName("issue");
    assertThat(issues.getLength()).isEqualTo(1);
    var element = (Element) issues.item(0);
    assertThat(text(element, "engineId")).isEqualTo("checkushka");
    assertThat(text(element, "ruleId")).isEqualTo("MetadataOrder");
    assertThat(text(element, "type")).isEqualTo("CODE_SMELL");
    assertThat(text(element, "severity")).isEqualTo("MINOR");
    assertThat(text(element, "effortMinutes")).isEqualTo("1");

    var primary = (Element) element.getElementsByTagName("primaryLocation").item(0);
    assertThat(text(primary, "message")).isEqualTo("Нарушен порядок");
    assertThat(text(primary, "filePath")).isEqualTo("src/Configuration/Configuration.mdo");
    assertThat(text(primary, "startLine")).isEqualTo("34");
  }

  private static Document render(AnalysisResult result) throws Exception {
    var buffer = new ByteArrayOutputStream();
    new GenericIssueReporter().report(result,
      new ReportContext(ROOT, new PrintStream(buffer, true, StandardCharsets.UTF_8)));
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new ByteArrayInputStream(buffer.toByteArray()));
  }

  private static String text(Element parent, String tag) {
    return parent.getElementsByTagName(tag).item(0).getTextContent();
  }
}
