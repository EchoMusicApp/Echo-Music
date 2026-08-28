package echo.music.iad1tya.ai

import android.content.Context
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.constants.AiProviderKey
import echo.music.iad1tya.constants.OpenRouterApiKey
import echo.music.iad1tya.constants.OpenRouterBaseUrlKey
import echo.music.iad1tya.constants.OpenRouterModelKey
import echo.music.iad1tya.constants.DeeplApiKey
import echo.music.iad1tya.db.InternalDatabase
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

object AiRecommendationHelper {
    private val client = OkHttpClient()

    private const val PLAYLIST_NAME = "Recommended by AI"

    suspend fun generateRecommendations(
        context: Context,
        onLog: (suspend (String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val database = InternalDatabase.newInstance(context)

        onLog?.invoke("Analyzing your listening history...")
        // 1. Gather User Taste
        val topSongs: List<Song> = database.topSongs(20).firstOrNull() ?: emptyList()
        val likedSongs: List<Song> = database.likedSongsByPlayTimeAsc().firstOrNull()?.take(20) ?: emptyList()

        if (topSongs.isEmpty() && likedSongs.isEmpty()) {
            onLog?.invoke("Not enough listening history found.")
            return@withContext // Not enough data
        }

        val tasteList = (topSongs + likedSongs).distinctBy { it.song.id }.take(30).map { songObj ->
            "${songObj.song.title} by ${songObj.artists.joinToString { artist -> artist.name }}"
        }

        val prompt = """
            Based on the following list of songs the user likes:
            ${tasteList.joinToString("\n")}
            
            Please recommend 20 similar but different songs.
            CRITICAL RULES:
            1. You MUST output ONLY a valid JSON array of objects.
            2. Do NOT include any markdown formatting (like ```json).
            3. Do NOT include any conversational text or explanations. Just the raw JSON array.
            
            Example format:
            [
              {"title": "Song Name", "artist": "Artist Name"}
            ]
        """.trimIndent()

        // 2. Query AI Provider
        onLog?.invoke("Connecting to AI Provider...")
        val aiProvider = context.dataStore.get(AiProviderKey, "OpenRouter")
        val jsonOutput = if (aiProvider == "Puter") {
            // Future Puter Implementation
            "[]"
        } else {
            val apiKey = context.dataStore.get(OpenRouterApiKey, "")
            val baseUrl = context.dataStore.get(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
            val model = context.dataStore.get(OpenRouterModelKey, "google/gemini-2.5-flash-lite")

            if (apiKey.isEmpty()) {
                onLog?.invoke("API Key is missing.")
                return@withContext
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                onLog?.invoke("AI Request failed: ${response.code}")
                return@withContext
            }

            val responseString = response.body?.string() ?: return@withContext
            val responseJson = JSONObject(responseString)
            val choices = responseJson.optJSONArray("choices") ?: return@withContext
            choices.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "[]"
        }

        // Clean output just in case of markdown formatting
        val cleanJsonStr = jsonOutput.replace("```json", "").replace("```", "").trim()

        val jsonArray = try {
            onLog?.invoke("Parsing AI response...")
            JSONArray(cleanJsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            onLog?.invoke("Failed to parse AI output.")
            return@withContext
        }

        // 3. Resolve with InnerTube and Save
        val resolvedSongs = mutableListOf<SongItem>()
        val totalSongs = jsonArray.length()
        onLog?.invoke("Found $totalSongs songs. Searching InnerTube...")
        
        for (i in 0 until totalSongs) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val title = item.optString("title")
            val artist = item.optString("artist")
            if (title.isNotEmpty()) {
                onLog?.invoke("Searching [${i + 1}/$totalSongs]: $title - $artist")
                val searchQuery = "$title $artist"
                val result = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                val topResult = result?.items?.firstOrNull() as? SongItem
                if (topResult != null) {
                    resolvedSongs.add(topResult)
                }
            }
        }

        if (resolvedSongs.isEmpty()) {
            onLog?.invoke("Failed to find any of the suggested songs.")
            return@withContext
        }

        // 4. Update Playlist
        onLog?.invoke("Updating playlist...")
        var playlist = database.searchPlaylists(PLAYLIST_NAME).firstOrNull()?.find { it.playlist.name == PLAYLIST_NAME }?.playlist
        if (playlist == null) {
            playlist = PlaylistEntity(
                name = PLAYLIST_NAME,
                bookmarkedAt = java.time.LocalDateTime.now(),
                isLocal = true,
                isEditable = true
            )
            database.insert(playlist)
        } else {
            // Clear existing songs in playlist
            database.clearPlaylist(playlist.id)
        }

        resolvedSongs.forEachIndexed { index, songItem ->
            val songEntity = SongEntity(
                id = songItem.id,
                title = songItem.title,
                duration = songItem.duration ?: 0,
                thumbnailUrl = songItem.thumbnail
            )
            database.insert(songEntity)
            database.insert(PlaylistSongMap(playlistId = playlist.id, songId = songItem.id, position = index))
        }
        
        onLog?.invoke("Done!")
    }
}
