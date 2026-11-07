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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Параметры проверки из конфигурационного файла.
 *
 * <p>Значения читаются как JSON-примитивы и списки строк; неизвестные ключи игнорируются,
 * чтобы конфигурация не ломалась при обновлении набора параметров.
 */
public final class CheckParameters {

  private static final CheckParameters EMPTY = new CheckParameters(Map.of());

  private final Map<String, Object> values;

  /**
   * Создаёт набор параметров.
   *
   * @param values значения параметров
   */
  public CheckParameters(Map<String, Object> values) {
    this.values = Map.copyOf(values);
  }

  /**
   * Пустой набор параметров.
   *
   * @return параметры, в которых любое имя не задано
   */
  public static CheckParameters empty() {
    return EMPTY;
  }

  /**
   * Ключи параметров.
   *
   * @return множество имён
   */
  public Set<String> names() {
    return values.keySet();
  }

  /**
   * Строковый параметр.
   *
   * @param name         имя параметра
   * @param defaultValue значение по умолчанию
   * @return значение параметра или {@code defaultValue}
   */
  public String string(String name, String defaultValue) {
    var value = values.get(name);
    return value instanceof String text ? text : defaultValue;
  }

  /**
   * Логический параметр.
   *
   * @param name         имя параметра
   * @param defaultValue значение по умолчанию
   * @return значение параметра или {@code defaultValue}
   */
  public boolean bool(String name, boolean defaultValue) {
    var value = values.get(name);
    return value instanceof Boolean flag ? flag : defaultValue;
  }

  /**
   * Целочисленный параметр.
   *
   * @param name         имя параметра
   * @param defaultValue значение по умолчанию
   * @return значение параметра или {@code defaultValue}
   */
  public int integer(String name, int defaultValue) {
    var value = values.get(name);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }

  /**
   * Список строк.
   *
   * @param name         имя параметра
   * @param defaultValue значение по умолчанию
   * @return список из конфигурации или {@code defaultValue}
   */
  public List<String> stringList(String name, List<String> defaultValue) {
    var value = values.get(name);
    if (!(value instanceof List<?> list)) {
      return defaultValue;
    }
    return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }
}
