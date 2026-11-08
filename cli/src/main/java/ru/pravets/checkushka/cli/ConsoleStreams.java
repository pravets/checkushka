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
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Кодировка стандартных потоков вывода.
 *
 * <p>Правило выбирает ту кодировку, которой ждёт потребитель:
 * <ol>
 *   <li>если поток подключён к консоли — кодовую страницу консоли;</li>
 *   <li>если консоль есть, а поток перенаправлен в пайп — тоже кодовую страницу консоли:
 *       так байты декодирует PowerShell ({@code [Console]::OutputEncoding}), да и
 *       {@code cmd | findstr} другого не ждёт;</li>
 *   <li>если поток перенаправлен в файл или консоли нет (CI, службы) — UTF-8,
 *       чтобы отчёты оставались машиночитаемыми.</li>
 * </ol>
 *
 * <p>Принудительно кодировку задают системным свойством
 * {@code -Dcheckushka.stdout.encoding=<имя>} или переменной окружения
 * {@code CHECKUSHKA_STDOUT_ENCODING}; свойство главнее переменной.
 */
final class ConsoleStreams {

  /** Системное свойство с принудительной кодировкой вывода. */
  static final String ENCODING_PROPERTY = "checkushka.stdout.encoding";

  /** Переменная окружения с принудительной кодировкой вывода. */
  static final String ENCODING_ENV = "CHECKUSHKA_STDOUT_ENCODING";

  /**
   * Поток поверх stdout, нужный для проверки «пайп или файл».
   *
   * <p>Хранится в статическом поле намеренно: закрытие потока закрыло бы файловый
   * дескриптор 1, после чего вывод перестал бы работать вовсе.
   */
  private static FileOutputStream stdoutProbe;

  private ConsoleStreams() {
  }

  /**
   * Перенастраивает {@code System.out} и {@code System.err} на выбранную кодировку.
   *
   * <p>Кроме замены потоков кодировка публикуется в системных свойствах: picocli берёт
   * её из {@code sun.stdout.encoding} и иначе пишет справку в UTF-8 мимо {@code System.out}.
   */
  static void configure() {
    var charset = choose(requested(), consoleCharset(), pipeCharset());
    publish(charset);
    System.setOut(stream(FileDescriptor.out, charset));
    System.setErr(stream(FileDescriptor.err, charset));
  }

  private static void publish(Charset charset) {
    System.setProperty("stdout.encoding", charset.name());
    System.setProperty("stderr.encoding", charset.name());
    System.setProperty("sun.stdout.encoding", charset.name());
    System.setProperty("sun.stderr.encoding", charset.name());
  }

  /**
   * Выбирает кодировку потоков вывода.
   *
   * @param requested      принудительная кодировка либо {@code null}
   * @param consoleCharset кодировка консоли, если поток идёт прямо в неё
   * @param pipeCharset    кодовая страница консоли, если поток уходит в пайп при живой консоли
   * @return кодировка для {@code System.out} и {@code System.err}
   */
  static Charset choose(Charset requested, Charset consoleCharset, Charset pipeCharset) {
    if (requested != null) {
      return requested;
    }
    if (consoleCharset != null) {
      return consoleCharset;
    }
    if (pipeCharset != null) {
      return pipeCharset;
    }
    return StandardCharsets.UTF_8;
  }

  /**
   * Кодировка из свойства или переменной окружения.
   *
   * @return кодировка либо {@code null}, если она не задана или неизвестна
   */
  static Charset requested() {
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

  private static Charset consoleCharset() {
    Console console = System.console();
    return console == null ? null : console.charset();
  }

  /**
   * Кодировка для вывода в пайп при подключённой консоли.
   *
   * <p>Признак живой консоли — {@code stdin.encoding}: JDK берёт его из кодовой страницы
   * консоли, а без консоли оставляет UTF-8. Сам по себе {@code stdout.encoding} не годится
   * (он равен ANSI-кодировке), а открытие {@code CONOUT$} отвечает «есть» даже без консоли.
   *
   * @return кодировка либо {@code null}, если консоли нет, пайпа нет или это не Windows
   */
  static Charset pipeCharset() {
    if (!isWindows()) {
      return null;
    }
    var consoleEncoding = stdinCharset();
    if (consoleEncoding == null || !stdoutIsPipe()) {
      return null;
    }
    return consoleEncoding;
  }

  private static Charset stdinCharset() {
    var name = System.getProperty("stdin.encoding");
    if (name == null || name.isBlank()) {
      return null;
    }
    try {
      var charset = Charset.forName(name.trim());
      return StandardCharsets.UTF_8.equals(charset) ? null : charset;
    } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
      return null;
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").startsWith("Windows");
  }

  private static boolean stdoutIsPipe() {
    try {
      if (stdoutProbe == null) {
        stdoutProbe = new FileOutputStream(FileDescriptor.out);
      }
      var lock = stdoutProbe.getChannel().tryLock();
      if (lock == null) {
        return true;
      }
      lock.release();
      return false;
    } catch (IOException | RuntimeException e) {
      return true;
    }
  }

  private static PrintStream stream(FileDescriptor descriptor, Charset charset) {
    return new PrintStream(new FileOutputStream(descriptor), true, charset);
  }
}
