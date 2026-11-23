package echo.music.usbaudio.model

/**
 * Volume mode for USB DAC playback.
 */
enum class VolumeMode {
    /**
     * Bit-perfect 0 dB gain. Audio data sent untouched; volume controlled externally.
     */
    PURE_BIT_PERFECT,

    /**
     * Hardware UAC Feature Unit volume control.
     */
    HARDWARE,

    /**
     * High-precision 64-bit float software attenuation.
     */
    SOFTWARE_64BIT
}
