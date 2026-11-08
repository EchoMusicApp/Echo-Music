package echo.music.iad1tya.playback

import androidx.media3.common.Player
import echo.music.iad1tya.playback.queues.Queue
import kotlinx.coroutines.flow.MutableStateFlow

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

  init { player.volume = playerVolume.value }

  fun setMuted(muted: Boolean) {
    if (muted == isMuted.value) return
    if (muted) { volumeBeforeMute = player.volume; player.volume = 0f } else player.volume = volumeBeforeMute
    isMuted.value = muted
  }
}
