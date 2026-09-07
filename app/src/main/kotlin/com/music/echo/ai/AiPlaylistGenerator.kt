package echo.music.iad1tya.ai

import android.content.Context
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.ai.weather.toSnapshotString
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
        weatherInfo: echo.music.iad1tya.ai.weather.WeatherInfo? = null,
        onLog: suspend (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val database = InternalDatabase.newInstance(context)
        
        onLog("Connecting to AI Provider...")

        val aiProvider = context.dataStore.get(AiProviderKey, "OpenRouter")
        
        val weatherDirective = if (weatherInfo != null) {
            """
            METEOROLOGICAL ATMOSPHERIC DIRECTIVE:
            Current Weather: ${weatherInfo.condition} (${weatherInfo.weatherEmoji})
            Temperature: ${weatherInfo.temperature}°C (Feels like ${weatherInfo.feelsLike}°C)
            Humidity: ${weatherInfo.humidity}%
            Wind Speed: ${weatherInfo.windSpeed} km/h
            Time of Day: ${if (weatherInfo.isDay) "Daytime" else "Nighttime"}

            ACOUSTIC SELECTION GUIDANCE:
            Match acoustic properties (tempo, genre, instruments, energy, valence) to these meteorological conditions:
            - Rainy/Gloomy/Thunderstorm: Lofi, acoustic indie, melancholic ambient, cozy jazz, warm piano, soft folk.
            - Sunny/Bright/Clear: Upbeat pop, vibrant indie, summer anthems, energetic dance, cheerful acoustic.
            - Foggy/Cold/Snowy: Soft ambient, chill synth, warm acoustic, atmospheric instrumental.
            - Nighttime: Late night synthwave, chill beats, smooth R&B, ambient lounge, nocturnal jazz.
            """.trimIndent()
        } else ""

        val systemPrompt = """
            You are a highly accurate music curator and historian. The user will ask for an AI playlist.
            You must output ONLY a valid JSON object with a creative playlist name and a list of exactly $numberOfSongs songs.
            
            $weatherDirective
            
            CRITICAL RULES:
            1. YOU MUST VERIFY THE RELEASE YEAR, MOVIE, AND ARTIST for EVERY SINGLE TRACK.
            2. Include songs that EXACTLY match the user's prompt and atmospheric weather directive.
            3. You MUST output ONLY raw JSON. Do NOT include markdown formatting (like ```json), explanations, or conversational text.
            
            Example format:
            {
              "name": "Creative Playlist Name",
              "songs": [
                {"title": "Song Name", "artist": "Artist Name"}
              ]
            }
        """.trimIndent()

        val fullUserPrompt = buildString {
            append("Mood: ")
            append(userPrompt.ifBlank { "Unspecified - drive solely by the weather vibe" })
            append("\n")
            if (weatherInfo != null) {
                append("Weather: ${weatherInfo.condition}, ${weatherInfo.temperature}°C (Feels like ${weatherInfo.feelsLike}°C), Humidity: ${weatherInfo.humidity}%, ${if (weatherInfo.isDay) "Daytime" else "Nighttime"}\n")
            }
            append("Desired playlist length: $numberOfSongs")
        }

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
                        put("content", fullUserPrompt)
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
        val weatherSnapshot = weatherInfo?.toSnapshotString()
        val playlistEntity = PlaylistEntity(
            name = playlistName,
            radioEndpointParams = weatherSnapshot,
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
