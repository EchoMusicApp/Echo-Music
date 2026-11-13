package echo.music.iad1tya.playback

import android.content.Context

/** Exporting songs to a folder is not ported yet; start() does nothing. */
class AudioExportService {
  companion object {
    fun cancelAll(context: Context) {}
    fun start(context: Context, songId: String, songTitle: String, songArtist: String, songAlbum: String, artworkUrl: String, targetDirectoryUri: String) {}
  }
}
