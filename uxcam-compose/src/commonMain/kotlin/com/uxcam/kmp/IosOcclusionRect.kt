package com.uxcam.kmp

import androidx.compose.ui.geometry.Rect

/**
 * Converts Compose window coordinates (pixels) to the window coordinates (points) expected by
 * UXCam's iOS hybrid-wrapper API.
 */
internal fun Rect.toIosWindowRect(density: Float): List<Double>? {
    if (width <= 0f || height <= 0f || !density.isFinite() || density <= 0f) return null
    val scale = density.toDouble()
    return listOf(
        left / scale,
        top / scale,
        width / scale,
        height / scale,
    )
}
