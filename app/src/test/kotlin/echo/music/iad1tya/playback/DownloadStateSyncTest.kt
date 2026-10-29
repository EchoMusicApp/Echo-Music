package echo.music.iad1tya.playback

import android.net.Uri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DownloadStateSyncTest {

  private fun createDownload(id: String, state: Int): Download {
    val request = DownloadRequest.Builder(id, Uri.parse("https://example.com/$id")).build()
    val failureReason =
      if (state == Download.STATE_FAILED) Download.FAILURE_REASON_UNKNOWN
      else Download.FAILURE_REASON_NONE
    return Download(request, state, 0L, 0L, 0L, 0, failureReason)
  }

  @Test
  fun allSongsCompletedYieldsCompletedState() {
    val songs = listOf("s1", "s2", "s3")
    val downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_COMPLETED),
      "s2" to createDownload("s2", Download.STATE_COMPLETED),
      "s3" to createDownload("s3", Download.STATE_COMPLETED),
    )
    assertEquals(Download.STATE_COMPLETED, DownloadUtil.computeDownloadState(songs, downloads))
  }

  @Test
  fun partiallyCompletedWithActiveDownloadYieldsDownloadingState() {
    val songs = listOf("s1", "s2", "s3")
    val downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_COMPLETED),
      "s2" to createDownload("s2", Download.STATE_DOWNLOADING),
    )
    assertEquals(Download.STATE_DOWNLOADING, DownloadUtil.computeDownloadState(songs, downloads))
  }

  @Test
  fun queuedSongYieldsDownloadingState() {
    val songs = listOf("s1", "s2")
    val downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_QUEUED),
    )
    assertEquals(Download.STATE_DOWNLOADING, DownloadUtil.computeDownloadState(songs, downloads))
  }

  @Test
  fun idleOrStoppedSongsYieldStoppedState() {
    val songs = listOf("s1", "s2")
    val downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_STOPPED),
      "s2" to createDownload("s2", Download.STATE_STOPPED),
    )
    assertEquals(Download.STATE_STOPPED, DownloadUtil.computeDownloadState(songs, downloads))
  }

  @Test
  fun emptySongListYieldsStoppedState() {
    val songs = emptyList<String>()
    val downloads = emptyMap<String, Download>()
    assertEquals(Download.STATE_STOPPED, DownloadUtil.computeDownloadState(songs, downloads))
  }

  @Test
  fun nullSongListYieldsStoppedState() {
    val downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_COMPLETED)
    )
    assertEquals(Download.STATE_STOPPED, DownloadUtil.computeDownloadState(null, downloads))
  }

  @Test
  fun stateTransitionsAcrossCollectionDownloadLifecycle() {
    val songs = listOf("s1", "s2")
    var downloads = emptyMap<String, Download>()

    // Initially nothing in downloads -> STOPPED
    assertEquals(Download.STATE_STOPPED, DownloadUtil.computeDownloadState(songs, downloads))

    // s1 gets queued -> DOWNLOADING
    downloads = mapOf("s1" to createDownload("s1", Download.STATE_QUEUED))
    assertEquals(Download.STATE_DOWNLOADING, DownloadUtil.computeDownloadState(songs, downloads))

    // s1 downloading, s2 queued -> DOWNLOADING
    downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_DOWNLOADING),
      "s2" to createDownload("s2", Download.STATE_QUEUED)
    )
    assertEquals(Download.STATE_DOWNLOADING, DownloadUtil.computeDownloadState(songs, downloads))

    // s1 completed, s2 downloading -> DOWNLOADING (crucial: avoids flapping back to STOPPED)
    downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_COMPLETED),
      "s2" to createDownload("s2", Download.STATE_DOWNLOADING)
    )
    assertEquals(Download.STATE_DOWNLOADING, DownloadUtil.computeDownloadState(songs, downloads))

    // s1 completed, s2 completed -> COMPLETED
    downloads = mapOf(
      "s1" to createDownload("s1", Download.STATE_COMPLETED),
      "s2" to createDownload("s2", Download.STATE_COMPLETED)
    )
    assertEquals(Download.STATE_COMPLETED, DownloadUtil.computeDownloadState(songs, downloads))

    // When collection becomes empty -> STOPPED
    assertEquals(Download.STATE_STOPPED, DownloadUtil.computeDownloadState(emptyList(), downloads))
  }

  @Test
  fun hasDownloadCorrectlyIdentifiesActiveAndCompletedSongs() {
    val downloads = mapOf(
      "completed" to createDownload("completed", Download.STATE_COMPLETED),
      "queued" to createDownload("queued", Download.STATE_QUEUED),
      "downloading" to createDownload("downloading", Download.STATE_DOWNLOADING),
      "stopped" to createDownload("stopped", Download.STATE_STOPPED),
      "failed" to createDownload("failed", Download.STATE_FAILED),
    )

    org.junit.Assert.assertTrue(DownloadUtil.hasDownload("completed", downloads))
    org.junit.Assert.assertTrue(DownloadUtil.hasDownload("queued", downloads))
    org.junit.Assert.assertTrue(DownloadUtil.hasDownload("downloading", downloads))
    org.junit.Assert.assertFalse(DownloadUtil.hasDownload("stopped", downloads))
    org.junit.Assert.assertFalse(DownloadUtil.hasDownload("failed", downloads))
    org.junit.Assert.assertFalse(DownloadUtil.hasDownload("missing", downloads))
  }
}
