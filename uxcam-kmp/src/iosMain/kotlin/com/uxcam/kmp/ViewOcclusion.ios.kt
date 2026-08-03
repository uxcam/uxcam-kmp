@file:OptIn(ExperimentalForeignApi::class)

package com.uxcam.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView
import com.uxcam.kmp.cinterop.UXCam as NativeUXCam

// iOS-only per-view occlusion: UIView has no common type, so these live on the iOS actual as
// extensions rather than in the expect surface.

fun UXCamKMP.occludeSensitiveView(view: UIView) = NativeUXCam.occludeSensitiveView(view)

fun UXCamKMP.occludeSensitiveViewWithoutGesture(view: UIView) =
    NativeUXCam.occludeSensitiveViewWithoutGesture(view)

fun UXCamKMP.unOccludeSensitiveView(view: UIView) = NativeUXCam.unOccludeSensitiveView(view)

/**
 * iOS-only hybrid rect occlusion: occludes window-space rects (`[x, y, w, h]` in points) on
 * subsequent frames. The list pushed for an [identity] REPLACES that identity's previous list
 * and persists until the next push. Bridged here so `:uxcam-compose` (and hybrid-UI consumers)
 * can reach the native rect API without their own cinterop.
 */
fun UXCamKMP.occludeRectsOnNextFrame(rects: List<List<Double>>, identity: String) =
    NativeUXCam.occludeRectsOnNextFrame(rects, withIdentity = identity)
