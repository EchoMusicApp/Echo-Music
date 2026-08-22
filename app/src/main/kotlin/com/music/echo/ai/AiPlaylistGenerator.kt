package echo.music.iad1tya.ai

import android.content.Context
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.constants.AiProviderKey
import echo.music.iad1tya.constants.OpenRouterApiKey
import echo.music.iad1tya.constants.OpenRouterBaseUrlKey
import echo.music.iad1tya.constants.OpenRouterModelKey
import echo.music.iad1tya.db.InternalDatabase
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

object AiPlaylistGenerator {
    private val client = OkHttpClient()

    suspend fun generatePlaylist(
        context: Context,
        userPrompt: String,
        numberOfSongs: Int = 15,
        onLog: suspend (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val database = InternalDatabase.newInstance(context)
        
        onLog("Connecting to AI Provider...")

        val aiProvider = context.dataStore.get(AiProviderKey, "OpenRouter")
        
        val systemPrompt = """
            You are a highly accurate and strict music historian. The user will ask for a playlist based on a specific prompt.
            You must output ONLY a valid JSON object with a creative playlist name and a list of exactly $numberOfSongs songs.
            
            CRITICAL RULES:
            1. YOU MUST VERIFY THE RELEASE YEAR, MOVIE, AND ARTIST/ACTOR for EVERY SINGLE TRACK.
            2. ONLY include songs that EXACTLY match the user's prompt (e.g., if they ask for "90s SRK", do NOT include recent songs like "Gerua" or "Do Anjaane Ajnabi" - every song MUST be from the 1990s AND feature Shahrukh Khan).
            3. If the user specifies an era (e.g. "90s"), EVERY SINGLE SONG MUST be released in that exact decade (e.g. 1990-1999). 
            4. You MUST output ONLY raw JSON. Do NOT include any markdown formatting (like ```json), explanations, or conversational text.
            
            Example format:
            {
              "name": "Creative Playlist Name",
              "songs": [
                {"title": "Song Name", "artist": "Artist Name"}
              ]
            }
        """.trimIndent()

        val jsonOutput = if (aiProvider == "Puter") {
            // Puter logic placeholder
            onLog("Puter is not implemented yet. Using dummy data.")
            "{}"
        } else {
            val apiKey = context.dataStore.get(OpenRouterApiKey, "")
            val baseUrl = context.dataStore.get(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
            val model = context.dataStore.get(OpenRouterModelKey, "google/gemini-2.5-flash-lite")

            if (apiKey.isEmpty()) {
                onLog("API Key is missing. Please set it in Settings.")
                return@withContext null
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    onLog("AI Request failed: ${response.code}")
                    return@withContext null
                }

                val responseString = response.body?.string() ?: return@withContext null
                val responseJson = JSONObject(responseString)
                val choices = responseJson.optJSONArray("choices") ?: return@withContext null
                choices.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "{}"
            } catch (e: Exception) {
                onLog("Network error: ${e.message}")
                return@withContext null
            }
        }

        onLog("Parsing AI response...")
        val cleanJsonStr = jsonOutput.replace("```json", "").replace("```", "").trim()
        
        val parsedJson = try {
            JSONObject(cleanJsonStr)
        } catch (e: Exception) {
            onLog("Failed to parse AI output.")
            return@withContext null
        }

        val playlistName = parsedJson.optString("name", "AI Playlist")
        val songsArray = parsedJson.optJSONArray("songs") ?: return@withContext null

        val resolvedSongs = mutableListOf<SongItem>()
        val totalSongs = songsArray.length()

        onLog("Found $totalSongs songs. Searching InnerTube...")

        for (i in 0 until totalSongs) {
            val item = songsArray.optJSONObject(i) ?: continue
            val title = item.optString("title")
            val artist = item.optString("artist")
            if (title.isNotEmpty()) {
                onLog("Searching [${i + 1}/$totalSongs]: $title - $artist")
                val searchQuery = "$title $artist"
                val result = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                val topResult = result?.items?.firstOrNull() as? SongItem
                if (topResult != null) {
                    resolvedSongs.add(topResult)
                }
            }
        }

        if (resolvedSongs.isEmpty()) {
            onLog("Failed to find any of the suggested songs.")
            return@withContext null
        }

        onLog("Creating playlist...")
        val playlistEntity = PlaylistEntity(
            name = playlistName,
            bookmarkedAt = java.time.LocalDateTime.now(),
            isLocal = true,
            isEditable = true
        )
        database.insert(playlistEntity)
        val playlistId = playlistEntity.id

        resolvedSongs.forEachIndexed { index, songItem ->
            val songEntity = SongEntity(
                id = songItem.id,
                title = songItem.title,
                duration = songItem.duration ?: 0,
                thumbnailUrl = songItem.thumbnail
            )
            database.insert(songEntity)
            database.insert(PlaylistSongMap(playlistId = playlistId, songId = songItem.id, position = index))
        }

        onLog("Done!")
        return@withContext playlistId
    }
}
