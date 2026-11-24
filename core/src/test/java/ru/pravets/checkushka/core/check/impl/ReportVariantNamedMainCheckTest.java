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

import com.github._1c_syntax.bsl.mdo.Report;
import com.github._1c_syntax.bsl.mdo.children.ObjectTemplate;
import com.github._1c_syntax.bsl.mdo.storage.DataCompositionSchema;
import com.github._1c_syntax.bsl.mdo.storage.DataCompositionSchema.SettingsVariant;
import com.github._1c_syntax.bsl.mdo.support.TemplateType;
import com.github._1c_syntax.bsl.types.MdoReference;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.CheckResult;

class ReportVariantNamedMainCheckTest {

  @TempDir
  private Path dir;

  @Test
  void reportsVariantNamedMain() {
    var report = report("Отчёт", dcsTemplate("ОсновнаяСхема", variant("Основной")));

    var results = ReportVariantNamedMainCheck.analyze(report,
      ReportVariantNamedMainCheck.DEFAULT_NAMES, dir.resolve("Configuration.xml"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).key()).isEqualTo(ReportVariantNamedMainCheck.KEY);
    assertThat(results.get(0).message())
      .isEqualTo("Вариант отчёта \"Основной\" в макете \"ОсновнаяСхема\" назван шаблонно: "
        + "имя варианта должно отражать его назначение");
    assertThat(results.get(0).location().mdoRef()).isEqualTo("Report.Отчёт");
  }

  @Test
  void ignoresOtherVariantNames() {
    var report = report("Отчёт", dcsTemplate("Схема", variant("Детальный"), variant("Сводный")));

    assertThat(analyze(report)).isEmpty();
  }

  @Test
  void comparesNamesIgnoringCaseAndSpaces() {
    var report = report("Отчёт", dcsTemplate("Схема", variant("  ОСНОВНОЙ  ")));

    assertThat(analyze(report)).hasSize(1);
  }

  @Test
  void reportsEachVariantAndEachTemplate() {
    var report = report("Отчёт",
      dcsTemplate("Схема1", variant("Основной"), variant("Основной")),
      dcsTemplate("Схема2", variant("Основной")));

    assertThat(analyze(report)).hasSize(3);
  }

  @Test
  void ignoresTemplatesWithoutDataCompositionSchema() {
    var report = report("Отчёт",
      ObjectTemplate.builder()
        .name("Табличный")
        .templateType(TemplateType.SPREADSHEET_DOCUMENT)
        .data(new DataCompositionSchema(List.of(), List.of(variant("Основной")), null))
        .build());

    assertThat(analyze(report)).isEmpty();
  }

  @Test
  void picksLineOfVariantInTemplate() throws IOException {
    var template = dir.resolve("Template.xml");
    Files.writeString(template, """
      <?xml version="1.0" encoding="UTF-8"?>
      <DataCompositionSchema xmlns="http://v8.1c.ru/8.1/data-composition-system/schema">
        <settingsVariant>
          <dcsset:name>Детальный</dcsset:name>
        </settingsVariant>
        <settingsVariant>
          <dcsset:name>Основной</dcsset:name>
        </settingsVariant>
      </DataCompositionSchema>
      """);
    var schema = new DataCompositionSchema(List.of(),
      List.of(variant("Детальный"), variant("Основной")), template.toUri());
    var report = report("Отчёт",
      ObjectTemplate.builder().name("Схема").templateType(TemplateType.DATA_COMPOSITION_SCHEME)
        .data(schema).build());

    var results = analyze(report);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).location().file()).isEqualTo(template);
    assertThat(results.get(0).location().line()).isEqualTo(7);
  }

  @Test
  void fallsBackToConfigurationFileWithoutLine() {
    var fallback = dir.resolve("Configuration.xml");
    var report = report("Отчёт", dcsTemplate("Схема", variant("Основной")));

    var results = ReportVariantNamedMainCheck.analyze(report,
      ReportVariantNamedMainCheck.DEFAULT_NAMES, fallback);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).location().file()).isEqualTo(fallback);
    assertThat(results.get(0).location().line()).isNull();
  }

  @Test
  void ignoresVariantsWithoutName() {
    var report = report("Отчёт", dcsTemplate("Схема",
      new SettingsVariant(null, null), new SettingsVariant("  ", null)));

    assertThat(analyze(report)).isEmpty();
  }

  @Test
  void usesCustomNames() {
    var report = report("Отчёт", dcsTemplate("Схема", variant("Отчёт"), variant("Основной")));

    var results = ReportVariantNamedMainCheck.analyze(report, List.of("Отчёт"),
      dir.resolve("Configuration.xml"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).message()).contains("\"Отчёт\"");
  }

  @Test
  void emptyNamesListDisablesCheck() {
    var report = report("Отчёт", dcsTemplate("Схема", variant("Основной")));

    assertThat(ReportVariantNamedMainCheck.analyze(report, List.of(" "), null)).isEmpty();
  }

  @Test
  void describesNamesParameter() {
    var parameters = new ReportVariantNamedMainCheck().parameters();

    assertThat(parameters).singleElement()
      .satisfies(parameter -> {
        assertThat(parameter.name()).isEqualTo("names");
        assertThat(parameter.type()).isEqualTo("string[]");
        assertThat(parameter.defaultValue()).contains("Основной");
      });
  }

  private List<CheckResult> analyze(Report report) {
    return ReportVariantNamedMainCheck.analyze(report,
      ReportVariantNamedMainCheck.DEFAULT_NAMES, dir.resolve("Configuration.xml"));
  }

  private static Report report(String name, ObjectTemplate... templates) {
    return Report.builder()
      .name(name)
      .mdoReference(MdoReference.create("Report." + name))
      .templates(List.of(templates))
      .build();
  }

  private static ObjectTemplate dcsTemplate(String name, SettingsVariant... variants) {
    return ObjectTemplate.builder()
      .name(name)
      .templateType(TemplateType.DATA_COMPOSITION_SCHEME)
      .data(new DataCompositionSchema(List.of(), List.of(variants), (URI) null))
      .build();
  }

  private static SettingsVariant variant(String name) {
    return new SettingsVariant(name, null);
  }
}
