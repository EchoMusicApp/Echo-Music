package echo.music.iad1tya.playback.crossfade

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.StateFlow

sealed interface ArmedStatus {
  data object Idle : ArmedStatus
  data class Preparing(val targetMediaId: String, val startedAt: Long) : ArmedStatus
  data class Armed(val standbyPlayer: ExoPlayer, val targetMediaId: String) : ArmedStatus
  data class Fading(val progress: Float) : ArmedStatus
}

interface StandbyCrossfadeScheduler {
  val isCrossfading: StateFlow<Boolean>
  val armedState: StateFlow<ArmedStatus>
  val preArmManager: AdaptivePreArmManager

  /**
   * Sets up a callback invoked when the crossfade transition completes and players should swap.
   * outgoingPlayer: The old player that just faded out (caller must release or reuse).
   * incomingPlayer: The newly promoted player that just faded in.
   */
  var onCrossfadeSwapped: ((outgoingPlayer: ExoPlayer, incomingPlayer: ExoPlayer) -> Unit)?

  /**
   * Notifies the scheduler of track or queue changes.
   */
  fun onQueueOrTrackChanged(
    primaryPlayer: ExoPlayer,
    nextItem: MediaItem?,
    crossfadeDurationMs: Long
  )

  /**
   * High frequency position tick to evaluate pre-arming and trigger timing without polling.
   */
  fun onPlaybackPositionTick(
    primaryPlayer: ExoPlayer,
    currentPositionMs: Long,
    durationMs: Long,
    playerFactory: () -> ExoPlayer
  )

  /**
   * Starts crossfade immediately if armed, or forces transition.
   */
  fun triggerCrossfadeNow(primaryPlayer: ExoPlayer)

  /**
   * Aborts active pre-arm or crossfade on user pause, seek, or queue clear.
   */
  fun cancelCrossfade()

  /**
   * Releases any allocated standby player resources.
   */
  fun release()
}
