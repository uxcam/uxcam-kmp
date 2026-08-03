package com.uxcam.kmp.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

abstract class UXCamKmpExtension {
    /**
     * Fail the build with an actionable message when the consumer's Kotlin version is older than
     * the version the uxcam-kmp klib was built with. Defaults to `true`.
     */
    abstract val verifyKotlinVersion: Property<Boolean>

    /**
     * Also install `com.uxcam:uxcam-compose` (the `Modifier.uxcamOcclude` helpers) when a Compose
     * plugin is detected on the consumer. Non-Compose consumers never get the artifact regardless
     * of this flag. Defaults to `true`.
     */
    abstract val installComposeHelpers: Property<Boolean>

    /**
     * Export the UXCamKMP API into every Apple framework this module builds, so it can be called
     * directly from Swift/Objective-C (adds the wrapper as an `api` dependency and `export`s it
     * on each framework binary). Defaults to `false` — the recommended pattern is calling UXCam
     * through your own shared Kotlin facade.
     */
    abstract val exportToIosFrameworks: Property<Boolean>

    /**
     * Log a reminder to link the native UXCam iOS SDK in Xcode (SwiftPM) when this module builds
     * iOS targets without the CocoaPods plugin. Defaults to `true`.
     */
    abstract val iosLinkReminder: Property<Boolean>

    /**
     * The `com.uxcam:uxcam-kmp` / `com.uxcam:uxcam-compose` version the plugin installs.
     * Defaults to the version this plugin was released with; override only to pick up a library
     * hotfix without waiting for a plugin release.
     */
    abstract val libraryVersion: Property<String>
}

private const val COCOAPODS_PLUGIN_ID = "org.jetbrains.kotlin.native.cocoapods"
private const val UXCAM_POD_NAME = "UXCam"

private val COMPOSE_PLUGIN_IDS = listOf(
    "org.jetbrains.compose",
    "org.jetbrains.kotlin.plugin.compose",
)

/**
 * Adds the KMP library dependency and uses the standard native dependency boundary:
 *
 *  - Android receives the UXCam Android SDK transitively from `uxcam-kmp`.
 *  - A Kotlin CocoaPods build receives a link-only `UXCam` pod.
 *  - A SwiftPM app links `uxcam-ios` directly in Xcode; the KMP framework stays static and
 *    leaves native SDK symbols for that final app link (a reminder is logged — see
 *    [UXCamKmpExtension.iosLinkReminder]).
 *
 * The plugin never downloads, rewrites, or merges Apple frameworks.
 */
class UXCamKmpPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("uxcamKmp", UXCamKmpExtension::class.java)
        ext.verifyKotlinVersion.convention(true)
        ext.installComposeHelpers.convention(true)
        ext.exportToIosFrameworks.convention(false)
        ext.iosLinkReminder.convention(true)
        ext.libraryVersion.convention(UXCamVersions.UXCAM_KMP)

        val pm = project.pluginManager
        val log = project.logger

        KotlinPluginLoadCheck.register(project)

        pm.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            // Added as a provider so a later `uxcamKmp { libraryVersion.set(...) }` is honoured.
            project.dependencies.addProvider(
                "commonMainImplementation",
                ext.libraryVersion.map { "com.uxcam:uxcam-kmp:$it" },
            )
            log.info("[uxcam-kmp] added com.uxcam:uxcam-kmp to commonMain")

            pm.withPlugin(COCOAPODS_PLUGIN_ID) {
                if (!HostManager.hostIsMac) return@withPlugin
                val cocoapods = (kotlin as ExtensionAware)
                    .extensions.getByType(CocoapodsExtension::class.java)
                if (cocoapods.pods.findByName(UXCAM_POD_NAME) == null) {
                    cocoapods.pod(UXCAM_POD_NAME) {
                        version = UXCamVersions.UXCAM_IOS
                        linkOnly = true
                        extraOpts = extraOpts + listOf("-compiler-option", "-fmodules")
                    }
                }
                val current = cocoapods.ios.deploymentTarget
                if (current == null ||
                    compareVersions(current, UXCamIosSdk.MIN_IOS_DEPLOYMENT_TARGET) < 0
                ) {
                    cocoapods.ios.deploymentTarget = UXCamIosSdk.MIN_IOS_DEPLOYMENT_TARGET
                }
                kotlin.addSwiftCompatLibrarySearchPath(project)
                kotlin.warnOnDynamicCocoapodsFrameworks(project)
                log.info(
                    "[uxcam-kmp] wired link-only UXCam ${UXCamVersions.UXCAM_IOS} CocoaPod",
                )
            }

            installComposeHelpersWhenDetected(project, ext) {
                kotlin.sourceSets.getByName("commonMain").dependencies {
                    implementation("com.uxcam:uxcam-compose:${ext.libraryVersion.get()}")
                }
                log.info("[uxcam-kmp] Compose detected — added com.uxcam:uxcam-compose")
            }

            project.whenEvaluated {
                if (ext.verifyKotlinVersion.get()) project.verifyKotlinVersion()
                if (ext.exportToIosFrameworks.get()) {
                    kotlin.exportUXCamKmpFromAppleFrameworks(ext.libraryVersion.get())
                }
                if (ext.iosLinkReminder.get() && !pm.hasPlugin(COCOAPODS_PLUGIN_ID)) {
                    kotlin.remindAboutIosAppLink(project)
                }
            }
        }

        project.afterEvaluate {
            if (pm.hasPlugin("org.jetbrains.kotlin.multiplatform")) return@afterEvaluate
            if (pm.hasPlugin("com.android.application") || pm.hasPlugin("com.android.library")) {
                project.dependencies.add(
                    "implementation",
                    "com.uxcam:uxcam-kmp:${ext.libraryVersion.get()}",
                )
                if (ext.installComposeHelpers.get() &&
                    COMPOSE_PLUGIN_IDS.any { pm.hasPlugin(it) }
                ) {
                    project.dependencies.add(
                        "implementation",
                        "com.uxcam:uxcam-compose:${ext.libraryVersion.get()}",
                    )
                }
            }
        }
    }
}

/** Runs [action] after the project is evaluated, or immediately when it already is. */
private fun Project.whenEvaluated(action: () -> Unit) {
    if (state.executed) action() else afterEvaluate { action() }
}

/**
 * Installs the Compose helpers as soon as a Compose plugin is applied — order-independent,
 * unlike a single afterEvaluate + hasPlugin check. The [UXCamKmpExtension.installComposeHelpers]
 * knob is read after evaluation so build-script configuration is honoured.
 */
private fun installComposeHelpersWhenDetected(
    project: Project,
    ext: UXCamKmpExtension,
    install: () -> Unit,
) {
    var handled = false
    COMPOSE_PLUGIN_IDS.forEach { id ->
        project.pluginManager.withPlugin(id) {
            if (handled) return@withPlugin
            handled = true
            project.whenEvaluated { if (ext.installComposeHelpers.get()) install() }
        }
    }
}

/**
 * Makes the UXCamKMP API callable from Swift/Objective-C: exported framework APIs must be `api`
 * dependencies of the exporting module AND explicitly exported per framework binary. Opt-in via
 * [UXCamKmpExtension.exportToIosFrameworks].
 */
internal fun KotlinMultiplatformExtension.exportUXCamKmpFromAppleFrameworks(version: String) {
    val notation = "com.uxcam:uxcam-kmp:$version"
    sourceSets.getByName("commonMain").dependencies { api(notation) }
    targets.withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach { target ->
            target.binaries.withType(Framework::class.java).configureEach { it.export(notation) }
        }
}

/**
 * On the SwiftPM path (iOS targets, no CocoaPods) the KMP framework leaves the native UXCam
 * symbols for the final Xcode app link; forgetting to add the package fails late with raw
 * linker errors. Remind early instead.
 */
internal fun KotlinMultiplatformExtension.remindAboutIosAppLink(project: Project) {
    val hasIosTarget = targets.withType(KotlinNativeTarget::class.java)
        .any { it.konanTarget.family == Family.IOS }
    if (!hasIosTarget) return
    project.logger.lifecycle(
        "[uxcam-kmp] ${project.path}: iOS app link — add the native UXCam SDK to the iOS app in " +
            "Xcode via SwiftPM: https://github.com/uxcam/uxcam-ios (version ${UXCamVersions.UXCAM_IOS}). " +
            "(Using CocoaPods or already added? Silence with uxcamKmp { iosLinkReminder.set(false) })",
    )
}

/**
 * Fails with an actionable message when the consumer Kotlin plugin cannot read this library's
 * metadata or Kotlin/Native platform references.
 */
internal fun Project.verifyKotlinVersion() {
    val kotlinVersion = runCatching { getKotlinPluginVersion() }.getOrNull() ?: return
    if (compareVersions(kotlinVersion, UXCamVersions.MIN_KOTLIN) < 0) {
        throw GradleException(
            """
            [uxcam-kmp] UXCam KMP requires Kotlin ${UXCamVersions.MIN_KOTLIN} or newer, but this project uses $kotlinVersion.
            The com.uxcam:uxcam-kmp library carries klib metadata from that Kotlin line and references
            Kotlin/Native platform libraries older Kotlin versions can't read or provide.

            Fix: set the Kotlin version to ${UXCamVersions.MIN_KOTLIN} or newer (e.g. in gradle/libs.versions.toml).
            To bypass this check: uxcamKmp { verifyKotlinVersion.set(false) }
            """.trimIndent(),
        )
    }
}

/**
 * A dynamic CocoaPods framework absorbs UXCam's static objects while the generated podspec also
 * links the pod into the app. Warn because that creates two SDK copies with separate state.
 */
internal fun KotlinMultiplatformExtension.warnOnDynamicCocoapodsFrameworks(project: Project) {
    targets.withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach { target ->
            target.binaries.withType(Framework::class.java).configureEach { framework ->
                if (framework.isStatic) return@configureEach
                project.logger.warn(
                    "[uxcam-kmp] '${target.name}:${framework.name}' is dynamic. Use a static " +
                        "Kotlin framework so CocoaPods resolves UXCam exactly once at the app link.",
                )
            }
        }
}

/**
 * CocoaPods links UXCam's static Swift SDK while producing native test and dynamic framework
 * binaries, so Kotlin/Native must be able to find the toolchain's Swift compatibility libraries.
 */
internal fun KotlinMultiplatformExtension.addSwiftCompatLibrarySearchPath(project: Project) {
    targets.withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach { target ->
            val opts = SwiftRuntimeLibraries.linkerOpts(project, target.targetName)
            if (opts.isEmpty()) return@configureEach
            target.binaries.withType(Framework::class.java).configureEach { it.linkerOpts(opts) }
            target.binaries.withType(TestExecutable::class.java).configureEach { it.linkerOpts(opts) }
        }
}
