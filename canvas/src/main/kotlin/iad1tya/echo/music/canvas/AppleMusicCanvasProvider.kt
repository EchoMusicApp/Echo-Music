




package iad1tya.echo.music.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private object AppleCanvasLogger {
    fun d(msg: String) = println("AppleMusicCanvas: D: $msg")
    fun w(msg: String) = println("AppleMusicCanvas: W: $msg")
    fun e(t: Throwable, msg: String) {
        println("AppleMusicCanvas: E: $msg")
        t.printStackTrace()
    }
}

object AppleMusicCanvasProvider {
    private const val APPLE_MUSIC_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
            ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
            "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
            ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
                register(ContentType.Text.JavaScript, KotlinxSerializationConverter(json))
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        AppleCanvasLogger.d("getByAlbumArtist: album='$album', artist='$artist'")
        val key = cacheKey("sa", album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchAndFetchMotion(album, artist, album, storefront, "albums")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("song", song, artist, album.orEmpty(), storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchAndFetchMotion(song, artist, album, storefront, "songs")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    suspend fun getByAlbumId(
        albumId: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("id", albumId, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = fetchMotionArtwork(albumId, storefront, null)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    private suspend fun searchAndFetchMotion(
        term: String,
        artist: String,
        album: String?,
        storefront: String,
        type: String,
    ): CanvasArtwork? {
        return runCatching {
            AppleCanvasLogger.d("searching for $type: $term (album: $album) in $storefront")
            var query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
            if (!album.isNullOrBlank() && !query.contains(album, ignoreCase = true)) {
                query = "$query $album"
            }
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", query)
                parameter("types", type)
                parameter("limit", "10")
                parameter("extend", "editorialVideo")
                parameter("include", "albums")
            }
            if (response.status != HttpStatusCode.OK) {
                AppleCanvasLogger.w("search failed with status ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject?.get(type)?.jsonObject?.get("data")?.jsonArray ?: return@runCatching null

            val scoredResults = results.mapNotNull { item ->
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: return@mapNotNull null
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultCollectionName = attributes["collectionName"]?.jsonPrimitive?.contentOrNull ?: ""

                val nameLower = resultName.lowercase(Locale.ROOT)
                val collectionLower = resultCollectionName.lowercase(Locale.ROOT)
                val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                    collectionLower.contains("playlist") || collectionLower.contains("set list") ||
                    nameLower.contains("essentials") || collectionLower.contains("essentials") ||
                    collectionLower.contains("dj mix") || collectionLower.contains("mixed") ||
                    collectionLower.contains("apple music") || collectionLower.contains("today's hits") ||
                    nameLower.contains("session") || collectionLower.contains("session")

                if (isBlacklisted) {
                    AppleCanvasLogger.d("  - Skipping blacklisted result: '$resultName' (Album: '$resultCollectionName')")
                    return@mapNotNull null
                }

                val artistFuzzy = resultArtistName.contains(artist, ignoreCase = true) || artist.contains(resultArtistName, ignoreCase = true)
                if (!artistFuzzy) return@mapNotNull null

                var score = if (resultArtistName.equals(artist, ignoreCase = true)) 10 else 5

                val nameMatch = resultName.equals(term, ignoreCase = true)
                val nameFuzzy = resultName.contains(term, ignoreCase = true) || term.contains(resultName, ignoreCase = true)
                if (nameMatch) {
                    score += 15
                } else if (nameFuzzy) {
                    score += 7
                } else {
                    score -= 10
                }

                val editionWords = listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")
                for (word in editionWords) {
                    val inTerm = term.contains(word, ignoreCase = true)
                    val inResult = resultName.contains(word, ignoreCase = true)
                    if (inTerm && inResult) score += 5
                    else if (inTerm != inResult && inResult) score -= 3
                }

                if (!album.isNullOrBlank() && resultCollectionName.isNotBlank()) {
                    val albumMatch = resultCollectionName.equals(album, ignoreCase = true)
                    val albumFuzzy = resultCollectionName.contains(album, ignoreCase = true) || album.contains(resultCollectionName, ignoreCase = true)
                    if (albumMatch) score += 12 else if (albumFuzzy) score += 6
                }

                if (score < 0) return@mapNotNull null

                AppleCanvasLogger.d("  - Result: '$resultName' by '$resultArtistName' (Album: '$resultCollectionName', ID: ${obj["id"]}) -> Score: $score")
                Triple(score, obj, resultArtistName)
            }.sortedByDescending { it.first }

            AppleCanvasLogger.d("Found ${scoredResults.size} scored results for term '$term'")

            for ((score, obj, resultArtistName) in scoredResults) {
                if (score < 0) {
                    AppleCanvasLogger.d("skipping result with low score: $score")
                    continue
                }

                val targetAlbumId = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                AppleCanvasLogger.d("trying resolve for $targetAlbumId (from ${obj["type"]?.jsonPrimitive?.contentOrNull})")

                val resolved = fetchMotionArtwork(targetAlbumId, storefront, resultArtistName)
                if (resolved != null) return@runCatching resolved
            }

            AppleCanvasLogger.d("no canvas found in resolution/lookup for $term after ${scoredResults.size} results")
            null
        }.getOrElse {
            if (it is CancellationException) throw it
            AppleCanvasLogger.e(it, "error in searchAndFetchMotion for $term")
            null
        }
    }

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        expectedArtist: String?,
    ): CanvasArtwork? {
        return runCatching {
            AppleCanvasLogger.d("fetching album $albumId")
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("extend", "editorialVideo")
                parameter("include", "tracks")
            }
            if (response.status != HttpStatusCode.OK) {
                AppleCanvasLogger.w("album fetch failed for $albumId: ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = data["attributes"]?.jsonObject
            val albumName = attributes?.get("name")?.jsonPrimitive?.contentOrNull
            val artistName = attributes?.get("artistName")?.jsonPrimitive?.contentOrNull

            if (!expectedArtist.isNullOrBlank() && artistName != null && !artistName.contains(expectedArtist, ignoreCase = true) && !expectedArtist.contains(artistName, ignoreCase = true)) {
                AppleCanvasLogger.d("fetchMotionArtwork: artist mismatch for $albumId ($artistName vs $expectedArtist)")
                return@runCatching null
            }

            val editorialVideo = attributes?.get("editorialVideo")?.jsonObject
            val hlsUrl = extractVideoUrl(editorialVideo)
            if (!hlsUrl.isNullOrBlank()) {
                AppleCanvasLogger.d("found editorialVideo for $albumId (album: $albumName, id: $albumId)")
                return@runCatching CanvasArtwork(
                    name = albumName,
                    artist = artistName,
                    albumId = albumId,
                    animated = hlsUrl,
                    videoUrl = hlsUrl,
                )
            }

            AppleCanvasLogger.d("no editorialVideo for $albumId (available keys: ${attributes?.keys})")
            null
        }.getOrElse {
            if (it is CancellationException) throw it
            AppleCanvasLogger.e(it, "error in fetchMotionArtwork for $albumId")
            null
        }
    }

    private fun extractVideoUrl(editorialVideo: JsonObject?): String? {
        if (editorialVideo == null) return null
        val assets = editorialVideo["assets"]?.jsonArray ?: return null
        for (asset in assets) {
            val obj = asset.jsonObject
            val url = obj["url"]?.jsonPrimitive?.contentOrNull
            if (!url.isNullOrBlank()) return url
        }
        AppleCanvasLogger.d("editorialVideo found but no video link in assets: ${editorialVideo.keys}")
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}