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
package ru.pravets.checkushka.core.check.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.pravets.checkushka.core.check.CheckParameters;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckRegistry;
import ru.pravets.checkushka.core.engine.CheckRunner;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.engine.ChecksMode;
import ru.pravets.checkushka.core.engine.IgnoreRule;
import ru.pravets.checkushka.core.source.ProjectLoader;

/**
 * Сквозная проверка на минимальных выгрузках обоих форматов: загрузчик, модель и проверка вместе.
 */
class YoLetterInMetadataProjectTest {

  @Test
  void findsLetterInNameSynonymAndContainedObjectOfDesignerDump() throws URISyntaxException {
    var issues = issues(dump("designer-yo"), CheckSettings.defaults());

    assertThat(issues).hasSize(3);
    assertThat(issues).extracting(CheckResult::message)
      .containsExactlyInAnyOrder(
        "Имя объекта \"Ёлка\" содержит букву \"ё\"",
        "Синоним (en) объекта \"Ёлка\" содержит букву \"ё\"",
        "Имя объекта \"СтавкаЁ\" содержит букву \"ё\"");
    assertThat(issues).allSatisfy(issue -> {
      assertThat(issue.location().line()).isNull();
      assertThat(issue.location().file().getFileName().toString()).isEqualTo("Ёлка.xml");
    });
    assertThat(issues).extracting(issue -> issue.location().mdoRef())
      .contains("Catalog.Ёлка.Attribute.СтавкаЁ");
  }

  @Test
  void findsLetterInContainedObjects() throws URISyntaxException {
    var issues = issues(dump("edt-yo"), CheckSettings.defaults());

    assertThat(issues).hasSize(6);
    assertThat(issues).extracting(issue -> issue.location().mdoRef())
      .containsExactlyInAnyOrder(
        "Catalog.Ёлка",
        "Catalog.Ёлка",
        "Catalog.Ёлка.Attribute.СтавкаЁ",
        "Catalog.Ёлка.TabularSection.Товары.Attribute.Цена",
        "Catalog.Ёлка.Command.СоздатьЁ",
        "Catalog.Ёлка.Template.МакетЁ");
    assertThat(issues).extracting(CheckResult::message)
      .containsExactlyInAnyOrder(
        "Имя объекта \"Ёлка\" содержит букву \"ё\"",
        "Синоним (en) объекта \"Ёлка\" содержит букву \"ё\"",
        "Имя объекта \"СтавкаЁ\" содержит букву \"ё\"",
        "Синоним (ru) объекта \"Цена\" содержит букву \"ё\"",
        "Имя объекта \"СоздатьЁ\" содержит букву \"ё\"",
        "Имя объекта \"МакетЁ\" содержит букву \"ё\"");
  }

  @Test
  void commentIsCheckedWhenConfigured() throws URISyntaxException {
    var settings = settings(
      Map.of("properties", List.of("name", "synonym", "comment")), List.of());

    var issues = issues(dump("edt-yo"), settings);

    assertThat(issues).hasSize(7);
    assertThat(issues).extracting(CheckResult::message)
      .contains("Комментарий объекта \"Ёлка\" содержит букву \"ё\"");
  }

  @Test
  void commentOnlyConfigChecksOnlyComment() throws URISyntaxException {
    var settings = settings(Map.of("properties", List.of("comment")), List.of());

    var issues = issues(dump("edt-yo"), settings);

    assertThat(issues).singleElement().satisfies(issue -> {
      assertThat(issue.message()).isEqualTo("Комментарий объекта \"Ёлка\" содержит букву \"ё\"");
      assertThat(issue.location().mdoRef()).isEqualTo("Catalog.Ёлка");
    });
  }

  @Test
  void topLevelScopeSkipsContainedObjects() throws URISyntaxException {
    var settings = settings(Map.of("scope", "top-level"), List.of());

    var issues = issues(dump("edt-yo"), settings);

    assertThat(issues).hasSize(2);
    assertThat(issues).extracting(issue -> issue.location().mdoRef())
      .containsOnly("Catalog.Ёлка");
  }

  @Test
  void languageFilterLimitsSynonym() throws URISyntaxException {
    var settings = settings(Map.of("languages", List.of("ru")), List.of());

    var issues = issues(dump("edt-yo"), settings);

    assertThat(issues).hasSize(5);
    assertThat(issues).extracting(CheckResult::message)
      .doesNotContain("Синоним (en) объекта \"Ёлка\" содержит букву \"ё\"");
  }

  @Test
  void ignoreRuleByNestedMdoRefRemovesOnlyThatFinding() throws URISyntaxException {
    var settings = settings(Map.of(), List.of(
      new IgnoreRule(YoLetterInMetadataCheck.KEY, "Catalog.Ёлка.Attribute.*", "тест")));

    var issues = issues(dump("edt-yo"), settings);

    assertThat(issues).hasSize(5);
    assertThat(issues).extracting(issue -> issue.location().mdoRef())
      .doesNotContain("Catalog.Ёлка.Attribute.СтавкаЁ");
  }

  private static CheckSettings settings(Map<String, Object> parameters, List<IgnoreRule> ignore) {
    return new CheckSettings(ChecksMode.ON, Set.of(),
      Map.of(YoLetterInMetadataCheck.KEY, new CheckParameters(parameters)),
      Map.of(), Severity.INFO, ignore, Set.of());
  }

  private static List<CheckResult> issues(Path root, CheckSettings settings) {
    var project = new ProjectLoader().load(root);
    return new CheckRunner(CheckRegistry.builtIn(), settings).run(project).issues().stream()
      .filter(issue -> YoLetterInMetadataCheck.KEY.equals(issue.key()))
      .toList();
  }

  private static Path dump(String name) throws URISyntaxException {
    return Path.of(Objects.requireNonNull(
      YoLetterInMetadataProjectTest.class.getResource("/dumps/" + name)).toURI());
  }
}
