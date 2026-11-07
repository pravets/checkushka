# checkushka

Проверка метаданных и форм конфигураций 1С:Предприятие 8 на соответствие стандартам разработки
и формальным правилам.

Инструмент устроен как библиотека + утилита: ядро проверок используется CLI, MCP-сервером,
а в перспективе — LSP-адаптером. Плагин SonarQube планируется отдельным репозиторием;
уже сейчас замечания импортируются в SonarQube без плагина через отчёты SARIF и Generic Issue.

Аналог [BSL Language Server](https://github.com/1c-syntax/bsl-language-server), но не для кода,
а для метаданных и форм. Чтение метаданных — через
[MDClasses](https://github.com/1c-syntax/mdclasses).

## Статус

Проект в разработке, версия 0.1.0: ядро проверок, CLI и MCP-сервер (stdio и streamable HTTP).
Следующие этапы — публикация артефактов (M4) и параллельный обход объектов (M5), см. [docs/roadmap.md](docs/roadmap.md).

## Требования

- JDK 21 или новее (сборка проверена на JDK 25);
- Gradle — через wrapper, отдельная установка не нужна.

## Сборка

```sh
./gradlew build
```

Отчёты тестов и покрытия — в `build/reports` соответствующего модуля.

## Поставка одним файлом

Основной артефакт — исполняемый fat-jar со всеми зависимостями (аналог `-exec.jar` у BSL LS):

```sh
java -jar checkushka-<версия>-exec.jar --version
java -jar checkushka-<версия>-exec.jar check путь/к/проекту
java -jar checkushka-<версия>-exec.jar mcp
```

Внутри — все команды, включая MCP-сервер (stdio и streamable HTTP). На Windows при запуске
через `java -jar` выполните `chcp 65001`, иначе кириллица отобразится мусором.

## Готовые сборки под ОС

В каждом релизе есть архивы со встроенным JRE — Java на машине не нужна:

| Архив | Запуск |
| --- | --- |
| `checkushka_win.zip` | `checkushka\checkushka.exe` (или `checkushka-console.bat` — сам переключает консоль в UTF-8) |
| `checkushka_nix.zip` | `checkushka/bin/checkushka` |
| `checkushka_mac.zip` | `checkushka.app` |

Образы собираются через `jpackage`, поэтому macOS-приложение не подписано — Gatekeeper
предупредит при первом запуске.

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

Стартовые скрипты Windows сами переключают консоль на UTF-8 на время работы и возвращают
прежнюю кодовую страницу после завершения. При прямом запуске через `java -cp` выполните
`chcp 65001` (или включите UTF-8 в настройках терминала) — иначе кириллица отобразится мусором.

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

## MCP-режим

MCP-сервер — это команда утилиты, как `mcp` в BSL Language Server:

```sh
checkushka mcp                                  # stdio (по умолчанию)
checkushka mcp --protocol streamable --server.port 8080
```

Для клиентов, которым нужен отдельный бинарник, есть дистрибутив `checkushka-mcp`
(в конфигурации клиента — `"command": "checkushka-mcp"`): в нём только сервер, без классов CLI.

Подключение по stdio:

```json
{
  "mcpServers": {
    "checkushka": {
      "command": "checkushka",
      "args": ["mcp"]
    }
  }
}
```

Инструменты:

| Инструмент | Назначение |
| --- | --- |
| `list_workspace_folders` | зарегистрированные рабочие папки |
| `register_workspace_folder` | разобрать проект по каталогу и зарегистрировать его |
| `unregister_workspace_folder` | освободить разобранную модель |
| `analyze_configuration` | запустить проверки и вернуть замечания |
| `list_checks` | доступные проверки |
| `describe_check` | описание проверки и её параметры |
| `list_metadata_objects` | объекты метаданных с фильтром по типу и имени |
| `get_metadata_object` | свойства объекта по ссылке |

Все инструменты анализа помечены как read-only; управление рабочими папками меняет состояние
сервера.

### Streamable HTTP

```sh
checkushka mcp --protocol streamable --server.port 8080
# эндпоинт: http://localhost:8080/mcp (путь меняется --mcp-path)
```

```json
{
  "mcpServers": {
    "checkushka": {
      "type": "streamable-http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

## Релиз

Релиз делается тегом, вручную ничего собирать не нужно:

```sh
# версия в build.gradle.kts должна совпадать с тегом
git tag -s vX.Y.Z -m "checkushka X.Y.Z"
git push origin vX.Y.Z
```

Workflow [release.yml](.github/workflows/release.yml):

1. проверяет, что версия проекта совпадает с тегом;
2. собирает и тестирует, создаёт черновик релиза и прикрепляет `-exec.jar`,
   `checkushka-core-*.jar`, sources и javadoc;
3. на трёх ОС собирает образы со встроенным JRE (`checkushka_win.zip`, `_nix.zip`, `_mac.zip`)
   скриптом [.github/scripts/package-image.py](.github/scripts/package-image.py);
4. снимает с релиза статус черновика.

Публикации в Maven Central в этом workflow нет. Локальная публикация `checkushka-core`
в staging-репозиторий остаётся доступной вручную (`./gradlew :checkushka-core:publish`);
GPG-подпись включается переменными `GPG_SIGNING_KEY` / `GPG_SIGNING_PASSWORD`.

## Структура

| Модуль | Назначение |
| --- | --- |
| `checkushka-core` | модель, движок проверок, конфигурация, репортёры |
| `checkushka-cli` | командная строка (picocli) |
| `checkushka-mcp` | MCP-сервер (stdio / streamable HTTP) |

## Лицензия

LGPL-3.0-or-later, см. [LICENSE](LICENSE).
