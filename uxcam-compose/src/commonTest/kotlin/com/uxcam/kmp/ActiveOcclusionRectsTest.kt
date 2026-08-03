package com.uxcam.kmp

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActiveOcclusionRectsTest {

    @Test
    fun removingNodeDropsItsRectangleFromNextSnapshot() {
        val active = ActiveOcclusionRects<String>()
        active.update("card", listOf(0, 10, 100, 50))
        active.update("email", listOf(0, 60, 100, 100))

        active.remove("card")

        assertEquals(listOf(listOf(0, 60, 100, 100)), active.snapshot())
    }

    @Test
    fun detachedNodeCanRemoveItsOwnRectangle() {
        val active = ActiveOcclusionRects<String>()
        active.update("card", listOf(0, 10, 100, 50))

        active.update("card", null)

        assertTrue(active.snapshot().isEmpty())
        assertTrue(!active.isNotEmpty())
    }

    @Test
    fun androidRectRoundsOutwardAndOffsetsOnlyWhenRequested() {
        val bounds = Rect(left = 1.4f, top = 2.6f, right = 10.1f, bottom = 20.2f)

        assertEquals(listOf(1, 2, 11, 21), bounds.toAndroidCaptureRect())
        assertEquals(
            listOf(101, 202, 111, 221),
            bounds.toAndroidCaptureRect(dialogOffsetX = 100, dialogOffsetY = 200),
        )
        assertEquals(null, Rect.Zero.toAndroidCaptureRect())
    }
}
