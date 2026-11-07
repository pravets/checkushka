plugins {
    `java-library`
    id("cloud.rio.license")
}

dependencies {
    api(libs.mdclasses)
    api(libs.slf4j.api)
    implementation(libs.jackson.databind)

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
