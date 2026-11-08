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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pravets.checkushka.core.engine.PrefixMode;
import ru.pravets.checkushka.core.source.SourceFormat;
import ru.pravets.checkushka.core.source.SourceOrder;

class MetadataOrderCheckTest {

  private static final Path FILE = Path.of("Configuration.xml");

  @Test
  void reportsOneIssuePerType() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульБ", 5),
      new SourceOrder.Entry("CommonModule", "МодульА", 6),
      new SourceOrder.Entry("Document", "ДокументБ", 7),
      new SourceOrder.Entry("Document", "ДокументА", 8));

    var results = MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of());

    assertThat(results).hasSize(2);
    assertThat(results.get(0).message()).isEqualTo(
      "Общие модули не отсортированы по имени: нарушений 1");
    assertThat(results.get(1).message()).isEqualTo(
      "Документы не отсортированы по имени: нарушений 1");
  }

  @Test
  void countsAllViolationsOfType() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульБ", 1),
      new SourceOrder.Entry("CommonModule", "МодульА", 2),
      new SourceOrder.Entry("CommonModule", "Модуль0", 3));

    var results = MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of());

    assertThat(results).hasSize(1);
    assertThat(results.get(0).message()).endsWith("нарушений 2");
  }

  @Test
  void pointsToFirstViolation() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульБ", 5),
      new SourceOrder.Entry("CommonModule", "МодульА", 6));

    var results = MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of());

    assertThat(results).hasSize(1);
    assertThat(results.get(0).location().line()).isEqualTo(6);
    assertThat(results.get(0).location().mdoRef()).isEqualTo("CommonModule");
  }

  @Test
  void unknownTypeFallsBackToPlatformName() {
    var order = order(
      new SourceOrder.Entry("UnknownThing", "Б", 1),
      new SourceOrder.Entry("UnknownThing", "А", 2));

    var results = MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of());

    assertThat(results).hasSize(1);
    assertThat(results.get(0).message()).startsWith("Метаданные типа \"UnknownThing\"");
  }

  @Test
  void detailObjectReportsEveryObject() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульБ", 5),
      new SourceOrder.Entry("CommonModule", "МодульА", 6));

    var results = MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of(),
      MetadataOrderCheck.DETAIL_OBJECT);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).message()).isEqualTo(
      "Объект \"МодульА\" должен располагаться раньше \"МодульБ\"");
    assertThat(results.get(0).location().mdoRef()).isEqualTo("CommonModule.МодульА");
  }

  @Test
  void acceptsSortedOrder() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульА", 5),
      new SourceOrder.Entry("CommonModule", "МодульБ", 6));

    assertThat(MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of())).isEmpty();
  }

  @Test
  void checksEachTypeSeparately() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульБ", 5),
      new SourceOrder.Entry("Catalog", "Альфа", 6),
      new SourceOrder.Entry("CommonModule", "МодульА", 7));

    assertThat(MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of())).hasSize(1);
  }

  @Test
  void comparesNumbersAsNumbers() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "Модуль10", 1),
      new SourceOrder.Entry("CommonModule", "Модуль2", 2));

    assertThat(MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of())).hasSize(1);
  }

  @Test
  void stripPrefixComparesNamesWithoutPrefix() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "МодульБ", 1),
      new SourceOrder.Entry("CommonModule", "об_МодульА", 2));

    assertThat(MetadataOrderCheck.analyze(order, PrefixMode.NONE, List.of())).isEmpty();
    assertThat(MetadataOrderCheck.analyze(order, PrefixMode.STRIP_PREFIX, List.of("об_")))
      .hasSize(1);
  }

  @Test
  void groupByPrefixKeepsPrefixedObjectsTogether() {
    var order = order(
      new SourceOrder.Entry("CommonModule", "об_МодульБ", 1),
      new SourceOrder.Entry("CommonModule", "об_МодульА", 2));

    assertThat(MetadataOrderCheck.analyze(order, PrefixMode.GROUP_BY_PREFIX, List.of("об_")))
      .hasSize(1);
  }

  @Test
  void emptyOrderProducesNothing() {
    assertThat(MetadataOrderCheck.analyze(order(), PrefixMode.NONE, List.of())).isEmpty();
  }

  private static SourceOrder order(SourceOrder.Entry... entries) {
    return new SourceOrder(FILE, SourceFormat.DESIGNER, List.of(entries));
  }
}
