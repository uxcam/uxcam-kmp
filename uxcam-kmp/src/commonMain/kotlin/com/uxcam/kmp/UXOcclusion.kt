package com.uxcam.kmp

/**
 * Blur algorithms supported by the native SDKs. iOS honours the algorithm choice
 * (`UXBlurType`); Android applies its single blur implementation and honours the radius only.
 */
enum class BlurType {
    Gaussian,
    Box,
    Bokeh,
}

/**
 * Base model describing a UXCam occlusion rule: *what* to hide (the concrete subtype) and
 * *where* to apply it ([screens] + [excludeMentionedScreens]). Mirrors the native
 * `UXCamOcclusionSetting` hierarchy.
 *
 * Subclass via [KMPUXCamOverlay] or [KMPUXCamBlur] and either pass instances in
 * [UXCamConfiguration.occlusions] (applies from session start — required for
 * [excludeMentionedScreens] on iOS) or apply at runtime with
 * [UXCamKMP.applyOverlayOcclusion] / [UXCamKMP.applyBlurOcclusion].
 *
 * @property screens screen names this rule applies to. Null/empty means "all screens".
 * @property excludeMentionedScreens when true, [screens] is treated as a block-list —
 *   occlude everywhere *except* those screens — instead of an allow-list.
 */
sealed class Occlusion {
    abstract val screens: List<String>?
    abstract val excludeMentionedScreens: Boolean
}

/**
 * Hides the matching screens behind a solid-colour overlay.
 *
 * @property color overlay colour packed as `0xAARRGGBB` (honoured on iOS; the Android SDK
 *   uses its built-in overlay colour). Defaults to opaque red.
 * @property hideGestures also suppress gesture capture on the occluded screen(s).
 */
data class KMPUXCamOverlay(
    val color: Int = 0xFFFF0000.toInt(),
    val hideGestures: Boolean = true,
    override val screens: List<String>? = null,
    override val excludeMentionedScreens: Boolean = false,
) : Occlusion()

/**
 * Blurs the matching screens.
 *
 * @property blurRadius blur strength; higher is blurrier (native default ~15).
 * @property blurType blur algorithm to use — see [BlurType].
 * @property hideGestures also suppress gesture capture on the occluded screen(s).
 */
data class KMPUXCamBlur(
    val blurRadius: Int = 15,
    val blurType: BlurType = BlurType.Gaussian,
    val hideGestures: Boolean = true,
    override val screens: List<String>? = null,
    override val excludeMentionedScreens: Boolean = false,
) : Occlusion()
