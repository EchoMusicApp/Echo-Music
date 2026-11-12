package echo.music.iad1tya.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.extensions.getQueueWindows
import echo.music.iad1tya.extensions.getCurrentQueueIndex
import echo.music.iad1tya.extensions.metadata
import echo.music.iad1tya.extensions.togglePlayPause
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.queues.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.playback.queues.YouTubeQueue
import kotlinx.coroutines.launch

// shortcut: desktop subset of the Android PlayerConnection (no like/library toggles, format info, crossfade/automix or SponsorBlock yet); grow as screens need it.
class PlayerConnection(
  val service: MusicService,
  val database: MusicDatabase,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : Player.Listener {
  val player: Player = service.player

  private val _playbackState = MutableStateFlow(player.playbackState)
  val playbackState: StateFlow<Int> = _playbackState
  private val _isPlaying = MutableStateFlow(player.isPlaying)
  val isPlaying: StateFlow<Boolean> = _isPlaying
  private val _mediaMetadata = MutableStateFlow<MediaMetadata?>(player.currentMediaItem?.metadata)
  val mediaMetadata: StateFlow<MediaMetadata?> = _mediaMetadata
  val error = MutableStateFlow<PlaybackException?>(null)
  val queueTitle = MutableStateFlow<String?>(null)
  val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
  val currentMediaItemIndex = MutableStateFlow(player.currentMediaItemIndex)
  val currentWindowIndex = MutableStateFlow(player.getCurrentQueueIndex())
  val shuffleModeEnabled = MutableStateFlow(player.shuffleModeEnabled)
  val repeatMode = MutableStateFlow(player.repeatMode)
  val canSkipPrevious = MutableStateFlow(player.hasPreviousMediaItem())
  val canSkipNext = MutableStateFlow(player.hasNextMediaItem())
  val isMuted = service.isMuted
  // shortcut: the Android version also covers crossfade/automix overlap; here it is just "is playing".
  val isEffectivelyPlaying: StateFlow<Boolean> get() = _isPlaying

  /** Listen Together (guest) can forbid local playback changes; [allowInternalSync] lets the sync code through. */
  var shouldBlockPlaybackChanges: (() -> Boolean)? = null
  @Volatile var allowInternalSync: Boolean = false
  var onSkipPrevious: (() -> Unit)? = null
  var onSkipNext: (() -> Unit)? = null
  var onRestartSong: (() -> Unit)? = null

  private fun blocked() = !allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true

  @OptIn(ExperimentalCoroutinesApi::class)
  val currentSong = mediaMetadata.flatMapLatest { database.song(it?.id) }
  @OptIn(ExperimentalCoroutinesApi::class)
  val currentLyrics = mediaMetadata.flatMapLatest { database.lyrics(it?.id) }
  @OptIn(ExperimentalCoroutinesApi::class)
  val currentFormat = mediaMetadata.flatMapLatest { database.format(it?.id) }

  init { player.addListener(this); refresh(); trackListening() }

  /**
   * Listening history: after 30 s of actual playback the song (so the History join finds it) and an Event are saved.
   * shortcut: the event is written once at the 30 s mark with playTime 30 s, not updated at the end with the real total; ignores the "history duration" setting.
   */
  private fun trackListening() {
    scope.launch {
      var id: String? = null
      var played = 0L
      var saved = false
      while (true) {
        kotlinx.coroutines.delay(1000)
        val cur = mediaMetadata.value
        if (cur?.id != id) { id = cur?.id; played = 0; saved = false }
        if (cur == null || saved || !isPlaying.value) continue
        played += 1000
        if (played >= 30_000) {
          saved = true
          // the "pause listen history" privacy setting: write nothing (neither the song/event nor the play time)
          if (android.content.Context().dataStore.data.first()[echo.music.iad1tya.constants.PauseListenHistoryKey] == true) continue
          database.query {
            insert(cur)
            incrementTotalPlayTime(cur.id, played)
            insert(echo.music.iad1tya.db.entities.Event(songId = cur.id, timestamp = java.time.LocalDateTime.now(), playTime = played))
          }
        }
      }
    }
  }

  private fun refresh() {
    queueWindows.value = player.getQueueWindows()
    currentMediaItemIndex.value = player.currentMediaItemIndex
    currentWindowIndex.value = player.getCurrentQueueIndex()
    canSkipPrevious.value = player.hasPreviousMediaItem()
    canSkipNext.value = player.hasNextMediaItem()
  }

  override fun onPlaybackStateChanged(playbackState: Int) { _playbackState.value = playbackState; _isPlaying.value = player.isPlaying }
  override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { _mediaMetadata.value = mediaItem?.metadata; refresh() }
  override fun onTimelineChanged(timeline: Timeline, reason: Int) = refresh()
  override fun onRepeatModeChanged(repeatMode: Int) { this.repeatMode.value = repeatMode; refresh() }
  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { this.shuffleModeEnabled.value = shuffleModeEnabled; refresh() }
  override fun onPlayerError(error: PlaybackException) { this.error.value = error }

  fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
    if (blocked()) return
    queueTitle.value = null
    service.playQueue(queue, playWhenReady)
    scope.launch { queue.getInitialStatus().let { queueTitle.value = it.title } }
  }

  fun playNext(item: MediaItem) = playNext(listOf(item))
  fun playNext(items: List<MediaItem>) { if (!blocked()) service.playNext(items) }
  fun addToQueue(item: MediaItem) = addToQueue(listOf(item))
  fun addToQueue(items: List<MediaItem>) { if (!blocked()) service.addToQueue(items) }

  fun setMuted(muted: Boolean) = service.setMuted(muted)
  val automixDebugInfo = service.automixDebugInfo
  val isCrossfading = service.isCrossfading
  val isAutomixing = service.isAutomixing
  fun toggleMute() = service.toggleMute()
  fun clearQueue(): MusicService.ClearedQueueState? = if (blocked()) null else service.clearQueue()
  fun restoreQueue(state: MusicService.ClearedQueueState) { if (!blocked()) service.restoreQueue(state) }

  // likes are also sent to YouTube Music when signed in (SyncUtils checks that); library toggle stays local.
  fun toggleLike() {
    scope.launch {
      currentSong.first()?.let { s ->
        val updated = s.song.toggleLike()
        database.query { update(updated) }
        runCatching { echo.music.iad1tya.di.DesktopGraph.resolve(echo.music.iad1tya.utils.SyncUtils::class.java).likeSong(updated) }
      }
    }
  }
  fun toggleLibrary() { scope.launch { currentSong.first()?.let { s -> database.query { update(s.song.toggleLibrary()) } } } }

  // Appends the radio after the playing song (the Android version also keeps the playing song and swaps the rest).
  fun startRadioSeamlessly() {
    if (blocked()) return
    val id = mediaMetadata.value?.id ?: return
    scope.launch {
      val items = YouTubeQueue(WatchEndpoint(videoId = id)).getInitialStatus().items.filter { it.mediaId != id }
      if (items.isNotEmpty()) addToQueue(items)
    }
  }
  fun togglePlayPause() { if (!blocked()) player.togglePlayPause() }
  fun play() { if (!blocked()) player.play() }
  fun pause() { if (!blocked()) player.pause() }
  fun seekTo(positionMs: Long) { if (!blocked()) player.seekTo(positionMs) }
  fun seekToNext() { if (!allowInternalSync && onSkipNext != null) onSkipNext?.invoke() else if (!blocked()) player.seekToNext() }
  fun seekToPrevious() { if (!allowInternalSync && onSkipPrevious != null) onSkipPrevious?.invoke() else if (!blocked()) player.seekToPrevious() }
  fun dispose() { player.removeListener(this) }
}
