plugins {
    id("cloud.rio.license") version "0.18.0" apply false
}

allprojects {
    group = "ru.pravets.checkushka"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        // classfile отключён: mdclasses несёт аннотации SpotBugs, которых нет в нашем classpath
        options.compilerArgs.addAll(listOf("-Xlint:all,-classfile", "-parameters"))
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.named("check") {
        dependsOn(tasks.withType<org.gradle.testing.jacoco.tasks.JacocoReport>())
    }
}
