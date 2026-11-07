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
package ru.pravets.checkushka.mcp;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import ru.pravets.checkushka.core.source.Project;
import ru.pravets.checkushka.core.source.ProjectLoader;

/**
 * Зарегистрированные рабочие папки MCP-сервера и разобранные модели проектов.
 *
 * <p>Разбор проекта выполняется при регистрации: повторные вызовы инструментов работают
 * с уже прочитанной моделью.
 */
public final class WorkspaceRegistry {

  private final Map<String, Workspace> workspaces = new ConcurrentHashMap<>();
  private final Function<Path, Project> loader;

  /**
   * Реестр с загрузкой проектов через mdclasses.
   */
  public WorkspaceRegistry() {
    this(path -> new ProjectLoader().load(path));
  }

  /**
   * Реестр с подменяемым загрузчиком (для тестов).
   *
   * @param loader функция разбора проекта
   */
  public WorkspaceRegistry(Function<Path, Project> loader) {
    this.loader = loader;
  }

  /**
   * Регистрирует рабочую папку.
   *
   * @param path каталог проекта
   * @param name имя рабочей папки или {@code null}, чтобы взять имя каталога
   * @return зарегистрированная папка
   */
  public Workspace register(Path path, String name) {
    var normalized = path.toAbsolutePath().normalize();
    var project = loader.apply(normalized);
    var workspace = new Workspace(
      normalized.toUri().toString(),
      name == null || name.isBlank() ? String.valueOf(normalized.getFileName()) : name,
      normalized,
      project);
    workspaces.put(workspace.uri(), workspace);
    return workspace;
  }

  /**
   * Удаляет рабочую папку.
   *
   * @param uri идентификатор папки
   * @return {@code true}, если папка была зарегистрирована
   */
  public boolean unregister(String uri) {
    return workspaces.remove(uri) != null;
  }

  /**
   * Список зарегистрированных папок.
   *
   * @return папки
   */
  public Collection<Workspace> list() {
    return List.copyOf(workspaces.values());
  }

  /**
   * Возвращает папку по идентификатору.
   *
   * @param uri идентификатор
   * @return папка
   * @throws IllegalArgumentException если папка не зарегистрирована
   */
  public Workspace require(String uri) {
    var workspace = workspaces.get(uri);
    if (workspace == null) {
      var known = workspaces.keySet().isEmpty() ? "нет зарегистрированных папок"
        : "зарегистрированы: " + String.join(", ", workspaces.keySet());
      throw new IllegalArgumentException(
        "Рабочая папка не найдена: " + uri + "; " + known
          + ". Зарегистрируйте её инструментом register_workspace_folder.");
    }
    return workspace;
  }

  /**
   * Зарегистрированная рабочая папка.
   *
   * @param uri     идентификатор вида {@code file:///C:/project}
   * @param name    имя для отображения
   * @param path    каталог проекта
   * @param project разобранная модель
   */
  public record Workspace(String uri, String name, Path path, Project project) {
  }
}
