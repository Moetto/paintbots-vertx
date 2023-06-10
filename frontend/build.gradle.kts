import com.github.gradle.node.npm.task.NpxTask

plugins {
    kotlin("multiplatform")
    id("com.github.node-gradle.node") version "5.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        sourceSets {
            val jvmMain by getting {
                resources.sourceDirectories.files.clear()
            }
        }
    }

    js(IR) {
        browser {
        }
        binaries.executable()
        sourceSets {
            val jsMain by getting {
                resources.source(objects.sourceDirectorySet("Tailwind", "Tailwind processed css").srcDir("${project.buildDir}/tailwind"))
                dependencies {
                    implementation(npm("tailwindcss", "3.2.1"))
                    implementation(project(":models"))
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.7.1")
                    implementation("io.ktor:ktor-client-websockets:2.3.0")
                    implementation("io.ktor:ktor-client-js:2.3.0")
                    implementation("dev.fritz2:core:1.0-RC5")
                    implementation("io.arrow-kt:arrow-core:1.2.0-RC")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.0")
                }
            }
        }
    }
}

tasks {
    val tailWindBuild by registering(NpxTask::class) {
        group = "build"
        description = "Build tailwind css"
        command.set("tailwindcss")
        args.set(
            listOf(
                "build",
                "-i",
                "src/jsMain/tailwind/main.css",
                "-o",
                "build/tailwind/main.css"
            )
        )
        inputs.files("src/jsMain/tailwind/main.css")
        inputs.files("tailwind.config.js")
        inputs.files(fileTree("src/jsMain/kotlin") {
            include("**/*.kt")
        }).withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.files("build/tailwind/main.css")
    }

    val jsProcessResources by getting {
        dependsOn(tailWindBuild)
    }

    val jsBrowserDistribution by getting

    val jvmJar by getting(Jar::class) {
        dependsOn(jsBrowserDistribution)
        from(files("build/distributions/"))
        rename {
            "webroot/$it"
        }
    }

}
