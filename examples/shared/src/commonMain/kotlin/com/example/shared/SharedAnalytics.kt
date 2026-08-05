package com.example.shared

import com.uxcam.kmp.KMPUXCamBlur
import com.uxcam.kmp.KMPUXCamOverlay
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.uxcamConfiguration

object SharedAnalytics {
    fun start(appKey: String) = UXCamKMP.startWithConfiguration(
        uxcamConfiguration(appKey) {
            enableCrashHandling = true
            // Startup occlusion rules work on both platforms (and are the only way
            // excludeMentionedScreens is honoured on iOS).
            occlusions = listOf(
                KMPUXCamBlur(blurRadius = 20, screens = listOf("PaymentScreen")),
            )
        }
    )

    fun tagScreen(name: String) = UXCamKMP.tagScreenName(name)

    fun logEvent(name: String) = UXCamKMP.logEvent(name)

    fun logEvent(name: String, properties: Map<String, Any?>) = UXCamKMP.logEvent(name, properties)

    fun setUser(id: String) = UXCamKMP.setUserIdentity(id)

    fun setSessionProperty(name: String, value: String) = UXCamKMP.setSessionProperty(name, value)

    fun reportBug(name: String) = UXCamKMP.reportBugEvent(name)

    fun blurScreen() = UXCamKMP.applyBlurOcclusion(KMPUXCamBlur(blurRadius = 15))

    fun overlayScreen() = UXCamKMP.applyOverlayOcclusion(KMPUXCamOverlay(color = 0xFF222222.toInt()))

    fun clearOcclusion() = UXCamKMP.removeOcclusion()

    fun onVerification(onResult: (Boolean, String?) -> Unit) = UXCamKMP.addVerificationListener(
        onSuccess = { onResult(true, null) },
        onFailure = { message -> onResult(false, message) },
    )

    fun isRecording(): Boolean = UXCamKMP.isRecording()

    fun sessionUrl(): String? = UXCamKMP.urlForCurrentSession()

    fun pendingSessions(): Int = UXCamKMP.pendingSessionCount()

    fun sdkVersion(): String = UXCamKMP.getSdkVersionInfo()

    fun optOut() = UXCamKMP.optOutOverall()

    fun optIn() = UXCamKMP.optInOverall()

    fun optInStatus(): Boolean = UXCamKMP.optInOverallStatus()
}
