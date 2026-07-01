plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    alias(libs.plugins.xjc)
}

description = "Framework-agnostic KSeF 2.0 client"

xjc {
    xsdDir.set(layout.projectDirectory.dir("src/main/resources/fa3"))
    // Package mapping is controlled per-schema by fa3-bindings.xjb; setting
    // defaultPackage here would override the .xjb and re-collide types from
    // the FA and ElementarneTypyDanych namespaces.
}

// The XJC binding customization (.xjb) lives next to the XSDs so it can use
// relative schemaLocation references. It is build-time-only and must not be
// bundled in any published artifact (main jar or sources jar).
tasks.named<Copy>("processResources") {
    exclude("fa3/*.xjb")
}

tasks.named<Jar>("sourcesJar") {
    exclude("fa3/*.xjb")
}

dependencies {
    api(libs.jackson.databind)
    api(libs.jackson.dataformat.yaml)
    api(libs.jackson.datatype.jsr310)

    api(libs.jakarta.xml.bind.api)
    runtimeOnly(libs.jaxb.runtime)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.wiremock)
    testImplementation(project(":ksef4j-test"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("schema-conformance", "smoke")
    }
}

tasks.register<Test>("validateFixtures") {
    description = "Runs FA(3) schema-conformance smoke tests against bundled fixtures."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("schema-conformance")
    }
    // Always rerun (otherwise Gradle caches a PASS even if XSD changes).
    outputs.upToDateWhen { false }
}

tasks.register<Test>("smokeTest") {
    description = "Live happy-path smoke test against the KSeF test environment (opt-in; needs KSEF_TOKEN + COMPANY_NIP)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("smoke")
    }
    // Surface the test's diagnostic System.out lines in the Gradle console.
    testLogging {
        showStandardStreams = true
    }
    // Always rerun (it is a live probe, never up-to-date).
    outputs.upToDateWhen { false }
}

// Test fixtures are for in-repo tests only; never publish them as an artifact.
(components["java"] as AdhocComponentWithVariants).let { java ->
    java.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    java.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}
