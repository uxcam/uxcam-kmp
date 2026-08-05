package com.uxcam.kmp

/**
 * No-op implementation for platforms where UXCam ships no native SDK (desktop/JVM, JS, Wasm,
 * Linux, Windows).
 *
 * Every call compiles and returns a benign default so shared code can reference the full
 * [UXCamKMP] surface without `expect`/platform guards; nothing is recorded on these targets.
 * These targets exist so a multi-target KMP consumer can resolve the dependency from
 * commonMain at all — without them, adding `uxcam-kmp` would fail their build at resolution.
 */
actual object UXCamKMP {

    // --- Lifecycle & session ---
    actual fun startWithConfiguration(configuration: UXCamConfiguration) {}
    actual fun startWithKey(appKey: String) {}
    actual fun startNewSession() {}
    actual fun stopSessionAndUploadData() {}
    actual fun stopSessionAndUploadData(onSessionStopped: () -> Unit) {}
    actual fun cancelCurrentSession() {}

    // --- Events ---
    actual fun logEvent(eventName: String) {}
    actual fun logEvent(eventName: String, properties: Map<String, Any?>?) {}
    actual fun logEventWithJson(eventName: String, json: String?) {}

    // --- Bug & exception reporting ---
    actual fun reportBugEvent(eventName: String) {}
    actual fun reportBugEvent(eventName: String, properties: Map<String, Any?>?) {}
    actual fun reportBugEventWithJson(eventName: String, json: String?) {}
    actual fun reportExceptionEvent(throwable: Throwable) {}
    actual fun reportExceptionEvent(throwable: Throwable, properties: Map<String, Any?>?) {}

    // --- User identity & properties ---
    actual fun setUserIdentity(userIdentity: String) {}
    actual fun setUserProperty(propertyName: String, value: String) {}
    actual fun setUserProperty(propertyName: String, value: Int) {}
    actual fun setUserProperty(propertyName: String, value: Float) {}
    actual fun setUserProperty(propertyName: String, value: Boolean) {}
    actual fun setPushNotificationToken(token: String) {}

    // --- Session properties ---
    actual fun setSessionProperty(propertyName: String, value: String) {}
    actual fun setSessionProperty(propertyName: String, value: Int) {}
    actual fun setSessionProperty(propertyName: String, value: Float) {}
    actual fun setSessionProperty(propertyName: String, value: Boolean) {}
    // --- Screen tagging & ignore lists ---
    actual fun tagScreenName(screenName: String) {}
    actual fun addScreenNameToIgnore(screenName: String) {}
    actual fun addScreenNamesToIgnore(screenNames: List<String>) {}
    actual fun removeScreenNameToIgnore(screenName: String) {}
    actual fun removeScreenNamesToIgnore(screenNames: List<String>) {}
    actual fun removeAllScreenNamesToIgnore() {}
    actual fun screenNamesBeingIgnored(): List<String> = emptyList()

    // --- Occlusion (screen-level) ---
    actual fun occludeSensitiveScreen(hideScreen: Boolean) {}
    actual fun occludeSensitiveScreen(hideScreen: Boolean, withoutGesture: Boolean) {}
    actual fun applyOverlayOcclusion(overlayOcclusion: KMPUXCamOverlay) {}
    actual fun applyBlurOcclusion(blurOcclusion: KMPUXCamBlur) {}
    actual fun removeOcclusion() {}

    // --- Recording control ---
    actual fun pauseScreenRecording() {}
    actual fun resumeScreenRecording() {}
    actual fun isRecording(): Boolean = false
    actual fun allowShortBreakForAnotherApp() {}
    actual fun allowShortBreakForAnotherApp(continueSession: Boolean) {}
    actual fun allowShortBreakForAnotherApp(millis: Int) {}
    // --- Opt in / out ---
    actual fun optInOverall() {}
    actual fun optOutOverall() {}
    actual fun optInOverallStatus(): Boolean = false
    actual fun optIntoVideoRecording() {}
    actual fun optOutOfVideoRecording() {}
    actual fun optInVideoRecordingStatus(): Boolean = false

    // --- Multi-session ---
    actual fun getMultiSessionRecord(): Boolean = false
    actual fun setMultiSessionRecord(enable: Boolean) {}

    // --- Crash handling ---
    actual fun disableCrashHandling(disabled: Boolean) {}

    // --- Verification ---
    actual fun addVerificationListener(onSuccess: () -> Unit, onFailure: (errorMessage: String) -> Unit) {}

    // --- URLs, uploads & status ---
    actual fun urlForCurrentSession(): String? = null
    actual fun urlForCurrentUser(): String? = null
    actual fun deletePendingUploads() {}
    actual fun pendingSessionCount(): Int = 0
    actual fun pendingUploads(onResult: (count: Int) -> Unit) = onResult(0)
    actual fun getSdkVersionInfo(): String = "UXCam KMP ${UXCamKmpVersions.KMP} (no-op platform)"
}
