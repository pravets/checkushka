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
package ru.pravets.checkushka.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import ru.pravets.checkushka.core.Checkushka;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.ParameterizedCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckRegistry;

/**
 * Инструменты MCP-сервера checkushka.
 *
 * <p>Все инструменты анализа помечены как read-only: клиент не должен запрашивать подтверждение
 * на каждый вызов. Инструменты управления рабочими папками меняют состояние сервера.
 */
public final class ToolFactory {

  private final WorkspaceRegistry registry;

  /**
   * Создаёт фабрику инструментов.
   *
   * @param registry реестр рабочих папок
   */
  public ToolFactory(WorkspaceRegistry registry) {
    this.registry = registry;
  }

  /**
   * Полный набор инструментов.
   *
   * @return описания инструментов для MCP
   */
  public List<McpServerFeatures.SyncToolSpecification> tools() {
    return List.of(
      listWorkspaceFolders(),
      registerWorkspaceFolder(),
      unregisterWorkspaceFolder(),
      analyzeConfiguration(),
      listChecks(),
      describeCheck(),
      listMetadataObjects(),
      getMetadataObject());
  }

  private McpServerFeatures.SyncToolSpecification listWorkspaceFolders() {
    return tool("list_workspace_folders", "Зарегистрированные рабочие папки с разобранными проектами.",
      Map.of(), List.of(), true,
      (args, workspaces) -> workspaces.list().stream()
        .map(workspace -> Map.of(
          "uri", workspace.uri(),
          "name", workspace.name(),
          "format", formatOf(workspace)))
        .toList());
  }

  private McpServerFeatures.SyncToolSpecification registerWorkspaceFolder() {
    return tool("register_workspace_folder",
      "Зарегистрировать каталог проекта (выгрузка конфигуратора или проект EDT) и разобрать метаданные.",
      Map.of(
        "path", property("string", "Каталог проекта"),
        "name", property("string", "Имя рабочей папки (по умолчанию — имя каталога)")),
      List.of("path"), false,
      (args, workspaces) -> {
        var workspace = workspaces.register(Path.of(require(args, "path")), string(args, "name"));
        return Map.of("uri", workspace.uri(), "name", workspace.name(), "format", formatOf(workspace));
      });
  }

  private McpServerFeatures.SyncToolSpecification unregisterWorkspaceFolder() {
    return tool("unregister_workspace_folder",
      "Удалить рабочую папку и освободить разобранную модель.",
      Map.of("uri", property("string", "Идентификатор рабочей папки")),
      List.of("uri"), false,
      (args, workspaces) -> {
        var uri = require(args, "uri");
        return Map.of("uri", uri, "removed", workspaces.unregister(uri));
      });
  }

  private McpServerFeatures.SyncToolSpecification analyzeConfiguration() {
    return tool("analyze_configuration",
      "Проверить метаданные зарегистрированного проекта и вернуть замечания.",
      Map.of(
        "workspaceFolder", property("string", "Идентификатор рабочей папки"),
        "checks", arrayProperty("Ключи проверок; пусто — все включённые"),
        "minSeverity", property("string", "Минимальная важность: BLOCKER..INFO"),
        "limit", property("integer", "Сколько замечаний вернуть (по умолчанию 200)")),
      List.of("workspaceFolder"), true,
      (args, workspaces) -> {
        var workspace = workspaces.require(require(args, "workspaceFolder"));
        var checks = strings(args, "checks");
        var minSeverity = Severity.parse(string(args, "minSeverity"), Severity.INFO);
        var limit = number(args, "limit", 200);

        var result = Checkushka.create().analyze(workspace.path());
        var issues = result.issues().stream()
          .filter(issue -> checks.isEmpty() || checks.contains(issue.key()))
          .filter(issue -> issue.severity().atLeast(minSeverity))
          .limit(limit)
          .map(ToolFactory::issueJson)
          .toList();

        return Map.of(
          "summary", Map.of(
            "issues", result.issues().size(),
            "returned", issues.size(),
            "checksRun", result.statistics().checksRun(),
            "objectsAnalyzed", result.statistics().objectsAnalyzed(),
            "durationMs", result.statistics().duration().toMillis()),
          "issues", issues);
      });
  }

  private McpServerFeatures.SyncToolSpecification listChecks() {
    return tool("list_checks", "Доступные проверки с ключами, важностью и уровнями применения.",
      Map.of(), List.of(), true,
      (args, workspaces) -> CheckRegistry.builtIn().all().stream()
        .map(ToolFactory::checkJson)
        .toList());
  }

  private McpServerFeatures.SyncToolSpecification describeCheck() {
    return tool("describe_check", "Описание проверки: метаданные и параметры.",
      Map.of("key", property("string", "Ключ проверки, например MetadataOrder")),
      List.of("key"), true,
      (args, workspaces) -> {
        var key = require(args, "key");
        var check = CheckRegistry.builtIn().find(key).orElseThrow(() -> new IllegalArgumentException(
          "Проверка не найдена: " + key + "; доступны: " + availableChecks()));
        var metadata = CheckRegistry.metadataOf(check);
        var result = new LinkedHashMap<String, Object>(checkJson(check));
        if (check instanceof ParameterizedCheck parameterized) {
          result.put("parameters", parameterized.parameters().stream()
            .map(parameter -> Map.of(
              "name", parameter.name(),
              "type", parameter.type(),
              "default", parameter.defaultValue(),
              "description", parameter.description()))
            .toList());
        }
        return result;
      });
  }

  private McpServerFeatures.SyncToolSpecification listMetadataObjects() {
    return tool("list_metadata_objects",
      "Объекты метаданных проекта: фильтр по типу и подстроке имени.",
      Map.of(
        "workspaceFolder", property("string", "Идентификатор рабочей папки"),
        "type", property("string", "Тип метаданных, например Catalog"),
        "query", property("string", "Подстрока имени"),
        "limit", property("integer", "Сколько объектов вернуть (по умолчанию 100)")),
      List.of("workspaceFolder"), true,
      (args, workspaces) -> {
        var workspace = workspaces.require(require(args, "workspaceFolder"));
        var type = string(args, "type");
        var query = string(args, "query");
        var limit = number(args, "limit", 100);
        return workspace.project().objects().stream()
          .filter(object -> type == null || object.getMdoType().name().equalsIgnoreCase(type))
          .filter(object -> query == null || object.getName().toLowerCase(java.util.Locale.ROOT)
            .contains(query.toLowerCase(java.util.Locale.ROOT)))
          .limit(limit)
          .map(object -> Map.of(
            "mdoRef", object.getMdoRef(),
            "name", object.getName(),
            "type", object.getMdoType().name()))
          .toList();
      });
  }

  private McpServerFeatures.SyncToolSpecification getMetadataObject() {
    return tool("get_metadata_object", "Свойства объекта метаданных по ссылке.",
      Map.of(
        "workspaceFolder", property("string", "Идентификатор рабочей папки"),
        "mdoRef", property("string", "Ссылка вида Catalog.Товары")),
      List.of("workspaceFolder", "mdoRef"), true,
      (args, workspaces) -> {
        var workspace = workspaces.require(require(args, "workspaceFolder"));
        var ref = require(args, "mdoRef");
        var object = workspace.project().objects().stream()
          .filter(item -> item.getMdoRef().equalsIgnoreCase(ref))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Объект не найден: " + ref));
        var result = new LinkedHashMap<String, Object>();
        result.put("mdoRef", object.getMdoRef());
        result.put("name", object.getName());
        result.put("type", object.getMdoType().name());
        result.put("synonym", object.getSynonym() == null ? "" : object.getSynonym().getAny());
        result.put("comment", object.getComment());
        return result;
      });
  }

  private McpServerFeatures.SyncToolSpecification tool(String name, String description,
                                                       Map<String, Object> properties, List<String> required,
                                                       boolean readOnly,
                                                       BiFunction<Map<String, Object>, WorkspaceRegistry, Object> handler) {
    var schema = new LinkedHashMap<String, Object>();
    schema.put("type", "object");
    schema.put("properties", properties);
    if (!required.isEmpty()) {
      schema.put("required", required);
    }

    var tool = McpSchema.Tool.builder()
      .name(name)
      .description(description)
      .inputSchema(schema)
      .annotations(McpSchema.ToolAnnotations.builder().readOnlyHint(readOnly).build())
      .build();

    return McpServerFeatures.SyncToolSpecification.builder()
      .tool(tool)
      .callHandler((exchange, request) -> invoke(handler, request.arguments()))
      .build();
  }

  private McpSchema.CallToolResult invoke(
    BiFunction<Map<String, Object>, WorkspaceRegistry, Object> handler, Map<String, Object> arguments) {
    try {
      return McpSchema.CallToolResult.builder()
        .addTextContent(JsonText.pretty(handler.apply(arguments == null ? Map.of() : arguments, registry)))
        .build();
    } catch (RuntimeException e) {
      var message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      return McpSchema.CallToolResult.builder().isError(true).addTextContent(message).build();
    }
  }

  private static Map<String, Object> issueJson(CheckResult issue) {
    var node = new LinkedHashMap<String, Object>();
    node.put("key", issue.key());
    node.put("message", issue.message());
    node.put("severity", issue.severity().name());
    node.put("file", issue.location().file().toString());
    if (issue.location().line() != null) {
      node.put("line", issue.location().line());
    }
    if (issue.location().mdoRef() != null) {
      node.put("mdoRef", issue.location().mdoRef());
    }
    return node;
  }

  private static Map<String, Object> checkJson(ru.pravets.checkushka.core.check.MetadataCheck check) {
    CheckMetadata metadata = CheckRegistry.metadataOf(check);
    var node = new LinkedHashMap<String, Object>();
    node.put("key", metadata.key());
    node.put("name", metadata.name());
    node.put("type", metadata.type().name());
    node.put("severity", metadata.severity().name());
    node.put("activatedByDefault", metadata.activatedByDefault());
    node.put("targets", java.util.Arrays.stream(metadata.targets()).map(Enum::name).toList());
    return node;
  }

  private static String availableChecks() {
    return String.join(", ", CheckRegistry.builtIn().all().stream()
      .map(check -> CheckRegistry.metadataOf(check).key()).sorted().toList());
  }

  private static String formatOf(WorkspaceRegistry.Workspace workspace) {
    return workspace.project() == null ? "unknown" : workspace.project().format().name();
  }

  private static Map<String, Object> property(String type, String description) {
    return Map.of("type", type, "description", description);
  }

  private static Map<String, Object> arrayProperty(String description) {
    return Map.of("type", "array", "items", Map.of("type", "string"), "description", description);
  }

  private static String require(Map<String, Object> arguments, String name) {
    var value = string(arguments, name);
    if (value == null) {
      throw new IllegalArgumentException("Не указан обязательный параметр: " + name);
    }
    return value;
  }

  private static String string(Map<String, Object> arguments, String name) {
    var value = arguments.get(name);
    return value instanceof String text && !text.isBlank() ? text : null;
  }

  private static List<String> strings(Map<String, Object> arguments, String name) {
    var value = arguments.get(name);
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }

  private static int number(Map<String, Object> arguments, String name, int defaultValue) {
    var value = arguments.get(name);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }
}
