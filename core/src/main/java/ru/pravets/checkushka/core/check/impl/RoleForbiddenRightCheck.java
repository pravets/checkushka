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

import com.github._1c_syntax.bsl.mdo.Role;
import com.github._1c_syntax.bsl.mdo.support.RoleRight;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckParameter;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.ParameterizedCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

/**
 * Проверка опасных прав в ролях.
 *
 * <p>Проверка сворачивает семейство однотипных правил «в роли установлено право X»: в роли,
 * которой нет в списке исключений, включено право из списка запрещённых. Права читаются из
 * модели mdclasses 0.22 — {@code Role.getData()} → {@code objectRights()} → {@code rights()}, —
 * поэтому одинаково обрабатываются права на объект и права уровня конфигурации: последние
 * приходят обычным {@code ObjectRight} с {@code name()} = ссылкой на конфигурацию.
 *
 * <p>Замечание выводится на пару «роль + право»: одно и то же право, выданное на десятки
 * объектов, не размножает отчёт. Строка не указывается: файл роли определяется по
 * {@code mdoRef}, а точное место права ищется в {@code Rights.xml} роли. {@code mdoRef}
 * замечания равен {@code Role.<Имя>}, так что проверку можно исключить по роли.
 *
 * <p>Значения параметра {@code rights} принимаются как имена констант {@link RoleRight}
 * ({@code INTERACTIVE_DELETE}), а также как английские и русские названия прав
 * ({@code InteractiveDelete}, {@code ИнтерактивноеУдаление}) без учёта регистра.
 * Пользовательский список заменяет значения по умолчанию, а не дополняет их.
 */
@CheckMetadata(
  key = RoleForbiddenRightCheck.KEY,
  name = "Запрещённое право в роли",
  type = CheckType.SECURITY,
  severity = Severity.MAJOR,
  tags = {CheckTag.BAD_PRACTICE},
  targets = CheckTarget.ROLE,
  minutesToFix = 10)
public final class RoleForbiddenRightCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "RoleForbiddenRight";

  /** Права, запрещённые по умолчанию: семейство из 23 правил каталога идей. */
  static final List<String> DEFAULT_RIGHTS = List.of(
    "ACTIVE_USERS",
    "ADMINISTRATION",
    "ALL_FUNCTIONS_MODE",
    "CONFIGURATION_EXTENSIONS_ADMINISTRATION",
    "DATA_ADMINISTRATION",
    "DELETE",
    "EXCLUSIVE_MODE",
    "INTERACTIVE_CLEAR_DELETION_MARK_PREDEFINED_DATA",
    "INTERACTIVE_DELETE",
    "INTERACTIVE_DELETE_MARKED_PREDEFINED_DATA",
    "INTERACTIVE_DELETE_PREDEFINED_DATA",
    "INTERACTIVE_OPEN_EXT_DATA_PROCESSORS",
    "INTERACTIVE_OPEN_EXT_REPORTS",
    "INTERACTIVE_SET_DELETION_MARK_PREDEFINED_DATA",
    "OUTPUT",
    "SAVE_USER_DATA",
    "AUTOMATION",
    "EXTERNAL_CONNECTION",
    "THICK_CLIENT",
    "THIN_CLIENT",
    "WEB_CLIENT",
    "UPDATE_DATA_BASE_CONFIGURATION",
    "EVENT_LOG");

  /** Роли, которые не проверяются по умолчанию. */
  static final List<String> DEFAULT_EXCLUDE_ROLES = List.of(
    "ПолныеПрава",
    "FullAccess",
    "АдминистраторСистемы",
    "SystemAdministrator");

  private static final String DEFAULT_RIGHTS_TEXT = jsonText(DEFAULT_RIGHTS);

  private static final String DEFAULT_EXCLUDE_ROLES_TEXT = jsonText(DEFAULT_EXCLUDE_ROLES);

  private static final CheckMetadata META =
    RoleForbiddenRightCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    if (!(context.mdo() instanceof Role role)) {
      return List.of();
    }
    var fallback = context.project().layout().fileOf(role.getMdoRef())
      .orElseGet(() -> context.project().root());
    return analyze(
      role,
      context.parameters().stringList("rights", DEFAULT_RIGHTS),
      context.parameters().stringList("excludeRoles", DEFAULT_EXCLUDE_ROLES),
      fallback);
  }

  /**
   * Проверяет одну роль.
   *
   * @param role         роль
   * @param rights       запрещённые права в любом поддерживаемом написании
   * @param excludeRoles роли, которые не проверяются
   * @param fallbackFile файл для замечания, если расположение роли определить не удалось
   * @return замечания по роли; не больше одного на право
   */
  static List<CheckResult> analyze(Role role, List<String> rights, List<String> excludeRoles,
                                   Path fallbackFile) {
    if (isExcluded(role.getName(), excludeRoles)) {
      return List.of();
    }
    var forbidden = resolve(rights);
    if (forbidden.isEmpty()) {
      return List.of();
    }

    var matched = new LinkedHashSet<RoleRight>();
    var data = role.getData();
    if (data != null) {
      for (var objectRight : nullSafe(data.objectRights())) {
        if (objectRight == null) {
          continue;
        }
        for (var right : nullSafe(objectRight.rights())) {
          if (right == null || right.name() == null || !right.value()) {
            continue;
          }
          if (forbidden.contains(right.name())) {
            matched.add(right.name());
          }
        }
      }
    }

    var results = new ArrayList<CheckResult>(matched.size());
    for (var right : matched) {
      results.add(new CheckResult(
        KEY,
        message(role.getName(), right),
        META.severity(),
        SourceLocation.of(fallbackFile, null, role.getMdoRef()),
        META.minutesToFix()));
    }
    return results;
  }

  /**
   * Разрешает значения параметра в права модели.
   *
   * @param names значения из конфигурации
   * @return распознанные права в порядке первого упоминания
   */
  static Set<RoleRight> resolve(List<String> names) {
    var resolved = new LinkedHashSet<RoleRight>();
    for (var name : nullSafe(names)) {
      var right = resolveOne(name);
      if (right != null) {
        resolved.add(right);
      }
    }
    return resolved;
  }

  private static RoleRight resolveOne(String name) {
    if (name == null) {
      return null;
    }
    var text = name.trim();
    if (text.isEmpty()) {
      return null;
    }
    try {
      return RoleRight.valueOf(text.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      // не имя константы — пробуем английское или русское название права
    }
    var byName = RoleRight.valueByName(text);
    return byName == RoleRight.UNKNOWN ? null : byName;
  }

  private static boolean isExcluded(String roleName, List<String> excludeRoles) {
    if (roleName == null || excludeRoles == null) {
      return false;
    }
    var name = roleName.trim();
    for (var excluded : excludeRoles) {
      if (excluded != null && name.equalsIgnoreCase(excluded.trim())) {
        return true;
      }
    }
    return false;
  }

  private static String message(String roleName, RoleRight right) {
    return "В роли \"" + roleName + "\" установлено запрещённое право \""
      + displayName(right) + "\"";
  }

  private static String displayName(RoleRight right) {
    var fullName = right.fullName();
    if (fullName != null) {
      var ru = fullName.getRu();
      if (ru != null && !ru.isBlank()) {
        return ru;
      }
      var en = fullName.getEn();
      if (en != null && !en.isBlank()) {
        return en;
      }
    }
    return right.name();
  }

  private static <T> List<T> nullSafe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private static String jsonText(List<String> values) {
    return values.stream().collect(Collectors.joining("\", \"", "[\"", "\"]"));
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(
      new CheckParameter(
        "rights", "string[]", DEFAULT_RIGHTS_TEXT,
        "Запрещённые права: имена RoleRight, английские или русские названия; регистр не важен"),
      new CheckParameter(
        "excludeRoles", "string[]", DEFAULT_EXCLUDE_ROLES_TEXT,
        "Роли, которые не проверяются; точное совпадение имени без учёта регистра"));
  }

  @Override
  public List<String> validateParameters(CheckParameters parameters) {
    var unknown = new LinkedHashSet<String>();
    for (var raw : parameters.stringList("rights", DEFAULT_RIGHTS)) {
      if (raw == null || raw.isBlank() || resolveOne(raw) != null) {
        continue;
      }
      unknown.add(raw.trim());
    }
    return unknown.stream()
      .map(name -> "Неизвестное право \"" + name
        + "\": допустимы имена RoleRight, английские и русские названия прав")
      .toList();
  }
}
