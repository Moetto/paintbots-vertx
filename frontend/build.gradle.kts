plugins {
    kotlin("js") version "1.8.21"
}

kotlin {
    js(IR) {
        browser {
        }
        binaries.executable()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.ktor:ktor-client-websockets:2.3.0")
    implementation("io.ktor:ktor-client-js:2.3.0")
}
