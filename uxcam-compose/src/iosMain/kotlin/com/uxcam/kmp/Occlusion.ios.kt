@file:OptIn(ExperimentalForeignApi::class)

package com.uxcam.kmp

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation. Compose renders the whole hierarchy into a single Metal-backed view,
 * so the native SDK's per-UIView occlusion can't see individual composables. Instead each
 * occluded node's window rect is forwarded through the SDK's hybrid-wrapper rect API
 * (`occludeRectsOnNextFrame:withIdentity:`), whose per-identity list REPLACES the previous
 * push and persists until the next one — so rects only need pushing when layout changes.
 */
actual fun Modifier.uxcamOcclude(identifier: String, isInDialog: Boolean): Modifier = composed {
    val density = LocalDensity.current.density
    val entry = remember { OccludedEntry() }
    entry.density = density
    DisposableEffect(entry) {
        onDispose { ComposeOcclusionRegistry.remove(entry) }
    }
    onGloballyPositioned { coordinates ->
        entry.coordinates = coordinates
        ComposeOcclusionRegistry.update(entry)
    }
}

internal class OccludedEntry {
    var coordinates: LayoutCoordinates? = null
    var density: Float = 1f
}

/**
 * Aggregates the window rects of every currently-occluded composable and pushes them to the
 * native SDK under one shared identity. Pushes are coalesced per run-loop turn (N nodes
 * repositioning in one layout pass produce one native call) and skipped when nothing moved,
 * so a static screen costs nothing after its first push.
 *
 * Main-thread only: Compose layout callbacks and the SDK's frame capture both run there,
 * and the capture reads these rects in the same run-loop turn as the screenshot.
 */
internal object ComposeOcclusionRegistry {

    // Single native identity for all Compose rects: per-identity pushes replace the prior
    // list, so aggregating under one identity makes every push self-cleaning for moved or
    // disposed nodes. User-supplied identifiers may repeat across nodes (e.g. one per list
    // row), so they can't serve as native identities.
    private const val NATIVE_IDENTITY = "uxcam-kmp-compose"

    // The native API ignores empty lists and has no public remove-by-identity yet. A single
    // zero-size rect passes the SDK's input guard but is dropped during parsing, leaving the
    // identity mapped to an empty list — i.e. it clears our rects.
    private val CLEAR_PAYLOAD = listOf(listOf(0.0, 0.0, 0.0, 0.0))

    private val entries = linkedSetOf<OccludedEntry>()
    private var lastPushed: List<List<Double>>? = null
    private var flushScheduled = false

    fun update(entry: OccludedEntry) {
        entries.add(entry)
        scheduleFlush()
    }

    fun remove(entry: OccludedEntry) {
        entries.remove(entry)
        scheduleFlush()
    }

    private fun scheduleFlush() {
        if (flushScheduled) return
        flushScheduled = true
        dispatch_async(dispatch_get_main_queue()) {
            flushScheduled = false
            flush()
        }
    }

    private fun flush() {
        val rects = entries.mapNotNull { it.windowRectPoints() }
        if (rects == lastPushed) return
        lastPushed = rects
        UXCamKMP.occludeRectsOnNextFrame(rects.ifEmpty { CLEAR_PAYLOAD }, NATIVE_IDENTITY)
    }
}

/**
 * The entry's current window-space rect in iOS points. [boundsInWindow] is already relative to
 * the current window, so converting it through the host UIView would apply the view offset twice
 * for embedded controllers and dialogs. Null when detached or fully outside the viewport
 * ([boundsInWindow] clips to the visible window, so off-screen nodes collapse to zero size).
 */
private fun OccludedEntry.windowRectPoints(): List<Double>? {
    val coords = coordinates?.takeIf { it.isAttached } ?: return null
    return coords.boundsInWindow().toIosWindowRect(density)
}
