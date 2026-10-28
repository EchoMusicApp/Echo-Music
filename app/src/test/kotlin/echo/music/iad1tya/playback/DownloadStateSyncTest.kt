package echo.music.iad1tya.playback

import androidx.media3.exoplayer.offline.Download
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStateSyncTest {

  /**
   * Helper function reflecting the UI download state resolution logic across
   * OnlinePlaylistScreen, AlbumScreen, BottomPlaylistScreen, AutoPlaylistScreen,
   * TopPlaylistScreen, and LocalPlaylistScreen.
   */
  private fun computeCollectionDownloadState(
    songIds: List<String>,
    downloads: Map<String, Int>,
  ): Int {
    return if (songIds.isNotEmpty() && songIds.all { downloads[it] == Download.STATE_COMPLETED }) {
      Download.STATE_COMPLETED
    } else if (
      songIds.any {
        downloads[it] == Download.STATE_QUEUED ||
          downloads[it] == Download.STATE_DOWNLOADING
      }
    ) {
      Download.STATE_DOWNLOADING
    } else {
      Download.STATE_STOPPED
    }
  }

  @Test
  fun allSongsCompletedYieldsCompletedState() {
    val songs = listOf("s1", "s2", "s3")
    val downloads = mapOf(
      "s1" to Download.STATE_COMPLETED,
      "s2" to Download.STATE_COMPLETED,
      "s3" to Download.STATE_COMPLETED,
    )
    assertEquals(Download.STATE_COMPLETED, computeCollectionDownloadState(songs, downloads))
  }

  @Test
  fun partiallyCompletedWithActiveDownloadYieldsDownloadingState() {
    // Previously songs.all { QUEUED || DOWNLOADING || COMPLETED } failed
    // as soon as s1 finished and s3 was not yet queued, falsely resetting the UI to STATE_STOPPED
    // and causing an infinite download retry loop.
    val songs = listOf("s1", "s2", "s3")
    val downloads = mapOf(
      "s1" to Download.STATE_COMPLETED,
      "s2" to Download.STATE_DOWNLOADING,
    )
    assertEquals(Download.STATE_DOWNLOADING, computeCollectionDownloadState(songs, downloads))
  }

  @Test
  fun queuedSongYieldsDownloadingState() {
    val songs = listOf("s1", "s2")
    val downloads = mapOf(
      "s1" to Download.STATE_QUEUED,
    )
    assertEquals(Download.STATE_DOWNLOADING, computeCollectionDownloadState(songs, downloads))
  }

  @Test
  fun idleOrStoppedSongsYieldStoppedState() {
    val songs = listOf("s1", "s2")
    val downloads = mapOf(
      "s1" to Download.STATE_STOPPED,
      "s2" to Download.STATE_STOPPED,
    )
    assertEquals(Download.STATE_STOPPED, computeCollectionDownloadState(songs, downloads))
  }

  @Test
  fun emptySongListYieldsStoppedState() {
    val songs = emptyList<String>()
    val downloads = emptyMap<String, Int>()
    assertEquals(Download.STATE_STOPPED, computeCollectionDownloadState(songs, downloads))
  }
}
