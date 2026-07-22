package iad1tya.echo.music.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Serializable
data class LosslessIndex(
    val items: List<LosslessTrack> = emptyList()
)

@Serializable
data class LosslessTrack(
    val song: String,
    val artist: String,
    val url: String
)

@Serializable
data class DonationGoal(
    val current: Int = 0,
    val target: Int = 100
)

object LosslessAPI {
    private val httpClient = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    
    private var cachedIndex: List<LosslessTrack>? = null
    private var lastFetchTime = 0L

    private suspend fun fetchMusicList(): List<LosslessTrack> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedIndex != null && (now - lastFetchTime) < 1000 * 60 * 60) {
            return@withContext cachedIndex!!
        }

        try {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/EchoMusicApp/Lossless-Database/refs/heads/main/music.json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val index = json.decodeFromString<LosslessIndex>(responseBody)
                    cachedIndex = index.items
                    lastFetchTime = now
                    return@withContext index.items
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lossless music index")
        }
        
        return@withContext cachedIndex ?: emptyList()
    }

    suspend fun getRecentTracks(limit: Int = 10): List<LosslessTrack> {
        val list = fetchMusicList()
        return list.take(limit)
    }

    suspend fun search(queryTitle: String, queryArtist: String): LosslessTrack? {
        val list = fetchMusicList()
        val titleTarget = queryTitle.trim().lowercase()
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
        val artistTarget = queryArtist.trim().lowercase()
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
        
        val track = list.find { track ->
            val trackTitle = track.song.trim().lowercase()
            val trackArtist = track.artist.trim().lowercase()
            
            val isTitleMatch = trackTitle == titleTarget || 
                trackTitle.contains(titleTarget) || 
                titleTarget.contains(trackTitle)
                
            val trackArtistParts = trackArtist.split(" & ", ", ", " and ")
            val targetArtistParts = artistTarget.split(" & ", ", ", " and ")
            
            val isArtistMatch = trackArtist == artistTarget || 
                trackArtist.contains(artistTarget) || 
                artistTarget.contains(trackArtist) ||
                trackArtistParts.any { artistTarget.contains(it) } ||
                targetArtistParts.any { trackArtist.contains(it) }
            
            isTitleMatch && isArtistMatch
        }
        
        return track?.let {
            val resolvedUrl = it.url.replace(
                "https://lossless.echomusic.fun/Music/",
                "https://raw.githubusercontent.com/EchoMusicApp/Lossless/main/Music/"
            )
            it.copy(url = resolvedUrl)
        }
    }

    suspend fun getDonationGoal(): DonationGoal = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://lossless.echomusic.fun/goal.json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    return@withContext json.decodeFromString<DonationGoal>(responseBody)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch donation goal")
        }
        return@withContext DonationGoal()
    }
}
