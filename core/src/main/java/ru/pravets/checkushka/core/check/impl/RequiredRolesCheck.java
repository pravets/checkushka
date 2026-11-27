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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckParameter;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.ParameterizedCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

/**
 * Проверка обязательных ролей конфигурации.
 *
 * <p>Стандарт 1С № 488 требует, чтобы в конфигурации были роли {@code ПолныеПрава},
 * {@code АдминистраторСистемы} и {@code ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок}.
 * Роли ищутся по имени без учёта регистра; английское написание считается тем же требованием,
 * что и русское (пары заданы в {@link #STANDARD_ROLE_GROUPS}). Одно замечание — на каждую
 * отсутствующую роль.
 *
 * <p>Проверяется только сама конфигурация: роли расширений требование не закрывают, а проект
 * без базовой конфигурации (одно расширение) не проверяется вовсе. Требование «роли должны
 * входить в свойство „Роли по умолчанию“» в проверку не входит.
 */
@CheckMetadata(
  key = RequiredRolesCheck.KEY,
  name = "Отсутствует обязательная роль",
  type = CheckType.CODE_SMELL,
  severity = Severity.MAJOR,
  tags = {CheckTag.STANDARD},
  targets = CheckTarget.CONFIGURATION,
  minutesToFix = 1)
public final class RequiredRolesCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "RequiredRoles";

  /** Имя параметра с перечнем обязательных ролей. */
  static final String PARAM_ROLES = "roles";

  /**
   * Стандартные роли и их английские написания: элементы одной группы — одна роль.
   *
   * <p>Группы — единственный источник истины: из них собирается и список по умолчанию,
   * и таблица синонимов.
   */
  static final List<List<String>> STANDARD_ROLE_GROUPS = List.of(
    List.of("ПолныеПрава", "FullAccess"),
    List.of("АдминистраторСистемы", "SystemAdministrator"),
    List.of("ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок",
      "InteractiveOpenExternalReportsAndDataProcessors"));

  /** Обязательные роли по умолчанию. */
  static final List<String> DEFAULT_ROLES = STANDARD_ROLE_GROUPS.stream()
    .flatMap(List::stream)
    .toList();

  private static final CheckMetadata META =
    RequiredRolesCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    var base = context.project().baseConfiguration();
    if (base.isEmpty()) {
      return List.of();
    }
    return analyze(
      base.getRoles(),
      context.parameters().stringList(PARAM_ROLES, DEFAULT_ROLES),
      context.project().configurationFile());
  }

  /**
   * Проверяет наличие ролей в конфигурации.
   *
   * @param roles             роли самой конфигурации, без расширений
   * @param required          обязательные роли; элементы одной группы синонимов — одна роль
   * @param configurationFile файл конфигурации для локации замечания
   * @return замечания, одно на каждую отсутствующую роль
   */
  static List<CheckResult> analyze(List<Role> roles, List<String> required, Path configurationFile) {
    var requirements = requirements(required);
    if (requirements.isEmpty()) {
      return List.of();
    }

    var present = new HashSet<String>();
    for (var role : roles) {
      if (role != null && role.getName() != null) {
        present.add(canonical(role.getName()));
      }
    }

    var results = new ArrayList<CheckResult>();
    for (var requirement : requirements.entrySet()) {
      if (present.contains(requirement.getKey())) {
        continue;
      }
      results.add(new CheckResult(
        KEY,
        "В конфигурации отсутствует обязательная роль \"" + requirement.getValue() + "\"",
        META.severity(),
        SourceLocation.of(configurationFile),
        META.minutesToFix()));
    }
    return results;
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(new CheckParameter(
      PARAM_ROLES,
      "string[]",
      DEFAULT_ROLES.stream()
        .map(name -> "\"" + name + "\"")
        .collect(Collectors.joining(", ", "[", "]")),
      "Обязательные роли; русское и английское написания стандартных ролей считаются одной ролью"));
  }

  /**
   * Требования без дублей: ключ — каноническое имя роли, значение — первое написание.
   */
  private static Map<String, String> requirements(List<String> required) {
    var requirements = new LinkedHashMap<String, String>();
    if (required == null) {
      return requirements;
    }
    for (var name : required) {
      if (name == null) {
        continue;
      }
      var trimmed = name.trim();
      if (!trimmed.isEmpty()) {
        requirements.putIfAbsent(canonical(trimmed), trimmed);
      }
    }
    return requirements;
  }

  /**
   * Каноническое имя роли: синонимы стандартных ролей сводятся к первому написанию группы.
   *
   * @param name имя роли или требования
   * @return каноническое имя в нижнем регистре
   */
  static String canonical(String name) {
    var key = name.trim().toLowerCase(Locale.ROOT);
    for (var group : STANDARD_ROLE_GROUPS) {
      for (var alias : group) {
        if (alias.toLowerCase(Locale.ROOT).equals(key)) {
          return group.get(0).toLowerCase(Locale.ROOT);
        }
      }
    }
    return key;
  }
}
