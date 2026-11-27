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
import ru.pravets.checkushka.core.check.CheckParameters;
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
 * {@code properties}, комментарий. Сравнение по умолчанию идёт без учёта регистра, поэтому
 * значение {@code letters} по умолчанию ({@code ["ё"]}) покрывает и «ё», и «Ё»; параметр
 * {@code caseSensitive} включает регистрозависимое сравнение. Комментарии по умолчанию
 * не проверяются.
 *
 * <p>Проверка работает на уровне {@link CheckTarget#MDO}, но по умолчанию обходит не только сам
 * объект, а и всех его потомков через {@link ChildrenOwner#getPlainChildren()}: реквизиты,
 * табличные части и их реквизиты, команды, макеты и прочие дочерние сущности модели. Параметр
 * {@code scope} со значением {@code top-level} ограничивает обход самим объектом. Движок
 * передаёт в качестве единицы анализа и вложенные объекты ({@link MDChild}): такие вызовы
 * пропускаются, иначе один и тот же объект попал бы в отчёт несколько раз — от своего имени
 * и от имени каждого предка. Позиция внутри файла для имени и синонима в модели не определена,
 * поэтому замечание указывает на файл верхнеуровневого объекта, а {@code mdoRef} — на
 * конкретного нарушителя.
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

  /** Область по умолчанию: объект и все его потомки. */
  static final String SCOPE_ALL = "all";

  /** Область «только сам объект, без потомков». */
  static final String SCOPE_TOP_LEVEL = "top-level";

  static final String PROPERTY_NAME = "name";
  static final String PROPERTY_SYNONYM = "synonym";
  static final String PROPERTY_COMMENT = "comment";

  private static final CheckMetadata META =
    YoLetterInMetadataCheck.class.getAnnotation(CheckMetadata.class);

  /**
   * Настройки проверки.
   *
   * @param letters       запрещённые подстроки
   * @param properties    какие свойства проверять: {@code name}, {@code synonym}, {@code comment}
   * @param scope         область: {@code all} — объект и потомки, {@code top-level} — только объект
   * @param caseSensitive учитывать ли регистр букв
   * @param languages     языки синонима; пустой список — все языки
   */
  record Options(List<String> letters, List<String> properties, String scope,
                 boolean caseSensitive, List<String> languages) {

    /**
     * Настройки по умолчанию.
     *
     * @return настройки, совпадающие с поведением проверки без конфигурации
     */
    static Options defaults() {
      return new Options(DEFAULT_LETTERS, DEFAULT_PROPERTIES, SCOPE_ALL, false, List.of());
    }
  }

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
    return analyze(mdo, options(context.parameters()), file);
  }

  /**
   * Проверяет объект метаданных и всех его потомков с настройками по умолчанию.
   *
   * @param object     объект метаданных
   * @param letters    запрещённые буквы
   * @param properties какие свойства проверять: {@code name}, {@code synonym}, {@code comment}
   * @param file       файл для замечания, если расположение объекта определить не удалось
   * @return замечания в порядке обхода объектов и свойств
   */
  static List<CheckResult> analyze(MD object, List<String> letters, List<String> properties, Path file) {
    return analyze(object, new Options(letters, properties, SCOPE_ALL, false, List.of()), file);
  }

  /**
   * Проверяет объект метаданных с заданными настройками.
   *
   * @param object  объект метаданных
   * @param options настройки проверки
   * @param file    файл для замечания, если расположение объекта определить не удалось
   * @return замечания в порядке обхода объектов и свойств
   */
  static List<CheckResult> analyze(MD object, Options options, Path file) {
    Objects.requireNonNull(object, "object");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(file, "file");
    return new Scanner(options, file).scan(object);
  }

  private static Options options(CheckParameters parameters) {
    return new Options(
      parameters.stringList("letters", DEFAULT_LETTERS),
      parameters.stringList("properties", DEFAULT_PROPERTIES),
      parameters.string("scope", SCOPE_ALL),
      parameters.bool("caseSensitive", false),
      parameters.stringList("languages", List.of()));
  }

  private static List<String> normalizeLetters(List<String> letters, boolean caseSensitive) {
    if (letters == null) {
      return List.of();
    }
    var normalized = new LinkedHashSet<String>();
    for (var letter : letters) {
      if (letter == null) {
        continue;
      }
      var value = letter.trim();
      if (!caseSensitive) {
        value = value.toLowerCase(Locale.ROOT);
      }
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

  private static Set<String> normalizeLanguages(List<String> languages) {
    if (languages == null || languages.isEmpty()) {
      return Set.of();
    }
    var normalized = new LinkedHashSet<String>();
    for (var language : languages) {
      if (language == null) {
        continue;
      }
      var value = language.trim().toLowerCase(Locale.ROOT);
      if (!value.isEmpty()) {
        normalized.add(value);
      }
    }
    return Set.copyOf(normalized);
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

  /**
   * Обход объектов и накопление замечаний.
   *
   * <p>Держит нормализованные настройки вместе, чтобы методы проверки не таскали длинный список
   * параметров.
   */
  private static final class Scanner {

    private final List<CheckResult> results = new ArrayList<>();
    private final List<String> letters;
    private final Set<String> properties;
    private final boolean topLevelOnly;
    private final boolean caseSensitive;
    private final Set<String> languages;
    private final Path file;

    Scanner(Options options, Path file) {
      this.caseSensitive = options.caseSensitive();
      this.letters = normalizeLetters(options.letters(), caseSensitive);
      this.properties = normalizeProperties(options.properties());
      this.topLevelOnly = isTopLevel(options.scope());
      this.languages = normalizeLanguages(options.languages());
      this.file = file;
    }

    List<CheckResult> scan(MD object) {
      if (letters.isEmpty() || properties.isEmpty()) {
        return List.of();
      }
      for (var current : objects(object)) {
        if (properties.contains(PROPERTY_NAME)) {
          checkValue(current, PROPERTY_NAME, null, current.getName());
        }
        if (properties.contains(PROPERTY_SYNONYM)) {
          checkSynonym(current);
        }
        if (properties.contains(PROPERTY_COMMENT)) {
          checkValue(current, PROPERTY_COMMENT, null, current.getComment());
        }
      }
      return results;
    }

    private List<MD> objects(MD object) {
      if (topLevelOnly || !(object instanceof ChildrenOwner owner)) {
        return List.of(object);
      }
      var children = owner.getPlainChildren();
      var result = new ArrayList<MD>(children.size() + 1);
      result.add(object);
      result.addAll(children);
      return result;
    }

    private void checkSynonym(MD object) {
      var synonym = object.getSynonym();
      if (synonym == null || synonym.isEmpty()) {
        return;
      }
      synonym.getContent().stream()
        .filter(entry -> entry.getLangKey() != null)
        .filter(entry -> languages.isEmpty()
          || languages.contains(entry.getLangKey().toLowerCase(Locale.ROOT)))
        .sorted(Comparator.comparing(MultiLanguageString.Entry::getLangKey))
        .forEach(entry -> checkValue(
          object, PROPERTY_SYNONYM, entry.getLangKey(), entry.getValue()));
    }

    private void checkValue(MD object, String property, String language, String value) {
      if (value == null || value.isEmpty()) {
        return;
      }
      var haystack = caseSensitive ? value : value.toLowerCase(Locale.ROOT);
      for (var letter : letters) {
        if (haystack.contains(letter)) {
          results.add(new CheckResult(
            KEY,
            message(object, property, language, letter),
            META.severity(),
            SourceLocation.of(file, null, object.getMdoRef()),
            META.minutesToFix()));
          return;
        }
      }
    }

    private static boolean isTopLevel(String scope) {
      return scope != null && SCOPE_TOP_LEVEL.equalsIgnoreCase(scope.trim());
    }
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(
      new CheckParameter(
        "letters", "string[]", "[\"ё\"]",
        "Запрещённые буквы; без учёта регистра, пока не включён caseSensitive"),
      new CheckParameter(
        "properties", "string[]", "[\"name\", \"synonym\"]",
        "Что проверять: name, synonym (все языки), comment"),
      new CheckParameter(
        "scope", "string", "\"all\"",
        "Область: all — объект и потомки, top-level — только сам объект"),
      new CheckParameter(
        "caseSensitive", "boolean", "false",
        "Учитывать регистр букв; по умолчанию «ё» и «Ё» равнозначны"),
      new CheckParameter(
        "languages", "string[]", "[]",
        "Языки синонима; пусто — все языки конфигурации"));
  }
}
