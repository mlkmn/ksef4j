plugins {
    `java-library`
    alias(libs.plugins.maven.publish.vanniktech)
}

description = "WireMock-based mock KSeF server for downstream integration testing"

dependencies {
    api(project(":ksef4j-core"))
    implementation(libs.wiremock)
    compileOnly(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(testFixtures(project(":ksef4j-core")))
}
