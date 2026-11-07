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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import ru.pravets.checkushka.core.engine.CheckRegistry;

/**
 * Список доступных проверок.
 */
@CommandLine.Command(
  name = "list-checks",
  description = "Показать доступные проверки.")
public final class ListChecksCommand implements Callable<Integer> {

  @CommandLine.Option(
    names = "--json",
    description = "Вывести в формате JSON.")
  private boolean json;

  @Override
  public Integer call() throws Exception {
    var registry = CheckRegistry.builtIn();
    if (json) {
      var mapper = new ObjectMapper();
      ArrayNode array = mapper.createArrayNode();
      for (var check : registry.all()) {
        var metadata = CheckRegistry.metadataOf(check);
        var node = array.addObject();
        node.put("key", metadata.key());
        node.put("name", metadata.name());
        node.put("type", metadata.type().name());
        node.put("severity", metadata.severity().name());
        node.put("activatedByDefault", metadata.activatedByDefault());
        var targets = node.putArray("targets");
        for (var target : metadata.targets()) {
          targets.add(target.name());
        }
      }
      System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(array));
      return 0;
    }

    System.out.printf("%-20s %-10s %-16s %s%n", "КЛЮЧ", "ВАЖНОСТЬ", "ТИП", "НАЗВАНИЕ");
    for (var check : registry.all()) {
      var metadata = CheckRegistry.metadataOf(check);
      System.out.printf("%-20s %-10s %-16s %s%n",
        metadata.key(), metadata.severity(), metadata.type(), metadata.name());
    }
    return 0;
  }
}
