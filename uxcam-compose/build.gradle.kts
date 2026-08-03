import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("uxcam-publishing")
}

// Compose occlusion helpers (Modifier.uxcamOcclude) split out of :uxcam-kmp so that:
//  - non-Compose consumers (SwiftUI + Views apps) never get compose-runtime/ui on their
//    runtime graph or a forced Compose version bump,
//  - :uxcam-kmp can declare targets compose-ui is not published for (linux/mingw).
// Same group + version + package (com.uxcam.kmp) as the core wrapper; the Gradle plugin
// auto-installs this artifact only when a Compose plugin is detected on the consumer.
group = "com.uxcam"
version = libs.versions.uxcamKmp.get()

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        publishLibraryVariants("release")
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    jvm()
    js {
        browser()
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        // Targets with no native UXCam SDK bind to the no-op Modifier actual.
        val noopMain by creating { dependsOn(commonMain.get()) }
        val jvmMain by getting { dependsOn(noopMain) }
        val jsMain by getting { dependsOn(noopMain) }
        val wasmJsMain by getting { dependsOn(noopMain) }

        commonMain.dependencies {
            api(project(":uxcam-kmp"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.uxcam.kmp.compose"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Publishing — shared Maven Central convention from build-logic (uxcam-publishing).
uxcamPom {
    name.set("UXCam KMP Compose")
    description.set(
        "Compose Multiplatform occlusion helpers for the UXCam KMP wrapper — " +
            "Modifier.uxcamOcclude hides individual composables from session recordings.",
    )
}
