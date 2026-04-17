package iad1tya.echo.music.canvas

import com.echo.innertube.CloudflareDnsResolver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

object ArchiveTuneCanvas {
    private const val BASE_URL = "https://artwork-archivetune.koiiverse.cloud/"

    @Volatile
    private var bearerToken: String? = null

    fun initialize(bearerToken: String?) {
        this.bearerToken = bearerToken?.trim()?.takeIf { it.isNotEmpty() }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    dns(CloudflareDnsResolver)
                }
            }
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 18_000
                socketTimeoutMillis = 18_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            defaultRequest {
                url(BASE_URL)
                bearerToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttlMs = 60_000L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        duration: Int? = null,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("sa", song, artist, album.orEmpty(), duration?.toString().orEmpty(), storefront)
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > System.currentTimeMillis()) return entry.value
            cache.remove(key)
        }

        val response = runCatching {
            client.get {
                parameter("s", song)
                parameter("a", artist)
                if (album != null) parameter("al", album)
                if (duration != null && duration > 0) parameter("d", duration)
                parameter("storefront", storefront)
            }
        }.getOrNull()

        val value = when (response?.status) {
            HttpStatusCode.OK -> runCatching { response.body<CanvasArtwork>() }.getOrNull()
            else -> null
        }

        cache[key] = CacheEntry(
            value = value,
            expiresAtMs = System.currentTimeMillis() + ttlMs,
        )

        return value
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        val normalized = parts
            .map { it.trim().lowercase(Locale.ROOT) }
            .joinToString("|")
        return "$prefix|$normalized"
    }
}

@Serializable
data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}

object CanvasArtworkPlaybackCache {
    private const val defaultMaxSize = 256
    private const val PERSIST_FILE = "canvas_artwork_cache.json"
    private const val PERSIST_DEBOUNCE_MS = 2_000L

    private val map = LinkedHashMap<String, CanvasArtwork>(defaultMaxSize, 0.75f, true)
    @Volatile private var maxSize = defaultMaxSize

    @Volatile private var cacheFile: File? = null
    private val persistScope = CoroutineScope(Dispatchers.IO)
    private var persistJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun init(filesDir: File) {
        cacheFile = File(filesDir, PERSIST_FILE)
        loadFromDisk()
    }

    @Synchronized
    fun get(mediaId: String): CanvasArtwork? {
        if (maxSize <= 0) return null
        return map[mediaId]
    }

    @Synchronized
    fun put(mediaId: String, artwork: CanvasArtwork) {
        val limit = maxSize
        if (limit <= 0) return
        if (mediaId.isBlank()) return
        map[mediaId] = artwork
        while (map.size > limit) {
            val it = map.entries.iterator()
            if (it.hasNext()) { it.next(); it.remove() }
        }
        schedulePersist()
    }

    @Synchronized
    fun clear() {
        map.clear()
        schedulePersist()
    }

    @Synchronized
    fun setMaxSize(value: Int) {
        maxSize = value.coerceAtLeast(0)
        if (maxSize == 0) {
            map.clear()
            schedulePersist()
            return
        }
        var evicted = false
        while (map.size > maxSize) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
                evicted = true
            } else {
                break
            }
        }
        if (evicted) schedulePersist()
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        runCatching {
            val raw = file.readText()
            if (raw.isBlank()) return
            val root = JSONObject(raw)
            val restored = LinkedHashMap<String, CanvasArtwork>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val mediaId = keys.next()
                val value = root.optJSONObject(mediaId) ?: continue
                val artwork = CanvasArtwork(
                    name = readOptionalString(value, "name"),
                    artist = readOptionalString(value, "artist"),
                    albumId = readOptionalString(value, "albumId"),
                    static = readOptionalString(value, "static"),
                    animated = readOptionalString(value, "animated"),
                    videoUrl = readOptionalString(value, "videoUrl"),
                )
                restored[mediaId] = artwork
            }
            map.clear()
            map.putAll(restored)
            while (maxSize > 0 && map.size > maxSize) {
                val it = map.entries.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                } else {
                    break
                }
            }
        }.onFailure {
            runCatching { file.delete() }
        }
    }

    private fun readOptionalString(obj: JSONObject, key: String): String? {
        return if (obj.isNull(key)) null else obj.optString(key, "")
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            writeToDisk()
        }
    }

    private fun writeToDisk() {
        val file = cacheFile ?: return
        runCatching {
            val snapshot: Map<String, CanvasArtwork>
            synchronized(this@CanvasArtworkPlaybackCache) {
                snapshot = LinkedHashMap(map)
            }
            val root = JSONObject()
            snapshot.forEach { (mediaId, artwork) ->
                val value = JSONObject()
                if (artwork.name != null) value.put("name", artwork.name)
                if (artwork.artist != null) value.put("artist", artwork.artist)
                if (artwork.albumId != null) value.put("albumId", artwork.albumId)
                if (artwork.static != null) value.put("static", artwork.static)
                if (artwork.animated != null) value.put("animated", artwork.animated)
                if (artwork.videoUrl != null) value.put("videoUrl", artwork.videoUrl)
                root.put(mediaId, value)
            }
            val raw = root.toString()
            file.writeText(raw)
        }
    }
}
