package com.uxcam.kmp.gradle

/**
 * Native UXCam iOS SDK compatibility facts shared by the CocoaPods and SwiftPM delivery paths.
 */
internal object UXCamIosSdk {
    // Kotlin/Native 2.4 frameworks in this project are built with an iOS 15 floor. Keep every
    // delivery path on the same minimum instead of advertising iOS 12 for CocoaPods while the
    // produced KMP framework itself requires iOS 15.
    const val MIN_IOS_DEPLOYMENT_TARGET = "15.0"
}

/**
 * Compares two dot-separated version strings numerically (`"2.4.0"` vs `"2.2.21"`). Any
 * pre-release suffix after `-` is ignored, missing components are treated as `0`.
 * Negative if [a] < [b].
 */
internal fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val aParts = parts(a)
    val bParts = parts(b)
    for (i in 0 until maxOf(aParts.size, bParts.size)) {
        val cmp = aParts.getOrElse(i) { 0 }.compareTo(bParts.getOrElse(i) { 0 })
        if (cmp != 0) return cmp
    }
    return 0
}
