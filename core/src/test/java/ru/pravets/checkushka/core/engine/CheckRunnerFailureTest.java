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
package ru.pravets.checkushka.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.github._1c_syntax.bsl.mdo.Catalog;
import com.github._1c_syntax.bsl.mdclasses.Configuration;
import com.github._1c_syntax.bsl.mdclasses.Solution;
import com.github._1c_syntax.bsl.types.MdoReference;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.Project;
import ru.pravets.checkushka.core.source.ProjectLayout;
import ru.pravets.checkushka.core.source.SourceFormat;
import ru.pravets.checkushka.core.source.SourceLocation;
import ru.pravets.checkushka.core.source.SourceOrder;

/**
 * Регрессионный тест: исключение проверки не должно превращаться в штатный пустой результат.
 */
class CheckRunnerFailureTest {

  @TempDir
  private Path tempDir;

  @Test
  void exceptionBecomesFailureInsteadOfEmptyResult() {
    var project = project();
    var runner = new CheckRunner(
      new CheckRegistry(List.of(new ThrowingCheck(), new ReportingCheck())), CheckSettings.defaults());

    var result = runner.run(project);

    assertThat(result.hasFailures()).isTrue();
    assertThat(result.failures()).singleElement().satisfies(failure -> {
      assertThat(failure.checkKey()).isEqualTo("ThrowingCheck");
      assertThat(failure.unit()).isEqualTo("Catalog.Товары");
      assertThat(failure.message()).isEqualTo("сломалась");
    });
    // Замечания исправной проверки не теряются, несмотря на сбой соседней.
    assertThat(result.issues()).singleElement()
      .satisfies(issue -> assertThat(issue.key()).isEqualTo("ReportingCheck"));
  }

  @Test
  void cleanRunHasNoFailures() {
    var result = new CheckRunner(
      new CheckRegistry(List.of(new ReportingCheck())), CheckSettings.defaults()).run(project());

    assertThat(result.hasFailures()).isFalse();
    assertThat(result.failures()).isEmpty();
  }

  private Project project() {
    var catalog = Catalog.builder()
      .name("Товары")
      .mdoReference(MdoReference.create("Catalog.Товары"))
      .build();
    var configuration = Configuration.builder().child(catalog).build();
    var solution = Solution.builder().mergedConfiguration(configuration).build();
    var configurationFile = tempDir.resolve("Configuration.xml");
    return new Project(tempDir, configurationFile, SourceFormat.DESIGNER, solution,
      ProjectLayout.scan(tempDir),
      SourceOrder.empty(configurationFile, SourceFormat.DESIGNER));
  }

  @CheckMetadata(key = "ThrowingCheck", name = "Падающая проверка", targets = CheckTarget.MDO)
  private static final class ThrowingCheck implements MetadataCheck {

    @Override
    public List<CheckResult> check(CheckContext context) {
      throw new IllegalStateException("сломалась");
    }
  }

  @CheckMetadata(key = "ReportingCheck", name = "Исправная проверка", targets = CheckTarget.MDO)
  private static final class ReportingCheck implements MetadataCheck {

    @Override
    public List<CheckResult> check(CheckContext context) {
      return List.of(new CheckResult("ReportingCheck", "замечание", Severity.MINOR,
        SourceLocation.of(context.project().configurationFile(), null, "Catalog.Товары"), 1));
    }
  }
}
