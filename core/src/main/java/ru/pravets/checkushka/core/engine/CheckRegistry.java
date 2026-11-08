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
package ru.pravets.checkushka.core.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.impl.MetadataOrderCheck;
import ru.pravets.checkushka.core.check.impl.RoleWithoutRightsCheck;

/**
 * Набор доступных проверок.
 *
 * <p>Встроенные проверки перечислены явно; подключение сторонних реализаций через
 * {@code ServiceLoader} запланировано отдельным этапом.
 */
public final class CheckRegistry {

  private final Map<String, MetadataCheck> checksByKey;

  /**
   * Создаёт реестр проверок.
   *
   * @param checks реализации проверок
   * @throws IllegalArgumentException если ключ проверки повторяется
   */
  public CheckRegistry(Collection<MetadataCheck> checks) {
    var map = new LinkedHashMap<String, MetadataCheck>();
    for (var check : checks) {
      var metadata = metadataOf(check);
      if (map.put(metadata.key(), check) != null) {
        throw new IllegalArgumentException("Дублирующийся ключ проверки: " + metadata.key());
      }
    }
    this.checksByKey = Collections.unmodifiableMap(map);
  }

  /**
   * Реестр со встроенными проверками.
   *
   * @return реестр
   */
  public static CheckRegistry builtIn() {
    return new CheckRegistry(List.of(
      new MetadataOrderCheck(),
      new RoleWithoutRightsCheck()));
  }

  /**
   * Все проверки реестра в порядке добавления.
   *
   * @return проверки
   */
  public Collection<MetadataCheck> all() {
    return checksByKey.values();
  }

  /**
   * Поиск проверки по ключу.
   *
   * @param key ключ
   * @return проверка или пустой результат
   */
  public Optional<MetadataCheck> find(String key) {
    return Optional.ofNullable(checksByKey.get(key));
  }

  /**
   * Проверяет, что все перечисленные ключи есть в реестре.
   *
   * <p>Нужен, чтобы опечатка или ключ удалённой проверки не превращались молча
   * в пустой список проверок.
   *
   * @param keys ключи проверок
   * @throws IllegalArgumentException если хотя бы один ключ неизвестен
   */
  public void requireKnownKeys(Collection<String> keys) {
    var unknown = keys.stream()
      .filter(key -> !checksByKey.containsKey(key))
      .sorted()
      .toList();
    if (unknown.isEmpty()) {
      return;
    }
    var available = checksByKey.keySet().stream().sorted().toList();
    throw new IllegalArgumentException(
      "Неизвестная проверка: " + String.join(", ", unknown)
        + "; доступны: " + String.join(", ", available));
  }

  /**
   * Метаданные проверки из аннотации {@link CheckMetadata}.
   *
   * @param check реализация проверки
   * @return метаданные
   * @throws IllegalStateException если аннотация отсутствует
   */
  public static CheckMetadata metadataOf(MetadataCheck check) {
    var metadata = check.getClass().getAnnotation(CheckMetadata.class);
    if (metadata == null) {
      throw new IllegalStateException(
        "У проверки " + check.getClass().getName() + " отсутствует @" + CheckMetadata.class.getSimpleName());
    }
    return metadata;
  }
}
