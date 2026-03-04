package iad1tya.echo.music.lyrics

import android.content.Context
import iad1tya.echo.music.constants.EnableLyricsPlus
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LyricLineResponse(
    val time: Long,
    val duration: Long,
    val text: String,
)

@Serializable
private data class LyricsPlusResponse(
    val type: String? = null,
    val lyrics: List<LyricLineResponse>? = null,
)

object LyricsPlusProvider : LyricsProvider {
    override val name = "LyricsPlus"

    private val baseUrls = listOf(
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus-seven.vercel.app",
    )

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableLyricsPlus] ?: false

    private suspend fun fetchFromUrl(
        url: String,
        title: String,
        artist: String,
        duration: Int,
    ): LyricsPlusResponse? = runCatching {
        val response = client.get("$url/v2/lyrics/get") {
            parameter("title", title)
            parameter("artist", artist)
            parameter("duration", if (duration > 0) duration else -1)
            parameter("source", "apple,lyricsplus,musixmatch,spotify,musixmatch-word")
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<LyricsPlusResponse>()
        } else {
            null
        }
    }.getOrNull()

    private suspend fun fetchLyrics(
        title: String,
        artist: String,
        duration: Int,
    ): LyricsPlusResponse? {
        for (baseUrl in baseUrls) {
            try {
                val result = fetchFromUrl(baseUrl, title, artist, duration)
                if (result != null && !result.lyrics.isNullOrEmpty()) {
                    return result
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun convertToLrc(response: LyricsPlusResponse?): String? {
        if (response?.lyrics.isNullOrEmpty()) return null

        return response!!.lyrics!!.mapNotNull { line ->
            val minutes = line.time / 1000 / 60
            val seconds = (line.time / 1000) % 60
            val millis = line.time % 1000 / 10
            if (line.text.isNotBlank()) {
                String.format("[%02d:%02d.%02d]%s", minutes, seconds, millis, line.text)
            } else {
                null
            }
        }.joinToString("\n")
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        val response = fetchLyrics(title, artist, duration)
        val lrc = convertToLrc(response)
        if (lrc.isNullOrBlank()) throw IllegalStateException("Lyrics unavailable")
        lrc
    }
}
