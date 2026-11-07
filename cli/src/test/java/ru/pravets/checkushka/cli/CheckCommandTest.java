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
package ru.pravets.checkushka.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CheckCommandTest {

  @Test
  void returnsErrorForMissingProject() {
    var exitCode = Main.createCommandLine()
      .execute("check", "no-such-project-dir-checkushka");

    assertThat(exitCode).isEqualTo(CheckCommand.EXIT_ERROR);
  }

  @Test
  void helpIsAvailable() {
    var exitCode = Main.createCommandLine().execute("--help");

    assertThat(exitCode).isZero();
  }

  @Test
  void checkCommandIsRegistered() {
    var commandLine = Main.createCommandLine();

    assertThat(commandLine.getSubcommands()).containsKey("check");
  }
}
