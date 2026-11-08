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

import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Кодировка стандартных потоков вывода.
 *
 * <p>Правило простое и повторяет то, что Java делает для {@code System.out} по умолчанию:
 * если поток подключён к консоли — писать в её кодировке (на Windows это текущая кодовая
 * страница: Cp866, Cp1251, UTF-8 после {@code chcp 65001}), иначе — в UTF-8, чтобы отчёты
 * в файлах и пайпах оставались машиночитаемыми.
 *
 * <p>Кодировку можно задать принудительно: системным свойством
 * {@code -Dcheckushka.stdout.encoding=<имя>} или переменной окружения
 * {@code CHECKUSHKA_STDOUT_ENCODING}. Свойство главнее переменной.
 */
final class ConsoleStreams {

  /** Системное свойство с принудительной кодировкой вывода. */
  static final String ENCODING_PROPERTY = "checkushka.stdout.encoding";

  /** Переменная окружения с принудительной кодировкой вывода. */
  static final String ENCODING_ENV = "CHECKUSHKA_STDOUT_ENCODING";

  private ConsoleStreams() {
  }

  /**
   * Перенастраивает {@code System.out} и {@code System.err} на выбранную кодировку.
   */
  static void configure() {
    var charset = charset(consoleCharset());
    System.setOut(stream(FileDescriptor.out, charset));
    System.setErr(stream(FileDescriptor.err, charset));
  }

  /**
   * Выбирает кодировку потоков вывода.
   *
   * @param consoleCharset кодировка подключённой консоли либо {@code null}, если консоли нет
   * @return кодировка для {@code System.out} и {@code System.err}
   */
  static Charset charset(Charset consoleCharset) {
    var requested = requestedCharset();
    if (requested != null) {
      return requested;
    }
    return consoleCharset == null ? StandardCharsets.UTF_8 : consoleCharset;
  }

  private static Charset consoleCharset() {
    Console console = System.console();
    return console == null ? null : console.charset();
  }

  private static Charset requestedCharset() {
    var name = System.getProperty(ENCODING_PROPERTY);
    if (name == null || name.isBlank()) {
      name = System.getenv(ENCODING_ENV);
    }
    if (name == null || name.isBlank()) {
      return null;
    }
    try {
      return Charset.forName(name.trim());
    } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
      System.err.println("Неизвестная кодировка вывода: " + name.trim() + "; используется автоматическая");
      return null;
    }
  }

  private static PrintStream stream(FileDescriptor descriptor, Charset charset) {
    return new PrintStream(new FileOutputStream(descriptor), true, charset);
  }
}
