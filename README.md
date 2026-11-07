# checkushka

Проверка метаданных и форм конфигураций 1С:Предприятие 8 на соответствие стандартам разработки
и формальным правилам.

Инструмент устроен как библиотека + утилита: ядро проверок используется CLI, MCP-сервером,
а в перспективе — плагином SonarQube и LSP-адаптером.

Аналог [BSL Language Server](https://github.com/1c-syntax/bsl-language-server), но не для кода,
а для метаданных и форм. Чтение метаданных — через
[MDClasses](https://github.com/1c-syntax/mdclasses).

## Статус

Проект в разработке. Текущий этап — M0 (каркас сборки).

## Требования

- JDK 21 или новее (сборка проверена на JDK 25);
- Gradle — через wrapper, отдельная установка не нужна.

## Сборка

```sh
./gradlew build
```

Отчёты тестов и покрытия — в `build/reports` соответствующего модуля.

## Использование

```sh
# проверить выгрузку конфигуратора или проект EDT
checkushka check путь/к/проекту

# только JSON, минимум MAJOR, только выбранные проверки
checkushka check путь --reports json --severity MAJOR --checks MetadataOrder

# код возврата 1, если есть замечания не ниже CRITICAL
checkushka check путь --fail-on CRITICAL
```

Коды возврата: `0` — замечаний нет, `1` — замечания есть, `2` — ошибка чтения проекта
или конфигурации.

На Windows консоль по умолчанию не в UTF-8: для корректной кириллицы запускайте
в консоли с `chcp 65001` или с `-Dstdout.encoding=UTF-8`.

### Конфигурация

Файл `checkushka.json` в корне проекта:

```json
{
  "source": { "format": "auto" },
  "checks": {
    "mode": "ON",
    "parameters": {
      "MetadataOrder": { "mode": "groupByPrefix", "prefixes": ["об_"] }
    },
    "severity": { "MetadataOrder": "MAJOR" }
  },
  "ignore": [
    { "check": "FormWithoutTitle", "mdo": "Catalog.*", "reason": "импорт от поставщика" }
  ]
}
```

Режимы `checks.mode`: `ON` (по умолчанию), `OFF`, `ONLY`, `EXCEPT`, `ALL`.

### Проверки

| Ключ | Что проверяет |
| --- | --- |
| `MetadataOrder` | порядок объектов в `ChildObjects` конфигурации; режимы префиксов `none`, `stripPrefix`, `groupByPrefix` |
| `RoleWithoutRights` | роль, у которой не установлено ни одного права |
| `FormWithoutTitle` | форма с незаполненным заголовком |

## Структура

| Модуль | Назначение |
| --- | --- |
| `checkushka-core` | модель, движок проверок, конфигурация, репортёры |
| `checkushka-cli` | командная строка (picocli) |
| `checkushka-mcp` | MCP-сервер (stdio / streamable HTTP) |

## Лицензия

LGPL-3.0-or-later, см. [LICENSE](LICENSE).
