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
package ru.pravets.checkushka.core.source;

import com.github._1c_syntax.bsl.mdo.Form;
import com.github._1c_syntax.bsl.mdo.FormOwner;
import com.github._1c_syntax.bsl.mdo.MD;
import com.github._1c_syntax.bsl.mdo.Role;
import com.github._1c_syntax.bsl.mdclasses.Configuration;
import com.github._1c_syntax.bsl.mdclasses.Solution;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Разобранный проект: модель метаданных, формат выгрузки, порядок объектов и индекс файлов.
 */
public final class Project {

  private final Path root;
  private final Path configurationFile;
  private final SourceFormat format;
  private final Solution solution;
  private final ProjectLayout layout;
  private final SourceOrder order;

  /**
   * Создаёт описание проекта.
   *
   * @param root              корень, с которого начат разбор
   * @param configurationFile файл конфигурации
   * @param format            формат выгрузки
   * @param solution          разобранная модель вместе с расширениями
   * @param layout            индекс файлов
   * @param order             порядок дочерних объектов конфигурации
   */
  public Project(Path root, Path configurationFile, SourceFormat format, Solution solution,
                 ProjectLayout layout, SourceOrder order) {
    this.root = root;
    this.configurationFile = configurationFile;
    this.format = format;
    this.solution = solution;
    this.layout = layout;
    this.order = order;
  }

  /**
   * Корень, с которого начат разбор.
   *
   * @return путь
   */
  public Path root() {
    return root;
  }

  /**
   * Файл конфигурации.
   *
   * @return путь
   */
  public Path configurationFile() {
    return configurationFile;
  }

  /**
   * Формат выгрузки.
   *
   * @return формат
   */
  public SourceFormat format() {
    return format;
  }

  /**
   * Разобранная модель: объединённая конфигурация с расширениями.
   *
   * @return решение
   */
  public Solution solution() {
    return solution;
  }

  /**
   * Объединённая конфигурация.
   *
   * @return конфигурация
   */
  public Configuration configuration() {
    return solution.getMergedConfiguration();
  }

  /**
   * Индекс файлов проекта.
   *
   * @return индекс
   */
  public ProjectLayout layout() {
    return layout;
  }

  /**
   * Порядок дочерних объектов конфигурации в исходном файле.
   *
   * @return порядок
   */
  public SourceOrder order() {
    return order;
  }

  /**
   * Объекты метаданных верхнего уровня.
   *
   * @return объекты
   */
  public Collection<MD> objects() {
    return configuration().getChildrenByMdoRef().values();
  }

  /**
   * Все формы конфигурации: общие и принадлежащие объектам.
   *
   * @return формы
   */
  public List<Form> forms() {
    var result = new java.util.ArrayList<Form>(configuration().getCommonForms());
    configuration().getChildrenByMdoRef().values().stream()
      .filter(FormOwner.class::isInstance)
      .map(FormOwner.class::cast)
      .forEach(owner -> result.addAll(owner.getForms()));
    return List.copyOf(result);
  }

  /**
   * Все роли конфигурации.
   *
   * @return роли
   */
  public List<Role> roles() {
    return configuration().getRoles();
  }
}
