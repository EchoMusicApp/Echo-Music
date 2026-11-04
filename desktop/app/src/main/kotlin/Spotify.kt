package echo.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports a public Spotify playlist: reads track names from Spotify's embed page (no login), then
 * finds each one on YouTube Music. shortcut: embed page lists at most the first 100 tracks.
 */
object Spotify {
    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    private val track = Regex(""""title":"((?:[^"\\]|\\.)*)","subtitle":"((?:[^"\\]|\\.)*)"""")
    private val playlistId = Regex("""playlist[/:]([A-Za-z0-9]+)""")

    fun unescape(s: String) =
        Regex("""\\u([0-9a-fA-F]{4})|\\(.)""").replace(s) { m ->
            m.groupValues[1].takeIf { it.isNotEmpty() }?.let { it.toInt(16).toChar().toString() } ?: m.groupValues[2]
        }

    suspend fun tracks(link: String): List<String> =
        withContext(Dispatchers.IO) {
            val id = playlistId.find(link)?.groupValues?.get(1) ?: error("That is not a Spotify playlist link")
            val req =
                HttpRequest.newBuilder(URI.create("https://open.spotify.com/embed/playlist/$id"))
                    .header("User-Agent", "Mozilla/5.0").GET().build()
            val html = http.send(req, HttpResponse.BodyHandlers.ofString()).body()
            val list = track.findAll(html).map { unescape(it.groupValues[1]) + " " + unescape(it.groupValues[2]) }.distinct().toList()
            check(list.isNotEmpty()) { "No tracks found (is the playlist public?)" }
            list
        }

    suspend fun toYouTube(names: List<String>, progress: (Int) -> Unit): List<SongItem> =
        withContext(Dispatchers.IO) {
            YouTubeSetup.ensureInit()
            names.mapIndexedNotNull { i, q ->
                progress(i + 1)
                YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull()
            }
        }
}
