package com.uxcam.kmp

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosOcclusionRectTest {

    @Test
    fun convertsWindowPixelsDirectlyToIosPoints() {
        val boundsInWindow = Rect(left = 20f, top = 40f, right = 120f, bottom = 240f)

        assertEquals(
            listOf(10.0, 20.0, 50.0, 100.0),
            boundsInWindow.toIosWindowRect(density = 2f),
        )
    }

    @Test
    fun rejectsEmptyBoundsAndInvalidDensity() {
        assertNull(Rect.Zero.toIosWindowRect(density = 2f))
        assertNull(Rect(0f, 0f, 10f, 10f).toIosWindowRect(density = 0f))
    }
}
