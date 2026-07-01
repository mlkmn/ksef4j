import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.maven.publish.vanniktech) apply false
}

allprojects {
    group = "io.github.mlkmn"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            // Target only hand-written sources; never touch xjc-generated build/** output.
            target("src/main/java/**/*.java", "src/test/java/**/*.java", "src/testFixtures/java/**/*.java")
            googleJavaFormat("1.22.0")
            removeUnusedImports()
        }
    }

    plugins.withId("java-library") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }

        tasks.withType<Javadoc>().configureEach {
            exclude("**/internal/**")
            (options as StandardJavadocDocletOptions).apply {
                addBooleanOption("Xdoclint:all,-missing", true)
                addBooleanOption("Werror", true)
            }
        }

        // vanniktech's javadoc jar (unlike the old withJavadocJar()) is not
        // wired into assemble/build by default; without this, -Werror
        // doclint checking above would silently stop running on every build.
        tasks.named("check") {
            dependsOn(tasks.withType<Javadoc>())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        // Propagate attribution into every published jar (main, sources,
        // javadoc) per Apache-2.0 4(d) and the bundled FA(3) XSD's MIT terms.
        // Targets AbstractArchiveTask (not just Jar) because vanniktech's
        // javadoc jar task (JavadocJar) does not extend Jar.
        tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
            from(rootProject.layout.projectDirectory.file("LICENSE")) { into("META-INF") }
            from(rootProject.layout.projectDirectory.file("NOTICE")) { into("META-INF") }
        }
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            pom {
                name.set(project.name)
                description.set(project.description ?: "ksef4j - a Java client for KSeF 2.0")
                url.set("https://github.com/mlkmn/ksef4j")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("mlkmn")
                        name.set("Adam Laguna")
                        url.set("https://github.com/mlkmn")
                    }
                }
                scm {
                    url.set("https://github.com/mlkmn/ksef4j")
                    connection.set("scm:git:https://github.com/mlkmn/ksef4j.git")
                    developerConnection.set("scm:git:ssh://git@github.com/mlkmn/ksef4j.git")
                }
            }
        }
    }
}
