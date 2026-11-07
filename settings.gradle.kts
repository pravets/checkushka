rootProject.name = "checkushka"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("checkushka-core", "checkushka-cli", "checkushka-mcp")

project(":checkushka-core").projectDir = file("core")
project(":checkushka-cli").projectDir = file("cli")
project(":checkushka-mcp").projectDir = file("mcp")
