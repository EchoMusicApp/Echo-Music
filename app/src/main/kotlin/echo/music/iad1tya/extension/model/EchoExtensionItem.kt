package echo.music.iad1tya.extension.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class EchoExtensionManifest(
    val extensions: List<EchoExtensionItem> = emptyList()
)

@Serializable
data class EchoExtensionItem(
    val id: String,
    val name: String,
    val packageName: String,
    val version: String,
    val versionCode: Int = 1,
    val description: String = "",
    val downloadUrl: String = "",
    val iconUrl: String = "",
    val type: String = "audio", // "audio", "video"
    val colorHex: String = "#00E5FF",
    val requiresLogin: Boolean = false,
    var isInstalled: Boolean = false,
    var isDownloading: Boolean = false
) {
    val brandColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF00E5FF)
        }
}
