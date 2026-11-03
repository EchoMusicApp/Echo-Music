package echo.music.iad1tya.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.PodcastPage
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

class YouTubeQueue(
  private var endpoint: WatchEndpoint,
  override val preloadItem: MediaMetadata? = null,
) : Queue {
  private var continuation: String? = null
  private var retryCount = 0
  private val maxRetries = 3
  private val isRadioQueue = endpoint.playlistId?.startsWith("RD") != false
  private val pageFilter = RadioQueuePageFilter()
  /** Started from an episode: YouTube's radio mixes in music, which must not follow it. */
  private var isEpisodeRadio = false

  override suspend fun getInitialStatus(): Queue.Status {
    return withContext(IO) {
      continuation = null
      pageFilter.reset()
      var lastException: Throwable? = null
      val isAddon = endpoint.videoId?.startsWith("addon_") == true
      val originalAddonItem = if (isAddon) preloadItem else null

      if (isAddon) {
          try {
              val query = "${preloadItem?.title ?: ""} ${preloadItem?.artists?.firstOrNull()?.name ?: ""}".trim()
              val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
              val ytTrack = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull()
              if (ytTrack != null) {
                  endpoint = WatchEndpoint(videoId = ytTrack.id, playlistId = "RDAMVM${ytTrack.id}")
              }
          } catch (e: Exception) {
              if (e is CancellationException) throw e
              e.printStackTrace()
          }
      }

      for (attempt in 0..maxRetries) {
        try {
          val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
          endpoint = nextResult.endpoint
          continuation = nextResult.continuation
          retryCount = 0
          
          val currentItem = nextResult.items.getOrNull(nextResult.currentIndex ?: 0)
          val currentSongId = currentItem?.id
          isEpisodeRadio = currentItem?.isPodcastEpisode == true
          // Its show is known: the service queues the show's next episode instead of a radio.
          if (isEpisodeRadio && preloadItem?.artists?.any { PodcastPage.isShowBrowseId(it.id) } == true) {
            continuation = null
            return@withContext Queue.Status(
              title = nextResult.title,
              items = listOf(preloadItem.toMediaItem()),
              mediaItemIndex = 0,
            )
          }
          val filtered = if (isRadioQueue) pageFilter.filter(nextResult.items) else nextResult.items
          val songs = if (isEpisodeRadio) filtered.filter { it.isPodcastEpisode } else filtered
          val itemsList = songs.map { it.toMediaItem() }.toMutableList()
          if (isAddon && originalAddonItem != null) {
              if (itemsList.isNotEmpty()) {
                  itemsList[0] = originalAddonItem.toMediaItem()
              } else {
                  itemsList.add(originalAddonItem.toMediaItem())
              }
          }
          
          return@withContext Queue.Status(
            title = nextResult.title,
            items = itemsList,
            mediaItemIndex = if (isAddon) 0 else
              songs.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0),
          )
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          lastException = e

          if (attempt == 0 && endpoint.videoId != null && endpoint.playlistId == null) {
            endpoint =
              WatchEndpoint(videoId = endpoint.videoId, playlistId = "RDAMVM${endpoint.videoId}")
          }
        }
      }
      
      if (isAddon && originalAddonItem != null) {
          return@withContext Queue.Status(
              title = originalAddonItem.title,
              items = listOf(originalAddonItem.toMediaItem()),
              mediaItemIndex = 0
          )
      }
      
      throw lastException ?: Exception("Failed to get initial status")
    }
  }

  override fun hasNextPage(): Boolean = continuation != null

  override suspend fun nextPage(): List<MediaItem> {
    return withContext(IO) {
      val visitedContinuations = mutableSetOf<String>()

      repeat(5) {
        val pageContinuation = continuation ?: return@withContext emptyList()
        if (!visitedContinuations.add(pageContinuation)) {
          continuation = null
          return@withContext emptyList()
        }
        for (attempt in 0..maxRetries) {
          try {
            val nextResult = YouTube.next(endpoint, pageContinuation).getOrThrow()
            endpoint = nextResult.endpoint
            continuation = nextResult.continuation?.takeUnless { it == pageContinuation }
            retryCount = 0
            val filtered = if (isRadioQueue) pageFilter.filter(nextResult.items) else nextResult.items
            val songs = if (isEpisodeRadio) filtered.filter { it.isPodcastEpisode } else filtered
            if (songs.isNotEmpty()) return@withContext songs.map { it.toMediaItem() }
            break
          } catch (e: Exception) {
            if (e is CancellationException) throw e
            retryCount++
            if (attempt == maxRetries) {
              throw e
            }
          }
        }
      }
      emptyList()
    }
  }

  companion object {

    fun radio(song: MediaMetadata): YouTubeQueue {
      return YouTubeQueue(WatchEndpoint(videoId = song.id), song)
    }
  }
}
