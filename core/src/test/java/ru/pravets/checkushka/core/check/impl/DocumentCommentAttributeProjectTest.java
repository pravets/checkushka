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
package ru.pravets.checkushka.core.check.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.config.ConfigLoader;
import ru.pravets.checkushka.core.engine.CheckRegistry;
import ru.pravets.checkushka.core.engine.CheckRunner;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.engine.ChecksMode;
import ru.pravets.checkushka.core.engine.IgnoreRule;
import ru.pravets.checkushka.core.source.ProjectLoader;

/**
 * Сквозная проверка на минимальных выгрузках обоих форматов: загрузчик, модель и проверка вместе.
 */
class DocumentCommentAttributeProjectTest {

  @TempDir
  private Path dir;

  @ParameterizedTest
  @ValueSource(strings = {"designer-document", "edt-document"})
  void findsViolationsInDump(String dump) throws URISyntaxException {
    var issues = issues(dump(dump), CheckSettings.defaults());

    assertThat(issues).extracting(issue -> issue.location().mdoRef())
      .containsExactlyInAnyOrder("Document.Счёт", "Document.Акт");
    assertThat(issues).allSatisfy(issue -> {
      assertThat(issue.location().line()).isNull();
      var document = issue.location().mdoRef().substring("Document.".length());
      assertThat(issue.location().file().toAbsolutePath().normalize()
        .endsWith(expectedFile(dump, document))).isTrue();
    });
  }

  @Test
  void ignoreRuleByDocumentRemovesFinding() throws URISyntaxException {
    var settings = new CheckSettings(ChecksMode.ON, Set.of(), Map.of(), Map.of(), Severity.INFO,
      List.of(new IgnoreRule(DocumentCommentAttributeCheck.KEY, "Document.Счёт", "тест")), Set.of());

    assertThat(issues(dump("designer-document"), settings))
      .singleElement().satisfies(issue ->
        assertThat(issue.location().mdoRef()).isEqualTo("Document.Акт"));
  }

  @Test
  void parametersFromCheckushkaJsonDisableMultilineRequirement() throws Exception {
    var configFile = dir.resolve("checkushka.json");
    Files.writeString(configFile, """
      {
        "checks": {
          "parameters": {
            "DocumentCommentAttribute": { "requireMultiline": false }
          }
        }
      }
      """);
    var root = dump("designer-document");
    var settings = new ConfigLoader().load(root, configFile).checks();

    assertThat(issues(root, settings))
      .singleElement().satisfies(issue ->
        assertThat(issue.location().mdoRef()).isEqualTo("Document.Счёт"));
  }

  private static List<CheckResult> issues(Path root, CheckSettings settings) {
    var project = new ProjectLoader().load(root);
    return new CheckRunner(CheckRegistry.builtIn(), settings).run(project).issues().stream()
      .filter(issue -> DocumentCommentAttributeCheck.KEY.equals(issue.key()))
      .toList();
  }

  private static Path dump(String name) throws URISyntaxException {
    return Path.of(Objects.requireNonNull(
      DocumentCommentAttributeProjectTest.class.getResource("/dumps/" + name)).toURI());
  }

  private static Path expectedFile(String dump, String document) {
    return "edt-document".equals(dump)
      ? Path.of("src", "Documents", document, document + ".mdo")
      : Path.of("Documents", document + ".xml");
  }
}
