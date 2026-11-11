package echo.music.iad1tya.utils

import android.content.Context
import androidx.core.content.edit
import com.music.innertube.models.PODCAST_BROWSE_PREFIX
import com.music.innertube.models.SongItem
import com.music.innertube.pages.HomePage
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_PODCAST_EPISODE
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
  /** Removed by the user from Continue listening; playing it again brings it back. */
  val dismissed: Boolean = false,
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

  fun asPlayed() = copy(positionMs = 0L, finished = true)

  /** Mislabelled music saved by older builds: short, and not linked to a show. */
  val isMislabelled: Boolean
    get() =
      durationMs in 1 until PodcastProgress.MIN_UNLINKED_EPISODE_MS &&
        showBrowseId?.startsWith(PODCAST_BROWSE_PREFIX) != true
}

/**
 * Listening progress of podcast episodes: where each one was left (to resume it and draw its
 * progress bar) and which ones were played to the end.
 */
object PodcastProgress {
  private const val PREFS_NAME = "podcast_progress"
  /** Positions this early are not worth resuming. */
  private const val MIN_POSITION_MS = 30_000L
  /** Closer than 5% of its length to the end (1 to 3 minutes) counts as played. */
  private const val END_MARGIN_MIN_MS = 60_000L
  private const val END_MARGIN_MAX_MS = 180_000L

  internal fun isNearEnd(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0 &&
      durationMs - positionMs < (durationMs / 20).coerceIn(END_MARGIN_MIN_MS, END_MARGIN_MAX_MS)
  private const val MAX_ENTRIES = 500
  /** Shorter items count as episodes only when they link to their show. */
  internal const val MIN_UNLINKED_EPISODE_MS = 10 * 60_000L
  /** YouTube's percentage from which an episode counts as played. */
  const val REMOTE_PLAYED_PERCENT = 95
  /** YouTube's percentage is coarse: only a clear lead over this device's position counts. */
  private const val REMOTE_AHEAD_MARGIN_MS = 60_000L

  private val prefs by lazy {
    AppContextHolder.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  private val _entries by lazy {
    val stored =
      prefs.all.mapNotNull { (id, value) -> (value as? String)?.let { parse(id, it) } }
    val (mislabelled, episodes) = stored.partition { it.isMislabelled }
    if (mislabelled.isNotEmpty()) prefs.edit { mislabelled.forEach { remove(it.id) } }
    // Left near the end by an older build, which counted a smaller margin as played.
    MutableStateFlow(
      episodes
        .map { if (it.isInProgress && isNearEnd(it.positionMs, it.durationMs)) it.asPlayed() else it }
        .associateBy { it.id }
    )
  }

  val entries: StateFlow<Map<String, EpisodeProgress>>
    get() = _entries.asStateFlow()

  fun get(id: String): EpisodeProgress? = _entries.value[id]

  /** Episodes started but not finished, most recently listened first. */
  fun inProgress(entries: Map<String, EpisodeProgress> = _entries.value): List<EpisodeProgress> =
    entries.values.filter { it.isInProgress && !it.dismissed }.sortedByDescending { it.updatedAt }

  /**
   * Whether [metadata] is a podcast episode. YouTube also labels some music uploads as episodes,
   * so the label alone is not enough: the item must link to its show or be episode-length.
   */
  fun isEpisode(metadata: MediaMetadata, durationMs: Long): Boolean =
    metadata.musicVideoType == MUSIC_VIDEO_TYPE_PODCAST_EPISODE &&
      (metadata.artists.any { it.id?.startsWith(PODCAST_BROWSE_PREFIX) == true } ||
        durationMs >= MIN_UNLINKED_EPISODE_MS)

  /** Takes an episode out of Continue listening; its progress stays. */
  fun dismiss(id: String) {
    val existing = get(id) ?: return
    save(existing.copy(dismissed = true))
  }

  /** Called when an episode starts playing again. */
  fun restore(id: String) {
    val existing = get(id) ?: return
    if (existing.dismissed) save(existing.copy(dismissed = false))
  }

  const val SKIP_BACK_MS = 15_000L
  const val SKIP_FORWARD_MS = 30_000L
  private val SPEEDS = listOf(1f, 1.25f, 1.5f, 1.75f, 2f, 0.75f)
  private const val SPEED_KEY = "speed"

  private val settings by lazy {
    AppContextHolder.appContext.getSharedPreferences("podcast_settings", Context.MODE_PRIVATE)
  }

  private val _speed by lazy { MutableStateFlow(settings.getFloat(SPEED_KEY, 1f)) }

  /** The speed of every podcast episode, kept apart from the one used for music. */
  val speed: StateFlow<Float>
    get() = _speed.asStateFlow()

  /** Moves to the next speed, wrapping around. */
  fun cycleSpeed() {
    val next = SPEEDS[(SPEEDS.indexOf(_speed.value) + 1) % SPEEDS.size]
    _speed.value = next
    settings.edit { putFloat(SPEED_KEY, next) }
  }

  fun speedLabel(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')) + "x"

  /**
   * Takes in what YouTube reports for the account, so episodes listened elsewhere resume from
   * there here too. The furthest position wins. Entries from YouTube sort behind what was
   * listened on this device, as their time is unknown.
   */
  fun mergeRemote(episode: SongItem): Boolean {
    val percent = episode.playedPercent?.takeIf { it > 0 } ?: return false
    val finished = percent >= REMOTE_PLAYED_PERCENT
    val durationMs =
      episode.duration?.times(1000L)?.takeIf { it > 0 } ?: if (finished) 0L else return false
    val positionMs = if (finished) 0L else durationMs * percent / 100
    val existing = get(episode.id)
    when {
      existing == null ->
        save(
          EpisodeProgress(
            id = episode.id,
            positionMs = positionMs,
            durationMs = durationMs,
            finished = finished,
            updatedAt = 0L,
            title = episode.title,
            show = episode.artists.firstOrNull()?.name,
            showBrowseId = episode.artists.firstOrNull()?.id,
            thumbnailUrl = episode.thumbnail,
          )
        )
      existing.finished -> Unit
      finished || positionMs > existing.positionMs + REMOTE_AHEAD_MARGIN_MS ->
        save(existing.copy(positionMs = positionMs, finished = finished))
    }
    return true
  }

  /**
   * Takes YouTube's own "continue listening" shelf out of [sections], merging its episodes in, so
   * only one such list shows. It is told by its title, or by every episode being in progress.
   */
  fun withoutContinueShelf(sections: List<HomePage.Section>, title: String): List<HomePage.Section> =
    sections.filterNot { section ->
      val episodes = section.items.filterIsInstance<SongItem>().filter { it.isPodcastEpisode }
      val isContinueShelf =
        episodes.isNotEmpty() &&
          (section.title.equals(title, ignoreCase = true) ||
            (episodes.size == section.items.size &&
              episodes.all { (it.playedPercent ?: 0) in 1 until REMOTE_PLAYED_PERCENT }))
      // Removed only when every episode made it into the app's list, so none disappears.
      isContinueShelf && episodes.map(::mergeRemote).all { it }
    }

  /** Records the position of the playing episode [metadata]. */
  fun update(metadata: MediaMetadata, positionMs: Long, durationMs: Long) {
    val existing = get(metadata.id)
    if (positionMs < MIN_POSITION_MS && existing == null) return
    val duration = durationMs.takeIf { it > 0 } ?: existing?.durationMs ?: 0L
    val finished = isNearEnd(positionMs, duration)
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
        dismissed = existing?.dismissed == true,
      )
    )
  }

  /** Marks an episode that played to its end. */
  fun markFinished(id: String) {
    val existing = get(id) ?: return
    save(existing.copy(positionMs = 0L, finished = true, updatedAt = System.currentTimeMillis()))
  }

  @Synchronized
  private fun save(entry: EpisodeProgress) {
    _entries.value = _entries.value + (entry.id to entry)
    prefs.edit { putString(entry.id, entry.toJson()) }
    trim()
  }

  @Synchronized
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
      .put("dismissed", dismissed)
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
        dismissed = o.optBoolean("dismissed"),
      )
    } catch (e: Exception) {
      null
    }
}
