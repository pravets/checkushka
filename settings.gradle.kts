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
        // Первые выпуски подтверждаются вручную в UI Central Portal; после проверки
        // можно переключить на AUTOMATIC, и портал будет публиковать сам
        publishingType = "USER_MANAGED"
        publicationName = "checkushka"
    }
}

include("checkushka-core", "checkushka-cli", "checkushka-mcp")

project(":checkushka-core").projectDir = file("core")
project(":checkushka-cli").projectDir = file("cli")
project(":checkushka-mcp").projectDir = file("mcp")
