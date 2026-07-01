import org.gradle.external.javadoc.StandardJavadocDocletOptions

allprojects {
    group = "io.github.mlkmn"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    plugins.withId("java-library") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
            withJavadocJar()
            withSourcesJar()
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

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        // Propagate attribution into every published jar (main, sources,
        // javadoc) per Apache-2.0 4(d) and the bundled FA(3) XSD's MIT terms.
        tasks.withType<Jar>().configureEach {
            from(rootProject.layout.projectDirectory.file("LICENSE")) { into("META-INF") }
            from(rootProject.layout.projectDirectory.file("NOTICE")) { into("META-INF") }
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("Spring-Boot-first Java library for KSeF 2.0")
                        url.set("https://github.com/mlkmn/ksef4j")
                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0")
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
    }
}
