import java.security.MessageDigest
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("uxcam-publishing")
}

group = "com.uxcam"
version = libs.versions.uxcamKmp.get()

val uxcamIosVersion = libs.versions.uxcamIos.get()
val uxcamIosChecksum = libs.versions.uxcamIosChecksum.get()
val uxcamWorkDir = layout.buildDirectory.dir("uxcam")
val uxcamXcframeworkDir = uxcamWorkDir.map { it.dir("UXCam.xcframework") }

/**
 * Verifies the SHA-256 of the resolved UXCam iOS XCFramework zip, then unzips it for cinterop.
 * Uses injected [ArchiveOperations]/[FileSystemOperations] instead of reaching through `project`
 * at execution time, so the task is configuration-cache compatible.
 */
abstract class UnzipUXCamXcframework : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archiveZip: ConfigurableFileCollection

    @get:Input
    abstract val expectedChecksum: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    @get:Inject
    abstract val fileOps: FileSystemOperations

    @TaskAction
    fun extract() {
        val zip = archiveZip.singleFile
        val actualChecksum = MessageDigest.getInstance("SHA-256")
            .digest(zip.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }
        check(actualChecksum.equals(expectedChecksum.get(), ignoreCase = true)) {
            "UXCam XCFramework checksum mismatch\n" +
                "  expected: ${expectedChecksum.get()}\n" +
                "  actual:   $actualChecksum\n" +
                "Update uxcamIosChecksum in gradle/libs.versions.toml if the SDK version changed intentionally."
        }
        val dest = outputDir.get().asFile
        fileOps.delete { delete(dest) }
        fileOps.copy {
            from(archiveOps.zipTree(zip))
            into(dest)
        }
    }
}

// The UXCam iOS XCFramework is modelled as a resolvable dependency: the `uxcamIosSdk` ivy repo in
// settings.gradle.kts maps `com.uxcam.ios:UXCam:<version>@zip` to the GitHub release zip, so Gradle
// downloads + caches it through normal dependency resolution — no hand-rolled download that reaches
// through `project` at execution time.
val uxcamIosFramework: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    uxcamIosFramework("com.uxcam.ios:UXCam:$uxcamIosVersion@zip")
}

val downloadUXCamXcframework by tasks.registering(UnzipUXCamXcframework::class) {
    description = "Verifies + unzips the UXCam iOS XCFramework $uxcamIosVersion for cinterop."
    archiveZip.from(uxcamIosFramework)
    expectedChecksum.set(uxcamIosChecksum)
    outputDir.set(uxcamWorkDir)
}

// Code-generate the wrapper's own version constants from the catalog. Used for native
// pluginType attribution and getSdkVersionInfo(), so they can never drift from the release.
val generateUXCamKmpVersions by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/uxcamVersions/kotlin")
    val kmpVersion = libs.versions.uxcamKmp.get()
    val iosVersion = uxcamIosVersion
    inputs.property("kmpVersion", kmpVersion)
    inputs.property("iosVersion", iosVersion)
    outputs.dir(outputDir)
    doLast {
        val pkgDir = outputDir.get().dir("com/uxcam/kmp").asFile
        pkgDir.mkdirs()
        File(pkgDir, "UXCamKmpVersions.kt").writeText(
            """
            |// Generated from gradle/libs.versions.toml — do not edit.
            |package com.uxcam.kmp
            |
            |internal object UXCamKmpVersions {
            |    const val KMP = "$kmpVersion"
            |    const val IOS_SDK = "$iosVersion"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        publishLibraryVariants("release")
    }

    val xcframework = XCFramework("UXCamKMP")
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64(), iosX64())
    iosTargets.forEach { target ->
        target.compilations.getByName("main").cinterops.create("UXCam") {
            defFile(project.file("src/nativeInterop/cinterop/UXCam.def"))
            val slice = when (target.konanTarget) {
                KonanTarget.IOS_ARM64 -> "ios-arm64"
                else -> "ios-arm64_x86_64-simulator"
            }
            val sliceDir = uxcamXcframeworkDir.get().dir(slice).asFile
            compilerOpts("-F${sliceDir.absolutePath}", "-fmodules")
        }
        target.binaries.framework {
            baseName = "UXCamKMP"
            isStatic = true
            xcframework.add(this)
        }
    }

    // UXCam ships no native SDK for desktop or web, but consumers with multi-target KMP apps
    // call the wrapper from commonMain, so these targets must resolve. They bind to the no-op
    // `noopMain` actuals (wired in sourceSets below) — every call compiles and does nothing.
    jvm()
    js {
        browser()
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    linuxX64()
    mingwX64()

    // Adding the manual noop dependsOn edges below disables KGP's automatic default hierarchy,
    // so re-apply it explicitly — otherwise iosMain/androidMain lose their edge to commonMain
    // and the expect/actual matcher fails.
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain { kotlin.srcDir(generateUXCamKmpVersions) }

        // Intermediate no-op source set shared by the targets UXCam has no native SDK for.
        val noopMain by creating { dependsOn(commonMain.get()) }
        val jvmMain by getting { dependsOn(noopMain) }
        val jsMain by getting { dependsOn(noopMain) }
        val wasmJsMain by getting { dependsOn(noopMain) }
        val linuxMain by getting { dependsOn(noopMain) }
        val mingwMain by getting { dependsOn(noopMain) }

        androidMain.dependencies {
            api(libs.uxcam.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<CInteropProcess>().configureEach {
    dependsOn(downloadUXCamXcframework)
}

android {
    namespace = "com.uxcam.kmp"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Publishing — shared Maven Central convention from build-logic (uxcam-publishing).
uxcamPom {
    name.set("UXCam KMP")
    description.set(
        "Kotlin Multiplatform wrapper for the native UXCam SDKs (Android + iOS) — " +
            "session recording and product analytics through a single shared Kotlin API.",
    )
}
