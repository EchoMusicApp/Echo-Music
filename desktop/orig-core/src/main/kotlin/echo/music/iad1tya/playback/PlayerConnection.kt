package echo.music.iad1tya.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.extensions.metadata
import echo.music.iad1tya.extensions.togglePlayPause
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.queues.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

// shortcut: desktop subset of the Android PlayerConnection (no service binding, like/library toggles, lyrics or format yet); grow as screens need it.
class PlayerConnection(val player: Player, val database: MusicDatabase, private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) : Player.Listener {
  private val _playbackState = MutableStateFlow(player.playbackState)
  val playbackState: StateFlow<Int> = _playbackState
  private val _isPlaying = MutableStateFlow(player.isPlaying)
  val isPlaying: StateFlow<Boolean> = _isPlaying
  private val _mediaMetadata = MutableStateFlow<MediaMetadata?>(player.currentMediaItem?.metadata)
  val mediaMetadata: StateFlow<MediaMetadata?> = _mediaMetadata
  private val _error = MutableStateFlow<PlaybackException?>(null)
  val error: StateFlow<PlaybackException?> = _error
  private val _queueTitle = MutableStateFlow<String?>(null)
  val queueTitle: StateFlow<String?> = _queueTitle

  @OptIn(ExperimentalCoroutinesApi::class)
  val currentSong = mediaMetadata.flatMapLatest { database.song(it?.id) }
  @OptIn(ExperimentalCoroutinesApi::class)
  val currentLyrics = mediaMetadata.flatMapLatest { database.lyrics(it?.id) }
  @OptIn(ExperimentalCoroutinesApi::class)
  val currentFormat = mediaMetadata.flatMapLatest { database.format(it?.id) }

  init { player.addListener(this) }

  override fun onPlaybackStateChanged(playbackState: Int) { _playbackState.value = playbackState; _isPlaying.value = player.isPlaying }
  override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { _mediaMetadata.value = mediaItem?.metadata }
  override fun onPlayerError(error: PlaybackException) { _error.value = error }

  fun playQueue(queue: Queue) {
    scope.launch {
      val status = queue.getInitialStatus()
      _queueTitle.value = status.title
      player.setMediaItems(status.items, status.mediaItemIndex, status.position)
      player.prepare()
      player.play()
    }
  }

  fun playNext(item: MediaItem) = playNext(listOf(item))
  fun playNext(items: List<MediaItem>) {
    if (player.mediaItemCount == 0) player.setMediaItems(items) else player.addMediaItems(player.currentMediaItemIndex + 1, items)
  }
  fun addToQueue(item: MediaItem) = addToQueue(listOf(item))
  fun addToQueue(items: List<MediaItem>) = player.addMediaItems(items)

  fun togglePlayPause() = player.togglePlayPause()
  fun play() = player.play()
  fun pause() = player.pause()
  fun seekTo(positionMs: Long) = player.seekTo(positionMs)
  fun seekToNext() = player.seekToNext()
  fun seekToPrevious() = player.seekToPrevious()
  fun dispose() { player.removeListener(this) }
}
