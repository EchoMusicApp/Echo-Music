package echo.music.iad1tya.playback.queues

import com.music.innertube.models.SongItem

internal class RadioQueuePageFilter {
  private val seenSongIds = mutableSetOf<String>()

  fun reset() = seenSongIds.clear()

  fun filter(songs: List<SongItem>): List<SongItem> =
    songs.filter { seenSongIds.add(it.id) }
}
