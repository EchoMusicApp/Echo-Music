package echo.music.iad1tya.lyrics

import android.content.Context
import com.music.paxsenix.Paxsenix
import echo.music.iad1tya.constants.EnablePaxsenixKey
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import timber.log.Timber

object PaxSenixLyricsProvider : LyricsProvider {
  private const val LOG_TAG = "EchoLyrics"

  override val name = "Paxsenix"

  override fun isEnabled(context: Context): Boolean {
    val enabled = context.dataStore[EnablePaxsenixKey] ?: true
    if (enabled) {
      Paxsenix.init(context)
    }
    return enabled
  }

  override suspend fun getLyrics(
    id: String,
    title: String,
    artist: String,
    duration: Int,
    album: String?,
  ): Result<String> {
    Timber.tag(LOG_TAG)
      .d(
        "[PaxsenixProvider] getLyrics title='%s', artist='%s', duration=%d",
        title,
        artist,
        duration,
      )
    return try {
      Paxsenix.getLyrics(title, artist, duration, album)
    } catch (e: Exception) {
      Timber.tag(LOG_TAG)
        .w(
          e,
          "[PaxsenixProvider] Failed to fetch lyrics: %s. Handled gracefully for fallback.",
          e.message,
        )
      Result.failure(e)
    }
  }

  override suspend fun getAllLyrics(
    id: String,
    title: String,
    artist: String,
    duration: Int,
    album: String?,
    callback: (String) -> Unit,
  ) {
    Timber.tag(LOG_TAG).d("[PaxsenixProvider] getAllLyrics title='%s', artist='%s'", title, artist)
    try {
      Paxsenix.getAllLyrics(title, artist, duration, album, callback)
    } catch (e: Exception) {
      Timber.tag(LOG_TAG).w(e, "[PaxsenixProvider] getAllLyrics failed: %s", e.message)
    }
  }
}
