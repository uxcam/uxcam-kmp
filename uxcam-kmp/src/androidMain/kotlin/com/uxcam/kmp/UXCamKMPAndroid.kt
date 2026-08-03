package com.uxcam.kmp

import com.uxcam.UXCam as NativeUXCam

/**
 * Android-only UXCam APIs. Keeping these out of the common facade prevents shared code from
 * compiling calls that would silently do nothing on iOS.
 */
fun UXCamKMP.markSessionAsFavorite() = NativeUXCam.markSessionAsFavorite()

fun UXCamKMP.setAutomaticScreenNameTagging(enable: Boolean) =
    NativeUXCam.setAutomaticScreenNameTagging(enable)

fun UXCamKMP.setImprovedScreenCaptureEnabled(enable: Boolean) =
    NativeUXCam.setImprovedScreenCaptureEnabled(enable)

fun UXCamKMP.resumeShortBreakForAnotherApp() =
    NativeUXCam.resumeShortBreakForAnotherApp()
