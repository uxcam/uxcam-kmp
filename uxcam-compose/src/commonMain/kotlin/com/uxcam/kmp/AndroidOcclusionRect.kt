package com.uxcam.kmp

import androidx.compose.ui.geometry.Rect
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Converts Compose window bounds to the integer screen rectangle accepted by Android UXCam's
 * frame-scoped occlusion API. Dialog content has its own window, so only that path needs the
 * host view's screen offset.
 */
internal fun Rect.toAndroidCaptureRect(
    dialogOffsetX: Int = 0,
    dialogOffsetY: Int = 0,
): List<Int>? {
    if (width <= 0f || height <= 0f) return null
    return listOf(
        floor(left).toInt() + dialogOffsetX,
        floor(top).toInt() + dialogOffsetY,
        ceil(right).toInt() + dialogOffsetX,
        ceil(bottom).toInt() + dialogOffsetY,
    )
}
