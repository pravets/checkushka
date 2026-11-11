rootProject.name = "checkushka"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Загрузка бандла в Central Portal (publishAggregationToCentralPortal).
    // Плагин сам собирает публикации всех проектов с maven-publish и отправляет один zip.
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

nmcpSettings {
    centralPortal {
        username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").getOrElse("")
        password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").getOrElse("")
        // Первая публикация (0.2.3) прошла вручную через Central Portal; дальше портал
        // публикует сам, без кнопки Publish
        publishingType = "AUTOMATIC"
        publicationName = "checkushka"
    }
}

include("checkushka-core", "checkushka-cli", "checkushka-mcp")

project(":checkushka-core").projectDir = file("core")
project(":checkushka-cli").projectDir = file("cli")
project(":checkushka-mcp").projectDir = file("mcp")
