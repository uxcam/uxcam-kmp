package com.uxcam.kmp

import android.util.Log
import com.uxcam.OnVerificationListener
import com.uxcam.UXCam as NativeUXCam
import com.uxcam.datamodel.UXConfig as NativeUXConfig
import com.uxcam.screenshot.model.UXCamBlur
import com.uxcam.screenshot.model.UXCamOcclusion as NativeOcclusion
import com.uxcam.screenshot.model.UXCamOverlay
import org.json.JSONObject

private const val TAG = "UXCamKMP"

actual object UXCamKMP {

    // Android removes occlusions by the same native object instance that was applied. Keep both
    // startup configuration rules and runtime rules so removeOcclusion() can clear all of them.
    private val activeOcclusions = TrackedOcclusions<NativeOcclusion>()

    // Latest verification callbacks; see addVerificationListener.
    private var onVerificationSuccess: (() -> Unit)? = null
    private var onVerificationFailure: ((String) -> Unit)? = null
    private var verificationListenerRegistered = false

    // --- Lifecycle & session ---

    actual fun startWithConfiguration(configuration: UXCamConfiguration) {
        if (!UXCamStartGuard.tryStart()) {
            Log.w(TAG, "already started — ignoring duplicate startWithConfiguration")
            return
        }
        NativeUXCam.pluginType("kmp", UXCamKmpVersions.KMP)
        val startupOcclusions = configuration.occlusions.map { it.toNativeOcclusion() }
        NativeUXCam.startWithConfiguration(configuration.toNativeConfig(startupOcclusions))
        activeOcclusions.replaceWith(startupOcclusions)
        if (configuration.occludeAllTextFields) NativeUXCam.occludeAllTextFields(true)
    }

    actual fun startWithKey(appKey: String) =
        startWithConfiguration(UXCamConfiguration(appKey))

    actual fun startNewSession() = NativeUXCam.startNewSession()

    actual fun stopSessionAndUploadData() {
        UXCamStartGuard.markStopped()
        NativeUXCam.stopSessionAndUploadData()
        activeOcclusions.clear()
    }

    actual fun stopSessionAndUploadData(onSessionStopped: () -> Unit) {
        UXCamStartGuard.markStopped()
        NativeUXCam.stopSessionAndUploadData {
            activeOcclusions.clear()
            onSessionStopped()
        }
    }

    actual fun cancelCurrentSession() {
        UXCamStartGuard.markStopped()
        NativeUXCam.cancelCurrentSession()
        activeOcclusions.clear()
    }

    // --- Events ---

    actual fun logEvent(eventName: String) = NativeUXCam.logEvent(eventName)

    actual fun logEvent(eventName: String, properties: Map<String, Any?>?) {
        // Static Map type selects the native Map overload (vs JSONObject); null hits the no-JSON path.
        NativeUXCam.logEvent(eventName, properties)
    }

    actual fun logEventWithJson(eventName: String, json: String?) {
        NativeUXCam.logEvent(eventName, json?.let(::parseJsonOrNull))
    }

    // --- Bug & exception reporting ---

    actual fun reportBugEvent(eventName: String) = NativeUXCam.reportBugEvent(eventName)

    actual fun reportBugEvent(eventName: String, properties: Map<String, Any?>?) {
        NativeUXCam.reportBugEvent(eventName, properties)
    }

    actual fun reportBugEventWithJson(eventName: String, json: String?) {
        NativeUXCam.reportBugEvent(eventName, json?.let(::parseJsonOrNull))
    }

    actual fun reportExceptionEvent(throwable: Throwable) =
        NativeUXCam.reportExceptionEvent(throwable)

    actual fun reportExceptionEvent(throwable: Throwable, properties: Map<String, Any?>?) {
        if (properties == null) NativeUXCam.reportExceptionEvent(throwable)
        else NativeUXCam.reportExceptionEvent(throwable, properties)
    }

    // --- User identity & properties ---

    actual fun setUserIdentity(userIdentity: String) = NativeUXCam.setUserIdentity(userIdentity)
    actual fun setUserProperty(propertyName: String, value: String) = NativeUXCam.setUserProperty(propertyName, value)
    actual fun setUserProperty(propertyName: String, value: Int) = NativeUXCam.setUserProperty(propertyName, value)
    actual fun setUserProperty(propertyName: String, value: Float) = NativeUXCam.setUserProperty(propertyName, value)
    actual fun setUserProperty(propertyName: String, value: Boolean) = NativeUXCam.setUserProperty(propertyName, value)
    actual fun setPushNotificationToken(token: String) = NativeUXCam.setPushNotificationToken(token)

    // --- Session properties ---

    actual fun setSessionProperty(propertyName: String, value: String) = NativeUXCam.setSessionProperty(propertyName, value)
    actual fun setSessionProperty(propertyName: String, value: Int) = NativeUXCam.setSessionProperty(propertyName, value)
    actual fun setSessionProperty(propertyName: String, value: Float) = NativeUXCam.setSessionProperty(propertyName, value)
    actual fun setSessionProperty(propertyName: String, value: Boolean) = NativeUXCam.setSessionProperty(propertyName, value)
    // --- Screen tagging & ignore lists ---

    actual fun tagScreenName(screenName: String) = NativeUXCam.tagScreenName(screenName)
    actual fun addScreenNameToIgnore(screenName: String) = NativeUXCam.addScreenNameToIgnore(screenName)
    actual fun addScreenNamesToIgnore(screenNames: List<String>) = NativeUXCam.addScreenNamesToIgnore(screenNames)
    actual fun removeScreenNameToIgnore(screenName: String) = NativeUXCam.removeScreenNameToIgnore(screenName)
    actual fun removeScreenNamesToIgnore(screenNames: List<String>) = NativeUXCam.removeScreenNamesToIgnore(screenNames)
    actual fun removeAllScreenNamesToIgnore() = NativeUXCam.removeAllScreenNamesToIgnore()
    actual fun screenNamesBeingIgnored(): List<String> = NativeUXCam.screenNamesBeingIgnored()

    // --- Occlusion (screen-level) ---

    actual fun occludeSensitiveScreen(hideScreen: Boolean) = NativeUXCam.occludeSensitiveScreen(hideScreen)
    actual fun occludeSensitiveScreen(hideScreen: Boolean, withoutGesture: Boolean) =
        NativeUXCam.occludeSensitiveScreen(hideScreen, withoutGesture)
    actual fun occludeAllTextFields(occludeAll: Boolean) = NativeUXCam.occludeAllTextFields(occludeAll)

    actual fun applyOverlayOcclusion(overlayOcclusion: KMPUXCamOverlay) =
        applyOcclusion(overlayOcclusion.toNativeOcclusion())

    actual fun applyBlurOcclusion(blurOcclusion: KMPUXCamBlur) =
        applyOcclusion(blurOcclusion.toNativeOcclusion())

    actual fun removeOcclusion() {
        activeOcclusions.drain().forEach(NativeUXCam::removeOcclusion)
    }

    // --- Recording control ---

    actual fun pauseScreenRecording() = NativeUXCam.pauseScreenRecording()
    actual fun resumeScreenRecording() = NativeUXCam.resumeScreenRecording()
    actual fun isRecording(): Boolean = NativeUXCam.isRecording()
    actual fun allowShortBreakForAnotherApp() = NativeUXCam.allowShortBreakForAnotherApp()
    actual fun allowShortBreakForAnotherApp(continueSession: Boolean) =
        NativeUXCam.allowShortBreakForAnotherApp(continueSession)
    actual fun allowShortBreakForAnotherApp(millis: Int) = NativeUXCam.allowShortBreakForAnotherApp(millis)
    // --- Opt in / out ---

    actual fun optInOverall() = NativeUXCam.optInOverall()
    actual fun optOutOverall() = NativeUXCam.optOutOverall()
    actual fun optInOverallStatus(): Boolean = NativeUXCam.optInOverallStatus()
    actual fun optIntoVideoRecording() = NativeUXCam.optIntoVideoRecording()
    actual fun optOutOfVideoRecording() = NativeUXCam.optOutOfVideoRecording()
    actual fun optInVideoRecordingStatus(): Boolean = NativeUXCam.optInVideoRecordingStatus()

    // --- Multi-session ---

    actual fun getMultiSessionRecord(): Boolean = NativeUXCam.getMultiSessionRecord()
    actual fun setMultiSessionRecord(enable: Boolean) = NativeUXCam.setMultiSessionRecord(enable)

    // --- Crash handling ---

    actual fun disableCrashHandling(disabled: Boolean) = NativeUXCam.disableCrashHandling(disabled)

    // --- Verification ---

    actual fun addVerificationListener(onSuccess: () -> Unit, onFailure: (errorMessage: String) -> Unit) {
        // The native SDK accumulates listeners and has no removal API. Register one native
        // listener and dispatch to the latest callbacks so re-registration replaces the previous
        // listener — same semantics as iOS.
        onVerificationSuccess = onSuccess
        onVerificationFailure = onFailure
        if (verificationListenerRegistered) return
        verificationListenerRegistered = true
        NativeUXCam.addVerificationListener(object : OnVerificationListener {
            override fun onVerificationSuccess() {
                onVerificationSuccess?.invoke()
            }

            override fun onVerificationFailed(errorMessage: String?) {
                onVerificationFailure?.invoke(errorMessage ?: "")
            }
        })
    }

    // --- URLs, uploads & status ---

    actual fun urlForCurrentSession(): String? = NativeUXCam.urlForCurrentSession()
    actual fun urlForCurrentUser(): String? = NativeUXCam.urlForCurrentUser()
    actual fun deletePendingUploads() = NativeUXCam.deletePendingUploads()
    actual fun pendingSessionCount(): Int = NativeUXCam.pendingSessionCount()
    actual fun pendingUploads(onResult: (count: Int) -> Unit) =
        NativeUXCam.pendingUploads(NativeUXCam.OnPendingUploadsCallback { count -> onResult(count) })
    actual fun getSdkVersionInfo(): String = NativeUXCam.getSdkVersionInfo()

    // --- Internal helpers ---

    private fun parseJsonOrNull(json: String): JSONObject? = runCatching { JSONObject(json) }
        .onFailure { Log.w(TAG, "invalid JSON properties dropped — ${it.message}") }
        .getOrNull()

    // Additive, matching the native SDKs and iOS: applying a rule never removes rules that are
    // already active. removeOcclusion() clears them all.
    private fun applyOcclusion(occlusion: NativeOcclusion) {
        NativeUXCam.applyOcclusion(occlusion)
        activeOcclusions.add(occlusion)
    }
}

private fun UXCamConfiguration.toNativeConfig(
    nativeOcclusions: List<NativeOcclusion>,
): NativeUXConfig =
    NativeUXConfig.Builder(userAppKey)
        .enableAutomaticScreenNameTagging(enableAutomaticScreenNameTagging)
        .enableMultiSessionRecord(enableMultiSessionRecord)
        .enableCrashHandling(enableCrashHandling)
        .enableIntegrationLogging(enableIntegrationLogging)
        .apply {
            if (nativeOcclusions.isNotEmpty()) {
                occlusions(nativeOcclusions)
            }
        }
        .build()

internal fun Occlusion.toNativeOcclusion(): NativeOcclusion = when (this) {
    is KMPUXCamOverlay -> UXCamOverlay.Builder()
        .withoutGesture(hideGestures)
        .apply { screens?.let { screens(it) } }
        .excludeMentionedScreens(excludeMentionedScreens)
        .build()
    is KMPUXCamBlur -> UXCamBlur.Builder()
        .blurRadius(blurRadius)
        .withoutGesture(hideGestures)
        .apply { screens?.let { screens(it) } }
        .excludeMentionedScreens(excludeMentionedScreens)
        .build()
}
