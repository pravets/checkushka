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
package ru.pravets.checkushka.core.report;

import java.util.Optional;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.engine.CheckRegistry;

/**
 * Справочник метаданных встроенных проверок для репортёров.
 *
 * <p>Репортёры получают только ключ проверки, а форматам SARIF и Generic Issue нужны ещё тип
 * и название правила.
 */
final class CheckCatalog {

  private CheckCatalog() {
  }

  static Optional<CheckMetadata> metadataOf(String key) {
    return CheckRegistry.builtIn().find(key).map(CheckRegistry::metadataOf);
  }

  static CheckType typeOf(String key) {
    return metadataOf(key).map(CheckMetadata::type).orElse(CheckType.CODE_SMELL);
  }

  static String nameOf(String key) {
    return metadataOf(key).map(CheckMetadata::name).orElse(key);
  }
}
