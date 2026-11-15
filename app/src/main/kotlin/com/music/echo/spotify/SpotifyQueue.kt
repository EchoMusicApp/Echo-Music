package com.music.echo.spotify

import androidx.media3.common.MediaItem
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.queues.Queue
import echo.music.iad1tya.spotify.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * Queue for Spotify playlist and collection playback.
 *
 * Resolves Spotify tracks into playable YouTube MediaItems while preserving
 * the playlist's exact track ordering, duplicate tracks, and starting position.
 */
class SpotifyQueue(
  private val title: String? = null,
  private val tracks: List<SpotifyTrack>,
  private val startIndex: Int = 0,
  override val preloadItem: MediaMetadata? = null,
  private val resolver: SpotifyPlaybackResolver,
) : Queue {

  private var pendingTracks: List<SpotifyTrack> = emptyList()

  override suspend fun getInitialStatus(): Queue.Status {
    return withContext(IO) {
      if (tracks.isEmpty()) {
        return@withContext Queue.Status(title = title, items = emptyList(), mediaItemIndex = 0)
      }

      val safeStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
      val from = (safeStartIndex - WINDOW_BEFORE).coerceAtLeast(0)
      val to = (safeStartIndex + WINDOW_AFTER).coerceAtMost(tracks.size)

      val initialWindow = tracks.subList(from, to)
      pendingTracks = tracks.subList(to, tracks.size)

      // Resolve initial window tracks concurrently
      val resolvedMap = resolver.resolveTracks(initialWindow)

      val resolvedItems = mutableListOf<MediaItem>()
      var targetMediaItemIndex = 0

      for ((indexInWindow, track) in initialWindow.withIndex()) {
        val songItem = resolvedMap[track.id]
        if (songItem != null) {
          val absoluteIndex = from + indexInWindow
          if (absoluteIndex < safeStartIndex) {
            targetMediaItemIndex++
          }
          resolvedItems.add(songItem.toMediaItem())
        }
      }

      val safeIndex = if (resolvedItems.isNotEmpty()) {
        targetMediaItemIndex.coerceIn(0, resolvedItems.lastIndex)
      } else {
        0
      }

      Queue.Status(
        title = title,
        items = resolvedItems,
        mediaItemIndex = safeIndex,
      )
    }
  }

  override fun hasNextPage(): Boolean = pendingTracks.isNotEmpty()

  override suspend fun nextPage(): List<MediaItem> {
    if (pendingTracks.isEmpty()) return emptyList()
    return withContext(IO) {
      val chunk = pendingTracks.take(PAGE_SIZE)
      pendingTracks = pendingTracks.drop(PAGE_SIZE)

      val resolvedMap = resolver.resolveTracks(chunk)
      chunk.mapNotNull { track -> resolvedMap[track.id]?.toMediaItem() }
    }
  }

  companion object {
    const val WINDOW_BEFORE = 50
    const val WINDOW_AFTER = 50
    const val PAGE_SIZE = 30
  }
}

