package echo.music.iad1tya.playback

import androidx.media3.common.Player
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

  /** Cast is not ported; callers only ask whether it is casting. */
  class CastConnectionHandler { val isCasting = MutableStateFlow(false) }
  val castConnectionHandler: CastConnectionHandler? = null

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun playQueue(queue: Queue, playWhenReady: Boolean = true): Job = scope.launch {
    val status = queue.getInitialStatus()
    queueTitle = status.title
    currentQueue = queue
    activeQueue.value = queue
    player.setMediaItems(status.items, status.mediaItemIndex, status.position)
    player.prepare()
    player.playWhenReady = playWhenReady
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
