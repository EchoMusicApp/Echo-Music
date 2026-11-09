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
}
