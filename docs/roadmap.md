# План развития

## Отложенные проверки

### FormGroupGroupingVariantEmpty — пустой вариант группировки у групп формы

**Статус:** заблокировано внешней зависимостью, ждёт доработки mdclasses.

**Что требуется проверить:** у элемента-группы формы не задан вариант группировки.

**Почему отложено:** в mdclasses 0.21 модель `FormGroup`
(`com.github._1c_syntax.bsl.mdo.storage.form.FormGroup`) содержит только `type`, `title`,
`comment`, `eventHandlers`, `elements`. Свойства «вариант группировки» в модели нет, а читать
исходный `form.xml` ради одного правила — временное решение, которое разойдётся с моделью.

**Порядок работ:**

1. Оформить issue в [1c-syntax/mdclasses](https://github.com/1c-syntax/mdclasses) с примером
   `form.xml`, где вариант группировки пуст и где задан.
2. По возможности подготовить PR: поле в `FormGroup` + чтение в `FormElementConverter` и
   `FormElementReaderContext`.
3. После релиза mdclasses с поддержкой — реализовать проверку на уровне `CheckTarget.FORM`
   в checkushka и добавить её в каталог `docs/checks`.
4. Поднять версию mdclasses и прогнать интеграционные фикстуры.

## Крупные направления

| Направление | Этап | Комментарий |
| --- | --- | --- |
| MCP-режим: stdio | M3 | сделано: 8 инструментов, тест на JSON-RPC handshake |
| MCP-режим: streamable HTTP | M3.1 | сделано: Jetty 12 (ee11), эндпоинт `/mcp`, тест со штатным клиентом SDK |
| Публикация артефактов (Maven Central) | M4 | сделано: `checkushka-core` публикуется в Central Portal по тегу, см. [publishing.md](publishing.md) |
| Публикация JSON-схемы | M4 | схема лежит в `schema/checkushka.schema.json`, нужен адрес на сайте документации |
| Каталог проверок в документации | M4 | сейчас `docs/checks/*.md` пишутся руками, позже — генерация из аннотаций |
| Параллельный обход объектов | M5 | замер сначала на БСП, потом оптимизация |
| Автоисправления (quick fix) | после v0.1 | для сортировки объектов и заголовков форм |
| LSP-адаптер | после появления клиента | ядро уже разделено: `Project` + `AnalysisService` без привязки к транспорту |

## SonarQube: отдельный репозиторий

Плагин SonarQube **не входит в этот репозиторий** и будет отдельным проектом — по образцу
[sonar-bsl-plugin-community](https://github.com/1c-syntax/sonar-bsl-plugin-community).
Причины:

- у плагина свой цикл релизов и своя версия Sonar API (`sonar-plugin-api`), менять их вместе
  с ядром проверок не нужно;
- тяжёлые тестовые зависимости (`sonar-testing-harness`, `sonar-core`) в этом репозитории
  не нужны;
- контракт между частями — стабильное публичное API `checkushka-core`.

Что уже готово на стороне этого репозитория:

- репортёры SARIF и Generic Issue — импорт замечаний в SonarQube **без плагина**
  (`sonar.externalIssuesReportPaths`);
- `AnalysisResult`, `CheckResult`, `SourceLocation`, `CheckRegistry` — то, из чего сенсор
  строит `NewIssue`;
- метаданные проверок (ключ, название, тип, важность, трудозатраты) — из них собирается
  `RulesDefinition` без ручного дублирования каталога.

Что должен сделать отдельный репозиторий:

1. подключить опубликованный `ru.pravets.checkushka:checkushka-core`;
2. `CheckushkaPlugin` + `RulesDefinition` из метаданных проверок;
3. язык `1c-metadata` с суффиксами `.mdo`/`.form`; для XML-выгрузок — `sonar.lang.patterns`;
4. сенсор: анализ каталога проекта и маппинг файла/строки в `NewIssue`;
5. тесты на `sonar-testing-harness` и прогон в SonarQube Community.

**Статус:** блокер снят — `ru.pravets.checkushka:checkushka-core` публикуется в Maven Central
по тегу (см. [publishing.md](publishing.md)), плагин может подключать его как обычную зависимость.

## Технический долг

- Плагин лицензионных заголовков `cloud.rio.license` ругается на `Task.project` во время
  выполнения: в Gradle 10 это станет ошибкой. Заменить на Spotless или на собственную задачу.
- XStream из mdclasses пишет предупреждения `sun.misc.Unsafe` на JDK 24+; в готовых образах
  они заглушены флагом `--sun-misc-unsafe-memory-access=allow`, в `java -jar` оставлены ради
  совместимости с JDK 21. Следить за обновлениями mdclasses.
