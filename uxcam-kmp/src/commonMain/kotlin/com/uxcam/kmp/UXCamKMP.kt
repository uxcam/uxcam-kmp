package com.uxcam.kmp

/**
 * Common, platform-neutral facade over the native UXCam SDKs. Mirrors the developer-facing
 * surface of the native API; each call binds to the platform `actual` at compile time
 * (Android → `com.uxcam.UXCam`, iOS → `UXCam.framework`). On platforms without a native
 * UXCam SDK (desktop/JVM, JS, Wasm, Linux, Windows) every call is a benign no-op, so shared
 * code can use the full surface without platform guards.
 *
 * Listener-based native APIs are exposed as Kotlin lambdas. Per-view occlusion
 * (`android.view.View` / `UIView`) has no common type, so it lives on the platform actuals
 * (see `Occlusion.android.kt` / `Occlusion.ios.kt`); per-composable occlusion is provided by
 * the `uxcam-compose` module (`Modifier.uxcamOcclude`).
 *
 * Platform-only native APIs are exposed as platform source-set extensions instead of common
 * methods that silently do nothing elsewhere. Where the native SDKs disagree on names (e.g. iOS
 * "schematic recording" vs Android "video recording") the Android name is used.
 */
expect object UXCamKMP {

    // --- Lifecycle & session ---
    fun startWithConfiguration(configuration: UXCamConfiguration)

    /** Convenience for [startWithConfiguration] with a default configuration. */
    fun startWithKey(appKey: String)
    fun startNewSession()
    fun stopSessionAndUploadData()
    fun stopSessionAndUploadData(onSessionStopped: () -> Unit)
    fun cancelCurrentSession()

    // --- Events ---
    fun logEvent(eventName: String)
    fun logEvent(eventName: String, properties: Map<String, Any?>?)

    /** Properties as a JSON object string (parsed to the native representation per platform). */
    fun logEventWithJson(eventName: String, json: String?)

    // --- Bug & exception reporting ---
    fun reportBugEvent(eventName: String)
    fun reportBugEvent(eventName: String, properties: Map<String, Any?>?)
    fun reportBugEventWithJson(eventName: String, json: String?)
    fun reportExceptionEvent(throwable: Throwable)
    fun reportExceptionEvent(throwable: Throwable, properties: Map<String, Any?>?)

    // --- User identity & properties ---
    fun setUserIdentity(userIdentity: String)
    fun setUserProperty(propertyName: String, value: String)
    fun setUserProperty(propertyName: String, value: Int)
    fun setUserProperty(propertyName: String, value: Float)
    fun setUserProperty(propertyName: String, value: Boolean)
    fun setPushNotificationToken(token: String)

    // --- Session properties ---
    fun setSessionProperty(propertyName: String, value: String)
    fun setSessionProperty(propertyName: String, value: Int)
    fun setSessionProperty(propertyName: String, value: Float)
    fun setSessionProperty(propertyName: String, value: Boolean)
    // --- Screen tagging & ignore lists ---
    fun tagScreenName(screenName: String)
    fun addScreenNameToIgnore(screenName: String)
    fun addScreenNamesToIgnore(screenNames: List<String>)
    fun removeScreenNameToIgnore(screenName: String)
    fun removeScreenNamesToIgnore(screenNames: List<String>)
    fun removeAllScreenNamesToIgnore()
    fun screenNamesBeingIgnored(): List<String>

    // --- Occlusion (screen-level) ---
    fun occludeSensitiveScreen(hideScreen: Boolean)
    fun occludeSensitiveScreen(hideScreen: Boolean, withoutGesture: Boolean)

    /** Applies [overlayOcclusion] in addition to any occlusion rules already active. */
    fun applyOverlayOcclusion(overlayOcclusion: KMPUXCamOverlay)

    /** Applies [blurOcclusion] in addition to any occlusion rules already active. */
    fun applyBlurOcclusion(blurOcclusion: KMPUXCamBlur)

    /**
     * Removes every occlusion rule applied through [applyOverlayOcclusion]/[applyBlurOcclusion].
     *
     * Platform caveat: on iOS, rules requested *before* [startWithConfiguration] are merged into
     * the startup configuration (the only point where the native iOS SDK honours
     * [Occlusion.excludeMentionedScreens]) and the native SDK cannot remove configuration-based
     * occlusions at runtime — on iOS those rules persist for the app's lifetime. On Android all
     * rules, including startup configuration ones, are removed.
     */
    fun removeOcclusion()

    // --- Recording control ---
    fun pauseScreenRecording()
    fun resumeScreenRecording()
    fun isRecording(): Boolean
    fun allowShortBreakForAnotherApp()
    fun allowShortBreakForAnotherApp(continueSession: Boolean)
    fun allowShortBreakForAnotherApp(millis: Int)
    // --- Opt in / out ---
    fun optInOverall()
    fun optOutOverall()
    fun optInOverallStatus(): Boolean
    fun optIntoVideoRecording()
    fun optOutOfVideoRecording()
    fun optInVideoRecordingStatus(): Boolean

    // --- Multi-session ---
    fun getMultiSessionRecord(): Boolean
    fun setMultiSessionRecord(enable: Boolean)

    // --- Crash handling ---
    fun disableCrashHandling(disabled: Boolean)

    // --- Verification ---

    /**
     * Registers callbacks for the SDK's asynchronous app-key verification. Registering again
     * replaces the previously registered listener on both platforms.
     */
    fun addVerificationListener(onSuccess: () -> Unit, onFailure: (errorMessage: String) -> Unit)

    // --- URLs, uploads & status ---
    fun urlForCurrentSession(): String?
    fun urlForCurrentUser(): String?
    fun deletePendingUploads()
    fun pendingSessionCount(): Int
    fun pendingUploads(onResult: (count: Int) -> Unit)
    fun getSdkVersionInfo(): String
}
