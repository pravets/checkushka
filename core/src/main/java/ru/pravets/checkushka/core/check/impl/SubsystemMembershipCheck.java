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

import com.github._1c_syntax.bsl.mdo.MD;
import com.github._1c_syntax.bsl.mdo.support.ObjectBelonging;
import com.github._1c_syntax.bsl.types.MDOType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
 * Проверка объектов конфигурации, не входящих ни в одну подсистему.
 *
 * <p>Объект вне дерева подсистем не виден в командном интерфейсе и обычно означает
 * незавершённую конфигурацию. Подсистемы ссылаются на объекты верхнего уровня, поэтому
 * реквизиты, формы и команды отдельно не проверяются — они входят в подсистему вместе
 * с владельцем. Заимствованные объекты расширений (см. {@link ObjectBelonging#ADOPTED})
 * не проверяются: их состав определяет расширение.
 *
 * <p>Принадлежность подсистемам проверяется по индексу {@code Project.isInSubsystem}:
 * дерево подсистем обходится один раз при разборе проекта, а не на каждый объект.
 */
@CheckMetadata(
  key = SubsystemMembershipCheck.KEY,
  name = "Объект вне подсистемы",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.DESIGN},
  targets = CheckTarget.MDO,
  minutesToFix = 1)
public final class SubsystemMembershipCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "SubsystemMembership";

  /**
   * Типы, которые вправе не входить в подсистемы: сервисные объекты, не участвующие
   * в командном интерфейсе.
   */
  static final List<MDOType> DEFAULT_EXCLUDE_TYPES = List.of(
    MDOType.SUBSYSTEM,
    MDOType.LANGUAGE,
    MDOType.EVENT_SUBSCRIPTION,
    MDOType.COMMON_PICTURE,
    MDOType.COMMAND_GROUP,
    MDOType.COMMON_ATTRIBUTE,
    MDOType.DEFINED_TYPE,
    MDOType.STYLE,
    MDOType.FUNCTIONAL_OPTION,
    MDOType.SESSION_PARAMETER,
    MDOType.SETTINGS_STORAGE,
    MDOType.XDTO_PACKAGE,
    MDOType.ROLE,
    MDOType.SCHEDULED_JOB,
    MDOType.DOCUMENT_NUMERATOR,
    MDOType.INTEGRATION_SERVICE,
    MDOType.WEB_SOCKET_CLIENT);

  /** Имена типов по умолчанию в нотации {@link MDOType#nameEn()}. */
  static final List<String> DEFAULT_EXCLUDE_TYPE_NAMES = DEFAULT_EXCLUDE_TYPES.stream()
    .map(MDOType::nameEn)
    .toList();

  private static final CheckMetadata META =
    SubsystemMembershipCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    var mdo = context.mdo();
    if (mdo == null || mdo.getObjectBelonging() == ObjectBelonging.ADOPTED) {
      return List.of();
    }
    var project = context.project();
    if (!project.isTopLevel(mdo) || project.isInSubsystem(mdo)) {
      return List.of();
    }
    if (excludedTypes(context.parameters()).contains(mdo.getMdoType())) {
      return List.of();
    }

    var file = project.layout().fileOf(mdo.getMdoRef()).orElse(project.configurationFile());
    return List.of(new CheckResult(
      KEY,
      "Объект \"" + mdo.getMdoRef() + "\" не входит ни в одну подсистему",
      META.severity(),
      SourceLocation.of(file, null, mdo.getMdoRef()),
      META.minutesToFix()));
  }

  /**
   * Разбирает список исключаемых типов.
   *
   * <p>Имена сравниваются без учёта регистра; неизвестное имя игнорируется, чтобы опечатка
   * в конфигурации не роняла запуск. Заданный список заменяет набор по умолчанию.
   *
   * @param parameters параметры проверки из конфигурации
   * @return типы, которые не проверяются
   */
  static Set<MDOType> excludedTypes(CheckParameters parameters) {
    return parameters.stringList("excludeTypes", DEFAULT_EXCLUDE_TYPE_NAMES).stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(name -> !name.isEmpty())
      .map(MDOType::fromValue)
      .flatMap(Optional::stream)
      .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(new CheckParameter(
      "excludeTypes", "string[]", defaultExcludeTypes(),
      "Типы объектов, которые вправе не входить в подсистемы; имена MDOType, регистр не важен"));
  }

  private static String defaultExcludeTypes() {
    return DEFAULT_EXCLUDE_TYPE_NAMES.stream()
      .map(name -> "\"" + name + "\"")
      .collect(Collectors.joining(", ", "[", "]"));
  }
}
