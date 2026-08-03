import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.uxcam.kmp")
}

kotlin {
    val xcf = XCFramework("Shared")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.uxcam:uxcam-kmp")).using(project(":uxcam-kmp"))
    }
}
