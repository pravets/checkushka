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

import com.github._1c_syntax.bsl.mdo.Catalog;
import com.github._1c_syntax.bsl.mdo.MD;
import com.github._1c_syntax.bsl.mdo.children.ObjectAttribute;
import com.github._1c_syntax.bsl.mdo.children.ObjectCommand;
import com.github._1c_syntax.bsl.mdo.children.ObjectTabularSection;
import com.github._1c_syntax.bsl.mdo.children.ObjectTemplate;
import com.github._1c_syntax.bsl.types.MdoReference;
import com.github._1c_syntax.bsl.types.MultiLanguageString;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.pravets.checkushka.core.check.CheckParameter;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;

class YoLetterInMetadataCheckTest {

  private static final Path FILE = Path.of("Catalogs", "Ёлка", "Ёлка.xml");

  @Test
  void findsLetterInTopLevelName() {
    var catalog = catalog("Ёлка");

    var results = analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES);

    assertThat(results).singleElement().satisfies(issue -> {
      assertThat(issue.key()).isEqualTo(YoLetterInMetadataCheck.KEY);
      assertThat(issue.severity()).isEqualTo(Severity.MINOR);
      assertThat(issue.message()).isEqualTo("Имя объекта \"Ёлка\" содержит букву \"ё\"");
      assertThat(issue.location().file()).isEqualTo(FILE);
      assertThat(issue.location().line()).isNull();
      assertThat(issue.location().mdoRef()).isEqualTo("Catalog.Ёлка");
    });
  }

  @Test
  void findsUppercaseLetterInSynonymOfAnyLanguage() {
    var catalog = Catalog.builder()
      .name("Подарки")
      .mdoReference(ref("Catalog.Подарки"))
      .synonym(MultiLanguageString.create("en", "Gift Ё"))
      .build();

    var results = analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES);

    assertThat(results).singleElement().satisfies(issue ->
      assertThat(issue.message())
        .isEqualTo("Синоним (en) объекта \"Подарки\" содержит букву \"ё\""));
  }

  @Test
  void checksEachSynonymLanguageSeparately() {
    var synonym = MultiLanguageString.create(Set.of(
      MultiLanguageString.Entry.create("ru", "Ёлка"),
      MultiLanguageString.Entry.create("en", "Ёлка")));
    var catalog = Catalog.builder()
      .name("Дерево")
      .mdoReference(ref("Catalog.Дерево"))
      .synonym(synonym)
      .build();

    var results = analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(issue -> issue.message())
      .containsExactly(
        "Синоним (en) объекта \"Дерево\" содержит букву \"ё\"",
        "Синоним (ru) объекта \"Дерево\" содержит букву \"ё\"");
  }

  @Test
  void commentIsSkippedByDefault() {
    var catalog = Catalog.builder()
      .name("Подарки")
      .mdoReference(ref("Catalog.Подарки"))
      .comment("комментарий с ё")
      .build();

    assertThat(analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES)).isEmpty();
  }

  @Test
  void commentIsCheckedWhenConfigured() {
    var catalog = Catalog.builder()
      .name("Подарки")
      .mdoReference(ref("Catalog.Подарки"))
      .comment("комментарий с ё")
      .build();

    var results = analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      List.of("name", "comment"));

    assertThat(results).singleElement().satisfies(issue -> {
      assertThat(issue.message())
        .isEqualTo("Комментарий объекта \"Подарки\" содержит букву \"ё\"");
      assertThat(issue.location().mdoRef()).isEqualTo("Catalog.Подарки");
    });
  }

  @Test
  void commentOnlyConfigChecksOnlyComment() {
    var catalog = Catalog.builder()
      .name("Ёлка")
      .mdoReference(ref("Catalog.Ёлка"))
      .synonym(MultiLanguageString.create("ru", "Ёлка"))
      .comment("комментарий с ё")
      .build();

    var results = analyze(catalog, options(List.of("ё"), List.of("comment"), "all", false, List.of()));

    assertThat(results).singleElement().satisfies(issue ->
      assertThat(issue.message())
        .isEqualTo("Комментарий объекта \"Ёлка\" содержит букву \"ё\""));
  }

  @Test
  void checksContainedObjects() {
    var catalog = Catalog.builder()
      .name("Подарки")
      .mdoReference(ref("Catalog.Подарки"))
      .attribute(ObjectAttribute.builder()
        .name("СтавкаЁ")
        .mdoReference(ref("Catalog.Подарки.Attribute.СтавкаЁ"))
        .build())
      .tabularSection(ObjectTabularSection.builder()
        .name("Товары")
        .mdoReference(ref("Catalog.Подарки.TabularSection.Товары"))
        .attribute(ObjectAttribute.builder()
          .name("Цена")
          .synonym(MultiLanguageString.create("ru", "Цена ё"))
          .mdoReference(ref("Catalog.Подарки.TabularSection.Товары.Attribute.Цена"))
          .build())
        .build())
      .command(ObjectCommand.builder()
        .name("СоздатьЁ")
        .mdoReference(ref("Catalog.Подарки.Command.СоздатьЁ"))
        .build())
      .template(ObjectTemplate.builder()
        .name("МакетЁ")
        .mdoReference(ref("Catalog.Подарки.Template.МакетЁ"))
        .build())
      .build();

    var results = analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES);

    assertThat(results).extracting(issue -> issue.location().mdoRef())
      .containsExactlyInAnyOrder(
        "Catalog.Подарки.Attribute.СтавкаЁ",
        "Catalog.Подарки.TabularSection.Товары.Attribute.Цена",
        "Catalog.Подарки.Command.СоздатьЁ",
        "Catalog.Подарки.Template.МакетЁ");
    assertThat(results).allSatisfy(issue -> assertThat(issue.location().line()).isNull());
  }

  @Test
  void topLevelScopeSkipsContainedObjects() {
    var catalog = catalogWithAttribute("СтавкаЁ");

    assertThat(analyze(catalog, options(List.of("ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES,
      "top-level", false, List.of()))).isEmpty();
    assertThat(analyze(catalog, options(List.of("ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES,
      "all", false, List.of()))).hasSize(1);
  }

  @Test
  void unknownScopeFallsBackToAll() {
    var catalog = catalogWithAttribute("СтавкаЁ");

    assertThat(analyze(catalog, options(List.of("ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES,
      "everything", false, List.of()))).hasSize(1);
  }

  @Test
  void caseSensitiveMatchesOnlyConfiguredCase() {
    var lower = options(List.of("ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES, "all", true, List.of());

    assertThat(analyze(catalog("Ёлка"), lower)).isEmpty();
    assertThat(analyze(catalog("ёлка"), lower)).singleElement().satisfies(issue ->
      assertThat(issue.message()).isEqualTo("Имя объекта \"ёлка\" содержит букву \"ё\""));

    var upper = options(List.of("Ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES, "all", true, List.of());

    assertThat(analyze(catalog("Ёлка"), upper)).singleElement();
    assertThat(analyze(catalog("ёлка"), upper)).isEmpty();
  }

  @Test
  void languageFilterLimitsSynonym() {
    var synonym = MultiLanguageString.create(Set.of(
      MultiLanguageString.Entry.create("ru", "Ёлка"),
      MultiLanguageString.Entry.create("en", "Gift Ё")));
    var catalog = Catalog.builder()
      .name("Дерево")
      .mdoReference(ref("Catalog.Дерево"))
      .synonym(synonym)
      .build();

    var russian = analyze(catalog, options(List.of("ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES,
      "all", false, List.of("RU")));

    assertThat(russian).singleElement().satisfies(issue ->
      assertThat(issue.message())
        .isEqualTo("Синоним (ru) объекта \"Дерево\" содержит букву \"ё\""));
    assertThat(analyze(catalog, options(List.of("ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES,
      "all", false, List.of()))).hasSize(2);
  }

  @Test
  void customLettersReplaceDefault() {
    var catalog = catalog("Ёлка");

    assertThat(analyze(catalog, List.of("ъ"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES))
      .isEmpty();

    var withLetter = analyze(catalog("Съезд"), List.of("ъ"),
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES);
    assertThat(withLetter).singleElement().satisfies(issue ->
      assertThat(issue.message()).isEqualTo("Имя объекта \"Съезд\" содержит букву \"ъ\""));
  }

  @Test
  void matchesLettersCaseInsensitively() {
    var catalog = catalog("ёлка");

    var results = analyze(catalog, List.of("Ё"), YoLetterInMetadataCheck.DEFAULT_PROPERTIES);

    assertThat(results).singleElement().satisfies(issue ->
      assertThat(issue.message()).isEqualTo("Имя объекта \"ёлка\" содержит букву \"ё\""));
  }

  @Test
  void emptyLettersOrPropertiesProduceNothing() {
    var catalog = catalog("Ёлка");

    assertThat(analyze(catalog, List.of(), YoLetterInMetadataCheck.DEFAULT_PROPERTIES))
      .isEmpty();
    assertThat(analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS, List.of()))
      .isEmpty();
  }

  @Test
  void unknownPropertiesAreIgnored() {
    var catalog = catalog("Ёлка");

    assertThat(analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS, List.of("synonyms")))
      .isEmpty();
  }

  @Test
  void cleanObjectProducesNothing() {
    var catalog = catalog("Елка");

    assertThat(analyze(catalog, YoLetterInMetadataCheck.DEFAULT_LETTERS,
      YoLetterInMetadataCheck.DEFAULT_PROPERTIES)).isEmpty();
  }

  @Test
  void parametersDescribeAllOptions() {
    var parameters = new YoLetterInMetadataCheck().parameters();

    assertThat(parameters).extracting(CheckParameter::name)
      .containsExactly("letters", "properties", "scope", "caseSensitive", "languages");
    assertThat(parameters.get(0).defaultValue()).isEqualTo("[\"ё\"]");
    assertThat(parameters.get(1).defaultValue()).isEqualTo("[\"name\", \"synonym\"]");
    assertThat(parameters.get(2).defaultValue()).isEqualTo("\"all\"");
    assertThat(parameters.get(3).defaultValue()).isEqualTo("false");
    assertThat(parameters.get(4).defaultValue()).isEqualTo("[]");
  }

  private static Catalog catalog(String name) {
    return Catalog.builder()
      .name(name)
      .mdoReference(ref("Catalog." + name))
      .build();
  }

  private static Catalog catalogWithAttribute(String attributeName) {
    return Catalog.builder()
      .name("Подарки")
      .mdoReference(ref("Catalog.Подарки"))
      .attribute(ObjectAttribute.builder()
        .name(attributeName)
        .mdoReference(ref("Catalog.Подарки.Attribute." + attributeName))
        .build())
      .build();
  }

  private static YoLetterInMetadataCheck.Options options(List<String> letters, List<String> properties,
                                                         String scope, boolean caseSensitive,
                                                         List<String> languages) {
    return new YoLetterInMetadataCheck.Options(letters, properties, scope, caseSensitive, languages);
  }

  private static List<CheckResult> analyze(MD object, List<String> letters, List<String> properties) {
    return YoLetterInMetadataCheck.analyze(object, letters, properties, FILE);
  }

  private static List<CheckResult> analyze(MD object, YoLetterInMetadataCheck.Options options) {
    return YoLetterInMetadataCheck.analyze(object, options, FILE);
  }

  private static MdoReference ref(String value) {
    return MdoReference.create(value);
  }
}
