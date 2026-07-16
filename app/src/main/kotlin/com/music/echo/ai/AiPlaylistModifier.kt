package iad1tya.echo.music.ai

import android.content.Context
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import iad1tya.echo.music.constants.AiProviderKey
import iad1tya.echo.music.constants.OpenRouterApiKey
import iad1tya.echo.music.constants.OpenRouterBaseUrlKey
import iad1tya.echo.music.constants.OpenRouterModelKey
import iad1tya.echo.music.db.InternalDatabase
import iad1tya.echo.music.db.entities.PlaylistSong
import iad1tya.echo.music.db.entities.PlaylistSongMap
import iad1tya.echo.music.db.entities.SongEntity
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object AiPlaylistModifier {
    private val client = OkHttpClient()

    suspend fun modifyPlaylist(
        context: Context,
        playlistId: String,
        currentSongs: List<PlaylistSong>,
        userPrompt: String,
        onLog: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val database = InternalDatabase.newInstance(context)
        
        onLog("Connecting to AI Provider...")

        val aiProvider = context.dataStore.get(AiProviderKey, "OpenRouter")
        
        val currentPlaylistJson = JSONArray().apply {
            currentSongs.forEach { playlistSong ->
                put(JSONObject().apply {
                    put("id", playlistSong.map.id)
                    put("title", playlistSong.song.song.title)
                    put("artist", playlistSong.song.artists.joinToString { it.name })
                })
            }
        }.toString()

        val systemPrompt = """
            You are a highly accurate and strict music historian. The user wants to modify an existing playlist based on a specific prompt.
            The user will provide an INSTRUCTION.
            The CURRENT playlist is:
            $currentPlaylistJson

            You must output ONLY a valid JSON object containing exactly two arrays: "remove_ids" (list of internal IDs to remove) and "additions" (list of objects with "title" and "artist").
            
            CRITICAL RULES:
            1. YOU MUST VERIFY THE RELEASE YEAR, MOVIE, AND ARTIST/ACTOR for EVERY SINGLE TRACK you add or remove.
            2. For removals, ONLY include the "id" from the CURRENT playlist that match the user's removal instruction.
            3. For additions, ONLY include songs that match the user's addition instruction.
            4. You MUST output ONLY raw JSON. Do NOT include any markdown formatting (like ```json), explanations, or conversational text.
            
            Example format:
            {
              "remove_ids": [123, 456],
              "additions": [
                {"title": "Song Name", "artist": "Artist Name"}
              ]
            }
        """.trimIndent()

        val jsonOutput = if (aiProvider == "Puter") {
            onLog("Puter is not implemented yet. Using dummy data.")
            "{}"
        } else {
            val apiKey = context.dataStore.get(OpenRouterApiKey, "")
            val baseUrl = context.dataStore.get(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
            val model = context.dataStore.get(OpenRouterModelKey, "google/gemini-2.5-flash-lite")

            if (apiKey.isEmpty()) {
                onLog("API Key is missing. Please set it in Settings.")
                return@withContext
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
                    return@withContext
                }

                val responseString = response.body?.string() ?: return@withContext
                val responseJson = JSONObject(responseString)
                val choices = responseJson.optJSONArray("choices") ?: return@withContext
                choices.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "{}"
            } catch (e: Exception) {
                onLog("Network error: ${e.message}")
                return@withContext
            }
        }

        onLog("Parsing AI response...")
        val cleanJsonStr = jsonOutput.replace("```json", "").replace("```", "").trim()
        
        val parsedJson = try {
            JSONObject(cleanJsonStr)
        } catch (e: Exception) {
            onLog("Failed to parse AI output.")
            return@withContext
        }

        val removeIdsArray = parsedJson.optJSONArray("remove_ids")
        val additionsArray = parsedJson.optJSONArray("additions")

        val totalToRemove = removeIdsArray?.length() ?: 0
        if (totalToRemove > 0) {
            onLog("Removing $totalToRemove songs...")
            for (i in 0 until totalToRemove) {
                val idToRemove = removeIdsArray?.optInt(i, -1) ?: -1
                if (idToRemove != -1) {
                    val mapEntry = currentSongs.find { it.map.id == idToRemove }?.map
                    if (mapEntry != null) {
                        database.delete(mapEntry)
                    }
                }
            }
        }

        val totalToAdd = additionsArray?.length() ?: 0
        if (totalToAdd > 0) {
            val resolvedSongs = mutableListOf<SongItem>()
            onLog("Searching for $totalToAdd new songs on InnerTube...")

            for (i in 0 until totalToAdd) {
                val item = additionsArray?.optJSONObject(i) ?: continue
                val title = item.optString("title")
                val artist = item.optString("artist")
                if (title.isNotEmpty()) {
                    onLog("Searching [${i + 1}/$totalToAdd]: $title - $artist")
                    val searchQuery = "$title $artist"
                    val result = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    val topResult = result?.items?.firstOrNull() as? SongItem
                    if (topResult != null) {
                        resolvedSongs.add(topResult)
                    }
                }
            }

            if (resolvedSongs.isNotEmpty()) {
                onLog("Adding ${resolvedSongs.size} songs to playlist...")
                
                // Get the current max position to append new songs
                val maxPosition = currentSongs.maxOfOrNull { it.map.position } ?: -1

                resolvedSongs.forEachIndexed { index, songItem ->
                    val songEntity = SongEntity(
                        id = songItem.id,
                        title = songItem.title,
                        duration = songItem.duration ?: 0,
                        thumbnailUrl = songItem.thumbnail
                    )
                    database.upsert(songEntity)
                    database.insert(
                        PlaylistSongMap(
                            playlistId = playlistId,
                            songId = songItem.id,
                            position = maxPosition + 1 + index
                        )
                    )
                }
            } else {
                onLog("Failed to find any of the suggested additions.")
            }
        }

        if (totalToRemove == 0 && totalToAdd == 0) {
            onLog("No modifications requested by AI.")
        } else {
            onLog("Done! Playlist modified.")
        }
    }
}
