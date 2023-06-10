plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

val copyResources: Configuration by configurations.creating {}

dependencies {
    copyResources(project(":frontend"))
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":models"))

                // jackson
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
                //jackson core
                implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.0")

                implementation("io.vertx:vertx-web:4.4.3")
                implementation("io.vertx:vertx-lang-kotlin-coroutines:4.4.3")

                implementation("io.vertx:vertx-lang-kotlin:4.4.2")
                implementation("io.arrow-kt:arrow-core:1.2.0-RC")

                runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.92.Final")
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:multik-core:0.2.2")
                implementation("org.jetbrains.kotlinx:multik-default:0.2.2")
                implementation ("com.sksamuel.hoplite:hoplite-core:2.7.4")
            }
        }
    }
}

tasks {
    val jvmJar by getting(Jar::class) {
        dependsOn(copyResources)
        with(copySpec {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(copyResources.map {
                zipTree(it).matching {
                    include("webroot/**")
                }
            })
        })
    }

    val runServer by creating(JavaExec::class) {
        dependsOn(jvmJar)
        classpath = files(jvmJar.archiveFile.get().asFile)
        classpath += sourceSets["main"].runtimeClasspath
        mainClass.set("paintbots.app.AppKt")
        workingDir = rootProject.projectDir
    }
}
