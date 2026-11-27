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

import com.github._1c_syntax.bsl.mdo.Role;
import com.github._1c_syntax.bsl.mdo.storage.RoleData;
import com.github._1c_syntax.bsl.mdo.support.RoleRight;
import com.github._1c_syntax.bsl.types.MDOType;
import com.github._1c_syntax.bsl.types.MdoReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;

class RoleForbiddenRightCheckTest {

  private static final Path ROLE_FILE = Path.of("Roles", "Оператор.xml");

  @Test
  void forbiddenRightOnObjectIsReported() {
    var role = role("Оператор", data(objectRight("Catalog.Товары",
      right(RoleRight.ADMINISTRATION, true))));

    var results = analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of());

    assertThat(results).singleElement().satisfies(result -> {
      assertThat(result.key()).isEqualTo(RoleForbiddenRightCheck.KEY);
      assertThat(result.severity()).isEqualTo(Severity.MAJOR);
      assertThat(result.location().mdoRef()).isEqualTo("Role.Оператор");
      assertThat(result.location().file()).isEqualTo(ROLE_FILE);
      assertThat(result.location().line()).isNull();
      assertThat(result.message()).contains("Оператор", "Администрирование");
    });
  }

  @Test
  void configurationLevelRightIsReported() {
    var configuration = MdoReference.create(MDOType.CONFIGURATION, "Тест");
    var role = role("Оператор", data(objectRight(configuration,
      right(RoleRight.DATA_ADMINISTRATION, true))));

    var results = analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of());

    assertThat(results).singleElement().satisfies(result ->
      assertThat(result.message()).contains("Оператор", "АдминистрированиеДанных"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ПолныеПрава", "полныеправа", "FULLACCESS", "АдминистраторСистемы"})
  void excludedRoleIsNotReported(String excluded) {
    var role = role(excluded, data(objectRight("Catalog.Товары",
      right(RoleRight.ADMINISTRATION, true))));

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS,
      RoleForbiddenRightCheck.DEFAULT_EXCLUDE_ROLES)).isEmpty();
  }

  @Test
  void roleOutsideExclusionsIsReported() {
    var role = role("Оператор", data(objectRight("Catalog.Товары",
      right(RoleRight.ADMINISTRATION, true))));

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS,
      RoleForbiddenRightCheck.DEFAULT_EXCLUDE_ROLES)).hasSize(1);
  }

  @Test
  void disabledRightIsNotReported() {
    var role = role("Оператор", data(objectRight("Catalog.Товары",
      right(RoleRight.ADMINISTRATION, false))));

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of())).isEmpty();
  }

  @Test
  void rightOutsideForbiddenListIsNotReported() {
    var role = role("Оператор", data(objectRight("Catalog.Товары",
      right(RoleRight.READ, true))));

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of())).isEmpty();
  }

  @Test
  void sameRightOnSeveralObjectsIsReportedOnce() {
    var role = role("Оператор", data(
      objectRight("Catalog.Товары", right(RoleRight.DELETE, true)),
      objectRight("Document.Заказ", right(RoleRight.DELETE, true))));

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of())).hasSize(1);
  }

  @Test
  void twoForbiddenRightsGiveTwoFindings() {
    var role = role("Оператор", data(
      objectRight("Catalog.Товары", right(RoleRight.DELETE, true)),
      objectRight("Catalog.Товары", right(RoleRight.THIN_CLIENT, true))));

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of())).hasSize(2);
  }

  @Test
  void roleWithoutDataGivesNothing() {
    var role = Role.builder()
      .name("Пустая")
      .mdoReference(MdoReference.create(MDOType.ROLE, "Пустая"))
      .data(null)
      .build();

    assertThat(analyze(role, RoleForbiddenRightCheck.DEFAULT_RIGHTS, List.of())).isEmpty();
  }

  @Test
  void emptyRightsListGivesNothing() {
    var role = role("Оператор", data(objectRight("Catalog.Товары",
      right(RoleRight.ADMINISTRATION, true))));

    assertThat(analyze(role, List.of(), List.of())).isEmpty();
  }

  @Test
  void parametersMatchDefaults() {
    var parameters = new RoleForbiddenRightCheck().parameters();

    assertThat(parameters).extracting(parameter -> parameter.name())
      .containsExactly("rights", "excludeRoles");
    assertThat(parameters.get(0).type()).isEqualTo("string[]");
    assertThat(parameters.get(0).defaultValue())
      .contains("ADMINISTRATION", "EVENT_LOG");
    assertThat(parameters.get(1).defaultValue())
      .contains("ПолныеПрава", "SystemAdministrator");
  }

  @Test
  void defaultRightsAllResolveToEnumConstants() {
    var resolved = RoleForbiddenRightCheck.resolve(RoleForbiddenRightCheck.DEFAULT_RIGHTS);

    assertThat(resolved).hasSize(23);
    assertThat(resolved).extracting(RoleRight::name)
      .containsExactlyElementsOf(RoleForbiddenRightCheck.DEFAULT_RIGHTS);
  }

  @Test
  void validationReportsUnknownRight() {
    var check = new RoleForbiddenRightCheck();
    var messages = check.validateParameters(new CheckParameters(Map.of(
      "rights", List.of("Adminstration"))));

    assertThat(messages).singleElement().satisfies(message ->
      assertThat(message).contains("Adminstration", "RoleRight"));
  }

  @Test
  void validationAcceptsAllSupportedSpellings() {
    var check = new RoleForbiddenRightCheck();
    var messages = check.validateParameters(new CheckParameters(Map.of(
      "rights", List.of("ADMINISTRATION", "administration", "Administration",
        "Администрирование", " DELETE "))));

    assertThat(messages).isEmpty();
  }

  @Test
  void unknownConstantIsNeitherResolvedNorAccepted() {
    assertThat(RoleForbiddenRightCheck.resolve(List.of("UNKNOWN"))).isEmpty();

    var messages = new RoleForbiddenRightCheck().validateParameters(new CheckParameters(Map.of(
      "rights", List.of("UNKNOWN"))));

    assertThat(messages).singleElement().satisfies(message ->
      assertThat(message).contains("UNKNOWN"));
  }

  @Test
  void unknownRightInModelIsNotReported() {
    var role = role("Оператор", data(objectRight("Catalog.Товары",
      right(RoleRight.UNKNOWN, true))));

    assertThat(analyze(role, List.of("UNKNOWN"), List.of())).isEmpty();
  }

  private static List<CheckResult> analyze(Role role, List<String> rights, List<String> excluded) {
    return RoleForbiddenRightCheck.analyze(role, rights, excluded, ROLE_FILE);
  }

  private static Role role(String name, RoleData data) {
    return Role.builder()
      .name(name)
      .mdoReference(MdoReference.create(MDOType.ROLE, name))
      .data(data)
      .build();
  }

  private static RoleData data(RoleData.ObjectRight... objectRights) {
    var builder = RoleData.builder();
    for (var objectRight : objectRights) {
      builder.objectRight(objectRight);
    }
    return builder.build();
  }

  private static RoleData.ObjectRight objectRight(String mdoRef, RoleData.Right... rights) {
    return objectRight(MdoReference.create(mdoRef), rights);
  }

  private static RoleData.ObjectRight objectRight(MdoReference mdoReference, RoleData.Right... rights) {
    return new RoleData.ObjectRight(mdoReference, List.of(rights));
  }

  private static RoleData.Right right(RoleRight name, boolean value) {
    return new RoleData.Right(name, value, "");
  }
}
