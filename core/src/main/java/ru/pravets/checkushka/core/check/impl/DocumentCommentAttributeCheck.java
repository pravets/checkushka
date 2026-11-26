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

import com.github._1c_syntax.bsl.mdo.Attribute;
import com.github._1c_syntax.bsl.mdo.Document;
import com.github._1c_syntax.bsl.mdo.children.ObjectAttribute;
import com.github._1c_syntax.bsl.types.ValueTypeDescription;
import com.github._1c_syntax.bsl.types.qualifiers.StringQualifiers;
import com.github._1c_syntax.bsl.types.value.PrimitiveValueType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
 * Проверка реквизита-комментария у документа.
 *
 * <p>У документа должен быть реквизит с именем из {@code names} (по умолчанию
 * {@code Комментарий} или {@code Comment}): строка неограниченной длины с включённым
 * многострочным режимом. Проверка объединяет два правила каталога идей — «реквизита нет»
 * и «реквизит неправильного типа».
 *
 * <p>Имена сравниваются без учёта регистра: идентификаторы 1С регистронезависимы.
 * Достаточно одного корректного реквизита из списка; если все найденные кандидаты
 * некорректны, замечание выводится по первому в порядке модели. На документ — одно
 * замечание.
 *
 * <p>Проверяются только собственные реквизиты документа: реквизиты табличных частей,
 * справочников и других объектов в область проверки не входят. Номер строки не
 * вычисляется: модель mdclasses не отдаёт координаты contained-объекта, поэтому
 * замечание указывает на файл документа.
 */
@CheckMetadata(
  key = DocumentCommentAttributeCheck.KEY,
  name = "Реквизит «Комментарий» у документа",
  type = CheckType.CODE_SMELL,
  severity = Severity.MINOR,
  tags = {CheckTag.DESIGN},
  targets = CheckTarget.MDO,
  minutesToFix = 5)
public final class DocumentCommentAttributeCheck implements MetadataCheck, ParameterizedCheck {

  /** Ключ проверки. */
  public static final String KEY = "DocumentCommentAttribute";

  /** Имена реквизита-комментария по умолчанию. */
  static final List<String> DEFAULT_NAMES = List.of("Комментарий", "Comment");

  private static final CheckMetadata META =
    DocumentCommentAttributeCheck.class.getAnnotation(CheckMetadata.class);

  @Override
  public List<CheckResult> check(CheckContext context) {
    if (!(context.mdo() instanceof Document document)) {
      return List.of();
    }
    var parameters = context.parameters();
    var file = context.project().layout().fileOf(document.getMdoRef())
      .orElseGet(() -> context.project().root());
    return analyze(
      document,
      parameters.stringList("names", DEFAULT_NAMES),
      parameters.bool("requireUnlimited", true),
      parameters.bool("requireMultiline", true),
      file);
  }

  /**
   * Проверяет реквизит-комментарий документа.
   *
   * @param document          документ
   * @param names             допустимые имена реквизита-комментария
   * @param requireUnlimited  требовать строку неограниченной длины
   * @param requireMultiline  требовать включённый многострочный режим
   * @param file              файл документа для замечания
   * @return замечания, не больше одного на документ
   */
  static List<CheckResult> analyze(Document document, List<String> names,
                                   boolean requireUnlimited, boolean requireMultiline, Path file) {
    var configured = configuredNames(names);
    if (configured.isEmpty()) {
      return List.of();
    }
    var keys = normalized(configured);

    var candidates = Optional.ofNullable(document.getAttributes()).orElse(List.of()).stream()
      .filter(Objects::nonNull)
      .filter(attribute -> attribute.getName() != null)
      .filter(attribute -> keys.contains(normalizeName(attribute.getName())))
      .toList();

    if (candidates.isEmpty()) {
      return List.of(result(
        missingMessage(document, configured.get(0)), file, document));
    }
    if (candidates.stream().anyMatch(
        attribute -> isValid(attribute, requireUnlimited, requireMultiline))) {
      return List.of();
    }

    var first = candidates.get(0);
    return List.of(result(
      invalidMessage(document, first.getName(), requireUnlimited, requireMultiline), file, document));
  }

  @Override
  public List<CheckParameter> parameters() {
    return List.of(
      new CheckParameter(
        "names", "string[]", "[\"Комментарий\", \"Comment\"]",
        "Имена реквизита-комментария; пустой список выключает проверку"),
      new CheckParameter(
        "requireUnlimited", "boolean", "true",
        "Требовать строку неограниченной длины"),
      new CheckParameter(
        "requireMultiline", "boolean", "true",
        "Требовать включённый многострочный режим"));
  }

  private static boolean isValid(Attribute attribute, boolean requireUnlimited,
                                 boolean requireMultiline) {
    var type = attribute.getValueType();
    if (!isString(type)) {
      return false;
    }
    if (requireUnlimited && length(type) != 0) {
      return false;
    }
    return !requireMultiline || isMultiLine(attribute);
  }

  private static boolean isString(ValueTypeDescription type) {
    return type != null && !type.isComposite() && type.contains(PrimitiveValueType.STRING);
  }

  private static long length(ValueTypeDescription type) {
    return Optional.ofNullable(type.getQualifiers()).orElse(List.of()).stream()
      .filter(StringQualifiers.class::isInstance)
      .map(StringQualifiers.class::cast)
      .mapToLong(StringQualifiers::getLength)
      .findFirst()
      .orElse(0L);
  }

  private static boolean isMultiLine(Attribute attribute) {
    return attribute instanceof ObjectAttribute objectAttribute && objectAttribute.isMultiLine();
  }

  private static List<String> configuredNames(List<String> names) {
    if (names == null) {
      return List.of();
    }
    var configured = new ArrayList<String>();
    for (var name : names) {
      if (name == null) {
        continue;
      }
      var trimmed = name.trim();
      if (!trimmed.isEmpty()) {
        configured.add(trimmed);
      }
    }
    return configured;
  }

  private static Set<String> normalized(List<String> names) {
    var normalized = new LinkedHashSet<String>();
    names.stream().map(DocumentCommentAttributeCheck::normalizeName).forEach(normalized::add);
    return normalized;
  }

  private static String normalizeName(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }

  private static String missingMessage(Document document, String name) {
    return "У документа \"" + document.getName() + "\" нет реквизита \"" + name + "\"";
  }

  private static String invalidMessage(Document document, String attributeName,
                                       boolean requireUnlimited, boolean requireMultiline) {
    var requirement = new StringBuilder("строкой");
    if (requireUnlimited) {
      requirement.append(" неограниченной длины");
    }
    if (requireMultiline) {
      requirement.append(" с включённым многострочным режимом");
    }
    return "Реквизит \"" + attributeName + "\" документа \"" + document.getName()
      + "\" должен быть " + requirement;
  }

  private static CheckResult result(String message, Path file, Document document) {
    return new CheckResult(
      KEY,
      message,
      META.severity(),
      SourceLocation.of(file, null, document.getMdoRef()),
      META.minutesToFix());
  }
}
