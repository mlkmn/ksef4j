rootProject.name = "ksef4j"

include("ksef4j-core")
include("ksef4j-spring-boot-starter")
include("ksef4j-test")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
