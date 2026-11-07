plugins {
    `java-library`
    application
    id("cloud.rio.license")
}

application {
    mainClass = "ru.pravets.checkushka.cli.Main"
    applicationName = "checkushka"
    // Явная UTF-8 для потоков вывода: иначе на Windows Java использует ANSI-кодировку
    // (Cp1251/Cp866), и кириллица ломается при перенаправлении и в MCP-протоколе
    applicationDefaultJvmArgs = listOf("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}

/**
 * Переключает консоль Windows на UTF-8 на время работы и возвращает исходную кодовую страницу.
 *
 * <p>Свойства JVM задают кодировку байтов, но сама консоль должна знать, что это UTF-8,
 * отсюда chcp. Переменная сохраняется до setlocal (иначе её сбросит endlocal),
 * а код возврата фиксируется до chcp (иначе chcp перезапишет ERRORLEVEL).
 */
tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        val file = windowsScript
        val text = file.readText()
        val startMarker = "@if \"%DEBUG%\"==\"\" @echo off"
        val exitMarker = "\"%COMSPEC%\" /c exit %ERRORLEVEL%"
        require(text.contains(startMarker)) { "Шаблон Windows-скрипта изменился: нет маркера начала" }
        require(text.contains(exitMarker)) { "Шаблон Windows-скрипта изменился: нет маркера выхода" }

        val start = listOf(
            startMarker,
            "@rem Switch console to UTF-8 for this run, previous code page is restored on exit",
            "for /f \"tokens=2 delims=:\" %%a in ('chcp') do set \"CHECKUSHKA_OLD_CP=%%a\"",
            "set \"CHECKUSHKA_OLD_CP=%CHECKUSHKA_OLD_CP: =%\"",
            "chcp 65001 >nul"
        ).joinToString("\r\n")

        val exit = listOf(
            "set \"CHECKUSHKA_EXIT=%ERRORLEVEL%\"",
            "if defined CHECKUSHKA_OLD_CP chcp %CHECKUSHKA_OLD_CP% >nul",
            exitMarker.replace("%ERRORLEVEL%", "%CHECKUSHKA_EXIT%")
        ).joinToString("\r\n")

        file.writeText(text.replace(startMarker, start).replace(exitMarker, exit))
    }
}

dependencies {
    implementation(project(":checkushka-core"))
    // MCP-сервер — одна из команд утилиты (как режим mcp в BSL Language Server);
    // отдельный дистрибутив checkushka-mcp остаётся для тех, кому нужен только сервер
    implementation(project(":checkushka-mcp"))
    implementation(libs.picocli)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

license {
    header = rootProject.file("gradle/LICENSE-HEADER.txt")
    skipExistingHeaders = false
    strictCheck = true
    ext["year"] = "2026"
    ext["name"] = "Checkushka authors and contributors"
    ext["project"] = "Checkushka"
    mapping("java", "SLASHSTAR_STYLE")
    include("**/*.java")
}
