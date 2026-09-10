package echo.music.iad1tya.extension

import com.music.innertube.YouTube
import com.music.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlatformDataBridge {

    fun getPlatformQuery(platformId: String): String {
        return when (platformId) {
            "jiosaavn" -> "JioSaavn Trending Hindi Punjabi Weekly Top"
            "gaana" -> "Gaana Top Bollywood Hits New Releases"
            "wynk" -> "Wynk Top 50 Hits Latest Songs"
            "hungama" -> "Hungama Bollywood Retro Classics 90s"
            "spotify" -> "Today's Top Hits Global Spotify Charts"
            "apple_music" -> "Apple Music Today Hits A-List Pop"
            "amazon_music" -> "Amazon Music Best of the Month Playlist"
            "deezer" -> "Deezer Flow Top Worldwide Hits"
            "tidal" -> "Tidal Master Quality HiFi Hits"
            "soundcloud" -> "SoundCloud Remixes Lo-Fi EDM Mashup"
            "bandcamp" -> "Bandcamp Indie New & Notable Releases"
            "audiomack" -> "Audiomack Top Hip-Hop Afrobeats Songs"
            "mixcloud" -> "Mixcloud Live DJ Set Electronic Club Dance"
            "tunein" -> "Top Internet Radio Live Stations Hits"
            "iheart" -> "iHeartRadio Top 40 Pop Chart"
            "radiogarden" -> "Radio Garden Live Stream Hits Worldwide"
            "accuradio" -> "AccuRadio Best Acoustic Hits"
            "anghami" -> "Anghami Arabic Top Hits & Trending"
            "boomplay" -> "Boomplay African Afropop Chart Top 100"
            "kkbox" -> "KKBOX Asian Mandopop K-Pop Trending"
            "netease" -> "NetEase Cloud Music Anime OST Hits"
            "yt_video" -> "Official 4K Music Videos Trending"
            "vimeo" -> "Cinematic 4K Music Videos Visualizer"
            "dailymotion" -> "Live Concert Stage Performances 4K"
            "bilibili" -> "Bilibili Anime Music Stage Concert"
            "archive_video" -> "Live Festival Vintage Concert Archive"
            else -> "Top Trending Worldwide Music"
        }
    }

    suspend fun fetchPlatformFeed(platformId: String): List<YTItem> = withContext(Dispatchers.IO) {
        try {
            val query = getPlatformQuery(platformId)
            val searchResult = YouTube.search(query).getOrNull()
            searchResult?.items.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
