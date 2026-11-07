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
 * Проверка форм с незаполненным заголовком.
 *
 * <p>Правило фиксирует пустое значение заголовка и не утверждает, что заголовок обязателен
 * всегда: для части форм платформа подставляет его автоматически. При необходимости проверку
 * отключают в конфигурации.
 */
@CheckMetadata(
  key = FormWithoutTitleCheck.KEY,
  name = "Форма без заголовка",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.STANDARD, CheckTag.DESIGN},
  targets = CheckTarget.FORM,
  minutesToFix = 1)
public final class FormWithoutTitleCheck implements MetadataCheck {

  /** Ключ проверки. */
  public static final String KEY = "FormWithoutTitle";

  private static final CheckMetadata META = FormWithoutTitleCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    var form = context.form();
    if (form == null) {
      return List.of();
    }
    var data = form.getData();
    if (data == null || data.isEmpty()) {
      return List.of();
    }
    var title = data.getTitle();
    if (title != null && !title.isEmpty()) {
      return List.of();
    }

    var file = context.project().layout().formFile(null, form.getName())
      .orElse(context.project().root());
    return List.of(new CheckResult(
      KEY,
      "У формы «" + form.getName() + "» не заполнен заголовок",
      META.severity(),
      SourceLocation.of(file, null, form.getName()),
      META.minutesToFix()));
  }
}
