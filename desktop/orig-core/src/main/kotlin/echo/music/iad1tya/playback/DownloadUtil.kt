package echo.music.iad1tya.playback

import androidx.media3.exoplayer.offline.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

// shortcut: desktop has no download manager yet, so nothing is ever downloaded; the map is the hook for it.
class DownloadUtil {
  private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
  val downloads: StateFlow<Map<String, Download>> = _downloads
  fun getDownload(songId: String): Flow<Download?> = _downloads.map { it[songId] }

  companion object {
    fun computeDownloadState(songIds: Collection<String>?, downloads: Map<String, Download>): Int {
      if (songIds.isNullOrEmpty()) return Download.STATE_STOPPED
      return if (songIds.all { downloads[it]?.state == Download.STATE_COMPLETED }) Download.STATE_COMPLETED
      else if (songIds.any { downloads[it]?.state == Download.STATE_QUEUED || downloads[it]?.state == Download.STATE_DOWNLOADING }) Download.STATE_DOWNLOADING
      else Download.STATE_STOPPED
    }
    fun hasDownload(songId: String, downloads: Map<String, Download>): Boolean {
      val s = downloads[songId]?.state
      return s == Download.STATE_COMPLETED || s == Download.STATE_QUEUED || s == Download.STATE_DOWNLOADING
    }
  }
}
