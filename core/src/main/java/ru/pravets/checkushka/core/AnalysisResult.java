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
package ru.pravets.checkushka.core;

import java.time.Duration;
import java.util.List;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;

/**
 * Результат анализа проекта.
 *
 * @param issues     найденные замечания в стабильном порядке
 * @param statistics статистика запуска
 */
public record AnalysisResult(List<CheckResult> issues, Statistics statistics) {

  public AnalysisResult {
    issues = List.copyOf(issues);
  }

  /**
   * Признак отсутствия замечаний.
   *
   * @return {@code true}, если замечаний нет
   */
  public boolean isEmpty() {
    return issues.isEmpty();
  }

  /**
   * Количество замечаний важности не ниже указанной.
   *
   * @param min нижняя граница важности
   * @return количество
   */
  public long countAtLeast(Severity min) {
    return issues.stream().filter(issue -> issue.severity().atLeast(min)).count();
  }

  /**
   * Статистика запуска анализа.
   *
   * @param checksRun       сколько проверок запущено
   * @param objectsAnalyzed сколько единиц анализа обработано
   * @param duration        длительность
   */
  public record Statistics(int checksRun, int objectsAnalyzed, Duration duration) {
  }
}
