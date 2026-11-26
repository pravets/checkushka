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

import com.github._1c_syntax.bsl.mdo.ChildrenOwner;
import com.github._1c_syntax.bsl.mdo.MD;
import com.github._1c_syntax.bsl.mdo.MDChild;
import com.github._1c_syntax.bsl.types.MultiLanguageString;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import ru.pravets.checkushka.core.check.CheckContext;
import ru.pravets.checkushka.core.check.CheckMetadata;
import ru.pravets.checkushka.core.check.CheckParameter;
import ru.pravets.checkushka.core.check.CheckResult;
import ru.pravets.checkushka.core.check.CheckTag;
import ru.pravets.checkushka.core.check.CheckTarget;
import ru.pravets.checkushka.core.check.CheckType;
import ru.pravets.checkushka.core.check.MetadataCheck;
import ru.pravets.checkushka.core.check.ParameterizedCheck;
import ru.pravets.checkushka.core.check.Severity;
import ru.pravets.checkushka.core.source.SourceLocation;

/**
 * Проверка буквы «ё» в именах и синонимах объектов метаданных.
 *
 * <p>Проверяются имя, синоним во всех языках конфигурации и, если он включён параметром
 * {@code properties}, комментарий. Сравнение идёт без учёта регистра, поэтому значение
 * {@code letters} по умолчанию ({@code ["ё"]}) покрывает и «ё», и «Ё». Комментарии
 * по умолчанию не проверяются.
 *
 * <p>Проверка работает на уровне {@link CheckTarget#MDO}, но обходит не только сам объект,
 * а и всех его потомков через {@link ChildrenOwner#getPlainChildren()}: реквизиты, табличные
 * части и их реквизиты, команды, макеты и прочие дочерние сущности модели. Движок передаёт
 * в качестве единицы анализа и вложенные объекты ({@link MDChild}): такие вызовы пропускаются,
 * иначе один и тот же объект попал бы в отчёт несколько раз — от своего имени и от имени
 * каждого предка. Позиция внутри файла для имени и синонима в модели не определена, поэтому
 * замечание указывает на файл верхнеуровневого объекта, а {@code mdoRef} — на конкретного
 * нарушителя.
 */
@CheckMetadata(
  key = YoLetterInMetadataCheck.KEY,
  name = "Буква \"ё\" в именах и синонимах метаданных",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.STANDARD},
  targets = CheckTarget.MDO,
  minutesToFix = 1)
public final class YoLetterInMetadataCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "YoLetterInMetadata";

  /** Буквы, запрещённые по умолчанию. */
  static final List<String> DEFAULT_LETTERS = List.of("ё");

  /** Свойства, проверяемые по умолчанию. */
  static final List<String> DEFAULT_PROPERTIES = List.of("name", "synonym");

  static final String PROPERTY_NAME = "name";
  static final String PROPERTY_SYNONYM = "synonym";
  static final String PROPERTY_COMMENT = "comment";

  private static final CheckMetadata META =
    YoLetterInMetadataCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    var mdo = context.mdo();
    if (mdo == null || mdo instanceof MDChild) {
      // Вложенные объекты уже обойдены от верхнеуровневого предка; отдельный вызов для них
      // дал бы дубль и потерял бы файл объекта.
      return List.of();
    }
    var file = context.project().layout().fileOf(mdo.getMdoRef())
      .orElseGet(() -> context.project().root());
    return analyze(
      mdo,
      context.parameters().stringList("letters", DEFAULT_LETTERS),
      context.parameters().stringList("properties", DEFAULT_PROPERTIES),
      file);
  }

  /**
   * Проверяет объект метаданных и всех его потомков.
   *
   * @param object     объект метаданных
   * @param letters    запрещённые буквы; сравнение без учёта регистра
   * @param properties какие свойства проверять: {@code name}, {@code synonym}, {@code comment}
   * @param file       файл для замечания, если расположение объекта определить не удалось
   * @return замечания в порядке обхода объектов и свойств
   */
  static List<CheckResult> analyze(MD object, List<String> letters, List<String> properties, Path file) {
    Objects.requireNonNull(object, "object");
    Objects.requireNonNull(file, "file");
    var forbidden = normalizeLetters(letters);
    var enabled = normalizeProperties(properties);
    if (forbidden.isEmpty() || enabled.isEmpty()) {
      return List.of();
    }

    var results = new ArrayList<CheckResult>();
    for (var current : metadataObjects(object)) {
      if (enabled.contains(PROPERTY_NAME)) {
        checkValue(results, current, PROPERTY_NAME, null, current.getName(), forbidden, file);
      }
      if (enabled.contains(PROPERTY_SYNONYM)) {
        checkSynonym(results, current, forbidden, file);
      }
      if (enabled.contains(PROPERTY_COMMENT)) {
        checkValue(results, current, PROPERTY_COMMENT, null, current.getComment(), forbidden, file);
      }
    }
    return results;
  }

  private static List<MD> metadataObjects(MD object) {
    if (!(object instanceof ChildrenOwner owner)) {
      return List.of(object);
    }
    var children = owner.getPlainChildren();
    var result = new ArrayList<MD>(children.size() + 1);
    result.add(object);
    result.addAll(children);
    return result;
  }

  private static void checkSynonym(List<CheckResult> sink, MD object, List<String> letters, Path file) {
    var synonym = object.getSynonym();
    if (synonym == null || synonym.isEmpty()) {
      return;
    }
    synonym.getContent().stream()
      .sorted(Comparator.comparing(MultiLanguageString.Entry::getLangKey))
      .forEach(entry -> checkValue(
        sink, object, PROPERTY_SYNONYM, entry.getLangKey(), entry.getValue(), letters, file));
  }

  private static void checkValue(List<CheckResult> sink, MD object, String property, String language,
                                 String value, List<String> letters, Path file) {
    if (value == null || value.isEmpty()) {
      return;
    }
    var lowerValue = value.toLowerCase(Locale.ROOT);
    for (var letter : letters) {
      if (lowerValue.contains(letter)) {
        sink.add(new CheckResult(
          KEY,
          message(object, property, language, letter),
          META.severity(),
          SourceLocation.of(file, null, object.getMdoRef()),
          META.minutesToFix()));
        return;
      }
    }
  }

  private static String message(MD object, String property, String language, String letter) {
    var name = object.getName() == null ? "" : object.getName();
    return switch (property) {
      case PROPERTY_NAME -> "Имя объекта \"" + name + "\" содержит букву \"" + letter + "\"";
      case PROPERTY_SYNONYM -> "Синоним (" + language + ") объекта \"" + name
        + "\" содержит букву \"" + letter + "\"";
      default -> "Комментарий объекта \"" + name + "\" содержит букву \"" + letter + "\"";
    };
  }

  private static List<String> normalizeLetters(List<String> letters) {
    if (letters == null) {
      return List.of();
    }
    var normalized = new LinkedHashSet<String>();
    for (var letter : letters) {
      if (letter == null) {
        continue;
      }
      var value = letter.trim().toLowerCase(Locale.ROOT);
      if (!value.isEmpty()) {
        normalized.add(value);
      }
    }
    return List.copyOf(normalized);
  }

  private static Set<String> normalizeProperties(List<String> properties) {
    if (properties == null) {
      return Set.of();
    }
    var normalized = new LinkedHashSet<String>();
    for (var property : properties) {
      if (property == null) {
        continue;
      }
      var value = property.trim().toLowerCase(Locale.ROOT);
      if (PROPERTY_NAME.equals(value) || PROPERTY_SYNONYM.equals(value)
        || PROPERTY_COMMENT.equals(value)) {
        normalized.add(value);
      }
    }
    return Set.copyOf(normalized);
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(
      new CheckParameter(
        "letters", "string[]", "[\"ё\"]",
        "Запрещённые буквы; сравнение без учёта регистра"),
      new CheckParameter(
        "properties", "string[]", "[\"name\", \"synonym\"]",
        "Что проверять: name, synonym (все языки), comment"));
  }
}
