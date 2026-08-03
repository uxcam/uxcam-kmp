package com.uxcam.kmp

/**
 * Ordered set of occlusion rules the wrapper is holding on to.
 *
 * Two uses: Android retains the applied native occlusion objects (the SDK removes rules by
 * object identity, including startup configuration rules), and iOS queues [Occlusion] rules
 * requested before the native SDK starts. Kept platform-neutral so the state transitions are
 * testable without linking against either native SDK.
 */
internal class TrackedOcclusions<T : Any> {
    private val values = mutableListOf<T>()

    fun replaceWith(newValues: List<T>) {
        values.clear()
        values.addAll(newValues)
    }

    fun add(value: T) {
        values.add(value)
    }

    fun drain(): List<T> = values.toList().also { values.clear() }

    fun clear() {
        values.clear()
    }
}
