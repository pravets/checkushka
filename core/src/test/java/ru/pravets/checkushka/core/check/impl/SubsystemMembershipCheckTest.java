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

import com.github._1c_syntax.bsl.mdo.Catalog;
import com.github._1c_syntax.bsl.mdo.MD;
import com.github._1c_syntax.bsl.mdo.Role;
import com.github._1c_syntax.bsl.mdo.Subsystem;
import com.github._1c_syntax.bsl.mdo.children.ObjectAttribute;
import com.github._1c_syntax.bsl.mdo.support.ObjectBelonging;
import com.github._1c_syntax.bsl.mdclasses.Configuration;
import com.github._1c_syntax.bsl.mdclasses.Solution;
import com.github._1c_syntax.bsl.types.MdoReference;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckRegistry;
import ru.pravets.checkushka.core.engine.CheckRunner;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.source.Project;
import ru.pravets.checkushka.core.source.ProjectLayout;
import ru.pravets.checkushka.core.source.ProjectLoader;
import ru.pravets.checkushka.core.source.SourceFormat;
import ru.pravets.checkushka.core.source.SourceOrder;

class SubsystemMembershipCheckTest {

  @TempDir
  private Path tempDir;

  @Test
  void reportsTopLevelObjectOutsideSubsystems() {
    var catalog = catalog("Товары");
    var project = project(Configuration.builder().child(catalog).build());

    assertThat(project.isInSubsystem(catalog)).isFalse();

    assertThat(check(project, catalog, CheckParameters.empty())).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo(SubsystemMembershipCheck.KEY);
      assertThat(issue.severity()).isEqualTo(Severity.MINOR);
      assertThat(issue.message()).contains("Catalog.Товары");
      assertThat(issue.location().mdoRef()).isEqualTo("Catalog.Товары");
      assertThat(issue.location().line()).isNull();
      assertThat(issue.location().file()).isEqualTo(project.configurationFile());
      assertThat(issue.minutesToFix()).isEqualTo(1);
    });
  }

  @Test
  void doesNotReportObjectInTopLevelSubsystem() {
    var catalog = catalog("Товары");
    var subsystem = subsystem("Продажи", catalog.getMdoReference());
    var project = project(Configuration.builder().subsystem(subsystem).child(catalog).build());

    assertThat(project.isInSubsystem(catalog)).isTrue();
    assertThat(check(project, catalog, CheckParameters.empty())).isEmpty();
  }

  @Test
  void doesNotReportObjectInNestedSubsystem() {
    var catalog = catalog("Товары");
    var nested = subsystem("Вложенная", catalog.getMdoReference());
    var sales = Subsystem.builder()
      .name("Продажи")
      .mdoReference(MdoReference.create("Subsystem.Продажи"))
      .subsystem(nested)
      .build();
    var project = project(Configuration.builder().subsystem(sales).child(catalog).build());

    assertThat(project.isInSubsystem(catalog)).isTrue();
    assertThat(check(project, catalog, CheckParameters.empty())).isEmpty();
  }

  @Test
  void reportsObjectFileWhenItExists() throws IOException {
    var catalog = catalog("Товары");
    var file = tempDir.resolve(Path.of("Catalogs", "Товары", "Товары.xml"));
    Files.createDirectories(file.getParent());
    Files.createFile(file);
    var project = project(Configuration.builder().child(catalog).build());

    assertThat(check(project, catalog, CheckParameters.empty())).singleElement()
      .satisfies(issue -> assertThat(issue.location().file()).isEqualTo(file));
  }

  @Test
  void doesNotReportServiceTypeByDefault() {
    var role = role("ПолныеПрава");
    var project = project(Configuration.builder().child(role).build());

    assertThat(check(project, role, CheckParameters.empty())).isEmpty();
  }

  @Test
  void doesNotReportAdoptedExtensionObject() {
    var adopted = Catalog.builder()
      .name("Заимствованный")
      .mdoReference(MdoReference.create("Catalog.Заимствованный"))
      .objectBelonging(ObjectBelonging.ADOPTED)
      .build();
    var project = project(Configuration.builder().child(adopted).build());

    assertThat(check(project, adopted, CheckParameters.empty())).isEmpty();
  }

  @Test
  void doesNotReportChildObject() {
    var catalog = catalog("Товары");
    var attribute = ObjectAttribute.builder().name("Код").build();
    var project = project(Configuration.builder().child(catalog).build());

    assertThat(check(project, attribute, CheckParameters.empty())).isEmpty();
  }

  @Test
  void customExcludeTypesReplaceDefaults() {
    var catalog = catalog("Товары");
    var role = role("ПолныеПрава");
    var project = project(Configuration.builder().child(catalog).child(role).build());
    var parameters = new CheckParameters(Map.of("excludeTypes", List.of("Catalog")));

    assertThat(check(project, catalog, parameters)).isEmpty();
    assertThat(check(project, role, parameters)).singleElement()
      .satisfies(issue -> assertThat(issue.location().mdoRef()).isEqualTo("Role.ПолныеПрава"));
  }

  @Test
  void emptyExcludeTypesDisablesExclusions() {
    var role = role("ПолныеПрава");
    var project = project(Configuration.builder().child(role).build());
    var parameters = new CheckParameters(Map.of("excludeTypes", List.of()));

    assertThat(check(project, role, parameters)).singleElement();
  }

  @Test
  void unknownExcludeTypeIsIgnored() {
    var catalog = catalog("Товары");
    var project = project(Configuration.builder().child(catalog).build());
    var parameters = new CheckParameters(Map.of("excludeTypes", List.of("НетТакого")));

    assertThat(check(project, catalog, parameters)).singleElement();
  }

  @Test
  void parametersDescribeExcludeTypes() {
    var parameters = new SubsystemMembershipCheck().parameters();

    assertThat(parameters).singleElement().satisfies(parameter -> {
      assertThat(parameter.name()).isEqualTo("excludeTypes");
      assertThat(parameter.type()).isEqualTo("string[]");
      assertThat(parameter.defaultValue())
        .contains("Subsystem")
        .contains("WebSocketClient");
      assertThat(parameter.description()).isNotBlank();
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"designer-report", "edt-report"})
  void findsObjectOutsideSubsystemsInDump(String dump) throws URISyntaxException {
    var project = new ProjectLoader().load(dump(dump));
    var issues = new CheckRunner(CheckRegistry.builtIn(), CheckSettings.defaults()).run(project)
      .issues().stream()
      .filter(issue -> SubsystemMembershipCheck.KEY.equals(issue.key()))
      .toList();

    assertThat(issues).singleElement()
      .satisfies(issue -> assertThat(issue.location().mdoRef()).isEqualTo("Report.Отчёт"));
  }

  private List<CheckResult> check(Project project, MD mdo, CheckParameters parameters) {
    return new SubsystemMembershipCheck().check(CheckContext.ofMdo(project, mdo, parameters));
  }

  private static Path dump(String name) throws URISyntaxException {
    return Path.of(Objects.requireNonNull(
      SubsystemMembershipCheckTest.class.getResource("/dumps/" + name)).toURI());
  }

  private Project project(Configuration configuration) {
    var solution = Solution.builder().mergedConfiguration(configuration).build();
    var configurationFile = tempDir.resolve("Configuration.xml");
    return new Project(tempDir, configurationFile, SourceFormat.DESIGNER, solution,
      ProjectLayout.scan(tempDir),
      SourceOrder.empty(configurationFile, SourceFormat.DESIGNER));
  }

  private static Catalog catalog(String name) {
    return Catalog.builder()
      .name(name)
      .mdoReference(MdoReference.create("Catalog." + name))
      .build();
  }

  private static Role role(String name) {
    return Role.builder()
      .name(name)
      .mdoReference(MdoReference.create("Role." + name))
      .build();
  }

  private static Subsystem subsystem(String name, MdoReference content) {
    return Subsystem.builder()
      .name(name)
      .mdoReference(MdoReference.create("Subsystem." + name))
      .addContent(content)
      .build();
  }
}
