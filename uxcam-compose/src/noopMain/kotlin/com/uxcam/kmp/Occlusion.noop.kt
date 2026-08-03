package com.uxcam.kmp

import androidx.compose.ui.Modifier

// No-op on platforms without a native UXCam SDK (desktop/JVM, JS, Wasm): returns the receiver
// unchanged so shared UI compiles and runs without occluding.
actual fun Modifier.uxcamOcclude(identifier: String, isInDialog: Boolean): Modifier = this
