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
    return Download(request, state, 0L, 0L, 0L, 0, 0)
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
}
