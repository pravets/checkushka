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

import com.github._1c_syntax.bsl.mdo.Attribute;
import com.github._1c_syntax.bsl.mdo.Document;
import com.github._1c_syntax.bsl.mdo.children.ObjectAttribute;
import com.github._1c_syntax.bsl.types.MdoReference;
import com.github._1c_syntax.bsl.types.ValueTypeDescription;
import com.github._1c_syntax.bsl.types.qualifiers.StringQualifiers;
import com.github._1c_syntax.bsl.types.value.PrimitiveValueType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.CheckResult;

class DocumentCommentAttributeCheckTest {

  private static final String QUOTE = Character.toString('"');

  private static final String FULL_REQUIREMENT =
    "должен быть строкой неограниченной длины с включённым многострочным режимом";

  @TempDir
  private Path dir;

  @Test
  void reportsMissingAttribute() {
    var results = analyze(document("Заказ"));

    assertThat(results).singleElement().satisfies(result -> {
      assertThat(result.key()).isEqualTo(DocumentCommentAttributeCheck.KEY);
      assertThat(result.message()).isEqualTo(
        "У документа " + QUOTE + "Заказ" + QUOTE + " нет реквизита " + QUOTE + "Комментарий" + QUOTE);
      assertThat(result.location().file()).isEqualTo(dir.resolve("Configuration.xml"));
      assertThat(result.location().line()).isNull();
      assertThat(result.location().mdoRef()).isEqualTo("Document.Заказ");
    });
  }

  @Test
  void acceptsUnlimitedMultilineComment() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createString(0), true));

    assertThat(analyze(document)).isEmpty();
  }

  @Test
  void acceptsEnglishName() {
    var document = document("Заказ",
      attribute("Comment", ValueTypeDescription.createString(0), true));

    assertThat(analyze(document)).isEmpty();
  }

  @Test
  void comparesNamesIgnoringCaseAndSpaces() {
    var document = document("Заказ",
      attribute("  кОмМеНтАрИй  ", ValueTypeDescription.createString(0), true));

    assertThat(analyze(document)).isEmpty();
  }

  @Test
  void reportsLimitedLength() {
    var document = document("Акт",
      attribute("Комментарий", ValueTypeDescription.createString(50), true));

    assertThat(analyze(document)).singleElement().satisfies(result ->
      assertThat(result.message()).isEqualTo(
        "Реквизит " + QUOTE + "Комментарий" + QUOTE + " документа " + QUOTE + "Акт"
          + QUOTE + " " + FULL_REQUIREMENT));
  }

  @Test
  void reportsDisabledMultiline() {
    var document = document("Акт",
      attribute("Комментарий", ValueTypeDescription.createString(0), false));

    assertThat(analyze(document)).singleElement().satisfies(result ->
      assertThat(result.message()).isEqualTo(
        "Реквизит " + QUOTE + "Комментарий" + QUOTE + " документа " + QUOTE + "Акт"
          + QUOTE + " " + FULL_REQUIREMENT));
  }

  @Test
  void reportsNonStringType() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createNumber(10), true));

    assertThat(analyze(document)).singleElement().satisfies(result ->
      assertThat(result.message()).contains("должен быть строкой"));
  }

  @Test
  void reportsCompositeType() {
    var composite = ValueTypeDescription.create(
      List.of(PrimitiveValueType.STRING, PrimitiveValueType.NUMBER),
      StringQualifiers.create(0));
    var document = document("Заказ", attribute("Комментарий", composite, true));

    assertThat(analyze(document)).hasSize(1);
  }

  @Test
  void acceptsOneValidCandidateAmongSeveral() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createString(50), true),
      attribute("Comment", ValueTypeDescription.createString(0), true));

    assertThat(analyze(document)).isEmpty();
  }

  @Test
  void reportsFirstCandidateInDocumentOrder() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createString(50), true),
      attribute("Comment", ValueTypeDescription.createString(50), true));

    assertThat(analyze(document)).singleElement().satisfies(result ->
      assertThat(result.message()).startsWith("Реквизит " + QUOTE + "Комментарий" + QUOTE));
  }

  @Test
  void emptyNamesDisableCheck() {
    var document = document("Заказ");

    assertThat(analyze(document, List.of(" "), true, true)).isEmpty();
  }

  @Test
  void usesCustomNames() {
    var valid = document("Заказ",
      attribute("Описание", ValueTypeDescription.createString(0), true));

    assertThat(analyze(valid, List.of("Описание"), true, true)).isEmpty();
    assertThat(analyze(document("Заказ"), List.of("Описание"), true, true))
      .singleElement().satisfies(result ->
        assertThat(result.message()).contains(QUOTE + "Описание" + QUOTE));
  }

  @Test
  void allowsFixedLengthWhenNotRequired() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createString(50), true));

    assertThat(analyze(document, DocumentCommentAttributeCheck.DEFAULT_NAMES, false, true))
      .isEmpty();
  }

  @Test
  void allowsSingleLineWhenNotRequired() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createString(0), false));

    assertThat(analyze(document, DocumentCommentAttributeCheck.DEFAULT_NAMES, true, false))
      .isEmpty();
  }

  @Test
  void allowsAnyStringWhenBothRequirementsOff() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createString(50), false));

    assertThat(analyze(document, DocumentCommentAttributeCheck.DEFAULT_NAMES, false, false))
      .isEmpty();
  }

  @Test
  void reportsNonStringEvenWhenRequirementsOff() {
    var document = document("Заказ",
      attribute("Комментарий", ValueTypeDescription.createNumber(10), false));

    assertThat(analyze(document, DocumentCommentAttributeCheck.DEFAULT_NAMES, false, false))
      .singleElement().satisfies(result ->
        assertThat(result.message()).isEqualTo(
          "Реквизит " + QUOTE + "Комментарий" + QUOTE + " документа " + QUOTE + "Заказ"
            + QUOTE + " должен быть строкой"));
  }

  @Test
  void describesParameters() {
    var parameters = new DocumentCommentAttributeCheck().parameters();

    assertThat(parameters).extracting(parameter -> parameter.name())
      .containsExactly("names", "requireUnlimited", "requireMultiline");
    assertThat(parameters).extracting(parameter -> parameter.type())
      .containsExactly("string[]", "boolean", "boolean");
    assertThat(parameters).extracting(parameter -> parameter.defaultValue())
      .containsExactly("[" + QUOTE + "Комментарий" + QUOTE + ", " + QUOTE + "Comment" + QUOTE + "]",
        "true", "true");
  }

  private List<CheckResult> analyze(Document document) {
    return analyze(document, DocumentCommentAttributeCheck.DEFAULT_NAMES, true, true);
  }

  private List<CheckResult> analyze(Document document, List<String> names,
                                    boolean requireUnlimited, boolean requireMultiline) {
    return DocumentCommentAttributeCheck.analyze(document, names, requireUnlimited,
      requireMultiline, dir.resolve("Configuration.xml"));
  }

  private static Document document(String name, Attribute... attributes) {
    return Document.builder()
      .name(name)
      .mdoReference(MdoReference.create("Document." + name))
      .attributes(List.of(attributes))
      .build();
  }

  private static ObjectAttribute attribute(String name, ValueTypeDescription type,
                                           boolean multiLine) {
    return ObjectAttribute.builder()
      .name(name)
      .type(type)
      .multiLine(multiLine)
      .build();
  }
}
