package echo.music.iad1tya.extension

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("savish_extensions_prefs", Context.MODE_PRIVATE)

    // Complete 28 Platforms Registry
    private val masterPlatforms = listOf(
        // === REGIONAL & INDIAN MUSIC ===
        PlatformExtension("jiosaavn", "JioSaavn", "Bollywood, Punjabi & Regional", Color(0xFFFF9900), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),
        PlatformExtension("gaana", "Gaana", "Indian Classics, Hindi & Bhojpuri", Color(0xFFE72C30), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),
        PlatformExtension("wynk", "Wynk Music", "Airtel Music & Regional Trends", Color(0xFF0066FF), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),
        PlatformExtension("hungama", "Hungama", "Retro Bollywood & 90s Hits", Color(0xFFFF5252), ExtensionMediaType.AUDIO, PlatformCategory.REGIONAL_INDIAN),

        // === MAINSTREAM GIANTS ===
        PlatformExtension("spotify", "Spotify", "Global Hits & Algorithmic Discovery", Color(0xFF1DB954), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("apple_music", "Apple Music", "Spatial Curated Editorial Playlists", Color(0xFFFA243C), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("amazon_music", "Amazon Music", "Prime Music & Global Catalog", Color(0xFF00A8E1), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("deezer", "Deezer", "Flow Engine & Lossless FLAC", Color(0xFFA238FF), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("tidal", "Tidal", "Hi-Fi & MQA Audiophile Playlists", Color(0xFFEEEEEE), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("qobuz", "Qobuz", "24-Bit Studio Master Audio", Color(0xFF2E7D32), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),

        // === INDIE, REMIXES & DJ ===
        PlatformExtension("soundcloud", "SoundCloud", "Underground, Remixes & EDM", Color(0xFFFF5500), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),
        PlatformExtension("bandcamp", "Bandcamp", "Independent Artist Direct Discography", Color(0xFF629AA9), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),
        PlatformExtension("audiomack", "Audiomack", "Mixtapes, Rap & Afrobeats", Color(0xFFFFA200), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),
        PlatformExtension("mixcloud", "Mixcloud", "Long DJ Sets & Live Club Radios", Color(0xFF5000FF), ExtensionMediaType.AUDIO, PlatformCategory.INDIE_REMIX),

        // === RADIO & WORLDWIDE BROADCAST ===
        PlatformExtension("tunein", "TuneIn", "100k+ Global AM/FM Radios", Color(0xFF14D8CC), ExtensionMediaType.AUDIO, PlatformCategory.RADIO),
        PlatformExtension("iheart", "iHeartRadio", "US Chart Stations & Live Shows", Color(0xFFC62828), ExtensionMediaType.AUDIO, PlatformCategory.RADIO),
        PlatformExtension("radiogarden", "Radio Garden", "Global Interactive FM Explorer", Color(0xFF00E676), ExtensionMediaType.AUDIO, PlatformCategory.RADIO),
        PlatformExtension("accuradio", "AccuRadio", "Curated Genre Internet Radio", Color(0xFF3F51B5), ExtensionMediaType.AUDIO, PlatformCategory.RADIO),

        // === ASIAN & AFRICAN SPECIALS ===
        PlatformExtension("anghami", "Anghami", "Arabic & Middle Eastern Hits", Color(0xFF9C27B0), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("boomplay", "Boomplay", "Afropop, Reggae & African Beats", Color(0xFF00B0FF), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("kkbox", "KKBOX", "Mandopop, J-Pop & K-Pop", Color(0xFF00C853), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),
        PlatformExtension("netease", "NetEase Music", "Asian Anime OST & Lyrical Hub", Color(0xFFD50000), ExtensionMediaType.AUDIO, PlatformCategory.MAINSTREAM),

        // === VIDEO ENGINES ===
        PlatformExtension("yt_video", "YouTube Visuals", "Official 4K Music Videos & Live", Color(0xFFFF0000), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("vimeo", "Vimeo", "Indie Visualizers & 4K Stages", Color(0xFF1AB7EA), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("dailymotion", "Dailymotion", "Concert Archives & Stage Feeds", Color(0xFF0066DC), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("bilibili", "Bilibili", "High-FPS Stage & Anime Visuals", Color(0xFF23ADE5), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),
        PlatformExtension("archive_video", "Archive Live", "Historical Live Concert Vault", Color(0xFF757575), ExtensionMediaType.VIDEO, PlatformCategory.VIDEO_SPECIAL, supportsDirectStream = true),

        // === PROTOCOL & SELF HOSTED ===
        PlatformExtension("m3u_stream", "M3U / IPTV", "Direct HTTP/HLS Stream Channels", Color(0xFF00E5FF), ExtensionMediaType.DUAL, PlatformCategory.CUSTOM_PROTOCOL, supportsDirectStream = true)
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
