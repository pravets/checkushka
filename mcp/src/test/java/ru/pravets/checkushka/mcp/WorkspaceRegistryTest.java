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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import ru.pravets.checkushka.core.source.Project;
import ru.pravets.checkushka.core.source.SourceFormat;
import ru.pravets.checkushka.core.source.SourceOrder;

class WorkspaceRegistryTest {

  @Test
  void registersListsAndUnregisters() {
    var registry = new WorkspaceRegistry(WorkspaceRegistryTest::project);

    var workspace = registry.register(Path.of("C:/project"), null);

    assertThat(workspace.name()).isEqualTo("project");
    assertThat(workspace.uri()).startsWith("file:");
    assertThat(registry.list()).containsExactly(workspace);
    assertThat(registry.require(workspace.uri())).isSameAs(workspace);
    assertThat(registry.unregister(workspace.uri())).isTrue();
    assertThat(registry.list()).isEmpty();
  }

  @Test
  void usesExplicitName() {
    var registry = new WorkspaceRegistry(WorkspaceRegistryTest::project);

    var workspace = registry.register(Path.of("C:/project"), "Демо");

    assertThat(workspace.name()).isEqualTo("Демо");
  }

  @Test
  void unknownWorkspaceExplainsHowToRegister() {
    var registry = new WorkspaceRegistry(WorkspaceRegistryTest::project);

    assertThatThrownBy(() -> registry.require("file:///nope"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("register_workspace_folder");
  }

  private static Project project(Path root) {
    var configuration = root.resolve("Configuration.xml");
    return new Project(root, configuration, SourceFormat.DESIGNER, null, null,
      SourceOrder.empty(configuration, SourceFormat.DESIGNER));
  }
}
