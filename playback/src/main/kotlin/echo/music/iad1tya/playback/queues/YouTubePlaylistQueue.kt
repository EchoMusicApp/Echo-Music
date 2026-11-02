package echo.music.iad1tya.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubePlaylistQueue(
  private val playlistId: String,
  private val playlistTitle: String? = null,
  private val initialSongs: List<SongItem> = emptyList(),
  private val initialContinuation: String? = null,
  private val startIndex: Int = 0,
  override val preloadItem: MediaMetadata? = null,
) : Queue {
  private var continuation: String? = initialContinuation
  private var retryCount = 0
  private val maxRetries = 3

  /**
   * Songs the playlist screen had already loaded beyond the initial window.
   * They are handed out by [nextPage] before any new continuation is fetched.
   */
  private var pendingSongs: List<SongItem> = emptyList()

  override suspend fun getInitialStatus(): Queue.Status {
    return withContext(IO) {
      if (initialSongs.isNotEmpty()) {
        // The playlist screen keeps loading pages in the background, so for long
        // or endless playlists (personal mixes) it could pass thousands of songs.
        // Queue only a window around the chosen song; load-more appends the rest
        // in chunks as playback approaches the end.
        val from = (startIndex - WINDOW_BEFORE).coerceAtLeast(0)
        val to = (startIndex + WINDOW_AFTER).coerceAtMost(initialSongs.size)
        pendingSongs = initialSongs.subList(to, initialSongs.size)
        Queue.Status(
          title = playlistTitle,
          items = initialSongs.subList(from, to).map { it.toMediaItem() },
          mediaItemIndex = startIndex - from,
        )
      } else {
        val playlistPage = YouTube.playlist(playlistId).getOrThrow()
        continuation = playlistPage.songsContinuation
        Queue.Status(
          title = playlistPage.playlist.title,
          items = playlistPage.songs.map { it.toMediaItem() },
          mediaItemIndex = startIndex,
        )
      }
    }
  }

  override fun hasNextPage(): Boolean = pendingSongs.isNotEmpty() || continuation != null

  /**
   * Whether songs already loaded by the playlist screen are still to be queued.
   * They are part of the playlist the user started, so they are appended even
   * when automatic loading of further pages is turned off.
   */
  fun hasPendingSongs(): Boolean = pendingSongs.isNotEmpty()

  override suspend fun nextPage(): List<MediaItem> {
    if (pendingSongs.isNotEmpty()) {
      val chunk = pendingSongs.take(PAGE_SIZE)
      pendingSongs = pendingSongs.drop(PAGE_SIZE)
      return chunk.map { it.toMediaItem() }
    }
    return withContext(IO) {
      val currentContinuation = continuation ?: return@withContext emptyList()
      var lastException: Throwable? = null

      for (attempt in 0..maxRetries) {
        try {
          val continuationPage = YouTube.playlistContinuation(currentContinuation).getOrThrow()
          continuation = continuationPage.continuation
          retryCount = 0
          return@withContext continuationPage.songs.map { it.toMediaItem() }
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

  private companion object {
    /** Songs kept before the chosen one, so "previous" still works. */
    const val WINDOW_BEFORE = 20
    /** Songs queued after the chosen one up front. */
    const val WINDOW_AFTER = 100
    /** Songs appended per load-more step from the already loaded list. */
    const val PAGE_SIZE = 50
  }
}
