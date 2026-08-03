package com.uxcam.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackedOcclusionsTest {

    @Test
    fun startupRulesAreRetainedUntilRemoval() {
        val tracked = TrackedOcclusions<String>()
        tracked.replaceWith(listOf("blur", "overlay"))

        assertEquals(listOf("blur", "overlay"), tracked.drain())
        assertTrue(tracked.drain().isEmpty())
    }

    @Test
    fun runtimeRuleReplacesRemovedStartupRules() {
        val tracked = TrackedOcclusions<String>()
        tracked.replaceWith(listOf("startup"))
        assertEquals(listOf("startup"), tracked.drain())

        tracked.add("runtime")

        assertEquals(listOf("runtime"), tracked.drain())
    }

    @Test
    fun drainReturnsRulesOnceInInsertionOrder() {
        val tracked = TrackedOcclusions<Occlusion>()
        val blur = KMPUXCamBlur(blurRadius = 20)
        val overlay = KMPUXCamOverlay(color = 0xFF222222.toInt())
        tracked.add(blur)
        tracked.add(overlay)

        assertEquals(listOf(blur, overlay), tracked.drain())
        assertTrue(tracked.drain().isEmpty())
    }

    @Test
    fun clearDropsQueuedRules() {
        val tracked = TrackedOcclusions<Occlusion>()
        tracked.add(KMPUXCamBlur(blurRadius = 20))

        tracked.clear()

        assertTrue(tracked.drain().isEmpty())
    }
}
