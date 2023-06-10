plugins {
    kotlin("multiplatform") version "1.8.22"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }
}
