package com.uxcam.kmp.sample

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app console for the sample: every UXCam call the sample makes is echoed here so the SDK's
 * behaviour can be followed on-device without logcat. Newest entry first, capped so a long
 * session cannot grow it without bound.
 */
object SampleLog {

    private const val MAX_ENTRIES = 200
    private val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US)

    val entries = mutableStateListOf<String>()

    fun add(message: String) {
        entries.add(0, "${timestamp.format(Date())}  $message")
        if (entries.size > MAX_ENTRIES) entries.removeRange(MAX_ENTRIES, entries.size)
    }

    fun clear() = entries.clear()
}
