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
package ru.pravets.checkushka.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

class CheckResultsTest {

  private static final Path FILE = Path.of("Configuration.xml");

  @Test
  void removesFullDuplicates() {
    var first = result("Сообщение", 1, "CommonModule.А");
    var duplicate = result("Сообщение", 1, "CommonModule.А");

    assertThat(CheckResults.distinct(List.of(first, duplicate))).containsExactly(first);
  }

  @Test
  void keepsResultsWithDifferentLocations() {
    var first = result("Сообщение", 1, "CommonModule.А");
    var second = result("Сообщение", 2, "CommonModule.Б");

    assertThat(CheckResults.distinct(List.of(first, second))).containsExactly(first, second);
  }

  @Test
  void keepsResultsWithDifferentMessages() {
    var first = result("Одно", 1, "CommonModule.А");
    var second = result("Другое", 1, "CommonModule.А");

    assertThat(CheckResults.distinct(List.of(first, second))).containsExactly(first, second);
  }

  private static CheckResult result(String message, Integer line, String mdoRef) {
    return new CheckResult("MetadataOrder", message, Severity.MINOR,
      SourceLocation.of(FILE, line, mdoRef), 1);
  }
}
