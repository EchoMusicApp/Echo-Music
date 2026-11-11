package echo.music.iad1tya.playback

import androidx.media3.common.Player
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.extensions.getCurrentQueueIndex
import echo.music.iad1tya.playback.queues.Queue
import androidx.media3.common.MediaItem
import echo.music.iad1tya.playback.queues.ListQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Desktop replacement for the Android foreground service. It is just the object that owns the player and the
 * bits of state other code reads from the service (volume, mute, current queue). No notification, no MediaSession.
 * shortcut: grows member by member as ported code asks for them.
 */
class MusicService(val player: Player) {
  val playerVolume = MutableStateFlow(1f)
  val isMuted = MutableStateFlow(false)
  val activeQueue = MutableStateFlow<Queue?>(null)
  var queueTitle: String? = null
  var currentQueue: Queue? = null
    private set

  /** Cast is not ported (same no-op handler as the F-Droid build); null means "not casting". */
  val castConnectionHandler: CastConnectionHandler? = null

  // Placeholders until streams are cached on disk (the Storage screen shows 0 bytes).
  val playerCache = androidx.media3.datasource.cache.SimpleCache()
  val downloadCache = androidx.media3.datasource.cache.SimpleCache()

  /** Live state of the automix engine (the player debug overlay); automix is not ported, so it stays null. */
  data class AutomixDebugInfo(
    val status: String,
    val outBpm: Float? = null,
    val outConfidence: Float? = null,
    val outMixOutMs: Long? = null,
    val inBpm: Float? = null,
    val inConfidence: Float? = null,
    val inMixInMs: Long? = null,
    val triggerTimeMs: Long? = null,
    val incomingStartMs: Long? = null,
    val tempoRatio: Float? = null,
  )
  val automixDebugInfo = MutableStateFlow<AutomixDebugInfo?>(null)

  fun toggleMute() = setMuted(!isMuted.value)

  /** What "clear queue" removed, so the Undo snackbar can put it back. */
  data class ClearedQueueState(val removedItems: List<MediaItem>, val startIndex: Int)

  // Keeps the playing song, drops everything after it (same as the Android service).
  fun clearQueue(): ClearedQueueState? {
    val cur = player.currentMediaItemIndex
    if (cur < 0 || player.mediaItemCount <= cur + 1) return null
    val removed = List(player.mediaItemCount - cur - 1) { player.getMediaItemAt(cur + 1 + it) }
    player.removeMediaItems(cur + 1, player.mediaItemCount)
    return ClearedQueueState(removed, cur + 1)
  }

  fun restoreQueue(state: ClearedQueueState) {
    if (state.removedItems.isEmpty()) return
    player.addMediaItems(minOf(state.startIndex, player.mediaItemCount), state.removedItems)
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val sleepTimer = SleepTimer(scope, player).also { player.addListener(it) }

  val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())
  fun clearPersistedQueueFiles() { runCatching { java.io.File(android.content.Context().filesDir, PERSISTENT_QUEUE_FILE).delete() } }
  val isCrossfading = MutableStateFlow(false)
  val isAutomixing = MutableStateFlow(false)
  fun clearAutomix() { automixItems.value = emptyList() }
  fun addToQueueAutomix(item: MediaItem, position: Int) { automixItems.value = automixItems.value.toMutableList().apply { removeAt(position) }; addToQueue(listOf(item)) }
  fun playNextAutomix(item: MediaItem, position: Int) { automixItems.value = automixItems.value.toMutableList().apply { removeAt(position) }; playNext(listOf(item)) }

  // shortcut: only the first "similar content" lookup of the Android version (no fallbacks, no setting checks).
  fun getAutomix(playlistId: String) {
    scope.launch {
      runCatching {
        val first = com.music.innertube.YouTube.next(com.music.innertube.models.WatchEndpoint(playlistId = playlistId)).getOrThrow()
        val second = com.music.innertube.YouTube.next(com.music.innertube.models.WatchEndpoint(playlistId = first.endpoint.playlistId)).getOrNull()
        automixItems.value = (second?.items ?: first.items).map { it.toMediaItem() }
      }
    }
  }
  fun getAutomixAlbum(albumId: String) {
    scope.launch { com.music.innertube.YouTube.album(albumId).onSuccess { getAutomix(it.album.playlistId) } }
  }

  // a newer click must win: a slower earlier radio fetch finishing late would otherwise replace the song just chosen
  private var playJob: Job? = null

  fun playQueue(queue: Queue, playWhenReady: Boolean = true): Job {
    playJob?.cancel()
    return scope.launch { playQueueNow(queue, playWhenReady) }.also { playJob = it }
  }

  private suspend fun playQueueNow(queue: Queue, playWhenReady: Boolean) {
    val status = queue.getInitialStatus()
    queueTitle = status.title
    currentQueue = queue
    activeQueue.value = queue
    // QueuePlayer is single-threaded: touch it only on the UI thread
    kotlinx.coroutines.withContext(Dispatchers.Main) {
      player.setMediaItems(status.items, status.mediaItemIndex, status.position)
      player.prepare()
      player.playWhenReady = playWhenReady
    }
  }

  data class QueueSnapshot(val items: List<MediaItem>, val mediaItemIndex: Int, val positionMs: Long, val queue: Queue, val title: String?)

  fun snapshotQueue(): QueueSnapshot? {
    val queue = currentQueue ?: return null
    if (player.mediaItemCount == 0) return null
    return QueueSnapshot(List(player.mediaItemCount) { player.getMediaItemAt(it) }, player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0L), queue, queueTitle)
  }

  fun restoreQueueSnapshot(snapshot: QueueSnapshot, playWhenReady: Boolean) {
    playQueue(ListQueue(snapshot.title, snapshot.items, snapshot.mediaItemIndex, snapshot.positionMs), playWhenReady)
    // keep the original queue so its paging continues after the restore
    currentQueue = snapshot.queue
    activeQueue.value = snapshot.queue
  }

  fun setPauseAtEndOfItems(queue: Queue, pause: Boolean) {
    if (currentQueue !== queue) return
    player.pauseAtEndOfMediaItems = pause
  }

  private var volumeBeforeMute = 1f

  // Same behaviour as the Android service: nothing queued yet starts playing; otherwise next/last in the queue.
  fun playNext(items: List<androidx.media3.common.MediaItem>) {
    if (player.mediaItemCount == 0) { player.setMediaItems(items); player.prepare(); player.play(); return }
    player.addMediaItems(player.currentMediaItemIndex + 1, items)
  }

  fun addToQueue(items: List<androidx.media3.common.MediaItem>) {
    if (player.mediaItemCount == 0) { player.setMediaItems(items); player.prepare(); player.play(); return }
    player.addMediaItems(items)
  }

  companion object { const val PERSISTENT_QUEUE_FILE = "persistent_queue.data" }

  init { player.volume = playerVolume.value }

  fun setMuted(muted: Boolean) {
    if (muted == isMuted.value) return
    if (muted) { volumeBeforeMute = player.volume; player.volume = 0f } else player.volume = volumeBeforeMute
    isMuted.value = muted
  }
}
