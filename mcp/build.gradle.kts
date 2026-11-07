plugins {
    `java-library`
    id("cloud.rio.license")
}

dependencies {
    implementation(project(":checkushka-core"))

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
