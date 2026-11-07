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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Сравнение имён объектов метаданных: без учёта регистра, с числовыми частями как числами.
 *
 * <p>Пример: {@code Модуль2} меньше {@code Модуль10}, а {@code Общий} и {@code общий}
 * считаются равными по основному порядку.
 */
public final class NameComparator implements Comparator<String> {

  private static final NameComparator INSTANCE = new NameComparator();
  private static final Pattern LEADING_ZEROS = Pattern.compile("^0+(?=\\d)");
  private static final Pattern DIGITS = Pattern.compile("\\d+");

  private final Collator collator;

  private NameComparator() {
    collator = Collator.getInstance(Locale.forLanguageTag("ru-RU"));
    collator.setStrength(Collator.PRIMARY);
    collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
  }

  /**
   * Общий экземпляр компаратора.
   *
   * @return компаратор
   */
  public static NameComparator instance() {
    return INSTANCE;
  }

  @Override
  public int compare(String left, String right) {
    // Имена, различающиеся только регистром, считаются равными: для порядка объектов
    // метаданных это ожидаемое поведение, а стабильность отчёта обеспечивается сортировкой
    // результатов в движке.
    return collator.compare(normalize(left), normalize(right));
  }

  private static String normalize(String value) {
    var builder = new StringBuilder(value.length());
    var matcher = DIGITS.matcher(value);
    var last = 0;
    while (matcher.find()) {
      builder.append(value, last, matcher.start());
      builder.append(padNumber(matcher.group()));
      last = matcher.end();
    }
    builder.append(value, last, value.length());
    return builder.toString();
  }

  private static String padNumber(String digits) {
    var withoutLeadingZeros = LEADING_ZEROS.matcher(digits).replaceFirst("");
    return "0".repeat(Math.max(0, 12 - withoutLeadingZeros.length())) + withoutLeadingZeros;
  }
}
