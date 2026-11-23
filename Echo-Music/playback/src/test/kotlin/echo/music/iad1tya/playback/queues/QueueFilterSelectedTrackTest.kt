package echo.music.iad1tya.playback.queues

import androidx.media3.common.MediaItem
import echo.music.iad1tya.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueFilterSelectedTrackTest {
  private fun createItem(id: String, explicit: Boolean = false, isVideo: Boolean = false): MediaItem {
    val meta = MediaMetadata(
      id = id,
      title = "Title $id",
      artists = emptyList(),
      duration = 100,
      thumbnailUrl = null,
      explicit = explicit,
      musicVideoType = if (isVideo) "MUSIC_VIDEO_TYPE_OMV" else null
    )
    return MediaItem.Builder()
      .setMediaId(id)
      .setUri("https://music.youtube.com/watch?v=$id")
      .setTag(meta)
      .build()
  }

  @Test
  fun selectedExplicitTrackIsNotFilteredOutWhenFilteringExplicit() {
    val items = listOf(
      createItem("song1", explicit = false),
      createItem("song2", explicit = true),
      createItem("song3", explicit = false)
    )
    val status = Queue.Status(
      title = "Test",
      items = items,
      mediaItemIndex = 1 // selected song2 (explicit)
    )

    val filtered = status.filterExplicit(true)

    // song2 should be kept because it is the currently selected track,
    // and mediaItemIndex must still point to song2!
    val currentSong = filtered.items[filtered.mediaItemIndex]
    assertEquals("song2", currentSong.mediaId)
  }

  @Test
  fun selectedVideoTrackIsNotFilteredOutWhenFilteringVideoSongs() {
    val items = listOf(
      createItem("song1", isVideo = false),
      createItem("song2", isVideo = true),
      createItem("song3", isVideo = false)
    )
    val status = Queue.Status(
      title = "Test",
      items = items,
      mediaItemIndex = 1 // selected song2 (video)
    )

    val filtered = status.filterVideoSongs(true)

    val currentSong = filtered.items[filtered.mediaItemIndex]
    assertEquals("song2", currentSong.mediaId)
  }
}
