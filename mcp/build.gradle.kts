plugins {
    `java-library`
    application
    id("cloud.rio.license")
}

application {
    mainClass = "ru.pravets.checkushka.mcp.CheckushkaMcp"
    applicationName = "checkushka-mcp"
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
