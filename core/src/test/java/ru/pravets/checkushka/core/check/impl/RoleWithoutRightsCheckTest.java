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

import com.github._1c_syntax.bsl.mdo.Role;
import com.github._1c_syntax.bsl.mdo.storage.RoleData;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoleWithoutRightsCheckTest {

  @Test
  void roleWithoutRightsDataHasNoRights() {
    var role = Role.builder()
      .name("Пустая")
      .data(RoleData.EMPTY)
      .build();

    assertThat(RoleWithoutRightsCheck.hasRights(role)).isFalse();
  }

  @Test
  void roleWithOnlyDisabledRightsHasNoRights() {
    var disabled = new RoleData.Right(null, false, "");
    var objectRight = new RoleData.ObjectRight(null, List.of(disabled));
    var role = Role.builder()
      .name("Выключенная")
      .data(RoleData.builder().objectRight(objectRight).build())
      .build();

    assertThat(RoleWithoutRightsCheck.hasRights(role)).isFalse();
  }

  @Test
  void roleWithEnabledRightHasRights() {
    var enabled = new RoleData.Right(null, true, "");
    var objectRight = new RoleData.ObjectRight(null, List.of(enabled));
    var role = Role.builder()
      .name("Рабочая")
      .data(RoleData.builder().objectRight(objectRight).build())
      .build();

    assertThat(RoleWithoutRightsCheck.hasRights(role)).isTrue();
  }
}
