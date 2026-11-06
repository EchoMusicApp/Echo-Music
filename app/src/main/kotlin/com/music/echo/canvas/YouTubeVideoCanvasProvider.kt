package echo.music.iad1tya.canvas

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object YouTubeVideoCanvasProvider {
  private const val TAG = "YouTubeVideoCanvasProvider"
  private val cache = ConcurrentHashMap<String, CanvasArtwork>()

  private suspend fun findOfficialVideoId(title: String, artist: String): String? {
    val cleanTitle = title.replace(Regex("""\s*\(.*?\)\s*"""), " ").trim()
    val cleanArtist = artist.split(',', '&', 'x', 'X', '/').firstOrNull()?.trim() ?: artist
    val query = "$cleanTitle $cleanArtist official music video"
    Timber.tag(TAG).d("Searching for official video: $query")

    return try {
      val result = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
      val items = result?.items.orEmpty()
      val bestMatch = items.firstOrNull { item ->
        val itemTitle = item.title.lowercase(Locale.ROOT)
        val hasOfficial = itemTitle.contains("official") || itemTitle.contains("video") || itemTitle.contains("mv")
        val isNotAudioOrLyric = !itemTitle.contains("lyric") && !itemTitle.contains("audio") && !itemTitle.contains("karaoke")
        hasOfficial && isNotAudioOrLyric
      } ?: items.firstOrNull()

      val foundId = bestMatch?.id?.takeIf { it.isNotBlank() }
      if (foundId != null) {
        Timber.tag(TAG).d("Found official video: '${bestMatch.title}' with id=$foundId")
      }
      foundId
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Failed to search for official video")
      null
    }
  }

  suspend fun getCanvasForVideo(
    videoId: String,
    title: String? = null,
    artist: String? = null,
    durationSec: Long? = null
  ): CanvasArtwork? {
    if (videoId.isBlank() || videoId.length < 5) return null
    cache[videoId]?.let { return it }

    return withContext(Dispatchers.IO) {
      // 1. Search for official music video first if title is available
      val officialVideoId = if (!title.isNullOrBlank()) {
        findOfficialVideoId(title, artist.orEmpty())
      } else null

      val targetVideoId = officialVideoId ?: videoId
      Timber.tag(TAG).d("Resolving canvas stream for targetVideoId: $targetVideoId (original: $videoId)")

      var videoUrl: String? = null
      var totalDurationSec: Long = if (targetVideoId == videoId) (durationSec?.takeIf { it > 0 } ?: 0L) else 0L

      // 2. Try InnerTube direct clients (VISIONOS first as it serves complete files)
      val clients = listOf(
        YouTubeClient.VISIONOS,
        YouTubeClient.TVHTML5,
        YouTubeClient.ANDROID_VR_1_65_10,
        YouTubeClient.ANDROID_VR_1_43_32
      )

      for (client in clients) {
        try {
          val response = YouTube.player(videoId = targetVideoId, client = client).getOrNull()
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
            Timber.tag(TAG).d("Found video stream via InnerTube (${client.clientName}) for $targetVideoId: $videoUrl")
            break
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "InnerTube client ${client.clientName} failed for targetVideoId: $targetVideoId")
        }
      }

      // 3. Fall back to NewPipeExtractor if direct clients didn't yield a stream
      if (videoUrl.isNullOrBlank()) {
        try {
          val newPipeResult = NewPipeExtractor.getVideoUrlAndDuration(targetVideoId)
          if (newPipeResult != null) {
            videoUrl = newPipeResult.first
            if (totalDurationSec <= 0L) {
              totalDurationSec = newPipeResult.second
            }
            Timber.tag(TAG).d("Found video stream via NewPipe for $targetVideoId: $videoUrl")
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "NewPipeExtractor failed for targetVideoId: $targetVideoId")
        }
      }

      // 4. If official video failed to resolve, try original videoId as fallback
      if (videoUrl.isNullOrBlank() && targetVideoId != videoId) {
        try {
          val newPipeResult = NewPipeExtractor.getVideoUrlAndDuration(videoId)
          if (newPipeResult != null) {
            videoUrl = newPipeResult.first
            if (totalDurationSec <= 0L) {
              totalDurationSec = newPipeResult.second
            }
            Timber.tag(TAG).d("Found fallback video stream via NewPipe for $videoId: $videoUrl")
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "Fallback NewPipeExtractor failed for videoId: $videoId")
        }
      }

      if (videoUrl.isNullOrBlank()) {
        Timber.tag(TAG).w("No video stream found for canvas targetVideoId: $targetVideoId")
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
      if (targetVideoId != videoId) {
        cache[targetVideoId] = artwork
      }
      artwork
    }
  }
}
