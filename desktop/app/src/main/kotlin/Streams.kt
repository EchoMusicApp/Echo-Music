package echo.desktop

import com.music.innertube.YouTube
import echo.desktop.core.YouTubeSetup
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedStream(val url: String, val userAgent: String, val description: String)

/** Resolves a direct audio URL for a video id, trying clients that return unciphered URLs. */
object StreamResolver {
    private val clients =
        listOf(
            "ANDROID_VR" to YouTubeClient.ANDROID_VR_1_43_32,
            "IOS" to YouTubeClient.IOS,
        )

    suspend fun resolve(videoId: String): Result<ResolvedStream> =
        withContext(Dispatchers.IO) {
            YouTubeSetup.ensureInit()
            val problems = mutableListOf<String>()
            for ((name, client) in clients) {
                val response = YouTube.player(videoId, client = client).getOrElse {
                    problems += "$name: ${it.message ?: it::class.simpleName}"
                    continue
                }
                val status = response.playabilityStatus
                val best =
                    response.streamingData
                        ?.adaptiveFormats
                        ?.filter { it.isAudio && !it.url.isNullOrBlank() }
                        ?.maxByOrNull { it.bitrate }
                if (best != null) {
                    return@withContext Result.success(
                        ResolvedStream(
                            url = best.url!!,
                            userAgent = client.userAgent,
                            description = "${best.mimeType.substringBefore(';')} ${best.bitrate / 1000} kbps via $name",
                        )
                    )
                }
                problems += "$name: ${status.status}${status.reason?.let { " ($it)" } ?: ""}"
            }
            Result.failure(IllegalStateException("No playable stream. " + problems.joinToString("; ")))
        }
}
