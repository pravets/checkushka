plugins {
    `java-library`
    application
    id("cloud.rio.license")
}

application {
    mainClass = "ru.pravets.checkushka.mcp.CheckushkaMcp"
    applicationName = "checkushka-mcp"
    // MCP по stdio обязан говорить UTF-8: иначе русские сообщения в ответах инструментов
    // уходят клиенту в ANSI-кодировке Windows
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
    implementation(libs.mcp.core)
    implementation(libs.mcp.json.jackson2)
    implementation(libs.jetty.ee11.servlet)
    // Servlet API приходит из контейнера Jetty, для компиляции достаточно compileOnly
    compileOnly(libs.jakarta.servlet.api)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
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
