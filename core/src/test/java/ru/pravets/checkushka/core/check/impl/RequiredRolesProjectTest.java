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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckRegistry;
import ru.pravets.checkushka.core.engine.CheckRunner;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.engine.ChecksMode;
import ru.pravets.checkushka.core.source.ProjectLoader;

/**
 * Сквозная проверка на минимальных выгрузках обоих форматов: загрузчик, модель и проверка вместе.
 *
 * <p>Проверка читает базовую конфигурацию, поэтому здесь важен не только обход объектов,
 * но и то, что реальный загрузчик заполняет её для обоих форматов.
 */
class RequiredRolesProjectTest {

  @ParameterizedTest
  @ValueSource(strings = {"designer-report", "edt-report"})
  void reportsEveryMissingStandardRole(String dump) throws URISyntaxException {
    var issues = issues(dump(dump), CheckSettings.defaults());

    assertThat(issues).hasSize(3);
    assertThat(issues).allSatisfy(issue -> {
      assertThat(issue.key()).isEqualTo(RequiredRolesCheck.KEY);
      assertThat(issue.severity()).isEqualTo(Severity.MAJOR);
      assertThat(issue.location().file()).endsWith(expectedConfigurationFile(dump));
      assertThat(issue.location().line()).isNull();
      assertThat(issue.location().mdoRef()).isNull();
    });
    assertThat(issues)
      .extracting(CheckResult::message)
      .anySatisfy(message -> assertThat(message).contains("ПолныеПрава"))
      .anySatisfy(message -> assertThat(message).contains("АдминистраторСистемы"))
      .anySatisfy(message -> assertThat(message)
        .contains("ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок"));
  }

  @Test
  void rolesParameterFromSettingsReplacesDefaults() throws URISyntaxException {
    var settings = new CheckSettings(ChecksMode.ON, Set.of(),
      Map.of(RequiredRolesCheck.KEY, new CheckParameters(Map.of(
        RequiredRolesCheck.PARAM_ROLES, List.of("МояРоль")))),
      Map.of(), Severity.INFO, List.of(), Set.of());

    var issues = issues(dump("designer-report"), settings);

    assertThat(issues).singleElement().satisfies(issue ->
      assertThat(issue.message()).contains("МояРоль"));
  }

  private static List<CheckResult> issues(Path root, CheckSettings settings) {
    var project = new ProjectLoader().load(root);
    return new CheckRunner(CheckRegistry.builtIn(), settings).run(project).issues().stream()
      .filter(issue -> RequiredRolesCheck.KEY.equals(issue.key()))
      .toList();
  }

  private static Path dump(String name) throws URISyntaxException {
    return Path.of(Objects.requireNonNull(
      RequiredRolesProjectTest.class.getResource("/dumps/" + name)).toURI());
  }

  private static Path expectedConfigurationFile(String dump) {
    return "edt-report".equals(dump)
      ? Path.of("Configuration", "Configuration.mdo")
      : Path.of("Configuration.xml");
  }
}
