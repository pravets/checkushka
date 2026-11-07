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

import picocli.CommandLine;
import ru.pravets.checkushka.core.Checkushka;

/**
 * Точка входа утилиты checkushka.
 */
@CommandLine.Command(
  name = "checkushka",
  mixinStandardHelpOptions = true,
  versionProvider = Main.VersionProvider.class,
  description = "Проверка метаданных и форм конфигураций 1С на соответствие стандартам.")
public final class Main implements Runnable {

  /**
   * Точка входа.
   *
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    System.exit(createCommandLine().execute(args));
  }

  /**
   * Собирает командную строку со всеми подкомандами.
   *
   * @return настроенная командная строка
   */
  public static CommandLine createCommandLine() {
    return new CommandLine(new Main())
      .addSubcommand(new CheckCommand());
  }

  @Override
  public void run() {
    System.out.println("checkushka " + Checkushka.version());
  }

  /**
   * Поставщик версии для параметра {@code --version}.
   */
  static final class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
      return new String[]{"checkushka " + Checkushka.version()};
    }
  }
}
