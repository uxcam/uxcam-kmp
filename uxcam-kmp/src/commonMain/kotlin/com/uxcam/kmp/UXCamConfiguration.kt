package com.uxcam.kmp

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

data class UXCamConfiguration(
    val userAppKey: String,
    val enableMultiSessionRecord: Boolean = true,
    val enableCrashHandling: Boolean = true,
    val enableIntegrationLogging: Boolean = false,
    val occlusions: List<Occlusion> = emptyList(),
) {
    init {
        require(userAppKey.isNotBlank()) { "UXCam app key must not be blank" }
    }
}

// The data class is the single source of truth for default values; the builder starts from a
// default instance and overrides only what the caller sets.
class UXCamConfigurationBuilder(appKey: String) {
    private val defaults = UXCamConfiguration(appKey)

    var enableMultiSessionRecord: Boolean = defaults.enableMultiSessionRecord
    var enableCrashHandling: Boolean = defaults.enableCrashHandling
    var enableIntegrationLogging: Boolean = defaults.enableIntegrationLogging
    var occlusions: List<Occlusion> = defaults.occlusions

    fun build(): UXCamConfiguration = defaults.copy(
        enableMultiSessionRecord = enableMultiSessionRecord,
        enableCrashHandling = enableCrashHandling,
        enableIntegrationLogging = enableIntegrationLogging,
        occlusions = occlusions,
    )
}

fun uxcamConfiguration(
    appKey: String,
    block: UXCamConfigurationBuilder.() -> Unit = {},
): UXCamConfiguration = UXCamConfigurationBuilder(appKey).apply(block).build()

@OptIn(ExperimentalAtomicApi::class)
internal object UXCamStartGuard {
    // True while a session started via startWithConfiguration/startWithKey is live. Guards
    // against duplicate starts; reset by stopSessionAndUploadData()/cancelCurrentSession() so
    // the SDK can be restarted (the native SDKs allow stop → start).
    private val started = AtomicBoolean(false)

    val isStarted: Boolean get() = started.load()

    /** Atomically claims the started state; false when a live start already holds it. */
    fun tryStart(): Boolean = started.compareAndSet(expectedValue = false, newValue = true)

    fun markStopped() {
        started.store(false)
    }
}
