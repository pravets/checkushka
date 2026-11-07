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
package ru.pravets.checkushka.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import ru.pravets.checkushka.core.Checkushka;
import ru.pravets.checkushka.core.CheckushkaSettings;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.config.ConfigException;
import ru.pravets.checkushka.core.config.ConfigLoader;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.engine.ChecksMode;
import ru.pravets.checkushka.core.report.ReportContext;
import ru.pravets.checkushka.core.report.ReportFormat;
import ru.pravets.checkushka.core.report.Reporters;
import ru.pravets.checkushka.core.source.ProjectException;
import ru.pravets.checkushka.core.source.SourceFormat;

/**
 * Команда анализа конфигурации.
 *
 * <p>Коды возврата: {@code 0} — замечаний не ниже порога нет, {@code 1} — замечания есть,
 * {@code 2} — ошибка чтения проекта или конфигурации.
 */
@CommandLine.Command(
  name = "check",
  description = "Проверить метаданные конфигурации и вывести замечания.")
public final class CheckCommand implements Callable<Integer> {

  /** Код возврата при отсутствии замечаний. */
  public static final int EXIT_OK = 0;

  /** Код возврата при найденных замечаниях. */
  public static final int EXIT_ISSUES = 1;

  /** Код возврата при ошибке. */
  public static final int EXIT_ERROR = 2;

  @CommandLine.Parameters(
    index = "0",
    arity = "0..1",
    paramLabel = "<путь>",
    description = "Каталог проекта: выгрузка конфигуратора или проект EDT (по умолчанию текущий).")
  private Path path = Path.of(".");

  @CommandLine.Option(
    names = {"-c", "--config"},
    paramLabel = "<файл>",
    description = "Путь к checkushka.json (по умолчанию ищется в корне проекта).")
  private Path configPath;

  @CommandLine.Option(
    names = "--reports",
    split = ",",
    paramLabel = "<формат>",
    defaultValue = "console",
    description = "Форматы отчёта через запятую: console, json.")
  private List<String> reports;

  @CommandLine.Option(
    names = "--severity",
    paramLabel = "<уровень>",
    description = "Минимальная важность замечаний в отчёте: BLOCKER, CRITICAL, MAJOR, MINOR, INFO.")
  private String severity;

  @CommandLine.Option(
    names = "--checks",
    split = ",",
    paramLabel = "<ключ>",
    description = "Ограничить запуск перечисленными проверками.")
  private List<String> checks;

  @CommandLine.Option(
    names = "--format",
    paramLabel = "<формат>",
    description = "Формат выгрузки: auto, designer, edt.")
  private String format;

  @CommandLine.Option(
    names = "--fail-on",
    paramLabel = "<уровень>",
    defaultValue = "MINOR",
    description = "Порог важности, при котором команда возвращает код 1.")
  private String failOn;

  @Override
  public Integer call() {
    try {
      var root = path.toAbsolutePath().normalize();
      var config = new ConfigLoader().load(root, configPath);
      var base = config.checks();
      var limited = checks != null && !checks.isEmpty();

      var settings = new CheckSettings(
        limited ? ChecksMode.ONLY : base.mode(),
        limited ? Set.copyOf(checks) : base.checkKeys(),
        base.parameters(),
        base.severityOverrides(),
        Severity.parse(severity, base.minSeverity()),
        base.ignore());

      var result = Checkushka
        .with(new CheckushkaSettings(configPath, SourceFormat.parse(format, null), settings))
        .analyze(root);

      var out = System.out;
      for (var report : reports) {
        Reporters.create(ReportFormat.parse(report, ReportFormat.CONSOLE))
          .report(result, new ReportContext(root, out));
      }

      return result.countAtLeast(Severity.parse(failOn, Severity.MINOR)) > 0 ? EXIT_ISSUES : EXIT_OK;
    } catch (ConfigException | ProjectException | IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return EXIT_ERROR;
    }
  }
}
