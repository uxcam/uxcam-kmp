package com.uxcam.kmp

import android.view.Choreographer
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.uxcam.UXCam as NativeUXCam
import org.json.JSONArray

/**
 * Compose has one Android host [View], so view occlusion cannot distinguish individual nodes.
 * Keep a complete set of live node bounds and submit it through UXCam's public frame-scoped rect
 * API. A disposed node is absent from the next frame instead of remaining in the SDK's persistent
 * Compose repository.
 */
actual fun Modifier.uxcamOcclude(identifier: String, isInDialog: Boolean): Modifier = composed {
    val view = LocalView.current
    val entry = remember(identifier, view, isInDialog) {
        AndroidOccludedEntry()
    }
    DisposableEffect(entry) {
        onDispose { AndroidComposeOcclusionRegistry.remove(entry) }
    }
    onGloballyPositioned { coordinates ->
        val offset = if (isInDialog) {
            IntArray(2).also(view::getLocationOnScreen)
        } else {
            ZERO_OFFSET
        }
        AndroidComposeOcclusionRegistry.update(
            entry,
            coordinates.boundsInWindow().toAndroidCaptureRect(offset[0], offset[1]),
        )
    }
}

private val ZERO_OFFSET = intArrayOf(0, 0)

private class AndroidOccludedEntry

/**
 * The Android API applies rectangles to the next captured frame, so active bounds are resubmitted
 * from a Choreographer callback while at least one occluded node is visible. All access is on the
 * main thread: Compose layout, disposal, Choreographer, and the native SDK call share that thread.
 */
private object AndroidComposeOcclusionRegistry {
    private val active = ActiveOcclusionRects<AndroidOccludedEntry>()
    private var frameScheduled = false

    private val frameCallback = Choreographer.FrameCallback {
        frameScheduled = false
        val rects = active.snapshot()
        if (rects.isNotEmpty()) {
            val payload = JSONArray()
            rects.forEach { rect ->
                payload.put(JSONArray().apply { rect.forEach(::put) })
            }
            NativeUXCam.occludeRectsOnNextFrame(payload)
        }
        if (active.isNotEmpty()) scheduleFrame()
    }

    fun update(entry: AndroidOccludedEntry, rect: List<Int>?) {
        active.update(entry, rect)
        if (active.isNotEmpty()) scheduleFrame()
    }

    fun remove(entry: AndroidOccludedEntry) {
        active.remove(entry)
    }

    private fun scheduleFrame() {
        if (frameScheduled) return
        frameScheduled = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }
}
