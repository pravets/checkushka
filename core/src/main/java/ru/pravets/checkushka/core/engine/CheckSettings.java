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
import java.util.Map;
import java.util.Set;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.Severity;

/**
 * Настройки запуска проверок, полученные из конфигурационного файла и параметров командной строки.
 *
 * @param mode              режим включения проверок
 * @param checkKeys         ключи для режимов {@link ChecksMode#ONLY} и {@link ChecksMode#EXCEPT}
 * @param parameters        параметры проверок по ключу
 * @param severityOverrides переопределения важности по ключу
 * @param minSeverity       нижняя граница важности для попадания в отчёт
 * @param ignore            правила исключения замечаний
 * @param disabledChecks    проверки, выключенные значением {@code false} в параметрах
 */
public record CheckSettings(ChecksMode mode, Set<String> checkKeys,
                            Map<String, CheckParameters> parameters,
                            Map<String, Severity> severityOverrides,
                            Severity minSeverity,
                            List<IgnoreRule> ignore,
                            Set<String> disabledChecks) {

  public CheckSettings {
    checkKeys = Set.copyOf(checkKeys);
    parameters = Map.copyOf(parameters);
    severityOverrides = Map.copyOf(severityOverrides);
    ignore = List.copyOf(ignore);
    disabledChecks = Set.copyOf(disabledChecks);
  }

  /**
   * Настройки по умолчанию: проверки, включённые в коде, без исключений.
   *
   * @return настройки
   */
  public static CheckSettings defaults() {
    return new CheckSettings(ChecksMode.ON, Set.of(), Map.of(), Map.of(), Severity.INFO, List.of(), Set.of());
  }

  /**
   * Параметры проверки.
   *
   * @param key ключ проверки
   * @return параметры или пустой набор
   */
  public CheckParameters parametersFor(String key) {
    return parameters.getOrDefault(key, CheckParameters.empty());
  }

  /**
   * Итоговая важность проверки с учётом переопределения.
   *
   * @param key      ключ проверки
   * @param declared важность из кода
   * @return важность для отчёта
   */
  public Severity severityFor(String key, Severity declared) {
    return severityOverrides.getOrDefault(key, declared);
  }

  /**
   * Проверяет, исключено ли замечание правилом из конфигурации.
   *
   * @param key    ключ проверки
   * @param mdoRef ссылка на объект метаданных или {@code null}
   * @return {@code true}, если замечание исключено
   */
  public boolean isIgnored(String key, String mdoRef) {
    return ignore.stream().anyMatch(rule -> rule.matches(key, mdoRef));
  }

  /**
   * Проверяет, участвует ли проверка в запуске согласно режиму.
   *
   * @param key               ключ проверки
   * @param activatedByDefault активность проверки по умолчанию
   * @return {@code true}, если проверка выполняется
   */
  public boolean isEnabled(String key, boolean activatedByDefault) {
    if (disabledChecks.contains(key)) {
      return false;
    }
    return switch (mode) {
      case OFF -> false;
      case ALL -> true;
      case ONLY -> checkKeys.contains(key);
      case EXCEPT -> !checkKeys.contains(key);
      case ON -> activatedByDefault || checkKeys.contains(key);
    };
  }
}
