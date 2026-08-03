plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    id("uxcam-publishing")
}

group = "com.uxcam"
version = libs.versions.uxcamKmp.get()

// Lowest Kotlin version that can consume the uxcam-kmp klib: the library is built with the
// catalog's Kotlin, which stamps that line's klib metadata — the floor is its minor (x.y.0).
val uxcamMinKotlin = libs.versions.kotlin.get().split(".").take(2).joinToString(".") + ".0"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

// Code-generate the version constants the plugin needs (UXCamVersions) from the catalog so
// they can never drift from the published artifacts.
val generateUXCamVersions by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/uxcamVersions/kotlin")
    val kmp = libs.versions.uxcamKmp.get()
    val ios = libs.versions.uxcamIos.get()
    val minKotlin = uxcamMinKotlin
    inputs.property("uxcamKmpVersion", kmp)
    inputs.property("uxcamIosVersion", ios)
    inputs.property("uxcamMinKotlin", minKotlin)
    outputs.dir(outputDir)
    doLast {
        val pkgDir = outputDir.get().dir("com/uxcam/kmp/gradle").asFile
        pkgDir.mkdirs()
        File(pkgDir, "UXCamVersions.kt").writeText(
            """
            |// Generated from gradle/libs.versions.toml — do not edit.
            |package com.uxcam.kmp.gradle
            |
            |internal object UXCamVersions {
            |    const val UXCAM_KMP = "$kmp"
            |    const val UXCAM_IOS = "$ios"
            |    const val MIN_KOTLIN = "$minKotlin"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin.sourceSets.main { kotlin.srcDir(generateUXCamVersions) }

gradlePlugin {
    plugins {
        create("uxcamKmp") {
            id = "com.uxcam.kmp"
            implementationClass = "com.uxcam.kmp.gradle.UXCamKmpPlugin"
            displayName = "UXCam KMP"
            description = "Wires UXCam into a Kotlin Multiplatform or Android module from a single plugin."
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

uxcamPom {
    name.set("UXCam KMP Gradle Plugin")
    description.set("Wires UXCam into a Kotlin Multiplatform or Android module from a single plugin.")
}
