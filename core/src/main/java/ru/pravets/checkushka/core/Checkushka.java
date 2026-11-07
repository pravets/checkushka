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

import java.nio.file.Path;
import java.util.Objects;
import ru.pravets.checkushka.core.config.ConfigLoader;
import ru.pravets.checkushka.core.engine.CheckRegistry;
import ru.pravets.checkushka.core.engine.CheckRunner;
import ru.pravets.checkushka.core.source.ProjectLoader;

/**
 * Фасад библиотеки checkushka.
 *
 * <p>Точка входа для встраивания проверок в другие инструменты: CLI, MCP-сервер, плагин SonarQube,
 * LSP-адаптер. Анализ одного проекта потокобезопасен, если не переиспользовать один экземпляр
 * настроек одновременно с изменением конфигурационных файлов.
 */
public final class Checkushka {

  private final CheckushkaSettings settings;

  private Checkushka(CheckushkaSettings settings) {
    this.settings = settings;
  }

  /**
   * Создаёт фасад с настройками по умолчанию.
   *
   * @return фасад
   */
  public static Checkushka create() {
    return new Checkushka(CheckushkaSettings.defaults());
  }

  /**
   * Создаёт фасад с настройками.
   *
   * @param settings настройки запуска
   * @return фасад
   */
  public static Checkushka with(CheckushkaSettings settings) {
    return new Checkushka(Objects.requireNonNull(settings, "settings"));
  }

  /**
   * Возвращает версию библиотеки.
   *
   * @return версия из манифеста, либо {@code dev}, если манифест недоступен
   */
  public static String version() {
    return Version.current();
  }

  /**
   * Анализирует проект.
   *
   * @param projectRoot каталог проекта: выгрузка конфигуратора или проект EDT
   * @return результат анализа
   */
  public AnalysisResult analyze(Path projectRoot) {
    var config = new ConfigLoader().load(projectRoot, settings.configPath());
    var checks = settings.checks() != null ? settings.checks() : config.checks();
    var format = settings.format() != null ? settings.format() : config.format();
    var project = new ProjectLoader().load(projectRoot, format);
    return new CheckRunner(CheckRegistry.builtIn(), checks).run(project);
  }
}
