package com.uxcam.kmp

import android.view.View
import com.uxcam.UXCam as NativeUXCam

// Android-only per-view occlusion: android.view.View has no common type, so these live on the
// Android actual as extensions rather than in the expect surface.

fun UXCamKMP.occludeSensitiveView(view: View) = NativeUXCam.occludeSensitiveView(view)

fun UXCamKMP.occludeSensitiveViewWithoutGesture(view: View) =
    NativeUXCam.occludeSensitiveViewWithoutGesture(view)

fun UXCamKMP.unOccludeSensitiveView(view: View) = NativeUXCam.unOccludeSensitiveView(view)
