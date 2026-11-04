package echo.desktop

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import echo.desktop.core.YouTubeSetup
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class ResolvedStream(val url: String, val userAgent: String, val description: String, val extension: String)

/**
 * Finds a playable audio URL for a video id. Every candidate is probed with a tiny ranged request
 * first, so we only hand VLC a URL that YouTube's server actually answers.
 */
object StreamResolver {
    /** Audio itags in order of preference: opus 160k, AAC 128k, opus 70k, opus 50k, AAC 256k, AAC 48k. */
    private val audioPreference get() = if (Settings.lowQuality) listOf(249, 139, 250, 140, 251, 141) else listOf(251, 140, 250, 249, 141, 139)

    private val http: HttpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    private val clients =
        listOf(
            "ANDROID_VR" to YouTubeClient.ANDROID_VR_1_43_32,
            "ANDROID_VR_NEW" to YouTubeClient.ANDROID_VR_1_65_10,
            "IOS" to YouTubeClient.IOS,
            "IPADOS" to YouTubeClient.IPADOS,
            "VISIONOS" to YouTubeClient.VISIONOS,
        )

    /** HTTP status of a 2-byte ranged GET, or -1 if the request itself failed. */
    private fun probe(url: String, userAgent: String): Int =
        try {
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Range", "bytes=0-1")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build()
            http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
        } catch (t: Throwable) {
            -1
        }

    /**
     * Playable-looking streams, best first, produced lazily: the caller stops at the first one it can
     * actually download. Why each one was skipped goes to [log].
     */
    fun candidates(videoId: String, log: MutableList<String>): Flow<ResolvedStream> =
        flow {
                YouTubeSetup.ensureInit()

                // 1) NewPipe extractor: handles YouTube's URL scrambling on its own.
                try {
                    val webUa = YouTubeClient.USER_AGENT_WEB
                    val found =
                        NewPipeExtractor.newPipePlayer(videoId)
                            .filter { it.first in audioPreference }
                            .sortedBy { audioPreference.indexOf(it.first) }
                    if (found.isEmpty()) log += "NewPipe: no audio streams"
                    for ((itag, url) in found) {
                        val code = probe(url, webUa)
                        log += "NewPipe itag $itag: HTTP $code"
                        if (code in 200..299) emit(ResolvedStream(url, webUa, "NewPipe itag $itag", if (itag in 249..251) "webm" else "m4a"))
                    }
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    log += "NewPipe failed: ${t.message ?: t::class.simpleName}"
                }

                // 2) InnerTube clients that sometimes hand out ready-to-use URLs.
                for ((name, client) in clients) {
                    val response = YouTube.player(videoId, client = client).getOrElse {
                        log += "$name: ${it.message ?: it::class.simpleName}"
                        continue
                    }
                    val audio =
                        response.streamingData?.adaptiveFormats?.filter { it.isAudio && !it.url.isNullOrBlank() }?.sortedByDescending { it.bitrate }.orEmpty()
                    if (audio.isEmpty()) {
                        log += "$name: ${response.playabilityStatus.status}"
                        continue
                    }
                    for (format in audio.take(3)) {
                        val code = probe(format.url!!, client.userAgent)
                        log += "$name itag ${format.itag}: HTTP $code"
                        if (code in 200..299) {
                            emit(ResolvedStream(format.url!!, client.userAgent, "$name itag ${format.itag}", if (format.mimeType.contains("webm")) "webm" else "m4a"))
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
}
