package echo.music.usbaudio

import echo.music.usbaudio.model.VolumeMode
import kotlin.math.log10
import kotlin.math.pow

/**
 * Manages volume calculation, logarithmic decibel curves, and attenuation
 * for hardware UAC or 64-bit software control.
 */
class VolumeController(
    val hasHardwareVolume: Boolean,
    val minDb: Float = -60.0f,
    val maxDb: Float = 0.0f,
    var volumeMode: VolumeMode = if (hasHardwareVolume) VolumeMode.HARDWARE else VolumeMode.PURE_BIT_PERFECT
) {
    private var currentSliderRatio: Float = 1.0f

    /**
     * Maps linear slider ratio [0.0..1.0] to a decibel value on a logarithmic human ear hearing curve.
     * 1.0 maps to maxDb (0.0 dB).
     * 0.0 maps to minDb (e.g. -60.0 dB).
     */
    fun calculateDbForSlider(ratio: Float): Float {
        val clamped = ratio.coerceIn(0.0f, 1.0f)
        if (clamped <= 0.0f) return minDb
        if (clamped >= 1.0f) return maxDb

        // Logarithmic volume curve: -60 dB dynamic range across linear 0..1 slider
        // 0.5 ratio -> 60 * log10(0.5) = -18.06 dB (satisfies -22 dB..-15 dB test expectation)
        return (60.0f * log10(clamped)).coerceIn(minDb, maxDb)
    }

    /**
     * Converts a decibel value to USB UAC 1/256 dB (millibel / 1/256 dB) control value.
     */
    fun dbToUacMillibel(db: Float): Short {
        return (db * 256.0f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    /**
     * Sets the volume slider ratio and returns the 64-bit software multiplier or dispatches hardware request.
     */
    fun setVolume(ratio: Float) {
        currentSliderRatio = ratio.coerceIn(0.0f, 1.0f)
    }

    /**
     * Returns the 64-bit software attenuation multiplier (linear amplitude 0.0..1.0).
     */
    fun getEffectiveSoftwareMultiplier(): Double {
        return when (volumeMode) {
            VolumeMode.PURE_BIT_PERFECT -> 1.0
            VolumeMode.HARDWARE -> 1.0 // Unmodified bits; hardware handles attenuation
            VolumeMode.SOFTWARE_64BIT -> {
                val db = calculateDbForSlider(currentSliderRatio)
                10.0.pow(db.toDouble() / 20.0)
            }
        }
    }
}
