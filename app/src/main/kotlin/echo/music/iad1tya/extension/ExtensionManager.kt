package echo.music.iad1tya.extension

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("savish_extensions_prefs", Context.MODE_PRIVATE)

    // Master Registry: Future me naye platform bas yahan add honge
    private val masterPlatforms = listOf(
        // === AUDIO ENGINES ===
        PlatformExtension("spotify", "Spotify", "Global Hits & Playlists", Color(0xFF1DB954), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("jiosaavn", "JioSaavn", "Bollywood & Regional Beats", Color(0xFFFF9900), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),
        PlatformExtension("gaana", "Gaana", "Indian Classics & Trending", Color(0xFFE72C30), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),
        PlatformExtension("wynk", "Wynk Music", "Airtel Music Library", Color(0xFF0066FF), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),
        PlatformExtension("apple_music", "Apple Music", "Spatial & Curated Playlists", Color(0xFFFA243C), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("amazon_music", "Amazon Music", "Prime Catalog", Color(0xFF00A8E1), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("deezer", "Deezer", "Flow & Hi-Fi Audio", Color(0xFFA238FF), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("tidal", "Tidal", "Master Quality Audio", Color(0xFFFFFFFF), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("soundcloud", "SoundCloud", "Remixes, Lo-Fi & EDM", Color(0xFFFF5500), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),
        PlatformExtension("bandcamp", "Bandcamp", "Independent Artists & Albums", Color(0xFF629AA9), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),
        PlatformExtension("audiomack", "Audiomack", "Mixtapes & Urban Music", Color(0xFFFFA200), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),
        PlatformExtension("tunein", "TuneIn", "100k+ Live Global Radio", Color(0xFF14D8CC), ExtensionMediaType.AUDIO, PlatformCategory.RADIO),

        // === VIDEO ENGINES ===
        PlatformExtension("yt_video", "YouTube Visuals", "Official 4K Music Videos", Color(0xFFFF0000), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("vimeo", "Vimeo Music", "Cinematic Music Visualizers", Color(0xFF1AB7EA), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("dailymotion", "Dailymotion", "Live Stage Concerts & Clips", Color(0xFF0066DC), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("bilibili", "Bilibili", "High-FPS Anime & Stage Tracks", Color(0xFF23ADE5), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("archive_video", "Live Concert Archive", "Vintage Performances & Festivals", Color(0xFF888888), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),

        // === DUAL & CUSTOM STREAM ===
        PlatformExtension("m3u_stream", "Custom Web Stream", "M3U / IPTV Direct Audio-Video URLs", Color(0xFF00E5FF), ExtensionMediaType.DUAL, PlatformCategory.CUSTOM_PROTOCOL, supportsDirectStream = true)
    )

    private val _activeExtensions = MutableStateFlow<List<PlatformExtension>>(emptyList())
    val activeExtensions: StateFlow<List<PlatformExtension>> = _activeExtensions.asStateFlow()

    init {
        loadEnabledExtensions()
    }

    private fun loadEnabledExtensions() {
        val enabledList = masterPlatforms.map { ext ->
            val isSavedEnabled = prefs.getBoolean("ext_${ext.id}", false)
            ext.copy(isEnabled = isSavedEnabled)
        }
        _activeExtensions.value = enabledList
    }

    fun toggleExtension(extensionId: String) {
        val current = _activeExtensions.value.toMutableList()
        val index = current.indexOfFirst { it.id == extensionId }
        if (index != -1) {
            val updated = current[index].copy(isEnabled = !current[index].isEnabled)
            current[index] = updated
            prefs.edit().putBoolean("ext_${extensionId}", updated.isEnabled).apply()
            _activeExtensions.value = current
        }
    }

    fun getEnabledForMode(isVideoMode: Boolean): List<PlatformExtension> {
        val targetType = if (isVideoMode) ExtensionMediaType.VIDEO else ExtensionMediaType.AUDIO
        return _activeExtensions.value.filter {
            it.isEnabled && (it.mediaType == targetType || it.mediaType == ExtensionMediaType.DUAL)
        }
    }
}
