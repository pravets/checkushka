plugins {
    `java-library`
    application
    id("com.gradleup.shadow")
    id("cloud.rio.license")
}

application {
    mainClass = "ru.pravets.checkushka.cli.Main"
    applicationName = "checkushka"
    // Кодировку stdout/stderr выбирает ConsoleStreams: кодировка консоли, если она есть,
    // иначе UTF-8. Свойства -Dstdout.encoding здесь не задаются намеренно, иначе JDK
    // считал бы консоль UTF-8 и в обычном cmd с Cp866 получалась бы каша.
}

/**
 * Готовая поставка одним файлом, как `-exec.jar` у BSL Language Server:
 * `java -jar checkushka-<версия>-exec.jar`.
 */
tasks.shadowJar {
    archiveBaseName = "checkushka"
    archiveClassifier = "exec"
    // MCP SDK и Jackson находят реализации через META-INF/services: при склейке
    // файлы нужно объединять, иначе сервер не стартует из fat-jar
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    manifest {
        attributes(
            "Main-Class" to "ru.pravets.checkushka.cli.Main",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Готовой поставкой считаем application-дистрибутив (скрипты + lib) и exec-jar.
// Собственные «shadow»-дистрибутивы плагина не нужны: они дублируют zip/tar
tasks.matching { it.name.startsWith("shadowDist") }.configureEach { enabled = false }

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Тест MCP запускает fat-jar как отдельный процесс, поэтому jar должен быть собран заранее
tasks.named<Test>("test") {
    dependsOn(tasks.shadowJar)
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
