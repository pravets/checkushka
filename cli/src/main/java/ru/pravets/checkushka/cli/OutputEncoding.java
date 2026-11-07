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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Настройка кодировки вывода.
 *
 * <p>В интерактивной консоли Windows Java пишет в кодировке консоли (обычно cp866), и это
 * правильно: текст отображается корректно. При перенаправлении вывода в файл или в конвейер
 * та же кодировка ломает машиночитаемые отчёты (JSON, SARIF), поэтому в этом случае вывод
 * принудительно переводится в UTF-8.
 */
final class OutputEncoding {

  private OutputEncoding() {
  }

  static void configure() {
    if (System.console() != null) {
      return;
    }
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true,
      StandardCharsets.UTF_8));
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true,
      StandardCharsets.UTF_8));
  }
}
