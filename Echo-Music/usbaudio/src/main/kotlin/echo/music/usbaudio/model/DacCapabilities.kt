package echo.music.usbaudio.model

/**
 * Parsed DAC capabilities including UAC version, supported audio formats, and volume controls.
 */
data class DacCapabilities(
    val uacVersion: Int,
    val supportedFormats: List<DacFormat> = emptyList(),
    val supportedSampleRates: List<Int> = emptyList(),
    val clockSourceId: Int? = null,
    val hasHardwareVolume: Boolean = false,
    val volumeFeatureUnitId: Int? = null,
    val minVolumeDb: Float = 0.0f,
    val maxVolumeDb: Float = 0.0f,
    val volumeResDb: Float = 0.0f
)
