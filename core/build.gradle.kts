plugins {
    `java-library`
    `maven-publish`
    signing
    id("cloud.rio.license")
}

dependencies {
    api(libs.mdclasses)
    // mdclasses объявляет библиотеку как implementation, но её типы (MDOType,
    // MultiLanguageString) встречаются в публичном API модели, поэтому она нужна
    // и нам, и потребителям core
    api(libs.bsl.common.library)
    api(libs.slf4j.api)
    // Jackson нужен и потребителям библиотеки: CLI собирает JSON для list-checks,
    // будущие интеграции (Sonar, MCP) работают с теми же моделями
    api(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Javadoc-джар обязателен для Maven Central. Локально javadoc упирается в ограничения
// песочницы DSH (инструмент не может создать каталог вывода), поэтому по умолчанию он
// собирается только в CI или по явному -Pjavadoc
val publishJavadoc = providers.environmentVariable("CI").isPresent
    || providers.gradleProperty("javadoc").isPresent

java {
    if (publishJavadoc) {
        withJavadocJar()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("checkushka-core")
                description.set("Проверка метаданных и форм конфигураций 1С:Предприятие на соответствие стандартам")
                url.set("https://github.com/pravets/checkushka")
                licenses {
                    license {
                        name.set("GNU Lesser General Public License v3.0 or later")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("pravets")
                        name.set("Iosif Pravets")
                        email.set("ip@technomarin.ru")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/pravets/checkushka.git")
                    developerConnection.set("scm:git:git@github.com:pravets/checkushka.git")
                    url.set("https://github.com/pravets/checkushka")
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    // Подпись включается только при наличии непустого ключа в окружении: локальная сборка
    // и CI без секретов работают без неё, публикация в Maven Central — с подписью
    val key = providers.environmentVariable("GPG_SIGNING_KEY").orNull
        ?.takeIf { it.isNotBlank() }
    val password = providers.environmentVariable("GPG_SIGNING_PASSWORD").orNull
        ?.takeIf { it.isNotBlank() }
    isRequired = key != null
    if (key != null) {
        useInMemoryPgpKeys(key, password)
    }
    sign(publishing.publications)
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
