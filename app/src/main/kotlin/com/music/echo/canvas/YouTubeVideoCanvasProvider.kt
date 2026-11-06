package echo.music.iad1tya.canvas

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object YouTubeVideoCanvasProvider {
  private const val TAG = "YouTubeVideoCanvasProvider"
  private val cache = ConcurrentHashMap<String, CanvasArtwork>()

  suspend fun getCanvasForVideo(
    videoId: String,
    title: String? = null,
    artist: String? = null,
    durationSec: Long? = null
  ): CanvasArtwork? {
    if (videoId.isBlank() || videoId.length < 5) return null
    cache[videoId]?.let { return it }

    return withContext(Dispatchers.IO) {
      var videoUrl: String? = null
      var totalDurationSec: Long = durationSec?.takeIf { it > 0 } ?: 0L

      // 1. Try InnerTube direct clients first (fastest)
      val clients = listOf(
        YouTubeClient.ANDROID_VR_1_43_32,
        YouTubeClient.ANDROID_VR_1_65_10,
        YouTubeClient.VISIONOS,
        YouTubeClient.TVHTML5
      )

      for (client in clients) {
        try {
          val response = YouTube.player(videoId = videoId, client = client).getOrNull()
          val streamingData = response?.streamingData ?: continue

          if (totalDurationSec <= 0L) {
            totalDurationSec = response.videoDetails?.lengthSeconds?.toLongOrNull() ?: 0L
          }

          val formats = (streamingData.formats.orEmpty() + streamingData.adaptiveFormats)
            .filter { format ->
              val isVideo = format.width != null || format.mimeType.startsWith("video/")
              val hasUrl = !format.url.isNullOrBlank()
              isVideo && hasUrl
            }

          val candidate = formats
            .filter { it.mimeType.contains("mp4", ignoreCase = true) }
            .minByOrNull {
              val h = it.height ?: 360
              kotlin.math.abs(h - 480)
            } ?: formats.firstOrNull()

          if (candidate != null && !candidate.url.isNullOrBlank()) {
            videoUrl = candidate.url
            Timber.tag(TAG).d("Found video stream via InnerTube (${client.clientName}): $videoUrl")
            break
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "InnerTube client ${client.clientName} failed for canvas")
        }
      }

      // 2. If InnerTube direct didn't yield a stream, fall back to NewPipeExtractor
      if (videoUrl.isNullOrBlank()) {
        try {
          val newPipeResult = NewPipeExtractor.getVideoUrlAndDuration(videoId)
          if (newPipeResult != null) {
            videoUrl = newPipeResult.first
            if (totalDurationSec <= 0L) {
              totalDurationSec = newPipeResult.second
            }
            Timber.tag(TAG).d("Found video stream via NewPipe: $videoUrl")
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "NewPipeExtractor failed for canvas videoId: $videoId")
        }
      }

      if (videoUrl.isNullOrBlank()) {
        Timber.tag(TAG).w("No video stream found for canvas videoId: $videoId")
        return@withContext null
      }

      // Calculate 5-second clip from the center of the video
      val totalDurationMs = if (totalDurationSec > 0) totalDurationSec * 1000L else 180_000L
      val clipDurationMs = 5_000L
      val centerMs = totalDurationMs / 2
      val startMs = (centerMs - clipDurationMs / 2).coerceAtLeast(0L)
      val endMs = (startMs + clipDurationMs).coerceAtMost(totalDurationMs)

      val artwork = CanvasArtwork(
        name = title,
        artist = artist,
        videoUrl = videoUrl,
        animated = videoUrl,
        clipStartMs = startMs,
        clipEndMs = endMs
      )

      cache[videoId] = artwork
      artwork
    }
  }
}
