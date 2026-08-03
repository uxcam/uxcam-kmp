@file:OptIn(ExperimentalForeignApi::class)

package com.uxcam.kmp

import com.uxcam.kmp.cinterop.UXBlurType
import com.uxcam.kmp.cinterop.UXCamBlurSetting
import com.uxcam.kmp.cinterop.UXCamConfiguration as NativeUXCamConfiguration
import com.uxcam.kmp.cinterop.UXCamOccludeAllTextFields
import com.uxcam.kmp.cinterop.UXCamOcclusion
import com.uxcam.kmp.cinterop.UXCamOcclusionSettingProtocol
import com.uxcam.kmp.cinterop.UXCamOverlaySetting
import com.uxcam.kmp.cinterop.UXCam_VerifyNotification
import com.uxcam.kmp.cinterop.UXCam_VerifyNotification_StartedKey
import com.uxcam.kmp.cinterop.UXCam as NativeUXCam
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSLog
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.UIKit.UIColor
import platform.darwin.NSObjectProtocol
import kotlin.experimental.ExperimentalNativeApi

/**
 * iOS implementation — backed by the native iOS UXCam SDK (`UXCam.framework`, bound via this
 * library's own cinterop; package `com.uxcam.kmp.cinterop`, aliased here as [NativeUXCam]).
 *
 * APIs without a native iOS equivalent are intentionally absent from the common facade and live
 * as Android source-set extensions instead.
 */
actual object UXCamKMP {

    // Token kept so the previous observer can be removed on re-registration.
    private var verificationObserver: NSObjectProtocol? = null

    // Occlusion rules applied before start are queued and merged into the startup configuration
    // (the only point where iOS honours excludeMentionedScreens).
    private val pendingOcclusions = TrackedOcclusions<Occlusion>()

    // --- Lifecycle & session ---

    actual fun startWithConfiguration(configuration: UXCamConfiguration) {
        // Short-circuit order matters: when the native SDK is already recording (started outside
        // the wrapper), the guard must stay unclaimed.
        if (NativeUXCam.isRecording() || !UXCamStartGuard.tryStart()) {
            NSLog("UXCam KMP: already started — ignoring duplicate startWithConfiguration")
            return
        }
        NativeUXCam.pluginType("kmp", version = UXCamKmpVersions.KMP)

        val native = NativeUXCamConfiguration(appKey = configuration.userAppKey)
        native.enableAutomaticScreenNameTagging = configuration.enableAutomaticScreenNameTagging
        native.enableMultiSessionRecord = configuration.enableMultiSessionRecord
        native.enableCrashHandling = configuration.enableCrashHandling
        native.enableIntegrationLogging = configuration.enableIntegrationLogging

        val startupOcclusions = configuration.occlusions + pendingOcclusions.drain()
        if (configuration.occludeAllTextFields || startupOcclusions.isNotEmpty()) {
            val occlusion = UXCamOcclusion(settings = emptyList<UXCamOcclusionSettingProtocol>())
            if (configuration.occludeAllTextFields) {
                occlusion.applySettings(
                    listOf(UXCamOccludeAllTextFields()),
                    screens = emptyList<String>(),
                    excludeMentionedScreens = false,
                )
            }
            startupOcclusions.forEach { rule ->
                occlusion.applySettings(
                    listOf(rule.toNativeSetting()),
                    screens = rule.screens ?: emptyList<String>(),
                    excludeMentionedScreens = rule.excludeMentionedScreens,
                )
            }
            native.occlusion = occlusion
        }
        NativeUXCam.startWithConfiguration(native, completionBlock = { started ->
            // Verification can fail asynchronously (for example, with an invalid app key). Do not
            // leave the wrapper permanently locked in "started" when the native SDK did not start.
            if (!started) UXCamStartGuard.markStopped()
        })
    }

    actual fun startWithKey(appKey: String) =
        startWithConfiguration(UXCamConfiguration(appKey))

    actual fun startNewSession() = NativeUXCam.startNewSession()

    actual fun stopSessionAndUploadData() {
        UXCamStartGuard.markStopped()
        NativeUXCam.stopSessionAndUploadData()
    }

    actual fun stopSessionAndUploadData(onSessionStopped: () -> Unit) {
        UXCamStartGuard.markStopped()
        NativeUXCam.stopSessionAndUploadData(onSessionStopped)
    }

    actual fun cancelCurrentSession() {
        UXCamStartGuard.markStopped()
        NativeUXCam.cancelCurrentSession()
    }

    // --- Events ---

    actual fun logEvent(eventName: String) = NativeUXCam.logEvent(eventName)
    actual fun logEvent(eventName: String, properties: Map<String, Any?>?) =
        NativeUXCam.logEvent(eventName, withProperties = properties?.toNSDictionaryMap())
    actual fun logEventWithJson(eventName: String, json: String?) =
        NativeUXCam.logEvent(eventName, withProperties = json?.let { parseJsonToMap(it) })

    // --- Bug & exception reporting ---

    actual fun reportBugEvent(eventName: String) =
        NativeUXCam.reportBugEvent(eventName, properties = null)
    actual fun reportBugEvent(eventName: String, properties: Map<String, Any?>?) =
        NativeUXCam.reportBugEvent(eventName, properties = properties?.toNSDictionaryMap())
    actual fun reportBugEventWithJson(eventName: String, json: String?) =
        NativeUXCam.reportBugEvent(eventName, properties = json?.let { parseJsonToMap(it) })
    actual fun reportExceptionEvent(throwable: Throwable) =
        reportExceptionEvent(throwable, null)
    @OptIn(ExperimentalNativeApi::class)
    actual fun reportExceptionEvent(throwable: Throwable, properties: Map<String, Any?>?) =
        NativeUXCam.reportExceptionEvent(
            name = throwable::class.simpleName ?: "Throwable",
            reason = throwable.message ?: "",
            callStacks = throwable.getStackTrace().toList(),
            properties = properties?.toNSDictionaryMap(),
        )

    // --- User identity & properties ---

    actual fun setUserIdentity(userIdentity: String) = NativeUXCam.setUserIdentity(userIdentity)
    actual fun setUserProperty(propertyName: String, value: String) =
        NativeUXCam.setUserProperty(propertyName, value = value)
    actual fun setUserProperty(propertyName: String, value: Int) =
        NativeUXCam.setUserProperty(propertyName, value = NSNumber(int = value))
    actual fun setUserProperty(propertyName: String, value: Float) =
        NativeUXCam.setUserProperty(propertyName, value = NSNumber(float = value))
    actual fun setUserProperty(propertyName: String, value: Boolean) =
        NativeUXCam.setUserProperty(propertyName, value = NSNumber(bool = value))
    actual fun setPushNotificationToken(token: String) =
        NativeUXCam.setPushNotificationToken(token)

    // --- Session properties ---

    actual fun setSessionProperty(propertyName: String, value: String) =
        NativeUXCam.setSessionProperty(propertyName, value = value)
    actual fun setSessionProperty(propertyName: String, value: Int) =
        NativeUXCam.setSessionProperty(propertyName, value = NSNumber(int = value))
    actual fun setSessionProperty(propertyName: String, value: Float) =
        NativeUXCam.setSessionProperty(propertyName, value = NSNumber(float = value))
    actual fun setSessionProperty(propertyName: String, value: Boolean) =
        NativeUXCam.setSessionProperty(propertyName, value = NSNumber(bool = value))
    // --- Screen tagging & ignore lists ---

    actual fun tagScreenName(screenName: String) = NativeUXCam.tagScreenName(screenName)
    actual fun addScreenNameToIgnore(screenName: String) = NativeUXCam.addScreenNameToIgnore(screenName)
    actual fun addScreenNamesToIgnore(screenNames: List<String>) = NativeUXCam.addScreenNamesToIgnore(screenNames)
    actual fun removeScreenNameToIgnore(screenName: String) = NativeUXCam.removeScreenNameToIgnore(screenName)
    actual fun removeScreenNamesToIgnore(screenNames: List<String>) = NativeUXCam.removeScreenNamesToIgnore(screenNames)
    actual fun removeAllScreenNamesToIgnore() = NativeUXCam.removeAllScreenNamesToIgnore()
    actual fun screenNamesBeingIgnored(): List<String> =
        NativeUXCam.screenNamesBeingIgnored().filterIsInstance<String>()

    // --- Occlusion (screen-level) ---

    actual fun occludeSensitiveScreen(hideScreen: Boolean) = NativeUXCam.occludeSensitiveScreen(hideScreen)
    actual fun occludeSensitiveScreen(hideScreen: Boolean, withoutGesture: Boolean) =
        NativeUXCam.occludeSensitiveScreen(hideScreen, hideGestures = withoutGesture)
    actual fun occludeAllTextFields(occludeAll: Boolean) = NativeUXCam.occludeAllTextFields(occludeAll)
    actual fun applyOverlayOcclusion(overlayOcclusion: KMPUXCamOverlay) = applyOcclusionRule(overlayOcclusion)
    actual fun applyBlurOcclusion(blurOcclusion: KMPUXCamBlur) = applyOcclusionRule(blurOcclusion)
    actual fun removeOcclusion() {
        // Rules requested through apply* before startup have not reached the native SDK yet, so
        // removing occlusion must clear that pending state as well as native runtime rules.
        pendingOcclusions.clear()
        NativeUXCam.removeOcclusion()
    }

    private fun applyOcclusionRule(rule: Occlusion) {
        if (!UXCamStartGuard.isStarted) {
            pendingOcclusions.add(rule)
            return
        }
        val setting = rule.toNativeSetting()
        val screens = rule.screens
        when {
            screens.isNullOrEmpty() -> NativeUXCam.applyOcclusion(setting)
            rule.excludeMentionedScreens -> {
                NSLog(
                    "UXCam KMP (iOS): excludeMentionedScreens is only honored when the occlusion is " +
                        "applied before startWithConfiguration — applying to the mentioned screens instead",
                )
                NativeUXCam.applyOcclusion(setting, toScreens = screens)
            }
            else -> NativeUXCam.applyOcclusion(setting, toScreens = screens)
        }
    }

    private fun Occlusion.toNativeSetting(): UXCamOcclusionSettingProtocol = when (this) {
        is KMPUXCamOverlay -> UXCamOverlaySetting(color = color.toUIColor())
            .also { it.hideGestures = hideGestures }
        is KMPUXCamBlur -> UXCamBlurSetting(blurType = blurType.toNative(), radius = blurRadius)
            .also { it.hideGestures = hideGestures }
    }

    // --- Recording control ---

    actual fun pauseScreenRecording() = NativeUXCam.pauseScreenRecording()
    actual fun resumeScreenRecording() = NativeUXCam.resumeScreenRecording()
    actual fun isRecording(): Boolean = NativeUXCam.isRecording()
    actual fun allowShortBreakForAnotherApp() = NativeUXCam.allowShortBreakForAnotherApp(true)
    actual fun allowShortBreakForAnotherApp(continueSession: Boolean) =
        NativeUXCam.allowShortBreakForAnotherApp(continueSession)
    actual fun allowShortBreakForAnotherApp(millis: Int) {
        NativeUXCam.setAllowShortBreakMaxDuration(millis.toDouble())
        NativeUXCam.allowShortBreakForAnotherApp(true)
    }
    // --- Opt in / out ---

    actual fun optInOverall() = NativeUXCam.optInOverall()
    actual fun optOutOverall() = NativeUXCam.optOutOverall()
    actual fun optInOverallStatus(): Boolean = NativeUXCam.optInOverallStatus()
    actual fun optIntoVideoRecording() = NativeUXCam.optIntoVideoRecordings()
    actual fun optOutOfVideoRecording() = NativeUXCam.optOutOfVideoRecordings()
    actual fun optInVideoRecordingStatus(): Boolean = NativeUXCam.optInVideoRecordingStatus()

    // --- Multi-session ---

    @Suppress("DEPRECATION")
    actual fun getMultiSessionRecord(): Boolean = NativeUXCam.getMultiSessionRecord()
    @Suppress("DEPRECATION")
    actual fun setMultiSessionRecord(enable: Boolean) = NativeUXCam.setMultiSessionRecord(enable)

    // --- Crash handling ---

    @Suppress("DEPRECATION")
    actual fun disableCrashHandling(disabled: Boolean) = NativeUXCam.disableCrashHandling(disabled)

    // --- Verification --- (iOS reports verification via an NSNotification, not a callback)

    actual fun addVerificationListener(onSuccess: () -> Unit, onFailure: (errorMessage: String) -> Unit) {
        verificationObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        verificationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UXCam_VerifyNotification,
            `object` = null,
            queue = null,
        ) { notification: NSNotification? ->
            val started = (notification?.userInfo?.get(UXCam_VerifyNotification_StartedKey) as? NSNumber)
                ?.boolValue ?: false
            if (started) onSuccess() else onFailure("UXCam verification failed")
        }
    }

    // --- URLs, uploads & status ---

    actual fun urlForCurrentSession(): String? = NativeUXCam.urlForCurrentSession()
    actual fun urlForCurrentUser(): String? = NativeUXCam.urlForCurrentUser()
    actual fun deletePendingUploads() = NativeUXCam.deletePendingUploads()
    actual fun pendingSessionCount(): Int = NativeUXCam.pendingUploads().toInt()
    actual fun pendingUploads(onResult: (count: Int) -> Unit) = onResult(NativeUXCam.pendingUploads().toInt())
    actual fun getSdkVersionInfo(): String =
        "UXCam KMP ${UXCamKmpVersions.KMP} (iOS SDK ${UXCamKmpVersions.IOS_SDK})"
}

// --- Bridging helpers ---

/** Bridges a Kotlin map to the `NSDictionary<NSString*, id>` the iOS SDK expects. */
@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.toNSDictionaryMap(): Map<Any?, *> = this as Map<Any?, *>

/** Parses a JSON object string into a property map via NSJSONSerialization; null on failure. */
@OptIn(BetaInteropApi::class)
private fun parseJsonToMap(json: String): Map<Any?, *>? {
    val data = NSString.create(string = json).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    @Suppress("UNCHECKED_CAST")
    return NSJSONSerialization.JSONObjectWithData(data, options = 0u, error = null) as? Map<Any?, *>
}

/** `0xAARRGGBB` → UIColor. */
private fun Int.toUIColor(): UIColor {
    val value = toLong() and 0xFFFFFFFFL
    return UIColor(
        red = ((value shr 16) and 0xFF).toDouble() / 255.0,
        green = ((value shr 8) and 0xFF).toDouble() / 255.0,
        blue = (value and 0xFF).toDouble() / 255.0,
        alpha = ((value shr 24) and 0xFF).toDouble() / 255.0,
    )
}

private fun BlurType.toNative() = when (this) {
    BlurType.Gaussian -> UXBlurType.UXBlurTypeGaussian
    BlurType.Box -> UXBlurType.UXBlurTypeBox
    BlurType.Bokeh -> UXBlurType.UXBlurTypeBokeh
}
