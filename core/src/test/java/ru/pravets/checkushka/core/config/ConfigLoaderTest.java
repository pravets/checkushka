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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.engine.ChecksMode;
import ru.pravets.checkushka.core.source.SourceFormat;

class ConfigLoaderTest {

  private static final String CONFIG = """
    {
      "language": "ru",
      "source": { "format": "designer" },
      "checks": {
        "mode": "ONLY",
        "keys": ["MetadataOrder"],
        "minSeverity": "MAJOR",
        "parameters": {
          "MetadataOrder": { "mode": "groupByPrefix", "prefixes": ["об_", "мс_"] }
        },
        "severity": { "MetadataOrder": "CRITICAL" }
      },
      "ignore": [
        { "check": "MetadataOrder", "mdo": "Catalog.Товары", "reason": "импорт от поставщика" }
      ]
    }
    """;

  @TempDir
  private Path projectDir;

  @Test
  void returnsDefaultsWithoutFile() {
    var config = new ConfigLoader().load(projectDir, null);

    assertThat(config.format()).isEqualTo(SourceFormat.AUTO);
    assertThat(config.checks().mode()).isEqualTo(ChecksMode.ON);
    assertThat(config.checks().minSeverity()).isEqualTo(Severity.INFO);
  }

  @Test
  void readsConfigFromProjectRoot() throws IOException {
    Files.writeString(projectDir.resolve("checkushka.json"), CONFIG);

    var config = new ConfigLoader().load(projectDir, null);

    assertThat(config.format()).isEqualTo(SourceFormat.DESIGNER);
    assertThat(config.checks().mode()).isEqualTo(ChecksMode.ONLY);
    assertThat(config.checks().checkKeys()).containsExactly("MetadataOrder");
    assertThat(config.checks().minSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(config.checks().severityFor("MetadataOrder", Severity.MINOR)).isEqualTo(Severity.CRITICAL);
    assertThat(config.checks().parametersFor("MetadataOrder").string("mode", "none"))
      .isEqualTo("groupByPrefix");
    assertThat(config.checks().parametersFor("MetadataOrder").stringList("prefixes", java.util.List.of()))
      .containsExactly("об_", "мс_");
  }

  @Test
  void appliesIgnoreRules() throws IOException {
    Files.writeString(projectDir.resolve("checkushka.json"), CONFIG);

    var settings = new ConfigLoader().load(projectDir, null).checks();

    assertThat(settings.isIgnored("MetadataOrder", "Catalog.Товары")).isTrue();
    assertThat(settings.isIgnored("MetadataOrder", "Catalog.Клиенты")).isFalse();
    assertThat(settings.isIgnored("RoleWithoutRights", "Catalog.Товары")).isFalse();
  }

  @Test
  void enabledChecksFollowMode() {
    var only = new ConfigLoaderTestSupport();

    assertThat(only.enabled(ChecksMode.ON, "Any", true)).isTrue();
    assertThat(only.enabled(ChecksMode.ON, "Any", false)).isFalse();
    assertThat(only.enabled(ChecksMode.ALL, "Any", false)).isTrue();
    assertThat(only.enabled(ChecksMode.OFF, "Any", true)).isFalse();
  }

  /** Проверяет семантику {@link ru.pravets.checkushka.core.engine.CheckSettings#isEnabled}. */
  private static final class ConfigLoaderTestSupport {

    boolean enabled(ChecksMode mode, String key, boolean activatedByDefault) {
      return new ru.pravets.checkushka.core.engine.CheckSettings(
        mode, java.util.Set.of("Listed"), java.util.Map.of(), java.util.Map.of(),
        Severity.INFO, java.util.List.of(), java.util.Set.of())
        .isEnabled(key, activatedByDefault);
    }
  }
}
