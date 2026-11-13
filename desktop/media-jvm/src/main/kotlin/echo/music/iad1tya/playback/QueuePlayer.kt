package echo.music.iad1tya.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import java.util.concurrent.CopyOnWriteArrayList

/** What actually makes sound. The engine tells it what to load and when to start, pause or seek. */
interface AudioBackend {
  /** Called when playback of this item should start loading; the backend reports back through [Callbacks]. */
  fun load(item: MediaItem, startPositionMs: Long, playWhenReady: Boolean)
  /** Optional: look the next item up in the background so it starts quickly. */
  fun prefetch(item: MediaItem) {}
  fun play()
  fun pause()
  fun stop()
  fun seekTo(positionMs: Long)
  fun setVolume(volume: Float)
  fun setSpeed(speed: Float)
  val positionMs: Long
  val durationMs: Long
  val bufferedMs: Long
  var callbacks: Callbacks?

  interface Callbacks {
    fun onReady(durationMs: Long)
    fun onBuffering()
    fun onEnded()
    fun onError(error: PlaybackException)
  }
}

/** Backend that does nothing; keeps the engine usable (and testable) before real audio is plugged in. */
class SilentBackend : AudioBackend {
  override fun load(item: MediaItem, startPositionMs: Long, playWhenReady: Boolean) { pos = startPositionMs; callbacks?.onReady(0) }
  override fun play() {}
  override fun pause() {}
  override fun stop() {}
  override fun seekTo(positionMs: Long) { pos = positionMs }
  override fun setVolume(volume: Float) {}
  override fun setSpeed(speed: Float) {}
  private var pos = 0L
  override val positionMs get() = pos
  override val durationMs get() = C.TIME_UNSET
  override val bufferedMs get() = pos
  override var callbacks: AudioBackend.Callbacks? = null
}

/**
 * Desktop implementation of Media3's Player: keeps the queue, the shuffle order, repeat modes and the play state,
 * and delegates sound to an [AudioBackend]. Not thread-safe: call it from one thread (the UI thread).
 */
class QueuePlayer(private val backend: AudioBackend = SilentBackend()) : Player, AudioBackend.Callbacks {
  private val items = ArrayList<MediaItem>()
  private var shuffleOrder: IntArray = IntArray(0)
  private val listeners = CopyOnWriteArrayList<Player.Listener>()

  private var index = C.INDEX_UNSET
  private var state = Player.STATE_IDLE
  private var wantsToPlay = false
  private var repeat = Player.REPEAT_MODE_OFF
  private var shuffle = false
  private var error: PlaybackException? = null
  private var volume_ = 1f
  private var params = PlaybackParameters.DEFAULT
  private var durationMs_ = C.TIME_UNSET

  init { backend.callbacks = this }

  // ---- timeline: windows in queue order, next/previous follow the shuffle order when it is on ----
  private inner class QueueTimeline(private val snapshot: List<MediaItem>, private val order: IntArray) : Timeline() {
    override fun getWindowCount() = snapshot.size
    override fun getWindow(windowIndex: Int, window: Window): Window {
      window.mediaItem = snapshot[windowIndex]
      window.uid = snapshot[windowIndex].mediaId + "#" + windowIndex
      window.durationUs = if (windowIndex == index && durationMs_ != C.TIME_UNSET) durationMs_ * 1000 else C.TIME_UNSET
      return window
    }
    private fun step(windowIndex: Int, repeatMode: Int, shuffleOn: Boolean, delta: Int): Int {
      if (snapshot.isEmpty()) return C.INDEX_UNSET
      if (repeatMode == Player.REPEAT_MODE_ONE) return windowIndex
      val pos = if (shuffleOn) order.indexOf(windowIndex) else windowIndex
      var next = pos + delta
      if (next < 0 || next >= snapshot.size) {
        if (repeatMode != Player.REPEAT_MODE_ALL) return C.INDEX_UNSET
        next = (next + snapshot.size) % snapshot.size
      }
      return if (shuffleOn) order[next] else next
    }
    override fun getNextWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean) = step(windowIndex, repeatMode, shuffleModeEnabled, 1)
    override fun getPreviousWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean) = step(windowIndex, repeatMode, shuffleModeEnabled, -1)
  }

  private var timeline: Timeline = Timeline.EMPTY
  private fun rebuildTimeline(reason: Int) {
    timeline = QueueTimeline(items.toList(), shuffleOrder.copyOf())
    listeners.forEach { it.onTimelineChanged(timeline, reason) }
  }

  private fun reshuffle() {
    // current item first, the rest in random order, so toggling shuffle keeps the current song playing
    val rest = items.indices.filter { it != index }.shuffled()
    shuffleOrder = (listOfNotNull(index.takeIf { it in items.indices }) + rest).toIntArray()
  }

  private fun keepOrderValid() { if (shuffle) reshuffle() else shuffleOrder = IntArray(items.size) { it } }

  // ---- Player: listeners and read-only state ----
  override fun addListener(listener: Player.Listener) { listeners.add(listener) }
  override fun removeListener(listener: Player.Listener) { listeners.remove(listener) }
  override fun getPlaybackState() = state
  override fun getPlayWhenReady() = wantsToPlay
  override fun isPlaying() = wantsToPlay && state == Player.STATE_READY
  override fun getRepeatMode() = repeat
  override fun getShuffleModeEnabled() = shuffle
  override fun getPlayerError() = error
  override fun getCurrentTimeline() = timeline
  override fun getCurrentMediaItem(): MediaItem? = items.getOrNull(index)
  override fun getMediaMetadata(): MediaMetadata = currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
  override fun getCurrentMediaItemIndex() = index
  override fun getMediaItemCount() = items.size
  override fun getMediaItemAt(index: Int) = items[index]
  override fun getPreviousMediaItemIndex() = if (index == C.INDEX_UNSET) C.INDEX_UNSET else timeline.getPreviousWindowIndex(index, repeat, shuffle)
  override fun getNextMediaItemIndex() = if (index == C.INDEX_UNSET) C.INDEX_UNSET else timeline.getNextWindowIndex(index, repeat, shuffle)
  override fun hasPreviousMediaItem() = previousMediaItemIndex != C.INDEX_UNSET
  override fun hasNextMediaItem() = nextMediaItemIndex != C.INDEX_UNSET
  override fun getCurrentPosition() = if (index == C.INDEX_UNSET) 0L else backend.positionMs
  override fun getDuration() = durationMs_
  override fun getBufferedPosition() = backend.bufferedMs
  override fun getVolume() = volume_
  override fun setVolume(volume: Float) { volume_ = volume; backend.setVolume(volume) }
  override fun getPlaybackParameters() = params
  override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
    params = playbackParameters; backend.setSpeed(playbackParameters.speed)
    listeners.forEach { it.onPlaybackParametersChanged(playbackParameters) }
  }

  // ---- Player: state changes ----
  private fun setState(newState: Int) {
    if (state == newState) return
    val wasPlaying = isPlaying
    state = newState
    listeners.forEach { it.onPlaybackStateChanged(newState) }
    if (wasPlaying != isPlaying) listeners.forEach { it.onIsPlayingChanged(isPlaying) }
  }

  override fun setPlayWhenReady(playWhenReady: Boolean) {
    if (wantsToPlay == playWhenReady) return
    val wasPlaying = isPlaying
    wantsToPlay = playWhenReady
    if (playWhenReady) backend.play() else backend.pause()
    listeners.forEach { it.onPlayWhenReadyChanged(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    if (wasPlaying != isPlaying) listeners.forEach { it.onIsPlayingChanged(isPlaying) }
  }

  override fun setRepeatMode(repeatMode: Int) {
    if (repeat == repeatMode) return
    repeat = repeatMode; listeners.forEach { it.onRepeatModeChanged(repeatMode) }
  }

  override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
    if (shuffle == shuffleModeEnabled) return
    shuffle = shuffleModeEnabled
    keepOrderValid(); rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
  }

  override fun prepare() { if (state == Player.STATE_IDLE && index != C.INDEX_UNSET) startItem(index, 0L, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
  override fun play() { if (state == Player.STATE_IDLE) prepare(); playWhenReady = true }
  override fun pause() { playWhenReady = false }
  override fun stop() { backend.stop(); setState(Player.STATE_IDLE) }
  override fun release() { backend.stop(); listeners.clear() }

  // ---- Player: moving around ----
  private fun startItem(newIndex: Int, positionMs: Long, reason: Int) {
    // setMediaItems already moved index, so a new playlist must always announce its first item (else the UI never shows the player)
    val changed = newIndex != index || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
    index = newIndex; error = null; durationMs_ = C.TIME_UNSET
    setState(Player.STATE_BUFFERING)
    backend.load(items[newIndex], positionMs, wantsToPlay)
    nextMediaItemIndex.takeIf { it != C.INDEX_UNSET }?.let { backend.prefetch(items[it]) }
    if (changed) {
      listeners.forEach { it.onMediaItemTransition(items[newIndex], reason); it.onMediaMetadataChanged(items[newIndex].mediaMetadata) }
    }
  }

  override fun seekTo(positionMs: Long) {
    if (index == C.INDEX_UNSET) return
    val old = Player.PositionInfo(currentMediaItem, index, currentPosition)
    backend.seekTo(positionMs)
    val new = Player.PositionInfo(currentMediaItem, index, positionMs)
    listeners.forEach { it.onPositionDiscontinuity(old, new, Player.DISCONTINUITY_REASON_SEEK) }
  }
  override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
    if (mediaItemIndex !in items.indices) return
    if (mediaItemIndex == index && state != Player.STATE_IDLE) backend.seekTo(positionMs)
    else startItem(mediaItemIndex, positionMs, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
  }
  override fun seekToNextMediaItem() { val n = nextMediaItemIndex; if (n != C.INDEX_UNSET) startItem(n, 0L, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) }
  override fun seekToNext() = seekToNextMediaItem()
  override fun seekToPreviousMediaItem() { val p = previousMediaItemIndex; if (p != C.INDEX_UNSET) startItem(p, 0L, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) }
  // Like Media3: within the first few seconds go to the previous item, afterwards restart the current one.
  override fun seekToPrevious() { if (currentPosition > 3000 || !hasPreviousMediaItem()) seekTo(0L) else seekToPreviousMediaItem() }

  // ---- Player: editing the queue ----
  override fun setMediaItem(mediaItem: MediaItem) = setMediaItems(listOf(mediaItem), 0, 0L)
  override fun setMediaItems(mediaItems: List<MediaItem>) = setMediaItems(mediaItems, 0, 0L)
  override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
    items.clear(); items.addAll(mediaItems)
    index = if (items.isEmpty()) C.INDEX_UNSET else startIndex.coerceIn(0, items.lastIndex)
    keepOrderValid(); rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    if (index == C.INDEX_UNSET) { backend.stop(); setState(Player.STATE_IDLE) }
    else startItem(index, if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
  }
  override fun addMediaItem(mediaItem: MediaItem) = addMediaItems(items.size, listOf(mediaItem))
  override fun addMediaItem(index: Int, mediaItem: MediaItem) = addMediaItems(index, listOf(mediaItem))
  override fun addMediaItems(mediaItems: List<MediaItem>) = addMediaItems(items.size, mediaItems)
  override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
    val at = index.coerceIn(0, items.size)
    items.addAll(at, mediaItems)
    if (this.index != C.INDEX_UNSET && at <= this.index) this.index += mediaItems.size
    if (this.index == C.INDEX_UNSET && items.isNotEmpty()) this.index = 0
    keepOrderValid(); rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
  }
  override fun removeMediaItem(index: Int) = removeMediaItems(index, index + 1)
  override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
    val from = fromIndex.coerceIn(0, items.size); val to = toIndex.coerceIn(from, items.size)
    if (from == to) return
    val removedCurrent = index in from until to
    repeat(to - from) { items.removeAt(from) }
    when {
      items.isEmpty() -> index = C.INDEX_UNSET
      index >= to -> index -= to - from
      removedCurrent -> index = from.coerceAtMost(items.lastIndex)
    }
    keepOrderValid(); rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    if (items.isEmpty()) { backend.stop(); setState(Player.STATE_IDLE) }
    else if (removedCurrent) startItem(index, 0L, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
  }
  override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
    if (currentIndex !in items.indices || newIndex !in items.indices || currentIndex == newIndex) return
    val current = items.getOrNull(index)
    items.add(newIndex, items.removeAt(currentIndex))
    index = if (current == null) C.INDEX_UNSET else items.indexOfFirst { it === current }
    keepOrderValid(); rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
  }
  override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
    if (index !in items.indices) return
    items[index] = mediaItem
    rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    // the playing item was swapped: the backend still plays the old stream, so load the new one where it was
    if (index == this.index && state != Player.STATE_IDLE) startItem(index, currentPosition, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
  }
  override fun clearMediaItems() = removeMediaItems(0, items.size)

  private var pauseAtEnd = false
  override fun getPauseAtEndOfMediaItems() = pauseAtEnd
  override fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean) { pauseAtEnd = pauseAtEndOfMediaItems }

  // ---- AudioBackend.Callbacks ----
  override fun onReady(durationMs: Long) { durationMs_ = durationMs; setState(Player.STATE_READY) }
  override fun onBuffering() { setState(Player.STATE_BUFFERING) }
  override fun onEnded() {
    if (repeat == Player.REPEAT_MODE_ONE) { startItem(index, 0L, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT); return }
    val next = nextMediaItemIndex
    if (pauseAtEnd) playWhenReady = false
    if (next != C.INDEX_UNSET) startItem(next, 0L, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) else setState(Player.STATE_ENDED)
  }
  override fun onError(error: PlaybackException) {
    this.error = error; setState(Player.STATE_IDLE)
    listeners.forEach { it.onPlayerError(error) }
  }
}
