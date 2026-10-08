package echo.music.iad1tya.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
  private var endpoint: WatchEndpoint,
  override val preloadItem: MediaMetadata? = null,
) : Queue {
  private var continuation: String? = null
  private var retryCount = 0
  private val maxRetries = 3

  override suspend fun getInitialStatus(): Queue.Status {
    return withContext(IO) {
      var lastException: Throwable? = null
      val isAddon = endpoint.videoId?.startsWith("addon_") == true
      var originalAddonItem = if (isAddon) preloadItem else null

      if (isAddon) {
          try {
              val query = "${preloadItem?.title ?: ""} ${preloadItem?.artists?.firstOrNull()?.name ?: ""}".trim()
              val searchResult = YouTube.search(query, com.music.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
              val ytTrack = searchResult?.items?.firstOrNull { it is com.music.innertube.models.SongItem } as? com.music.innertube.models.SongItem
              if (ytTrack != null) {
                  endpoint = WatchEndpoint(videoId = ytTrack.id, playlistId = "RDAMVM${ytTrack.id}")
              }
          } catch (e: Exception) {
              e.printStackTrace()
          }
      }

      for (attempt in 0..maxRetries) {
        try {
          val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
          endpoint = nextResult.endpoint
          continuation = nextResult.continuation
          retryCount = 0
          
          val itemsList = nextResult.items.map { it.toMediaItem() }.toMutableList()
          if (isAddon && originalAddonItem != null) {
              if (itemsList.isNotEmpty()) {
                  // Replace the first item with the addon item so it plays the addon track, but gets YT recommendations!
                  itemsList[0] = originalAddonItem!!.toMediaItem()
              } else {
                  itemsList.add(originalAddonItem!!.toMediaItem())
              }
          }
          
          return@withContext Queue.Status(
            title = nextResult.title,
            items = itemsList,
            mediaItemIndex = nextResult.currentIndex ?: 0,
          )
        } catch (e: Exception) {
          lastException = e

          if (attempt == 0 && endpoint.videoId != null && endpoint.playlistId == null) {
            endpoint =
              WatchEndpoint(videoId = endpoint.videoId, playlistId = "RDAMVM${endpoint.videoId}")
          }
        }
      }
      
      if (isAddon && originalAddonItem != null) {
          return@withContext Queue.Status(
              title = originalAddonItem!!.title,
              items = listOf(originalAddonItem!!.toMediaItem()),
              mediaItemIndex = 0
          )
      }
      
      throw lastException ?: Exception("Failed to get initial status")
    }
  }

  override fun hasNextPage(): Boolean = continuation != null

  override suspend fun nextPage(): List<MediaItem> {
    return withContext(IO) {
      var lastException: Throwable? = null

      for (attempt in 0..maxRetries) {
        try {
          val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
          endpoint = nextResult.endpoint
          continuation = nextResult.continuation
          retryCount = 0
          return@withContext nextResult.items.map { it.toMediaItem() }
        } catch (e: Exception) {
          lastException = e
          retryCount++
          if (retryCount >= maxRetries) {
            continuation = null
          }
        }
      }
      throw lastException ?: Exception("Failed to get next page")
    }
  }

  companion object {

    fun radio(song: MediaMetadata): YouTubeQueue {
      return YouTubeQueue(WatchEndpoint(videoId = song.id), song)
    }
  }
}
