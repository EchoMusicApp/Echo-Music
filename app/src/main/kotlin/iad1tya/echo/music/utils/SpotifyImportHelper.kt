/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package iad1tya.echo.music.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object SpotifyImportHelper {
    private const val TAG = "SpotifyImportHelper"
    private val client = OkHttpClient.Builder().build()

    data class ImportProgress(
        val playlistName: String,
        val totalTracks: Int,
        val processedTracks: Int,
        val foundSongIds: List<String>,
        val failedTracks: List<String>,
    )

    /**
     * Extracts song titles and artists from a Spotify playlist URL.
     * Uses multiple strategies: API with anonymous token, embed page parsing, HTML scraping.
     */
    suspend fun getPlaylistSongs(url: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = "Spotify Import"

        try {
            val playlistId = extractPlaylistId(url)
            if (playlistId == null) {
                Log.e(TAG, "Could not extract playlist ID from URL: $url")
                return@withContext playlistName to emptyList()
            }

            // Strategy 1: Try API with anonymous access token
            val accessToken = getSpotifyAccessToken()
            if (accessToken != null) {
                try {
                    val (name, apiSongs) = fetchTracksViaApi(playlistId, accessToken)
                    if (name.isNotEmpty()) playlistName = name
                    if (apiSongs.isNotEmpty()) {
                        Log.i(TAG, "API method fetched ${apiSongs.size} songs")
                        return@withContext playlistName to apiSongs
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "API method failed, falling back to embed", e)
                }
            }

            // Strategy 2: Embed page parsing
            try {
                val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
                val doc = Jsoup.connect(embedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()

                val nextDataScript = doc.select("script#__NEXT_DATA__").first()
                if (nextDataScript != null) {
                    val json = nextDataScript.html()
                    val jsonObject = JSONObject(json)
                    val entity = jsonObject.optJSONObject("props")
                        ?.optJSONObject("pageProps")
                        ?.optJSONObject("state")
                        ?.optJSONObject("data")
                        ?.optJSONObject("entity")

                    if (entity != null) {
                        playlistName = entity.optString("name").takeIf { it.isNotBlank() }
                            ?: entity.optString("title").takeIf { it.isNotBlank() }
                            ?: playlistName

                        val trackList = entity.optJSONArray("trackList")
                        if (trackList != null) {
                            for (i in 0 until trackList.length()) {
                                val trackObj = trackList.optJSONObject(i) ?: continue
                                val title = trackObj.optString("title").takeIf { it.isNotBlank() } ?: continue
                                val subtitle = trackObj.optString("subtitle")
                                songs.add(title to subtitle)
                            }
                        }
                    }
                }
                if (songs.isNotEmpty()) {
                    Log.i(TAG, "Embed method fetched ${songs.size} songs")
                    return@withContext playlistName to songs.toList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Embed method failed, falling back to HTML", e)
            }

            // Strategy 3: HTML scraping fallback
            try {
                val doc = Jsoup.connect("https://open.spotify.com/playlist/$playlistId")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()

                val titleEl = doc.selectFirst("meta[property=og:title]")
                if (titleEl != null) {
                    playlistName = titleEl.attr("content")
                }

                val trackElements = doc.select("meta[name=music:song]")
                for (el in trackElements) {
                    val trackUrl = el.attr("content")
                    val trackDoc = try {
                        Jsoup.connect(trackUrl).userAgent("Mozilla/5.0").get()
                    } catch (_: Exception) { null }
                    val trackTitle = trackDoc?.selectFirst("meta[property=og:title]")?.attr("content")
                    val trackArtist = trackDoc?.selectFirst("meta[property=og:description]")?.attr("content")
                    if (trackTitle != null) {
                        songs.add(trackTitle to (trackArtist ?: ""))
                    }
                }
                Log.i(TAG, "HTML method fetched ${songs.size} songs")
            } catch (e: Exception) {
                Log.e(TAG, "All methods failed", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting playlist: ${e.message}", e)
        }

        playlistName to songs.toList()
    }

    /**
     * Search YouTube Music for a song by title and artist, returning the best match video ID.
     */
    suspend fun searchYouTubeForSong(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$title $artist"
            val result = iad1tya.echo.music.innertube.YouTube.search(query, iad1tya.echo.music.innertube.YouTube.SearchFilter.FILTER_SONG)
            val items = result.getOrNull()?.items ?: return@withContext null
            val songItem = items.filterIsInstance<iad1tya.echo.music.innertube.models.SongItem>().firstOrNull()
            songItem?.id
        } catch (e: Exception) {
            Log.e(TAG, "YouTube search failed for '$title - $artist': ${e.message}")
            null
        }
    }

    private fun extractPlaylistId(url: String): String? {
        val patterns = listOf(
            Regex("playlist/([a-zA-Z0-9]+)"),
            Regex("playlist%2F([a-zA-Z0-9]+)"),
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun getSpotifyAccessToken(): String? {
        return try {
            val request = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            JSONObject(body).optString("accessToken").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Spotify access token: ${e.message}")
            null
        }
    }

    private fun fetchTracksViaApi(
        playlistId: String,
        accessToken: String,
    ): Pair<String, List<Pair<String, String>>> {
        val songs = mutableListOf<Pair<String, String>>()
        var playlistName = ""
        var nextUrl: String? =
            "https://api.spotify.com/v1/playlists/$playlistId/tracks?offset=0&limit=100&fields=items(track(name,artists(name))),total,next"

        while (nextUrl != null) {
            val json = fetchSpotifyApiPage(nextUrl, accessToken) ?: break

            if (playlistName.isEmpty()) {
                try {
                    val nameRequest = Request.Builder()
                        .url("https://api.spotify.com/v1/playlists/$playlistId?fields=name")
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val nameResponse = client.newCall(nameRequest).execute()
                    val nameBody = nameResponse.body?.string()
                    if (nameBody != null) {
                        playlistName = JSONObject(nameBody).optString("name")
                    }
                } catch (_: Exception) { }
            }

            val items = json.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val track = items.optJSONObject(i)?.optJSONObject("track") ?: continue
                val name = track.optString("name").takeIf { it.isNotBlank() } ?: continue
                val artistsArray = track.optJSONArray("artists")
                val artists = buildString {
                    if (artistsArray != null) {
                        for (j in 0 until artistsArray.length()) {
                            if (j > 0) append(", ")
                            append(artistsArray.optJSONObject(j)?.optString("name") ?: "")
                        }
                    }
                }
                songs.add(name to artists)
            }

            nextUrl = json.optString("next").takeIf { it.isNotBlank() && it != "null" }
        }

        return playlistName to songs
    }

    private fun fetchSpotifyApiPage(
        url: String,
        accessToken: String,
    ): JSONObject? {
        var attempts = 0
        while (attempts < 4) {
            attempts++
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()

            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 1L
                Log.w(TAG, "Spotify rate-limited, retrying in ${retryAfterSeconds}s (attempt $attempts)")
                TimeUnit.SECONDS.sleep(retryAfterSeconds.coerceAtLeast(1L))
                continue
            }

            if (!response.isSuccessful) {
                Log.w(TAG, "Spotify API fetch failed: HTTP ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            return JSONObject(body)
        }

        return null
    }
}
