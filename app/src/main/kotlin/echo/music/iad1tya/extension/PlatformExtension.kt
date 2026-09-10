package echo.music.iad1tya.extension

import androidx.compose.ui.graphics.Color

enum class ExtensionMediaType {
    AUDIO,
    VIDEO,
    DUAL
}

enum class PlatformCategory {
    MAINSTREAM,
    REGIONAL_INDIAN,
    INDIE_REMIX,
    RADIO,
    VIDEO_SPECIAL,
    CUSTOM_PROTOCOL
}

data class PlatformExtension(
    val id: String,
    val name: String,
    val description: String,
    val brandColor: Color,
    val mediaType: ExtensionMediaType,
    val category: PlatformCategory,
    val isEnabled: Boolean = false,
    val supportsDirectStream: Boolean = false,
    val apiEndpoint: String? = null
)
