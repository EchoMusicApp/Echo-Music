package iad1tya.echo.music.lyrics

import android.content.Context
import iad1tya.echo.music.constants.EnableBetterLyricsKey
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class TTMLResponse(
    val ttml: String
)

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

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
            defaultRequest {
                url("https://lyrics-api.boidu.dev")
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableBetterLyricsKey] ?: true

    private suspend fun fetchTTML(
        title: String,
        artist: String,
        duration: Int,
    ): String? = runCatching {
        val response = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) parameter("d", duration)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<TTMLResponse>().ttml
        } else {
            null
        }
    }.getOrNull()

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        val ttml = fetchTTML(title, artist, duration)
            ?: throw IllegalStateException("Lyrics unavailable")
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) throw IllegalStateException("Failed to parse lyrics")
        TTMLParser.toLRC(parsedLines)
    }
}
