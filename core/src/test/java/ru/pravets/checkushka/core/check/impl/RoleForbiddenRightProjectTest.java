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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.CheckParameters;
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
 * Сквозная проверка на минимальной выгрузке конструктора: загрузчик, mdclasses 0.22 и проверка
 * вместе. Отдельно подтверждается, что право уровня конфигурации приходит обычным ObjectRight.
 */
class RoleForbiddenRightProjectTest {

  @Test
  void defaultSettingsReportForbiddenRightsAndSkipExcludedRole() throws URISyntaxException {
    var issues = issues(dump(), CheckSettings.defaults());

    assertThat(issues).hasSize(2);
    assertThat(issues).allSatisfy(issue -> {
      assertThat(issue.location().mdoRef()).isEqualTo("Role.Оператор");
      assertThat(issue.location().line()).isNull();
      assertThat(issue.location().file().getFileName().toString()).isEqualTo("Оператор.xml");
    });
    assertThat(issues).extracting(CheckResult::message)
      .anySatisfy(message -> assertThat(message).contains("Администрирование"))
      .anySatisfy(message -> assertThat(message).contains("Удаление"));
  }

  @Test
  void customRightsReplaceDefaults() throws URISyntaxException {
    var issues = issues(dump(), settings(List.of("READ"), List.of("ПолныеПрава")));

    assertThat(issues).singleElement().satisfies(issue ->
      assertThat(issue.message()).contains("Чтение"));
  }

  @Test
  void ignoreRuleByRoleRemovesFindings() throws URISyntaxException {
    var settings = new CheckSettings(ChecksMode.ON, Set.of(), Map.of(), Map.of(), Severity.INFO,
      List.of(new IgnoreRule(RoleForbiddenRightCheck.KEY, "Role.Оператор", "тест")), Set.of());

    assertThat(issues(dump(), settings)).isEmpty();
  }

  @Test
  void parametersComeFromCheckushkaJson(@TempDir Path projectDir) throws Exception {
    copy(dump(), projectDir);
    var config = projectDir.resolve("checkushka.json");

    Files.writeString(config, """
      {
        "checks": {
          "mode": "ONLY",
          "keys": ["RoleForbiddenRight"],
          "parameters": {
            "RoleForbiddenRight": { "rights": ["READ"], "excludeRoles": ["ПолныеПрава"] }
          }
        }
      }
      """);
    assertThat(issues(projectDir, new ConfigLoader().load(projectDir, null).checks()))
      .singleElement()
      .satisfies(issue -> assertThat(issue.message()).contains("Чтение"));

    Files.writeString(config, """
      {
        "checks": {
          "mode": "ONLY",
          "keys": ["RoleForbiddenRight"],
          "parameters": {
            "RoleForbiddenRight": { "rights": ["READ"], "excludeRoles": ["Оператор"] }
          }
        }
      }
      """);
    assertThat(issues(projectDir, new ConfigLoader().load(projectDir, null).checks())).isEmpty();
  }

  private static List<CheckResult> issues(Path root, CheckSettings settings) {
    var project = new ProjectLoader().load(root);
    return new CheckRunner(CheckRegistry.builtIn(), settings).run(project).issues().stream()
      .filter(issue -> RoleForbiddenRightCheck.KEY.equals(issue.key()))
      .toList();
  }

  private static CheckSettings settings(List<String> rights, List<String> excludeRoles) {
    var parameters = new CheckParameters(Map.of(
      "rights", rights,
      "excludeRoles", excludeRoles));
    return new CheckSettings(ChecksMode.ON, Set.of(),
      Map.of(RoleForbiddenRightCheck.KEY, parameters),
      Map.of(), Severity.INFO, List.of(), Set.of());
  }

  private static Path dump() throws URISyntaxException {
    return Path.of(Objects.requireNonNull(
      RoleForbiddenRightProjectTest.class.getResource("/dumps/designer-roles")).toURI());
  }

  private static void copy(Path source, Path target) throws IOException {
    try (Stream<Path> paths = Files.walk(source)) {
      for (var path : paths.toList()) {
        var destination = target.resolve(source.relativize(path).toString());
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
        } else {
          Files.createDirectories(destination.getParent());
          Files.copy(path, destination);
        }
      }
    }
  }
}
