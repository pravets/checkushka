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

import java.util.Map;

/**
 * Русские названия типов метаданных для сообщений проверок.
 *
 * <p>Кавычки в сообщениях только ASCII: в кодовой странице Cp866 символов {@code « »} нет,
 * и консоль Windows показывала бы вместо них {@code ?}.
 */
final class MetadataTypeNames {

  private static final Map<String, String> PLURALS = Map.ofEntries(
    Map.entry("AccountingRegister", "Регистры бухгалтерии"),
    Map.entry("AccumulationRegister", "Регистры накопления"),
    Map.entry("BusinessProcess", "Бизнес-процессы"),
    Map.entry("CalculationRegister", "Регистры расчёта"),
    Map.entry("Catalog", "Справочники"),
    Map.entry("ChartOfAccounts", "Планы счетов"),
    Map.entry("ChartOfCalculationTypes", "Планы видов расчёта"),
    Map.entry("ChartOfCharacteristicTypes", "Планы видов характеристик"),
    Map.entry("CommonAttribute", "Общие реквизиты"),
    Map.entry("CommonCommand", "Общие команды"),
    Map.entry("CommonForm", "Общие формы"),
    Map.entry("CommonModule", "Общие модули"),
    Map.entry("CommonPicture", "Общие картинки"),
    Map.entry("CommonTemplate", "Общие макеты"),
    Map.entry("Constant", "Константы"),
    Map.entry("DataProcessor", "Обработки"),
    Map.entry("DefinedType", "Определяемые типы"),
    Map.entry("Document", "Документы"),
    Map.entry("DocumentJournal", "Журналы документов"),
    Map.entry("DocumentNumerator", "Нумераторы документов"),
    Map.entry("Enum", "Перечисления"),
    Map.entry("EventSubscription", "Подписки на события"),
    Map.entry("ExchangePlan", "Планы обмена"),
    Map.entry("FilterCriterion", "Критерии отбора"),
    Map.entry("FunctionalOption", "Функциональные опции"),
    Map.entry("FunctionalOptionsParameter", "Параметры функциональных опций"),
    Map.entry("HTTPService", "HTTP-сервисы"),
    Map.entry("InformationRegister", "Регистры сведений"),
    Map.entry("IntegrationService", "Сервисы интеграции"),
    Map.entry("Language", "Языки"),
    Map.entry("Report", "Отчёты"),
    Map.entry("Role", "Роли"),
    Map.entry("ScheduledJob", "Регламентные задания"),
    Map.entry("Sequence", "Последовательности"),
    Map.entry("SettingsStorage", "Хранилища настроек"),
    Map.entry("Style", "Стили"),
    Map.entry("StyleItem", "Элементы стиля"),
    Map.entry("Task", "Задачи"),
    Map.entry("WebService", "Web-сервисы"),
    Map.entry("WSReference", "WS-ссылки"),
    Map.entry("XDTOPackage", "XDTO-пакеты"));

  private MetadataTypeNames() {
  }

  /**
   * Название типа во множественном числе.
   *
   * @param type тип в нотации платформы, например {@code CommonModule}
   * @return название для сообщения; для неизвестного типа — {@code Метаданные типа "..."}
   */
  static String plural(String type) {
    var known = PLURALS.get(type);
    return known != null ? known : "Метаданные типа \"" + type + "\"";
  }
}
