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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pravets.checkushka.core.AnalysisResult;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.source.Project;

/**
 * Движок проверок: перебирает единицы анализа и применяет к ним подходящие проверки.
 *
 * <p>Обход последовательный: набор объектов метаданных у mdclasses ленивый, а выигрыш от
 * параллелизма появляется только на больших конфигурациях — это отдельная задача оптимизации.
 */
public final class CheckRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckRunner.class);

  private static final Comparator<CheckResult> ORDER = Comparator
    .comparing((CheckResult result) -> result.location().file().toString())
    .thenComparing(result -> result.location().line() == null ? Integer.MAX_VALUE : result.location().line())
    .thenComparing(CheckResult::key)
    .thenComparing(CheckResult::message);

  private final CheckRegistry registry;
  private final CheckSettings settings;

  /**
   * Создаёт движок.
   *
   * @param registry реестр проверок
   * @param settings настройки запуска
   */
  public CheckRunner(CheckRegistry registry, CheckSettings settings) {
    this.registry = registry;
    this.settings = settings;
  }

  /**
   * Выполняет анализ проекта.
   *
   * @param project разобранный проект
   * @return результат анализа
   */
  public AnalysisResult run(Project project) {
    var started = Instant.now();
    var activeChecks = activeChecks();
    var found = new ArrayList<CheckResult>();
    var units = 0;

    for (var target : CheckTarget.values()) {
      var targetChecks = activeChecks.stream()
        .filter(check -> appliesTo(check, target))
        .toList();
      if (targetChecks.isEmpty()) {
        continue;
      }
      units += analyze(project, target, targetChecks, found);
    }

    var issues = found.stream()
      .filter(issue -> issue.severity().atLeast(settings.minSeverity()))
      .filter(issue -> !settings.isIgnored(issue.key(), issue.location().mdoRef()))
      .sorted(ORDER)
      .toList();

    return new AnalysisResult(issues,
      new AnalysisResult.Statistics(activeChecks.size(), units, Duration.between(started, Instant.now())));
  }

  private List<MetadataCheck> activeChecks() {
    return registry.all().stream()
      .filter(check -> settings.isEnabled(CheckRegistry.metadataOf(check).key(),
        CheckRegistry.metadataOf(check).activatedByDefault()))
      .toList();
  }

  private static boolean appliesTo(MetadataCheck check, CheckTarget target) {
    for (var checkTarget : CheckRegistry.metadataOf(check).targets()) {
      if (checkTarget == target) {
        return true;
      }
    }
    return false;
  }

  private int analyze(Project project, CheckTarget target, List<MetadataCheck> checks, List<CheckResult> sink) {
    return switch (target) {
      case CONFIGURATION -> {
        for (var check : checks) {
          sink.addAll(runCheck(check, new CheckContext(project, null, null, settings.parametersFor(keyOf(check)))));
        }
        yield 1;
      }
      case MDO -> {
        var objects = project.objects();
        for (var object : objects) {
          for (var check : checks) {
            sink.addAll(runCheck(check,
              new CheckContext(project, object, null, settings.parametersFor(keyOf(check)))));
          }
        }
        yield objects.size();
      }
      case FORM -> {
        var forms = project.forms();
        for (var form : forms) {
          for (var check : checks) {
            sink.addAll(runCheck(check,
              new CheckContext(project, null, form, settings.parametersFor(keyOf(check)))));
          }
        }
        yield forms.size();
      }
      case ROLE -> {
        var roles = project.roles();
        for (var role : roles) {
          for (var check : checks) {
            sink.addAll(runCheck(check,
              new CheckContext(project, role, null, settings.parametersFor(keyOf(check)))));
          }
        }
        yield roles.size();
      }
    };
  }

  private List<CheckResult> runCheck(MetadataCheck check, CheckContext context) {
    var metadata = CheckRegistry.metadataOf(check);
    try {
      return check.check(context).stream()
        .map(result -> withSeverity(result, settings.severityFor(metadata.key(), result.severity())))
        .toList();
    } catch (RuntimeException e) {
      LOGGER.warn("Проверка {} завершилась ошибкой: {}", metadata.key(), e.getMessage(), e);
      return List.of();
    }
  }

  private static CheckResult withSeverity(CheckResult result, ru.pravets.checkushka.core.check.Severity severity) {
    if (severity == result.severity()) {
      return result;
    }
    return new CheckResult(result.key(), result.message(), severity, result.location(), result.minutesToFix());
  }

  private static String keyOf(MetadataCheck check) {
    return CheckRegistry.metadataOf(check).key();
  }
}
