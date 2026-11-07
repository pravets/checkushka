plugins {
    `java-library`
    application
    id("cloud.rio.license")
}

application {
    mainClass = "ru.pravets.checkushka.cli.Main"
    applicationName = "checkushka"
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
