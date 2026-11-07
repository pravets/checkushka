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

import java.util.concurrent.Callable;
import picocli.CommandLine;
import ru.pravets.checkushka.core.check.ParameterizedCheck;
import ru.pravets.checkushka.core.engine.CheckRegistry;

/**
 * Подробности об одной проверке: метаданные и параметры.
 */
@CommandLine.Command(
  name = "describe-check",
  description = "Показать описание проверки и её параметры.")
public final class DescribeCheckCommand implements Callable<Integer> {

  @CommandLine.Parameters(
    index = "0",
    paramLabel = "<ключ>",
    description = "Ключ проверки, например MetadataOrder.")
  private String key;

  @Override
  public Integer call() {
    var registry = CheckRegistry.builtIn();
    var check = registry.find(key).orElse(null);
    if (check == null) {
      System.err.println("Проверка не найдена: " + key);
      System.err.println("Доступные проверки: " + String.join(", ",
        registry.all().stream().map(item -> CheckRegistry.metadataOf(item).key()).sorted().toList()));
      return 2;
    }

    var metadata = CheckRegistry.metadataOf(check);
    System.out.println("Ключ:        " + metadata.key());
    System.out.println("Название:    " + metadata.name());
    System.out.println("Тип:         " + metadata.type());
    System.out.println("Важность:    " + metadata.severity());
    System.out.println("По умолчанию: " + (metadata.activatedByDefault() ? "включена" : "выключена"));
    System.out.println("Трудозатраты: " + metadata.minutesToFix() + " мин");
    System.out.println("Уровни:      " + String.join(", ",
      java.util.Arrays.stream(metadata.targets()).map(Enum::name).toList()));
    System.out.println("Теги:        " + String.join(", ",
      java.util.Arrays.stream(metadata.tags()).map(Enum::name).toList()));

    if (check instanceof ParameterizedCheck parameterized && !parameterized.parameters().isEmpty()) {
      System.out.println();
      System.out.println("Параметры:");
      for (var parameter : parameterized.parameters()) {
        System.out.printf("  %-12s %-9s по умолчанию: %-8s %s%n",
          parameter.name(), parameter.type(), parameter.defaultValue(), parameter.description());
      }
    }
    return 0;
  }
}
