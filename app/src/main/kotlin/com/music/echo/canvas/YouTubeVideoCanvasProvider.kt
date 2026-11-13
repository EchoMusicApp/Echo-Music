package echo.music.iad1tya.canvas

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
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
    val cleanTitle =
      title
        .replace(Regex("""\s*[\(\[].*?[\)\]]\s*"""), " ")
        .replace(Regex("""\s*[-|/].*"""), " ")
        .trim()
    val cleanArtist =
      artist
        .split(',', '&', 'x', 'X', '/', ';')
        .firstOrNull()
        ?.replace(Regex("""\s*[\(\[].*?[\)\]]\s*"""), " ")
        ?.trim() ?: artist.trim()

    val titleKeywords =
      cleanTitle
        .lowercase(Locale.ROOT)
        .split(' ')
        .map { it.trim().filter { c -> c.isLetterOrDigit() } }
        .filter { it.length > 2 }

    val artistKeywords =
      cleanArtist
        .lowercase(Locale.ROOT)
        .split(' ')
        .map { it.trim().filter { c -> c.isLetterOrDigit() } }
        .filter { it.length > 2 }

    val negativeKeywords =
      listOf(
        "mashup",
        "mash up",
        "mash-up",
        "remix",
        "dj",
        "chillout",
        "slowed",
        "reverb",
        "speed up",
        "sped up",
        "nightcore",
        "bass boosted",
        "lofi",
        "lo-fi",
        "8d audio",
        "8d",
        "status",
        "short",
        "shorts",
        "tiktok",
        "reels",
        "cover",
        "reaction",
        "review",
        "bts",
        "behind the scenes",
        "making of",
        "teaser",
        "trailer",
        "promo",
        "karaoke",
        "instrumental",
        "lyrics",
        "lyric video",
        "lyrics video",
        "full audio",
        "audio only",
        "jukebox",
        "live performance",
        "concert",
        "unplugged",
        "acoustic",
        "dance cover",
        "choreography",
        "fan edit",
        "edit",
        "whatsapp status",
        "scene",
        "scenes",
        "dialogue",
        "movie scene",
        "episode",
      )

    val queries =
      listOf(
        "\"$cleanTitle\" $cleanArtist official music video",
        "\"$cleanTitle\" $cleanArtist official video",
        "$cleanTitle $cleanArtist official video",
        "$cleanTitle $cleanArtist",
      )

    for (query in queries) {
      try {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
        val items = result?.items.orEmpty()

        val scored = items.mapNotNull { item ->
          val itemTitle = item.title.lowercase(Locale.ROOT)
          val itemArtists =
            (item as? SongItem)?.artists.orEmpty().map { it.name.lowercase(Locale.ROOT) }

          // 1. MUST NOT contain negative keywords (unless song title explicitly has them)
          val hasNegative = negativeKeywords.any { neg ->
            itemTitle.contains(neg) && !cleanTitle.lowercase(Locale.ROOT).contains(neg)
          }
          if (hasNegative) return@mapNotNull null

          // 2. Strict Title Matching:
          val lowerCleanTitle = cleanTitle.lowercase(Locale.ROOT)
          val hasExactTitle = itemTitle.contains(lowerCleanTitle)
          val matchedTitleKeywordsCount = titleKeywords.count { kw -> itemTitle.contains(kw) }

          // All keywords must match if <= 2 keywords; at least 75% if more
          val titleMatches =
            when {
              hasExactTitle -> true
              titleKeywords.isEmpty() -> true
              titleKeywords.size <= 2 -> matchedTitleKeywordsCount == titleKeywords.size
              else -> matchedTitleKeywordsCount.toFloat() / titleKeywords.size >= 0.75f
            }
          if (!titleMatches) return@mapNotNull null

          // 3. Artist Matching:
          val matchesArtist =
            artistKeywords.isEmpty() ||
              artistKeywords.any { kw -> itemTitle.contains(kw) } ||
              itemArtists.any { a -> artistKeywords.any { kw -> a.contains(kw) } }

          var score = 0

          if (hasExactTitle) {
            score += 50
          } else {
            score += matchedTitleKeywordsCount * 15
          }

          if (matchesArtist) {
            score += 40
          }

          // Bonus for official music video markers
          if (
            itemTitle.contains("official music video") ||
              itemTitle.contains("official video") ||
              itemTitle.contains("official mv")
          ) {
            score += 40
          } else if (itemTitle.contains("music video") || itemTitle.contains("mv")) {
            score += 20
          }

          // Bonus if SongItem is a verified YouTube Music video
          if (item is SongItem && item.isVideoSong) {
            score += 35
          }

          // Strict threshold: must score at least 80 to avoid false positives
          if (score >= 80 && item.id.isNotBlank()) {
            Pair(item, score)
          } else {
            null
          }
        }

        val best = scored.maxByOrNull { it.second }?.first
        if (best != null) {
          Timber.tag(TAG).d("Found verified official video: '${best.title}' with id=${best.id}")
          return best.id
        }
      } catch (e: Exception) {
        Timber.tag(TAG).w(e, "Error searching for official video query: $query")
      }
    }
    return null
  }

  suspend fun getCanvasForVideo(
    videoId: String,
    title: String? = null,
    artist: String? = null,
    durationSec: Long? = null,
  ): CanvasArtwork? {
    if (videoId.isBlank() || videoId.length < 5) return null
    cache[videoId]?.let {
      return it
    }

    return withContext(Dispatchers.IO) {
      val officialVideoId =
        if (!title.isNullOrBlank()) {
          findOfficialVideoId(title, artist.orEmpty())
        } else null

      val targetVideoId = officialVideoId ?: videoId
      Timber.tag(TAG)
        .d("Resolving canvas stream for targetVideoId: $targetVideoId (original: $videoId)")

      var videoUrl: String? = null
      var totalDurationSec: Long =
        if (targetVideoId == videoId) (durationSec?.takeIf { it > 0 } ?: 0L) else 0L

      // Try InnerTube direct clients (VISIONOS first as it serves complete files)
      val clients =
        listOf(
          YouTubeClient.VISIONOS,
          YouTubeClient.TVHTML5,
          YouTubeClient.ANDROID_VR_1_65_10,
          YouTubeClient.ANDROID_VR_1_43_32,
        )

      for (client in clients) {
        try {
          val response = YouTube.player(videoId = targetVideoId, client = client).getOrNull()
          val streamingData = response?.streamingData ?: continue

          if (totalDurationSec <= 0L) {
            totalDurationSec = response.videoDetails?.lengthSeconds?.toLongOrNull() ?: 0L
          }

          val formats =
            (streamingData.formats.orEmpty() + streamingData.adaptiveFormats).filter { format ->
              val isVideo = format.width != null || format.mimeType.startsWith("video/")
              val hasUrl = !format.url.isNullOrBlank()
              isVideo && hasUrl
            }

          // Prioritize high quality: 1080p MP4, then 1080p, then 720p MP4, then 720p
          val candidate =
            formats
              .filter {
                it.mimeType.contains("mp4", ignoreCase = true) && (it.height ?: 0) in 1080..1440
              }
              .maxByOrNull { it.height ?: 0 }
              ?: formats.filter { (it.height ?: 0) in 1080..1440 }.maxByOrNull { it.height ?: 0 }
              ?: formats
                .filter {
                  it.mimeType.contains("mp4", ignoreCase = true) && (it.height ?: 0) in 720..1079
                }
                .maxByOrNull { it.height ?: 0 }
              ?: formats.filter { (it.height ?: 0) in 720..1079 }.maxByOrNull { it.height ?: 0 }
              ?: formats
                .filter { it.mimeType.contains("mp4", ignoreCase = true) }
                .maxByOrNull { it.height ?: 0 }
              ?: formats.maxByOrNull { it.height ?: 0 }

          if (candidate != null && !candidate.url.isNullOrBlank()) {
            videoUrl = candidate.url
            Timber.tag(TAG)
              .d(
                "Found high quality (${candidate.height}p) stream via InnerTube (${client.clientName}) for $targetVideoId: $videoUrl"
              )
            break
          }
        } catch (e: Exception) {
          Timber.tag(TAG)
            .w(e, "InnerTube client ${client.clientName} failed for targetVideoId: $targetVideoId")
        }
      }

      // Fall back to NewPipeExtractor if direct clients didn't yield a stream
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

      // If official video failed to resolve, try original videoId as fallback
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

      val artwork =
        CanvasArtwork(
          name = title,
          artist = artist,
          videoUrl = videoUrl,
          animated = videoUrl,
          clipStartMs = startMs,
          clipEndMs = endMs,
        )

      cache[videoId] = artwork
      if (targetVideoId != videoId) {
        cache[targetVideoId] = artwork
      }
      artwork
    }
  }
}
