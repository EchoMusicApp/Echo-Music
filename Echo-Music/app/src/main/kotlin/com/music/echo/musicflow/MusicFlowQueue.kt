package echo.music.iad1tya.musicflow

import androidx.media3.common.MediaItem
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.queues.Queue

/**
 * Feeds the shared MusicService queue from a [MusicFlowSession]. The service's own load-more
 * pipeline calls [nextPage] near the end of the queue, so background playback keeps extending the
 * same feed the pager shows.
 */
class MusicFlowQueue(
  private val title: String?,
  private val items: List<MusicFlowItem>,
  private val startIndex: Int,
  private val session: MusicFlowSession,
) : Queue {
  private val generation = session.generation

  override val preloadItem: MediaMetadata? = null

  override suspend fun getInitialStatus() =
    Queue.Status(title, items.map { it.song.toMediaItem() }, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))

  override fun hasNextPage(): Boolean = session.generation == generation && session.state.value.hasMore

  override suspend fun nextPage(): List<MediaItem> {
    if (session.generation != generation) return emptyList()
    return session.loadMore().map { it.song.toMediaItem() }
  }

  companion object {
    const val BROWSE_SKIP_THRESHOLD_MS = 10_000L
  }
}
