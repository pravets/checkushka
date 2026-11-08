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
package ru.pravets.checkushka.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.CheckSettings;
import ru.pravets.checkushka.core.engine.ChecksMode;

class CheckushkaTest {

  @TempDir
  private Path projectDir;

  @Test
  void versionIsNotEmpty() {
    assertThat(Checkushka.version()).isNotBlank();
  }

  @Test
  void rejectsUnknownCheckKey() {
    var checks = new CheckSettings(ChecksMode.ONLY, Set.of("FormWithoutTitle"),
      Map.of(), Map.of(), Severity.INFO, List.of(), Set.of());
    var checkushka = Checkushka.with(new CheckushkaSettings(null, null, checks));

    assertThatThrownBy(() -> checkushka.analyze(projectDir))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("FormWithoutTitle")
      .hasMessageContaining("MetadataOrder");
  }
}
