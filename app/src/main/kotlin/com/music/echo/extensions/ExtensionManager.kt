package com.music.echo.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class AddonEndpoint(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Serializable
data class AddonSearchResponse(
    val tracks: List<AddonTrack> = emptyList()
)

@Serializable
data class AddonTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Int? = null,
    val artworkURL: String? = null,
    val albumArtworkURL: String? = null
)

@Serializable
data class AddonStreamResponse(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    val streamQuality: String = "",
    val audioQuality: String = "",
    val codec: String? = null
)

@Singleton
class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient()
    private val ADDONS_KEY = stringPreferencesKey("extensions_addons_list")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAddons(): List<AddonEndpoint> {
        val jsonStr = context.dataStore.data.map { it[ADDONS_KEY] }.first() ?: "[]"
        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addAddon(endpoint: AddonEndpoint) {
        val current = getAddons().toMutableList()
        current.add(endpoint)
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }
    
    suspend fun toggleAddon(id: String, enabled: Boolean) {
        val current = getAddons().map { if (it.id == id) it.copy(enabled = enabled) else it }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun removeAddon(id: String) {
        val current = getAddons().filter { it.id != id }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun search(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val addons = getAddons().filter { it.enabled }
        val results = mutableListOf<SongItem>()
        
        for (addon in addons) {
            try {
                val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
                val searchUrl = "${baseUrl.removeSuffix("/")}/search?q=${query.replace(" ", "%20")}"
                val request = Request.Builder().url(searchUrl).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                    val addonResults = addonResponse.tracks
                    results.addAll(addonResults.map { 
                        SongItem(
                            id = "addon_${addon.id}_${it.id}",
                            title = it.title,
                            artists = listOf(Artist(name = it.artist, id = null)),
                            thumbnail = it.artworkURL ?: it.albumArtworkURL ?: "",
                            duration = it.duration
                        )
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results
    }

    suspend fun getStreamUrl(fullId: String): String? = withContext(Dispatchers.IO) {
        // fullId format: addon_{addonId}_{trackId}
        if (!fullId.startsWith("addon_")) return@withContext null
        val parts = fullId.split("_", limit = 3)
        if (parts.size < 3) return@withContext null
        val addonId = parts[1]
        val trackId = parts[2]
        
        val addon = getAddons().find { it.id == addonId } ?: return@withContext null
        
        try {
            val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
            val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream?id=${trackId}"
            val request = Request.Builder().url(streamUrlReq).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val streamRes: AddonStreamResponse = json.decodeFromString(body)
                return@withContext streamRes.url
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun resolveStream(title: String?, artist: String?, duration: Long?, videoId: String): AddonStreamResponse? = withContext(Dispatchers.IO) {
        if (videoId.startsWith("addon_")) {
            val parts = videoId.split("_", limit = 3)
            if (parts.size == 3) {
                val addonId = parts[1]
                val trackId = parts[2]
                val addons = getAddons().filter { it.enabled }
                val targetAddon = addons.find { it.id == addonId }
                if (targetAddon != null) {
                    val baseUrl = if (targetAddon.url.startsWith("http")) targetAddon.url else "https://" + targetAddon.url
                    val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/$trackId?quality=LOSSLESS"
                    val streamReq = Request.Builder().url(streamUrlReq).build()
                    val streamRes = httpClient.newCall(streamReq).execute()
                    val streamBody = streamRes.body?.string()
                    if (streamRes.isSuccessful && !streamBody.isNullOrEmpty()) {
                        return@withContext json.decodeFromString<AddonStreamResponse>(streamBody)
                    }
                }
            }
            return@withContext null
        }
        if (title == null || artist == null) return@withContext null
        val addons = getAddons().filter { it.enabled }
        if (addons.isEmpty()) return@withContext null
        
        val query = "$title $artist"
        for (addon in addons) {
            try {
                android.util.Log.e("ExtensionManager", "Searching addon ${addon.url} for $title $artist")
                val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
                val searchUrl = "${baseUrl.removeSuffix("/")}/search?q=${query.replace(" ", "%20")}"
                val request = Request.Builder().url(searchUrl).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                    val addonResults = addonResponse.tracks
                    // Very basic Matcher:
                    val match = addonResults.firstOrNull { 
                        it.title.contains(title, ignoreCase = true) 
                    } ?: addonResults.firstOrNull()
                    
                    if (match != null) {
                        android.util.Log.e("ExtensionManager", "Matched track: ${match.title} by ${match.artist}. Fetching stream...")
                        val baseUrl = if (addon.url.startsWith("http")) addon.url else "https://" + addon.url
                        val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/${match.id}?quality=LOSSLESS"
                        val streamReq = Request.Builder().url(streamUrlReq).build()
                        val streamRes = httpClient.newCall(streamReq).execute()
                        val streamBody = streamRes.body?.string()
                        if (streamRes.isSuccessful && !streamBody.isNullOrEmpty()) {
                            android.util.Log.e("ExtensionManager", "Stream response: $streamBody")
                            val streamData: AddonStreamResponse = json.decodeFromString(streamBody)
                            return@withContext streamData
                        } else {
                            android.util.Log.e("ExtensionManager", "Stream request failed: ${streamRes.code} ${streamRes.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
}
