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

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Режим учёта префиксов при сравнении имён объектов.
 */
public enum PrefixMode {

  /** Сравнивать полные имена. */
  NONE,

  /** Отбрасывать префикс: имя сравнивается так, как будто префикса нет. */
  STRIP_PREFIX,

  /** Группировать объекты по префиксу, внутри группы сравнивать остаток имени. */
  GROUP_BY_PREFIX;

  private static final Pattern AUTO_PREFIX = Pattern.compile("^([A-Za-zА-Яа-яЁё]{2,5}[_\\-])(.+)$");

  /**
   * Разбирает режим из конфигурации без учёта регистра.
   *
   * @param value        значение из файла
   * @param defaultValue режим по умолчанию
   * @return режим
   */
  public static PrefixMode parse(String value, PrefixMode defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    for (var mode : values()) {
      if (normalize(mode.name()).equals(normalize(value))) {
        return mode;
      }
    }
    return defaultValue;
  }

  private static String normalize(String value) {
    return value.replace("_", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Возвращает ключ сортировки для имени.
   *
   * @param name     имя объекта
   * @param prefixes настроенные префиксы; пустой список включает автоопределение
   * @return ключ, по которому сравниваются имена в этом режиме
   */
  public String sortKey(String name, List<String> prefixes) {
    return switch (this) {
      case NONE -> name;
      case STRIP_PREFIX -> strip(name, prefixes);
      case GROUP_BY_PREFIX -> {
        var prefix = prefixOf(name, prefixes);
        // \u0001 сортируется раньше любых печатных символов: он делит группы префиксов
        yield prefix + '\u0001' + strip(name, prefixes);
      }
    };
  }

  /**
   * Определяет префикс имени.
   *
   * @param name     имя объекта
   * @param prefixes настроенные префиксы; пустой список включает автоопределение
   * @return префикс или пустая строка
   */
  public String prefixOf(String name, List<String> prefixes) {
    if (prefixes == null || prefixes.isEmpty()) {
      var matcher = AUTO_PREFIX.matcher(name);
      return matcher.matches() ? matcher.group(1) : "";
    }
    var lowerName = name.toLowerCase(Locale.ROOT);
    var result = "";
    for (var prefix : prefixes) {
      if (prefix != null && !prefix.isEmpty() && lowerName.startsWith(prefix.toLowerCase(Locale.ROOT))
        && prefix.length() > result.length()) {
        result = prefix;
      }
    }
    return result;
  }

  private String strip(String name, List<String> prefixes) {
    var prefix = prefixOf(name, prefixes);
    return prefix.isEmpty() ? name : name.substring(prefix.length());
  }
}
