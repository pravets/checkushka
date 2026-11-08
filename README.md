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

Внутри — все команды, включая MCP-сервер (stdio и streamable HTTP). Кодировка вывода
выбирается под потребителя: в консоли и в пайпе при живой консоли (PowerShell, `| findstr`) —
текущая кодовая страница консоли, поэтому `chcp` переключать не нужно; при перенаправлении
в файл и там, где консоли нет (CI, службы), — UTF-8, чтобы отчёты оставались машиночитаемыми.

На JDK 24+ XStream из mdclasses пишет в stderr предупреждения про `sun.misc.Unsafe`.
В готовых образах они заглушены флагом `--sun-misc-unsafe-memory-access=allow`; при запуске
через `java -jar` флаг нужно добавить самому — в jar он не встроен, чтобы не ломать запуск
на JDK 21.

## Использование как библиотеки

Ядро проверок публикуется в Maven Central под координатами
`ru.pravets.checkushka:checkushka-core`:

```kotlin
dependencies {
    implementation("ru.pravets.checkushka:checkushka-core:0.2.3")
}
```

```java
var result = Checkushka.with(CheckushkaSettings.defaults())
  .analyze(Path.of("путь/к/выгрузке"));
```

Вместе с ядром приезжают `mdclasses`, `bsl-common-library`, `jackson` и `slf4j`; для отчётов
в JSON/SARIF ничего дополнительно подключать не нужно. Как выпускается артефакт — в
[docs/publishing.md](docs/publishing.md).

## Готовые сборки под ОС

В каждом релизе есть архивы со встроенным JRE — Java на машине не нужна:

| Архив | Запуск |
| --- | --- |
| `checkushka_win.zip` | `checkushka\checkushka.exe` (есть псевдоним `checkushka-console.bat`) |
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

Кодировка вывода определяется автоматически: в консоли и в пайпе при живой консоли
(например, в PowerShell) кириллица пишется в текущей кодовой странице консоли, а при
перенаправлении в файл или при запуске без консоли — в UTF-8. Принудительно задать её можно
переменной `CHECKUSHKA_STDOUT_ENCODING` или свойством `-Dcheckushka.stdout.encoding`.

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
    { "check": "RoleWithoutRights", "mdo": "Role.*", "reason": "роли поставщика" }
  ]
}
```

Режимы `checks.mode`: `ON` (по умолчанию), `OFF`, `ONLY`, `EXCEPT`, `ALL`.

### Проверки

| Ключ | Что проверяет |
| --- | --- |
| `MetadataOrder` | порядок объектов в `ChildObjects` конфигурации; режимы префиксов `none`, `stripPrefix`, `groupByPrefix`; по умолчанию одно замечание на тип метаданных, `detail: object` — на каждый объект |
| `RoleWithoutRights` | роль, у которой не установлено ни одного права |

Ключи проверок в `--checks` и `checks.keys` сверяются с реестром: неизвестный ключ
(опечатка или удалённая проверка) — ошибка с кодом возврата 2 и списком доступных проверок.

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
4. публикует `checkushka-core` в Maven Central (job «Публикация в Maven Central»; при
   `USER_MANAGED` публикацию подтверждают кнопкой Publish в Central Portal);
5. снимает с релиза статус черновика.

Секреты, ключ на keyserver и порядок проверки описаны в
[docs/publishing.md](docs/publishing.md). Если секреты не заданы, шаг публикации в Central
пропускается с сообщением и не ломает релиз.

## Структура

| Модуль | Назначение |
| --- | --- |
| `checkushka-core` | модель, движок проверок, конфигурация, репортёры |
| `checkushka-cli` | командная строка (picocli) |
| `checkushka-mcp` | MCP-сервер (stdio / streamable HTTP) |

## Лицензия

LGPL-3.0-or-later, см. [LICENSE](LICENSE).
