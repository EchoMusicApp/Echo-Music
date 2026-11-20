package echo.music.iad1tya.podcast

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class VideoTrackOption(
  val label: String, // e.g. "1080p", "720p", "480p", "360p"
  val height: Int,
  val videoUrl: String,
  val audioUrl: String? = null,
  val isMuxed: Boolean = true,
)

object PodcastVideoProvider {
  private const val TAG = "PodcastVideoProvider"

  /**
   * The music video of a song: a video of this song only, close to its length, and not a
   * trailer, a lyric video or another take of it. [durationSec] is the song's length, when known.
   */
  suspend fun findOfficialMusicVideoId(title: String, artist: String, durationSec: Int? = null): String? =
    withContext(Dispatchers.IO) {
      if (title.isBlank()) return@withContext null
      try {
        val cleanTitle = cleanSongTitle(title)
        val cleanArtist = cleanArtistName(artist)
        val queries =
          listOfNotNull(
              if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle,
              if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist official music video"
              else "$cleanTitle official video",
            )
            .distinct()

        for (query in queries) {
          val result = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
          // Only an official video of this song: uploads by anyone else were remixes, parodies
          // and lyric videos titled like the song, and the first official video of any result
          // was another song by the same artist when this one has no video of its own.
          val candidates =
            result
              ?.items
              ?.filterIsInstance<SongItem>()
              .orEmpty()
              .filter { item ->
                item.musicVideoType in OFFICIAL_VIDEO_TYPES &&
                  (item.title.contains(cleanTitle, ignoreCase = true) ||
                    cleanTitle.contains(item.title, ignoreCase = true)) &&
                  isSameTake(item, cleanTitle, durationSec)
              }
          val match =
            candidates.firstOrNull { it.musicVideoType == MUSIC_VIDEO_TYPE_OMV }
              ?: candidates.firstOrNull()
          if (match != null) {
            Timber.tag(TAG).d("Found the music video: ${match.id} - ${match.title}")
            return@withContext match.id
          }
        }
        null
      } catch (e: Exception) {
        Timber.tag(TAG).w(e, "Failed to find official music video for $title by $artist")
        null
      }
    }

  /** Words of a video that is not the song as released, unless the song's own title has them. */
  private val OTHER_TAKE_WORDS =
    listOf(
      "trailer", "teaser", "snippet", "preview", "anteprima", "lyric", "lyrics", "testo",
      "letra", "paroles", "mashup", "acoustic", "acustic", "live", "cover", "reaction",
      "karaoke", "instrumental", "slowed", "sped up", "remix", "rmx", "parodia", "parody",
      "behind the scenes", "audio", "visualizer", "visualiser", "visual",
    )

  /** Videos from the artist or the label: an official music video, or their own upload. */
  private val OFFICIAL_VIDEO_TYPES =
    setOf(MUSIC_VIDEO_TYPE_OMV, "MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC")

  /**
   * Whether [item] is the song itself: a 50-second trailer titled like the song was played as
   * its video. It must not be much shorter than the song, and its title must not name another take.
   */
  private fun isSameTake(item: SongItem, cleanTitle: String, durationSec: Int?): Boolean {
    val itemTitle = item.title.lowercase()
    val songTitle = cleanTitle.lowercase()
    fun names(text: String, word: String) = Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(text)
    if (OTHER_TAKE_WORDS.any { names(itemTitle, it) && !names(songTitle, it) }) return false
    val itemDuration = item.duration
    if (durationSec != null && durationSec > 0 && itemDuration != null && itemDuration > 0) {
      // Not much shorter (a trailer or a snippet), while a video can run longer than the song
      // with an intro or an ending of its own.
      if (itemDuration < durationSec * 3 / 4) return false
      if (itemDuration > durationSec + maxOf(120, durationSec / 2)) return false
    }
    return true
  }

  private fun cleanSongTitle(raw: String): String {
    return raw
      .replace(Regex("\\s*\\[[^]]*]"), "")
      .replace(
        Regex("\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)", RegexOption.IGNORE_CASE),
        "",
      )
      .replace(
        Regex(
          "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
          RegexOption.IGNORE_CASE,
        ),
        "",
      )
      .replace(
        Regex(
          "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
          RegexOption.IGNORE_CASE,
        ),
        "",
      )
      .trim('-', ' ', '(', ')')
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  private fun cleanArtistName(raw: String): String {
    return raw
      .split(
        Regex(
          "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
          RegexOption.IGNORE_CASE,
        ),
        limit = 2,
      )
      .firstOrNull()
      .orEmpty()
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  suspend fun resolveVideoTracks(videoId: String): List<VideoTrackOption> =
    withContext(Dispatchers.IO) {
      val results = mutableListOf<VideoTrackOption>()

      // 1. Try InnerTube direct player clients (VISIONOS first, then TVHTML5)
      val clients =
        listOf(
          YouTubeClient.VISIONOS,
          YouTubeClient.TVHTML5,
          YouTubeClient.ANDROID_VR_1_65_10,
        )

      for (client in clients) {
        try {
          val response = YouTube.player(videoId = videoId, client = client).getOrNull()
          val streamingData = response?.streamingData ?: continue

          val bestAudio =
            streamingData.adaptiveFormats
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
                  isMuxed = true,
                )
              )
            }
          }

          // Adaptive video formats (1080p, 720p, 480p, 360p, etc.)
          for (f in
            streamingData.adaptiveFormats.filter {
              (it.width != null || it.mimeType.startsWith("video/")) && !it.url.isNullOrBlank()
            }) {
            val url = f.url ?: continue
            val h = f.height ?: continue
            if (h >= 144) {
              results.add(
                VideoTrackOption(
                  label = "${h}p",
                  height = h,
                  videoUrl = url,
                  audioUrl = bestAudio,
                  isMuxed = false,
                )
              )
            }
          }

          if (results.isNotEmpty()) {
            Timber.tag(TAG)
              .d("Resolved ${results.size} video streams via InnerTube ${client.clientName}")
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
                isMuxed = t.isMuxed,
              )
            )
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "NewPipe failed to resolve video tracks for $videoId")
        }
      }

      // Deduplicate by resolution height and pick best stream
      val distinctTracks =
        results
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
