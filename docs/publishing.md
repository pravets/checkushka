# Публикация в Maven Central

Артефакт `ru.pravets.checkushka:checkushka-core` публикуется в Maven Central из релизного
workflow при пуше тега `v*`. Namespace `ru.pravets` подтверждён в Central Portal.

## Как устроено

- Загрузка идёт через **Central Portal Publisher API** плагином
  [nmcp](https://gradleup.com/nmcp/) (`com.gradleup.nmcp.settings` в `settings.gradle.kts`).
  Плагин собирает публикации всех проектов с `maven-publish` — сейчас это только
  `checkushka-core`; `checkushka-cli` и `checkushka-mcp` приложения и не публикуются.
- В бандл попадают `jar`, `-sources.jar`, `-javadoc.jar`, `.pom`, `.module` и `.asc`-подпись
  на каждый файл. Javadoc-джар собирается только в CI (по переменной `CI`) или с `-Pjavadoc`.
- `publishingType = "AUTOMATIC"`: портал публикует загруженный бандл сам. Первая публикация
  (`0.2.3`) проходила в режиме `USER_MANAGED` с ручным подтверждением в UI; сейчас включён
  автоматический режим, и выпуск по тегу доходит до Maven Central без клика.
- Job `Публикация в Maven Central` в [release.yml](../.github/workflows/release.yml) идёт
  параллельно сборке образов и не блокирует GitHub-релиз. Если секреты не заданы, шаг
  печатает «Секреты Maven Central не заданы — публикация пропущена» и завершается успешно.

## Ключ подписи

Для артефактов используется отдельный ключ, не тот, которым подписываются коммиты:

```
011681C6935840855A4AC924A4E78D837B8CF8F9
ed25519, [SC], действует до 2029-11-11, UID Iosif Pravets <i@pravets.ru>
```

Публичная часть выложена на `keys.openpgp.org` и `keyserver.ubuntu.com` — без этого Central
не проверит подписи.

**Экспорт только через `--output`**: `gpg --armor --output key.asc --export-secret-keys <FPR>`.
Если писать через `>` в PowerShell, файл получится в UTF-16 с NUL-байтами, и GitHub Actions
не сможет запустить workflow с таким секретом — job падает на старте (`startup_failure`,
0 шагов). Проверить файл перед загрузкой: первая строка `-----BEGIN PGP PRIVATE KEY BLOCK-----`,
NUL-байтов нет.

## Секреты репозитория

| Секрет | Значение |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | username user-token'а из Central Portal (Account → Generate User Token) |
| `MAVEN_CENTRAL_PASSWORD` | пароль того же токена |
| `GPG_SIGNING_KEY` | приватный ключ в ASCII-armor, целиком |
| `GPG_SIGNING_PASSWORD` | пароль ключа; для ключа без пароля — пустая строка |

Публичный ключ должен быть доступен с keyserver'ов, иначе Portal не проверит подписи:

```sh
gpg --keyserver keys.openpgp.org --send-keys <FINGERPRINT>
gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>
```

keys.openpgp.org присылает письмо для подтверждения адреса — без подтверждения ключ не отдаётся.

## Выпуск версии

```sh
# 1. поднять версию в build.gradle.kts (она же версия артефакта)
# 2. подписанный коммит и тег
git commit -S -am "build: выпуск X.Y.Z"
git tag -s vX.Y.Z -m "checkushka X.Y.Z"
git push origin master && git push origin vX.Y.Z
```

Дальше workflow сам: проверит версию против тега, соберёт и протестирует, опубликует
GitHub-релиз и загрузит бандл в Central Portal. Затем в Portal нажать **Publish**
(при `USER_MANAGED`).

Артефакт появляется в `https://repo1.maven.org/maven2/ru/pravets/checkushka/checkushka-core/X.Y.Z/`
в течение 10–30 минут после публикации.

## Проверка перед выпуском

```sh
# локально: артефакты и подписи в staging-репозитории
GPG_SIGNING_KEY="$(cat key.asc)" GPG_SIGNING_PASSWORD=... \
  ./gradlew :checkushka-core:publishAllPublicationsToStagingRepository -Pjavadoc

# что именно уедет в Portal (zip-бандл, без загрузки)
GPG_SIGNING_KEY="$(cat key.asc)" GPG_SIGNING_PASSWORD=... ./gradlew nmcpZipAggregation -Pjavadoc
```

Проверить потребление:

```kotlin
dependencies {
    implementation("ru.pravets.checkushka:checkushka-core:X.Y.Z")
}
```

## Если Central отклонил бандл

- **Невалидный POM** — правится в `core/build.gradle.kts`, публикуется новой версией.
- **Подпись не проверена** — ключ не на keyserver или истёк.
- **Версия уже опубликована** — переиспользовать версию нельзя, нужен новый номер.
  Поэтому ошибки в метаданных исправляются следующей версией, а не перезаписью.

## Snapshots

Для промежуточных сборок есть `./gradlew publishAggregationToCentralSnapshots` (версия
с суффиксом `-SNAPSHOT`). В основном workflow snapshots не публикуются.
