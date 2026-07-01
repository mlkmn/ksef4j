plugins {
    `java-library`
    `maven-publish`
}

description = "Spring Boot autoconfiguration for ksef4j"

// The Spring Boot configuration-processor only claims @ConfigurationProperties / @DefaultValue,
// so -Xlint:processing would warn on the other Spring annotations (@Bean, @AutoConfiguration, etc.).
// Suppress that single sub-lint while keeping -Xlint:all -Werror for everything else.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-processing")
}

dependencies {
    api(project(":ksef4j-core"))
    api(libs.spring.boot.starter)
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertj.core)
    testImplementation(testFixtures(project(":ksef4j-core")))
    testRuntimeOnly(libs.junit.platform.launcher)
}
