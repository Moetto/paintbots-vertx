rootProject.name = "paintbots-vertx"
include("models")
include("app")
include("frontend")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}
