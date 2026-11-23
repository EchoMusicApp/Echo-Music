package echo.music.iad1tya.musicflow

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class MusicFlowFeedState(
  val items: List<MusicFlowItem> = emptyList(),
  val selectedIndex: Int = 0,
  val isInitialLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val error: MusicFlowFeedError? = null,
  val hasMore: Boolean = true,
  val isOffline: Boolean = false,
)

/**
 * Process-scoped Music Flow feed. Pages are append-only so a page index, once shown, always maps
 * to the same song for the lifetime of the session; swiping back is therefore exact history.
 */
@Singleton
class MusicFlowSession @Inject constructor(private val source: MusicFlowFeedSource) {
  private val mutex = Mutex()
  private val _state = MutableStateFlow(MusicFlowFeedState())
  val state: StateFlow<MusicFlowFeedState> = _state.asStateFlow()

  private var cursor: MusicFlowCursor? = null
  private var signals = MusicFlowSignals(MusicFlowCandidateFilter(), emptySet())
  private val seenSongIds = mutableSetOf<String>()
  private val seenTrackKeys = mutableSetOf<String>()
  private val hiddenSongIds = mutableSetOf<String>()
  private val lessLikeArtists = mutableSetOf<String>()

  var rankingConfig = MusicFlowRankingConfig()

  var queueHandle: Any? = null

  var generation = 0L
    private set
  var queueSnapshot: Any? = null

  val isStarted: Boolean
    get() = _state.value.items.isNotEmpty()

  suspend fun start(nowPlaying: MusicFlowItem?) {
    if (isStarted) return
    reset(nowPlaying)
  }

  suspend fun reset(nowPlaying: MusicFlowItem?) =
    mutex.withLock {
      seenSongIds.clear()
      seenTrackKeys.clear()
      queueHandle = null
      generation++
      val seedItems = listOfNotNull(nowPlaying)
      seedItems.forEach(::markSeen)
      _state.value = MusicFlowFeedState(items = seedItems, isInitialLoading = true)

      signals =
        try {
          source.signals()
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Timber.tag(TAG).w(e, "Music Flow signals unavailable; continuing without them")
          MusicFlowSignals(MusicFlowCandidateFilter(), emptySet())
        }
      cursor = source.open(nowPlaying?.song)

      var fetches = 0
      var error: MusicFlowFeedError? = null
      while (_state.value.items.size < INITIAL_TARGET && cursor?.hasMore == true && fetches < MAX_FETCHES_PER_LOAD) {
        fetches++
        val outcome = fetchAndAppendLocked()
        if (outcome is FetchOutcome.Failed) {
          error = outcome.error
          break
        }
      }

      if (error == MusicFlowFeedError.OFFLINE && _state.value.items.size <= seedItems.size) {
        appendOfflineLocked()
      }

      _state.update { current ->
        current.copy(
          isInitialLoading = false,
          hasMore = cursor?.hasMore == true && !current.isOffline,
          error =
            when {
              current.items.isNotEmpty() -> null
              error != null -> error
              else -> MusicFlowFeedError.EMPTY
            },
        )
      }
    }

  /** Appends at most one bounded round of candidates and returns exactly the newly added items. */
  suspend fun loadMore(): List<MusicFlowItem> =
    mutex.withLock {
      val current = _state.value
      if (!current.hasMore || current.isInitialLoading || current.isLoadingMore || current.items.size >= MAX_FEED_SIZE) {
        return@withLock emptyList()
      }
      _state.update { it.copy(isLoadingMore = true) }
      val before = _state.value.items.size
      var fetches = 0
      var failure: MusicFlowFeedError? = null
      while (_state.value.items.size == before && cursor?.hasMore == true && fetches < MAX_FETCHES_PER_LOAD) {
        fetches++
        val outcome = fetchAndAppendLocked()
        if (outcome is FetchOutcome.Failed) {
          failure = outcome.error
          break
        }
      }
      val items = _state.value.items
      _state.update {
        it.copy(
          isLoadingMore = false,
          hasMore = cursor?.hasMore == true && items.size < MAX_FEED_SIZE,
          error = if (items.size == before) failure else null,
        )
      }
      items.drop(before)
    }

  fun select(index: Int) {
    _state.update { current ->
      if (current.items.isEmpty()) current
      else current.copy(selectedIndex = index.coerceIn(0, current.items.lastIndex))
    }
  }

  fun indexOf(songId: String?): Int =
    if (songId == null) -1 else _state.value.items.indexOfFirst { it.songId == songId }

  fun hide(songId: String) {
    hiddenSongIds.add(songId)
  }

  fun lessLikeThis(item: MusicFlowItem) {
    item.primaryArtistKey?.let(lessLikeArtists::add)
  }

  fun isHidden(songId: String) = songId in hiddenSongIds

  private fun markSeen(item: MusicFlowItem) {
    seenSongIds.add(item.songId)
    seenTrackKeys.add(item.trackKey)
  }

  private suspend fun fetchAndAppendLocked(): FetchOutcome {
    val activeCursor = cursor ?: return FetchOutcome.Failed(MusicFlowFeedError.FAILED)
    val raw =
      try {
        activeCursor.next()
      } catch (e: MusicFlowOfflineException) {
        return FetchOutcome.Failed(MusicFlowFeedError.OFFLINE)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.tag(TAG).w(e, "Music Flow fetch failed")
        return FetchOutcome.Failed(MusicFlowFeedError.FAILED)
      }
    appendLocked(raw)
    return FetchOutcome.Appended
  }

  private fun appendLocked(raw: List<MusicFlowItem>) {
    val filter =
      signals.filter.copy(
        seenSongIds = seenSongIds,
        seenTrackKeys = seenTrackKeys,
        hiddenSongIds = signals.filter.hiddenSongIds + hiddenSongIds,
      )
    val current = _state.value.items
    val room = MAX_FEED_SIZE - current.size
    if (room <= 0) return
    val ranked =
      rankMusicFlowCandidates(
          candidates = filterMusicFlowCandidates(raw, filter),
          feedTailArtists = current.takeLast(rankingConfig.artistSpacing).map { it.primaryArtistKey },
          likedArtists = signals.likedArtists,
          lessLikeArtists = lessLikeArtists,
          config = rankingConfig,
        )
        .take(room)
    ranked.forEach(::markSeen)
    if (ranked.isNotEmpty()) {
      _state.update { it.copy(items = it.items + ranked, error = null) }
    }
  }

  private suspend fun appendOfflineLocked() {
    val offline =
      try {
        source.offlineCandidates()
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        emptyList()
      }
    if (offline.isEmpty()) return
    val before = _state.value.items.size
    appendLocked(offline)
    if (_state.value.items.size > before) {
      _state.update { it.copy(isOffline = true) }
    }
  }

  private sealed interface FetchOutcome {
    data object Appended : FetchOutcome

    data class Failed(val error: MusicFlowFeedError) : FetchOutcome
  }

  companion object {
    private const val TAG = "MusicFlowSession"
    const val INITIAL_TARGET = 12
    const val PREFETCH_THRESHOLD = 4
    const val MAX_FETCHES_PER_LOAD = 3
    const val MAX_FEED_SIZE = 300
  }
}
