package echo.music.iad1tya.playback.crossfade

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StandbyCrossfadeSchedulerImpl(
  private val scope: CoroutineScope,
  override val preArmManager: AdaptivePreArmManager = AdaptivePreArmManager()
) : StandbyCrossfadeScheduler {

  private val _isCrossfading = MutableStateFlow(false)
  override val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()

  private val _armedState = MutableStateFlow<ArmedStatus>(ArmedStatus.Idle)
  override val armedState: StateFlow<ArmedStatus> = _armedState.asStateFlow()

  override var onCrossfadeSwapped: ((outgoingPlayer: ExoPlayer, incomingPlayer: ExoPlayer) -> Unit)? = null

  private var nextMediaItem: MediaItem? = null
  private var crossfadeDurationMs: Long = 0L
  private var standbyPlayerInstance: ExoPlayer? = null
  private var standbyListener: Player.Listener? = null
  private var crossfadeJob: Job? = null
  private var prepStartTimeMs: Long = 0L

  override fun onQueueOrTrackChanged(
    primaryPlayer: ExoPlayer,
    nextItem: MediaItem?,
    crossfadeDurationMs: Long
  ) {
    if (nextMediaItem?.mediaId != nextItem?.mediaId) {
      cancelCrossfade()
    }
    this.nextMediaItem = nextItem
    this.crossfadeDurationMs = crossfadeDurationMs
  }

  override fun onPlaybackPositionTick(
    primaryPlayer: ExoPlayer,
    currentPositionMs: Long,
    durationMs: Long,
    playerFactory: () -> ExoPlayer
  ) {
    if (crossfadeDurationMs <= 0 || durationMs <= crossfadeDurationMs || nextMediaItem == null) {
      return
    }

    val remainingMs = durationMs - currentPositionMs
    val leadTimeMs = preArmManager.currentLeadTimeMs

    // Check if we need to pre-arm
    if (_armedState.value is ArmedStatus.Idle && remainingMs <= leadTimeMs && remainingMs > crossfadeDurationMs) {
      val targetItem = nextMediaItem ?: return
      prepStartTimeMs = System.currentTimeMillis()
      _armedState.value = ArmedStatus.Preparing(targetItem.mediaId, prepStartTimeMs)

      val standby = playerFactory()
      standbyPlayerInstance = standby
      standby.volume = 0f
      standby.playWhenReady = false

      val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          this@StandbyCrossfadeSchedulerImpl.onStandbyPlayerStateChanged(playbackState)
        }
      }
      standbyListener = listener
      standby.addListener(listener)

      standby.setMediaItem(targetItem)
      standby.prepare()

      // Check immediate state or wait for listener
      if (standby.playbackState == Player.STATE_READY) {
        onStandbyPlayerStateChanged(Player.STATE_READY)
      }
    }

    // Check if trigger timestamp has arrived
    if (_armedState.value is ArmedStatus.Armed && remainingMs <= crossfadeDurationMs && !_isCrossfading.value) {
      startCrossfade(primaryPlayer)
    }
  }

  fun onStandbyPlayerStateChanged(playbackState: Int) {
    val current = _armedState.value
    if (current is ArmedStatus.Preparing && playbackState == Player.STATE_READY) {
      val prepDuration = System.currentTimeMillis() - prepStartTimeMs
      if (prepDuration > 0) {
        preArmManager.recordPreparationDuration(prepDuration)
      }
      val standby = standbyPlayerInstance
      if (standby != null) {
        _armedState.value = ArmedStatus.Armed(standby, current.targetMediaId)
      }
    }
  }

  override fun triggerCrossfadeNow(primaryPlayer: ExoPlayer) {
    if (!_isCrossfading.value && _armedState.value is ArmedStatus.Armed) {
      startCrossfade(primaryPlayer)
    }
  }

  private var activePrimaryPlayer: ExoPlayer? = null

  private fun startCrossfade(primaryPlayer: ExoPlayer) {
    val standby = standbyPlayerInstance ?: return
    if (_isCrossfading.value) return
    activePrimaryPlayer = primaryPlayer
    _isCrossfading.value = true
    standby.playWhenReady = true

    crossfadeJob?.cancel()
    crossfadeJob = scope.launch {
      val steps = (crossfadeDurationMs / 20L).toInt().coerceAtLeast(10)
      for (i in 0..steps) {
        val progress = i.toFloat() / steps.toFloat()
        _armedState.value = ArmedStatus.Fading(progress)

        val gains = EqualPowerCurve.calculateGains(progress)
        try {
          primaryPlayer.volume = gains.outgoingGain
          standby.volume = gains.incomingGain
        } catch (_: Exception) {}

        delay(20L)
      }

      // Reset volume levels before handing off
      try {
        primaryPlayer.volume = 1.0f
        standby.volume = 1.0f
      } catch (_: Exception) {}

      // Finish crossfade & perform swap
      _isCrossfading.value = false
      _armedState.value = ArmedStatus.Idle
      val outgoing = primaryPlayer
      val incoming = standby
      standbyListener?.let { incoming.removeListener(it) }
      standbyListener = null
      standbyPlayerInstance = null
      activePrimaryPlayer = null
      onCrossfadeSwapped?.invoke(outgoing, incoming)
    }
  }

  override fun cancelCrossfade() {
    crossfadeJob?.cancel()
    crossfadeJob = null
    _isCrossfading.value = false
    _armedState.value = ArmedStatus.Idle
    activePrimaryPlayer?.let {
      try {
        it.volume = 1.0f
      } catch (_: Exception) {}
    }
    activePrimaryPlayer = null
    standbyPlayerInstance?.let {
      try {
        standbyListener?.let { l -> it.removeListener(l) }
        it.stop()
        it.clearMediaItems()
        it.release()
      } catch (_: Exception) {}
    }
    standbyListener = null
    standbyPlayerInstance = null
  }

  override fun release() {
    cancelCrossfade()
  }
}
