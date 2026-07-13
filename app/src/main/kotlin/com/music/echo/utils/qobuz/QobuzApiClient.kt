package iad1tya.echo.music.utils.qobuz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

// Models

@Serializable
data class SquidWtfEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class QobuzSearchData(
    val query: String? = null,
    val tracks: QobuzTrackList? = null,
    val albums: QobuzAlbumList? = null,
    val switchTo: String? = null,
)

@Serializable
data class QobuzTrackList(
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val items: List<QobuzTrack> = emptyList(),
)

@Serializable
data class QobuzAlbumList(
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val items: List<QobuzAlbum> = emptyList(),
)

@Serializable
data class QobuzTrack(
    val id: Long,
    val title: String,
    val duration: Int = 0,
    val isrc: String? = null,
    val performer: QobuzPerformer? = null,
    val album: QobuzAlbum? = null,
    @SerialName("maximum_bit_depth") val maximumBitDepth: Int = 0,
    @SerialName("maximum_sampling_rate") val maximumSamplingRate: Float = 0f,
    val streamable: Boolean = true,
    val hires: Boolean = false,
    val version: String? = null,
)

@Serializable
data class QobuzPerformer(
    val id: Long? = null,
    val name: String,
)

@Serializable
data class QobuzAlbum(
    val id: String? = null,
    val title: String? = null,
    @SerialName("tracks_count") val tracksCount: Int = 0,
    val artist: QobuzPerformer? = null,
    val image: QobuzImage? = null,
    val upc: String? = null,
    @SerialName("released_at") val releasedAt: Long = 0L,
)

@Serializable
data class QobuzImage(
    val small: String? = null,
    val thumbnail: String? = null,
    val large: String? = null,
    val back: String? = null,
)

@Serializable
data class QobuzDownloadData(
    val url: String? = null,
)

object QobuzQuality {
    const val MP3_320: Int = 5
    const val FLAC_CD: Int = 6
    const val FLAC_HIRES_96: Int = 7
    const val FLAC_HIRES_192: Int = 27
}

class QobuzApiException(
    val status: Int,
    override val message: String?,
) : RuntimeException("squid.wtf $status: $message")

// Client

class QobuzApiClient {
    private companion object {
        // Shared across instances. OkHttp is designed to be reused (a single pool
        // of connections and threads); previously a new client was built for every
        // QobuzApiClient(), and one was created per lossless resolution attempt.
        private val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Echo-Music/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
        private const val baseUrl: String = "https://qobuz.kennyy.com.br/api"
        private val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    suspend fun search(
        query: String,
        offset: Int = 0,
    ): QobuzSearchData = withContext(Dispatchers.IO) {
        val url = "$baseUrl/get-music".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("offset", offset.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        executeAndParseEnvelope<QobuzSearchData>(request)
    }

    suspend fun getFileUrl(
        trackId: Long,
        quality: Int = QobuzQuality.FLAC_HIRES_192,
    ): QobuzDownloadData = withContext(Dispatchers.IO) {
        val url = "$baseUrl/download-music".toHttpUrl().newBuilder()
            .addQueryParameter("track_id", trackId.toString())
            .addQueryParameter("quality", quality.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        executeAndParseEnvelope<QobuzDownloadData>(request)
    }

    private inline fun <reified T> executeAndParseEnvelope(request: Request): T {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val parsedMessage = runCatching {
                    json.decodeFromString<SquidWtfEnvelope<T>>(body).error
                }.getOrNull()
                throw QobuzApiException(
                    status = response.code,
                    message = parsedMessage ?: response.message.ifBlank { "HTTP ${response.code}" },
                )
            }

            val envelope = runCatching { json.decodeFromString<SquidWtfEnvelope<T>>(body) }
                .getOrElse { e ->
                    throw QobuzApiException(
                        status = response.code,
                        message = "malformed JSON: ${e.message}",
                    )
                }

            if (!envelope.success || envelope.data == null) {
                throw QobuzApiException(
                    status = response.code,
                    message = envelope.error ?: "empty data with success=${envelope.success}",
                )
            }
            return envelope.data
        }
    }
}
