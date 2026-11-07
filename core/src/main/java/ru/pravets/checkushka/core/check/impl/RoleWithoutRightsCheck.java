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

import java.util.List;
import java.util.Optional;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

/**
 * Проверка ролей, у которых не установлено ни одного права.
 */
@CheckMetadata(
  key = RoleWithoutRightsCheck.KEY,
  name = "Роль без прав",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.STANDARD, CheckTag.DESIGN},
  targets = CheckTarget.ROLE,
  minutesToFix = 1)
public final class RoleWithoutRightsCheck implements MetadataCheck {

  /** Ключ проверки. */
  public static final String KEY = "RoleWithoutRights";

  private static final CheckMetadata META = RoleWithoutRightsCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    if (!(context.mdo() instanceof com.github._1c_syntax.bsl.mdo.Role role)) {
      return List.of();
    }
    if (hasRights(role)) {
      return List.of();
    }

    var file = context.project().layout().fileOf(role.getMdoRef()).orElse(context.project().root());
    return List.of(new CheckResult(
      KEY,
      "Роль «" + role.getName() + "» не содержит установленных прав",
      META.severity(),
      SourceLocation.of(file, null, role.getMdoRef()),
      META.minutesToFix()));
  }

  static boolean hasRights(com.github._1c_syntax.bsl.mdo.Role role) {
    var data = role.getData();
    if (data == null) {
      return false;
    }
    var objectRights = Optional.ofNullable(data.objectRights()).orElse(List.of());
    return objectRights.stream()
      .flatMap(objectRight -> objectRight.rights().stream())
      .anyMatch(right -> right.value());
  }
}
