plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.22"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}


kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.0")
            }
        }
    }
    jvm()
    js(IR) {
        browser()
    }
}
