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

import com.github._1c_syntax.bsl.mdclasses.Configuration;
import com.github._1c_syntax.bsl.mdclasses.ConfigurationExtension;
import com.github._1c_syntax.bsl.mdclasses.Solution;
import com.github._1c_syntax.bsl.mdo.Role;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckRegistry;
import ru.pravets.checkushka.core.source.Project;
import ru.pravets.checkushka.core.source.SourceFormat;
import ru.pravets.checkushka.core.source.SourceOrder;

class RequiredRolesCheckTest {

  private static final Path CONFIGURATION = Path.of("Configuration.xml");

  @Test
  void allDefaultRolesPresentHasNoFindings() {
    var roles = roles("ПолныеПрава", "АдминистраторСистемы",
      "ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок");

    assertThat(analyze(roles)).isEmpty();
  }

  @Test
  void englishSpellingIsAccepted() {
    var roles = roles("FullAccess", "SystemAdministrator",
      "InteractiveOpenExternalReportsAndDataProcessors");

    assertThat(analyze(roles)).isEmpty();
  }

  @Test
  void eachMissingRoleIsReportedSeparately() {
    var results = analyze(roles("ПолныеПрава"));

    assertThat(results).hasSize(2);
    assertThat(results).extracting(CheckResult::message).containsExactlyInAnyOrder(
      "В конфигурации отсутствует обязательная роль \"АдминистраторСистемы\"",
      "В конфигурации отсутствует обязательная роль \"ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок\"");
    assertThat(results).allSatisfy(result -> {
      assertThat(result.key()).isEqualTo(RequiredRolesCheck.KEY);
      assertThat(result.severity()).isEqualTo(Severity.MAJOR);
      assertThat(result.location().file()).isEqualTo(CONFIGURATION);
      assertThat(result.location().line()).isNull();
      assertThat(result.location().mdoRef()).isNull();
    });
  }

  @Test
  void matchingIgnoresCase() {
    var roles = roles("полныеправа", "АДМИНИСТРАТОРСИСТЕМЫ",
      "интерактивноеоткрытиевнешнихотчетовиобработок");

    assertThat(analyze(roles)).isEmpty();
  }

  @Test
  void customRolesReplaceDefaults() {
    var roles = roles("ПолныеПрава", "АдминистраторСистемы",
      "ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок");

    var results = analyze(roles, List.of("МояРоль"));

    assertThat(results).singleElement().satisfies(result ->
      assertThat(result.message()).contains("МояРоль"));
  }

  @Test
  void emptyRolesRequireNothing() {
    assertThat(analyze(roles("ПолныеПрава"), List.of())).isEmpty();
  }

  @Test
  void nonStandardNamesAreSeparateRequirements() {
    var results = analyze(roles("Своя1"), List.of("Своя1", "Своя2"));

    assertThat(results).singleElement().satisfies(result ->
      assertThat(result.message()).contains("Своя2"));
  }

  @Test
  void duplicateAndBlankEntriesCollapseIntoOneRequirement() {
    var results = analyze(roles(), List.of("FullAccess", "ПолныеПрава", " ", ""));

    assertThat(results).singleElement().satisfies(result -> {
      assertThat(result.message()).contains("FullAccess");
      assertThat(result.message()).doesNotContain("ПолныеПрава");
    });
  }

  @Test
  void roleFromExtensionDoesNotSatisfyRequirement(@TempDir Path root) {
    var fullAccess = Role.builder().name("ПолныеПрава").build();
    var administrator = Role.builder().name("АдминистраторСистемы").build();
    var externalReports = Role.builder()
      .name("ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок")
      .build();

    var base = Configuration.builder()
      .name("Тест")
      .role(fullAccess)
      .role(administrator)
      .build();
    var extension = ConfigurationExtension.builder()
      .name("Расширение")
      .role(externalReports)
      .build();
    var merged = Configuration.builder()
      .name("Тест")
      .role(fullAccess)
      .role(administrator)
      .role(externalReports)
      .build();
    var solution = Solution.builder()
      .baseConfiguration(base)
      .mergedConfiguration(merged)
      .extensions(List.of(extension))
      .build();

    var results = new RequiredRolesCheck().check(CheckContext.ofConfiguration(
      project(root, solution), CheckParameters.empty()));

    assertThat(results).singleElement().satisfies(result ->
      assertThat(result.message()).contains("ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок"));
  }

  @Test
  void rolesParameterOverridesDefaults(@TempDir Path root) {
    var custom = Role.builder().name("МояРоль").build();
    var solution = Solution.builder()
      .baseConfiguration(Configuration.builder().name("Тест").role(custom).build())
      .mergedConfiguration(Configuration.builder().name("Тест").role(custom).build())
      .build();
    var project = project(root, solution);

    var known = new CheckParameters(Map.of(RequiredRolesCheck.PARAM_ROLES, List.of("МояРоль")));
    assertThat(new RequiredRolesCheck().check(CheckContext.ofConfiguration(project, known)))
      .isEmpty();

    var unknown = new CheckParameters(Map.of(RequiredRolesCheck.PARAM_ROLES, List.of("ДругаяРоль")));
    assertThat(new RequiredRolesCheck().check(CheckContext.ofConfiguration(project, unknown)))
      .singleElement()
      .satisfies(result -> assertThat(result.message()).contains("ДругаяРоль"));
  }

  @Test
  void projectWithoutBaseConfigurationIsSkipped(@TempDir Path root) {
    var role = Role.builder().name("ПолныеПрава").build();
    var solution = Solution.builder()
      .baseConfiguration(Configuration.EMPTY)
      .mergedConfiguration(Configuration.builder().name("Расширение").role(role).build())
      .build();

    var results = new RequiredRolesCheck().check(
      CheckContext.ofConfiguration(project(root, solution), CheckParameters.empty()));

    assertThat(results).isEmpty();
  }

  @Test
  void registeredWithExpectedMetadata() {
    var check = CheckRegistry.builtIn().find(RequiredRolesCheck.KEY);

    assertThat(check).isPresent();
    var metadata = CheckRegistry.metadataOf(check.orElseThrow());
    assertThat(metadata.targets()).containsExactly(CheckTarget.CONFIGURATION);
    assertThat(metadata.type()).isEqualTo(CheckType.CODE_SMELL);
    assertThat(metadata.severity()).isEqualTo(Severity.MAJOR);
    assertThat(metadata.tags()).containsExactly(CheckTag.STANDARD);
    assertThat(metadata.activatedByDefault()).isTrue();
  }

  private static List<CheckResult> analyze(List<Role> roles) {
    return analyze(roles, RequiredRolesCheck.DEFAULT_ROLES);
  }

  private static List<CheckResult> analyze(List<Role> roles, List<String> required) {
    return RequiredRolesCheck.analyze(roles, required, CONFIGURATION);
  }

  private static List<Role> roles(String... names) {
    return Arrays.stream(names).map(name -> Role.builder().name(name).build()).toList();
  }

  private static Project project(Path root, Solution solution) {
    var configurationFile = root.resolve("Configuration.xml");
    return new Project(root, configurationFile, SourceFormat.DESIGNER, solution, null,
      SourceOrder.empty(configurationFile, SourceFormat.DESIGNER));
  }
}
