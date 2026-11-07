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
package ru.pravets.checkushka.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.engine.ChecksMode;
import ru.pravets.checkushka.core.engine.IgnoreRule;
import ru.pravets.checkushka.core.source.SourceFormat;

/**
 * Чтение конфигурационного файла {@code checkushka.json}.
 *
 * <p>Файл ищется по явно указанному пути, затем в корне проекта. Если файла нет, применяются
 * настройки по умолчанию. Неизвестные ключи игнорируются, ошибки разбора сообщаются понятным
 * текстом.
 */
public final class ConfigLoader {

  private static final String FILE_NAME = "checkushka.json";

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Загружает конфигурацию.
   *
   * @param projectRoot    корень проекта
   * @param explicitConfig явный путь к файлу или {@code null}
   * @return конфигурация
   * @throws ConfigException если файл найден, но не разбирается
   */
  public CheckushkaConfig load(Path projectRoot, Path explicitConfig) {
    var file = explicitConfig != null ? explicitConfig : projectRoot.resolve(FILE_NAME);
    if (!Files.isRegularFile(file)) {
      return CheckushkaConfig.defaults();
    }

    JsonNode root;
    try {
      root = mapper.readTree(Files.readString(file));
    } catch (IOException e) {
      throw new ConfigException("Не удалось прочитать " + file + ": " + e.getMessage(), e);
    }
    if (root == null || !root.isObject()) {
      throw new ConfigException("Ожидался JSON-объект в " + file);
    }

    var language = text(root, "language", "ru");
    var format = SourceFormat.valueOf(
      text(root.path("source"), "format", "auto").toUpperCase(java.util.Locale.ROOT));
    var checks = root.path("checks");
    var settings = new CheckSettings(
      ChecksMode.parse(text(checks, "mode", "on"), ChecksMode.ON),
      keys(checks.path("keys")),
      parameters(checks.path("parameters")),
      severities(checks.path("severity")),
      Severity.parse(text(checks, "minSeverity", "info"), Severity.INFO),
      ignoreRules(root.path("ignore")));

    return new CheckushkaConfig(language, format, settings);
  }

  private static String text(JsonNode node, String field, String defaultValue) {
    var value = node.get(field);
    return value != null && value.isTextual() ? value.asText() : defaultValue;
  }

  private static Set<String> keys(JsonNode node) {
    var result = new LinkedHashSet<String>();
    if (node.isArray()) {
      node.forEach(item -> {
        if (item.isTextual()) {
          result.add(item.asText());
        }
      });
    }
    return result;
  }

  private static Map<String, CheckParameters> parameters(JsonNode node) {
    var result = new LinkedHashMap<String, CheckParameters>();
    if (!node.isObject()) {
      return result;
    }
    var fields = node.fields();
    while (fields.hasNext()) {
      var entry = fields.next();
      if (entry.getValue().isObject()) {
        var values = new LinkedHashMap<String, Object>();
        var nested = entry.getValue().fields();
        while (nested.hasNext()) {
          var parameter = nested.next();
          values.put(parameter.getKey(), toValue(parameter.getValue()));
        }
        result.put(entry.getKey(), new CheckParameters(values));
      }
    }
    return result;
  }

  private static Object toValue(JsonNode node) {
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isNumber()) {
      return node.numberValue();
    }
    if (node.isArray()) {
      var values = new ArrayList<String>();
      node.forEach(item -> {
        if (item.isTextual()) {
          values.add(item.asText());
        }
      });
      return values;
    }
    return node.asText();
  }

  private static Map<String, Severity> severities(JsonNode node) {
    var result = new LinkedHashMap<String, Severity>();
    if (!node.isObject()) {
      return result;
    }
    var fields = node.fields();
    while (fields.hasNext()) {
      var entry = fields.next();
      if (entry.getValue().isTextual()) {
        result.put(entry.getKey(), Severity.parse(entry.getValue().asText(), Severity.MINOR));
      }
    }
    return result;
  }

  private static List<IgnoreRule> ignoreRules(JsonNode node) {
    var result = new ArrayList<IgnoreRule>();
    if (!node.isArray()) {
      return result;
    }
    for (var item : node) {
      if (!item.isObject()) {
        continue;
      }
      result.add(new IgnoreRule(
        text(item, "check", null),
        text(item, "mdo", null),
        text(item, "reason", "")));
    }
    return result;
  }
}
