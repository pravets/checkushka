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

import com.github._1c_syntax.bsl.mdo.Form;
import com.github._1c_syntax.bsl.mdo.MD;
import java.util.Objects;
import ru.pravets.checkushka.core.source.Project;

/**
 * Контекст одной единицы анализа.
 *
 * <p>Заполняется только та часть, которая соответствует уровню проверки: например,
 * для проверки уровня {@link CheckTarget#ROLE} заполнено поле {@code mdo} с ролью.
 *
 * @param project    разобранный проект
 * @param mdo        объект метаданных или {@code null} для уровня конфигурации
 * @param form       форма или {@code null}, если проверка не про формы
 * @param parameters параметры проверки из конфигурации
 */
public record CheckContext(Project project, MD mdo, Form form, CheckParameters parameters) {

  public CheckContext {
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(parameters, "parameters");
  }

  /**
   * Контекст уровня конфигурации.
   *
   * @param project    проект
   * @param parameters параметры проверки
   * @return контекст
   */
  public static CheckContext ofConfiguration(Project project, CheckParameters parameters) {
    return new CheckContext(project, null, null, parameters);
  }

  /**
   * Контекст объекта метаданных.
   *
   * @param project    проект
   * @param mdo        объект метаданных
   * @param parameters параметры проверки
   * @return контекст
   */
  public static CheckContext ofMdo(Project project, MD mdo, CheckParameters parameters) {
    return new CheckContext(project, mdo, null, parameters);
  }

  /**
   * Контекст формы.
   *
   * @param project    проект
   * @param form       форма
   * @param parameters параметры проверки
   * @return контекст
   */
  public static CheckContext ofForm(Project project, Form form, CheckParameters parameters) {
    return new CheckContext(project, null, form, parameters);
  }
}
