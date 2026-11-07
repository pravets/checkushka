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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import ru.pravets.checkushka.core.config.ConfigTemplate;

/**
 * Создание конфигурационного файла с настройками по умолчанию.
 */
@CommandLine.Command(
  name = "init",
  description = "Создать checkushka.json с настройками по умолчанию.")
public final class ConfigInitCommand implements Callable<Integer> {

  @CommandLine.Option(
    names = {"-o", "--output"},
    paramLabel = "<файл>",
    description = "Куда записать файл (по умолчанию checkushka.json в текущем каталоге).")
  private Path output;

  @CommandLine.Option(
    names = "--force",
    description = "Перезаписать существующий файл.")
  private boolean force;

  @Override
  public Integer call() {
    var target = output != null ? output : Path.of(ConfigTemplate.FILE_NAME);
    if (Files.exists(target) && !force) {
      System.err.println("Файл уже существует: " + target + " (используйте --force)");
      return 2;
    }
    try {
      var parent = target.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(target, ConfigTemplate.render(), StandardCharsets.UTF_8);
      System.out.println("Создан " + target);
      return 0;
    } catch (IOException e) {
      System.err.println("Ошибка записи " + target + ": " + e.getMessage());
      return 2;
    }
  }
}
