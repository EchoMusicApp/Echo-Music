package echo.music.iad1tya.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Remembers where podcast episodes were left, so they continue from there when played again.
 *
 * Entries are kept per video id as "position:savedAt"; finished episodes (and the oldest ones
 * beyond [MAX_ENTRIES]) are dropped.
 */
class PodcastResumeStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /** The saved position for [id], or 0 when there is none. */
  fun position(id: String): Long =
    prefs.getString(id, null)?.substringBefore(':')?.toLongOrNull() ?: 0L

  fun update(id: String, positionMs: Long, durationMs: Long) {
    if (positionMs < MIN_POSITION_MS) return
    if (durationMs > 0 && durationMs - positionMs < END_MARGIN_MS) {
      clear(id)
      return
    }
    prefs.edit { putString(id, "$positionMs:${System.currentTimeMillis()}") }
    trim()
  }

  fun clear(id: String) {
    if (prefs.contains(id)) prefs.edit { remove(id) }
  }

  private fun trim() {
    val entries = prefs.all
    if (entries.size <= MAX_ENTRIES) return
    val oldest =
      entries.entries
        .sortedBy { (it.value as? String)?.substringAfter(':')?.toLongOrNull() ?: 0L }
        .take(entries.size - MAX_ENTRIES)
    prefs.edit { oldest.forEach { remove(it.key) } }
  }

  private companion object {
    const val PREFS_NAME = "podcast_resume"
    /** Positions this early are not worth resuming. */
    const val MIN_POSITION_MS = 30_000L
    /** Closer than this to the end counts as finished. */
    const val END_MARGIN_MS = 60_000L
    const val MAX_ENTRIES = 200
  }
}
