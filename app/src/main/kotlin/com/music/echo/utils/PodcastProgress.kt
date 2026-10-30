package echo.music.iad1tya.utils

import android.content.Context
import androidx.core.content.edit
import echo.music.iad1tya.models.MediaMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** How far an episode was listened, plus what is needed to show it without loading it again. */
data class EpisodeProgress(
  val id: String,
  val positionMs: Long,
  val durationMs: Long,
  val finished: Boolean,
  val updatedAt: Long,
  val title: String,
  val show: String?,
  val showBrowseId: String?,
  val thumbnailUrl: String?,
) {
  /** Played fraction in 0..1, or null when the duration is unknown. */
  val fraction: Float?
    get() =
      when {
        finished -> 1f
        durationMs > 0 -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> null
      }

  val remainingMs: Long?
    get() = if (durationMs > 0) (durationMs - positionMs).coerceAtLeast(0) else null

  val isInProgress: Boolean
    get() = !finished && positionMs > 0
}

/**
 * Listening progress of podcast episodes: where each one was left (to resume it and draw its
 * progress bar) and which ones were played to the end.
 */
object PodcastProgress {
  private const val PREFS_NAME = "podcast_progress"
  /** Positions this early are not worth resuming. */
  private const val MIN_POSITION_MS = 30_000L
  /** Closer than this to the end counts as played. */
  private const val END_MARGIN_MS = 60_000L
  private const val MAX_ENTRIES = 500

  private val prefs by lazy {
    AppContextHolder.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  private val _entries by lazy {
    MutableStateFlow(
      prefs.all.mapNotNull { (id, value) -> (value as? String)?.let { parse(id, it) } }
        .associateBy { it.id }
    )
  }

  val entries: StateFlow<Map<String, EpisodeProgress>>
    get() = _entries.asStateFlow()

  fun get(id: String): EpisodeProgress? = _entries.value[id]

  /** Episodes started but not finished, most recently listened first. */
  fun inProgress(entries: Map<String, EpisodeProgress> = _entries.value): List<EpisodeProgress> =
    entries.values.filter { it.isInProgress }.sortedByDescending { it.updatedAt }

  /** Records the position of the playing episode [metadata]. */
  fun update(metadata: MediaMetadata, positionMs: Long, durationMs: Long) {
    val existing = get(metadata.id)
    if (positionMs < MIN_POSITION_MS && existing == null) return
    val duration = durationMs.takeIf { it > 0 } ?: existing?.durationMs ?: 0L
    val finished = duration > 0 && duration - positionMs < END_MARGIN_MS
    save(
      EpisodeProgress(
        id = metadata.id,
        positionMs = if (finished) 0L else positionMs,
        durationMs = duration,
        finished = finished || (existing?.finished == true && positionMs < MIN_POSITION_MS),
        updatedAt = System.currentTimeMillis(),
        title = metadata.title,
        show = metadata.artists.firstOrNull()?.name ?: existing?.show,
        showBrowseId = metadata.artists.firstOrNull()?.id ?: existing?.showBrowseId,
        thumbnailUrl = metadata.thumbnailUrl ?: existing?.thumbnailUrl,
      )
    )
  }

  /** Marks an episode that played to its end. */
  fun markFinished(id: String) {
    val existing = get(id) ?: return
    save(existing.copy(positionMs = 0L, finished = true, updatedAt = System.currentTimeMillis()))
  }

  private fun save(entry: EpisodeProgress) {
    _entries.value = _entries.value + (entry.id to entry)
    prefs.edit { putString(entry.id, entry.toJson()) }
    trim()
  }

  private fun trim() {
    val all = _entries.value
    if (all.size <= MAX_ENTRIES) return
    val oldest = all.values.sortedBy { it.updatedAt }.take(all.size - MAX_ENTRIES).map { it.id }
    _entries.value = all - oldest.toSet()
    prefs.edit { oldest.forEach { remove(it) } }
  }

  private fun EpisodeProgress.toJson(): String =
    JSONObject()
      .put("position", positionMs)
      .put("duration", durationMs)
      .put("finished", finished)
      .put("updatedAt", updatedAt)
      .put("title", title)
      .putOpt("show", show)
      .putOpt("showBrowseId", showBrowseId)
      .putOpt("thumbnail", thumbnailUrl)
      .toString()

  private fun parse(id: String, json: String): EpisodeProgress? =
    try {
      val o = JSONObject(json)
      EpisodeProgress(
        id = id,
        positionMs = o.optLong("position"),
        durationMs = o.optLong("duration"),
        finished = o.optBoolean("finished"),
        updatedAt = o.optLong("updatedAt"),
        title = o.optString("title"),
        show = o.optString("show").takeIf { it.isNotEmpty() },
        showBrowseId = o.optString("showBrowseId").takeIf { it.isNotEmpty() },
        thumbnailUrl = o.optString("thumbnail").takeIf { it.isNotEmpty() },
      )
    } catch (e: Exception) {
      null
    }
}
