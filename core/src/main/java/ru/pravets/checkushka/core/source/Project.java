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
import com.github._1c_syntax.bsl.mdo.MDChild;
import com.github._1c_syntax.bsl.mdo.Role;
import com.github._1c_syntax.bsl.mdclasses.Configuration;
import com.github._1c_syntax.bsl.mdclasses.Solution;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Разобранный проект: модель метаданных, формат выгрузки, порядок объектов и индекс файлов.
 *
 * <p>Формы и объекты собираются без дублей: после слияния конфигурации с расширениями одна
 * и та же форма может встретиться несколько раз, а имена форм уникальны только внутри объекта.
 */
public final class Project {

  private final Path root;
  private final Path configurationFile;
  private final SourceFormat format;
  private final Solution solution;
  private final ProjectLayout layout;
  private final SourceOrder order;

  private final List<Form> forms;
  private final Map<Form, String> formOwners;

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

    var owners = new IdentityHashMap<Form, String>();
    this.forms = solution == null ? List.of() : collectForms(owners);
    this.formOwners = owners;
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
   * Объекты метаданных верхнего уровня без дублей.
   *
   * @return объекты
   */
  public Collection<MD> objects() {
    if (solution == null) {
      return List.of();
    }
    var unique = new LinkedHashMap<String, MD>();
    configuration().getChildrenByMdoRef().values()
      .forEach(object -> unique.putIfAbsent(object.getMdoRef(), object));
    return List.copyOf(unique.values());
  }

  /**
   * Все формы конфигурации: общие и принадлежащие объектам, без дублей.
   *
   * @return формы
   */
  public List<Form> forms() {
    return forms;
  }

  /**
   * Ссылка на владельца формы.
   *
   * @param form форма
   * @return ссылка на объект-владелец или пустой результат для общих форм
   */
  public Optional<String> ownerOf(Form form) {
    var owner = formOwners.get(form);
    if (owner != null && !owner.isBlank()) {
      return Optional.of(owner);
    }
    if (form instanceof MDChild child && !child.getOwner().isEmpty()) {
      return Optional.of(child.getOwner().getMdoRef());
    }
    return Optional.empty();
  }

  /**
   * Возвращает файл формы.
   *
   * @param form форма
   * @return файл формы или пустой результат, если расположение определить не удалось
   */
  public Optional<Path> locationOf(Form form) {
    if (form == null || layout == null) {
      return Optional.empty();
    }
    var owner = ownerOf(form);
    if (owner.isPresent()) {
      var owned = layout.formFile(owner.get(), form.getName());
      if (owned.isPresent()) {
        return owned;
      }
    }
    if (owner.isEmpty() && form instanceof MD object) {
      var own = layout.fileOf(object.getMdoRef());
      if (own.isPresent()) {
        return own;
      }
    }
    return layout.formFile(null, form.getName());
  }

  /**
   * Все роли конфигурации.
   *
   * @return роли
   */
  public List<Role> roles() {
    return solution == null ? List.of() : configuration().getRoles();
  }

  private List<Form> collectForms(Map<Form, String> owners) {
    var collected = new ArrayList<Form>();
    var seen = new LinkedHashSet<String>();

    for (var common : configuration().getCommonForms()) {
      var ref = common.getMdoRef();
      if (seen.add(ref)) {
        collected.add(common);
        owners.put(common, "");
      }
    }

    for (var object : configuration().getChildrenByMdoRef().values()) {
      if (!(object instanceof FormOwner formOwner)) {
        continue;
      }
      var ownerRef = object.getMdoRef();
      for (var form : formOwner.getForms()) {
        if (seen.add(ownerRef + '\u0001' + form.getName())) {
          collected.add(form);
          owners.put(form, ownerRef);
        }
      }
    }
    return Collections.unmodifiableList(collected);
  }
}
