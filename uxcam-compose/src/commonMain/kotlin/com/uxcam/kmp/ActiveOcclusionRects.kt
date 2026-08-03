package com.uxcam.kmp

/**
 * Owns the latest capture rectangle for every live Compose node.
 *
 * The platform bridge snapshots the complete set for each capture update, so removing a node
 * also removes its rectangle instead of leaving stale native state behind.
 */
internal class ActiveOcclusionRects<Key : Any> {
    private val rects = linkedMapOf<Key, List<Int>>()

    fun update(key: Key, rect: List<Int>?) {
        if (rect == null) rects.remove(key) else rects[key] = rect
    }

    fun remove(key: Key) {
        rects.remove(key)
    }

    fun snapshot(): List<List<Int>> = rects.values.toList()

    fun isNotEmpty(): Boolean = rects.isNotEmpty()
}
