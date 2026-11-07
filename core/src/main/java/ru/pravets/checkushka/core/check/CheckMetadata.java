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
package ru.pravets.checkushka.core.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Описание проверки в коде. Метаданные используются движком для фильтрации, а также
 * командой {@code list-checks} и MCP-инструментом {@code describe_check}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckMetadata {

  /**
   * Уникальный ключ проверки; совпадает по смыслу с ключом диагностики в BSL LS.
   *
   * @return ключ
   */
  String key();

  /**
   * Человекочитаемое название.
   *
   * @return название
   */
  String name();

  /**
   * Класс замечания.
   *
   * @return тип
   */
  CheckType type() default CheckType.CODE_SMELL;

  /**
   * Важность по умолчанию.
   *
   * @return важность
   */
  Severity severity() default Severity.MINOR;

  /**
   * Теги проверки.
   *
   * @return теги
   */
  CheckTag[] tags() default {};

  /**
   * Уровни, к которым применяется проверка.
   *
   * @return цели
   */
  CheckTarget[] targets();

  /**
   * Включена ли проверка по умолчанию.
   *
   * @return признак активности
   */
  boolean activatedByDefault() default true;

  /**
   * Трудозатраты на исправление одного замечания, минуты.
   *
   * @return минуты
   */
  int minutesToFix() default 0;
}
