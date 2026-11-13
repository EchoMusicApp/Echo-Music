package echo.music.iad1tya.podcast

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class VideoTrackOption(
  val label: String, // e.g. "1080p", "720p", "480p", "360p"
  val height: Int,
  val videoUrl: String,
  val audioUrl: String? = null,
  val isMuxed: Boolean = true
)

object PodcastVideoProvider {
  private const val TAG = "PodcastVideoProvider"

  suspend fun resolveVideoTracks(videoId: String): List<VideoTrackOption> = withContext(Dispatchers.IO) {
    val results = mutableListOf<VideoTrackOption>()

    // 1. Try InnerTube direct player clients (VISIONOS first, then TVHTML5)
    val clients = listOf(
      YouTubeClient.VISIONOS,
      YouTubeClient.TVHTML5,
      YouTubeClient.ANDROID_VR_1_65_10
    )

    for (client in clients) {
      try {
        val response = YouTube.player(videoId = videoId, client = client).getOrNull()
        val streamingData = response?.streamingData ?: continue

        val bestAudio = streamingData.adaptiveFormats
          .filter { it.mimeType.startsWith("audio/") && !it.url.isNullOrBlank() }
          .maxByOrNull { it.bitrate ?: 0 }
          ?.url

        // Muxed formats (audio + video combined)
        for (f in streamingData.formats.orEmpty()) {
          val url = f.url ?: continue
          val h = f.height ?: continue
          if (h >= 144) {
            results.add(
              VideoTrackOption(
                label = "${h}p",
                height = h,
                videoUrl = url,
                audioUrl = null,
                isMuxed = true
              )
            )
          }
        }

        // Adaptive video formats (1080p, 720p, 480p, 360p, etc.)
        for (f in streamingData.adaptiveFormats.filter { (it.width != null || it.mimeType.startsWith("video/")) && !it.url.isNullOrBlank() }) {
          val url = f.url ?: continue
          val h = f.height ?: continue
          if (h >= 144) {
            results.add(
              VideoTrackOption(
                label = "${h}p",
                height = h,
                videoUrl = url,
                audioUrl = bestAudio,
                isMuxed = false
              )
            )
          }
        }

        if (results.isNotEmpty()) {
          Timber.tag(TAG).d("Resolved ${results.size} video streams via InnerTube ${client.clientName}")
          break
        }
      } catch (e: Exception) {
        Timber.tag(TAG).w(e, "InnerTube failed to resolve video tracks for $videoId")
      }
    }

    // 2. Fall back to NewPipe Extractor if InnerTube did not yield video tracks
    if (results.isEmpty()) {
      try {
        val npTracks = NewPipeExtractor.getVideoTracks(videoId)
        for (t in npTracks) {
          results.add(
            VideoTrackOption(
              label = t.label,
              height = t.height,
              videoUrl = t.videoUrl,
              audioUrl = t.audioUrl,
              isMuxed = t.isMuxed
            )
          )
        }
      } catch (e: Exception) {
        Timber.tag(TAG).w(e, "NewPipe failed to resolve video tracks for $videoId")
      }
    }

    // Deduplicate by resolution height and pick best stream
    val distinctTracks = results
      .groupBy { it.height }
      .mapNotNull { (_, tracks) ->
        // Prefer MP4/muxed if available, or first valid
        tracks.firstOrNull { it.videoUrl.contains("mp4", ignoreCase = true) && it.isMuxed }
          ?: tracks.firstOrNull { it.videoUrl.contains("mp4", ignoreCase = true) }
          ?: tracks.firstOrNull()
      }
      .sortedByDescending { it.height }

    distinctTracks
  }
}
