package echo.music.iad1tya.utils

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Playlist ids known to be podcast shows.
 *
 * Saved podcasts are stored as ordinary playlists, so the library tells them apart by this set,
 * which is filled whenever a show page is opened.
 */
@Singleton
class PodcastShowStore @Inject constructor(@ApplicationContext context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _ids = MutableStateFlow(prefs.getStringSet(KEY_IDS, null).orEmpty().toSet())
  val ids: StateFlow<Set<String>> = _ids.asStateFlow()

  fun add(id: String) {
    if (id in _ids.value) return
    val updated = _ids.value + id
    _ids.value = updated
    prefs.edit { putStringSet(KEY_IDS, updated) }
  }

  private companion object {
    const val PREFS_NAME = "podcast_shows"
    const val KEY_IDS = "ids"
  }
}
