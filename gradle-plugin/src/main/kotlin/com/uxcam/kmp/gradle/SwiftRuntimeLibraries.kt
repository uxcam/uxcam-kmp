package com.uxcam.kmp.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logging

/**
 * The native UXCam iOS SDK is a **static** framework built with an older Swift toolchain, so its
 * object files carry autolink references to Swift's backward-compatibility static libraries
 * (`libswiftCompatibility56.a`, `libswiftCompatibilityConcurrency.a`, …). Those libraries ship
 * inside the active Xcode toolchain but are NOT on the Kotlin/Native linker's default search path,
 * so linking the consumer's iOS framework — where UXCam's objects are force-loaded — fails with:
 *
 * ```
 * ld: warning: Could not find or use auto-linked library 'swiftCompatibility56'
 * Undefined symbols for architecture arm64:
 *   "__swift_FORCE_LOAD_$_swiftCompatibility56", referenced from:
 *       __swift_FORCE_LOAD_$_swiftCompatibility56_$_UXCam in UXCam(...).o
 * ```
 *
 * The symbols come from real `.a` files that already exist; ld just can't find them. Adding the
 * toolchain's per-platform Swift static-lib directory with `-L` is enough — the autolink hints
 * UXCam already carries then resolve. Both delivery paths link the same static SDK, so both the
 * CocoaPods path and the self-provisioned path inject these options.
 */
internal object SwiftRuntimeLibraries {

    private val logger = Logging.getLogger(SwiftRuntimeLibraries::class.java)

    // Kotlin/Native Apple target → the toolchain subdirectory holding that platform's Swift static
    // compatibility libs. UXCam is currently supported only on iOS targets.
    private val PLATFORM_DIR_BY_TARGET = mapOf(
        "iosArm64" to "iphoneos",
        "iosSimulatorArm64" to "iphonesimulator",
        "iosX64" to "iphonesimulator",
    )

    /**
     * `-L<dir>` options that put the Swift compatibility libs for [targetName] on the linker search
     * path. Empty when the target is unsupported or the Xcode developer dir can't be resolved (the
     * link then behaves as before — no regression, just the original error if it would have failed).
     */
    fun linkerOpts(project: Project, targetName: String): List<String> {
        val platform = PLATFORM_DIR_BY_TARGET[targetName] ?: return emptyList()
        val developerDir = developerDir(project) ?: return emptyList()
        val dir = "$developerDir/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$platform"
        return listOf("-L$dir")
    }

    /** Active Xcode developer dir, from `$DEVELOPER_DIR` (set during Xcode builds) or `xcode-select -p`. */
    private fun developerDir(project: Project): String? {
        System.getenv("DEVELOPER_DIR")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return runCatching {
            project.providers.exec { it.commandLine("xcode-select", "-p") }
                .standardOutput.asText.get().trim()
        }.getOrElse { error ->
            logger.warn(
                "[uxcam-kmp] could not resolve the Xcode developer dir via xcode-select; skipping the " +
                    "Swift compatibility library search path. If the iOS link fails with " +
                    "'__swift_FORCE_LOAD_\$_swiftCompatibility*', set DEVELOPER_DIR or add the " +
                    "'-L<toolchain>/usr/lib/swift/<platform>' linker option manually.",
                error,
            )
            null
        }
    }
}
