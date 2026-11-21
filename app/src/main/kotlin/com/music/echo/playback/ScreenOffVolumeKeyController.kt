package echo.music.iad1tya.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.VolumeProvider
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.media3.common.Player
import timber.log.Timber

/**
 * Routes screen-off volume-key presses to track navigation while preserving
 * ordinary volume adjustment for short presses.
 *
 * Android routes volume keys to an active remote-playback MediaSession while
 * the display is off. A short press is applied to the music stream on release;
 * a hold navigates tracks without changing the volume.
 */
internal class ScreenOffVolumeKeyController(
  context: Context,
  private val audioManager: AudioManager,
  private val isPlaybackRequested: () -> Boolean,
  private val onPlayRequested: () -> Unit,
  private val onPauseRequested: () -> Unit,
  private val onNextTrack: () -> Unit,
  private val onPreviousTrack: () -> Unit,
) {
  companion object {
    private const val TAG = "ScreenOffVolumeKeys"
    private const val LONG_PRESS_TIMEOUT_MS = 500L
  }

  private val appContext = context.applicationContext
  private val powerManager = requireNotNull(appContext.getSystemService(PowerManager::class.java))
  private val handler = Handler(Looper.getMainLooper())

  private var enabled = false
  private var pressedDirection = 0
  private var longPressTriggered = false
  private var longPressRunnable: Runnable? = null
  private var currentPlaybackState = PlaybackState.STATE_NONE

  private val volumeProvider =
    object : VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 100, 50) {
      override fun onAdjustVolume(direction: Int) {
        handler.post { handleVolumeAdjustment(direction) }
      }
    }

  private val mediaSession =
    MediaSession(appContext, "EchoMusicScreenOffVolumeKeys").apply {
      setCallback(
        object : MediaSession.Callback() {
          override fun onPlay() = handlePlaybackCommand(requestedPlaying = true)

          override fun onPause() = handlePlaybackCommand(requestedPlaying = false)

          override fun onSkipToNext() = onNextTrack()

          override fun onSkipToPrevious() = onPreviousTrack()
        },
        handler
      )
    }

  private fun handlePlaybackCommand(requestedPlaying: Boolean) {
    if (currentPlaybackState == PlaybackState.STATE_BUFFERING) {
      if (isPlaybackRequested()) onPauseRequested() else onPlayRequested()
    } else if (requestedPlaying) {
      onPlayRequested()
    } else {
      onPauseRequested()
    }
  }

  fun update(
    playWhenReady: Boolean,
    playbackState: Int,
    refreshPriority: Boolean = false,
  ) {
    val shouldIntercept =
      playWhenReady &&
        (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY) &&
        !powerManager.isInteractive
    if (shouldIntercept) {
      currentPlaybackState =
        if (playbackState == Player.STATE_BUFFERING) {
          PlaybackState.STATE_BUFFERING
        } else {
          PlaybackState.STATE_PLAYING
        }
      if (!enabled) {
        enabled = true
        mediaSession.setPlaybackToRemote(volumeProvider)
        mediaSession.setPlaybackState(buildPlaybackState(currentPlaybackState))
        mediaSession.setActive(true)
        Timber.tag(TAG).d("Screen-off volume-key controls enabled")
      } else {
        if (refreshPriority) {
          refreshSessionPriority()
        } else {
          mediaSession.setPlaybackState(buildPlaybackState(currentPlaybackState))
        }
      }
    } else if (enabled) {
      enabled = false
      currentPlaybackState = PlaybackState.STATE_NONE
      cancelPendingPress()
      mediaSession.setActive(false)
      mediaSession.setPlaybackToLocal(
        AudioAttributes.Builder()
          .setLegacyStreamType(AudioManager.STREAM_MUSIC)
          .build()
      )
      mediaSession.setPlaybackState(
        buildPlaybackState(PlaybackState.STATE_NONE)
      )
      Timber.tag(TAG).d("Screen-off volume-key controls disabled")
    }
  }

  private fun handleVolumeAdjustment(direction: Int) {
    if (!enabled) return

    if (direction == 0) {
      finishPress()
      return
    }

    if (direction != AudioManager.ADJUST_RAISE &&
      direction != AudioManager.ADJUST_LOWER
    ) {
      return
    }

    // Ignore repeated adjustment callbacks generated while a key is held.
    if (pressedDirection != 0) return

    pressedDirection = direction
    longPressTriggered = false
    val runnable =
      Runnable {
        if (enabled && pressedDirection == direction && !longPressTriggered) {
          longPressTriggered = true
          if (direction == AudioManager.ADJUST_RAISE) {
            Timber.tag(TAG).d("Long press volume up: next track")
            onNextTrack()
            refreshSessionPriority(PlaybackState.STATE_SKIPPING_TO_NEXT)
          } else {
            Timber.tag(TAG).d("Long press volume down: previous track")
            onPreviousTrack()
            refreshSessionPriority(PlaybackState.STATE_SKIPPING_TO_PREVIOUS)
          }
        }
      }
    longPressRunnable = runnable
    handler.postDelayed(runnable, LONG_PRESS_TIMEOUT_MS)
  }

  private fun finishPress() {
    if (pressedDirection == 0) return
    longPressRunnable?.let(handler::removeCallbacks)
    longPressRunnable = null

    if (!longPressTriggered) {
      // A tap remains a normal volume adjustment; only a hold is remapped.
      audioManager.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        pressedDirection,
        AudioManager.FLAG_SHOW_UI
      )
    }

    pressedDirection = 0
    longPressTriggered = false
  }

  private fun cancelPendingPress() {
    longPressRunnable?.let(handler::removeCallbacks)
    longPressRunnable = null
    pressedDirection = 0
    longPressTriggered = false
  }

  private fun buildPlaybackState(state: Int): PlaybackState =
    PlaybackState.Builder()
      .setActions(
        PlaybackState.ACTION_PLAY or
          PlaybackState.ACTION_PAUSE or
          PlaybackState.ACTION_SKIP_TO_NEXT or
          PlaybackState.ACTION_SKIP_TO_PREVIOUS
      )
      .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
      .build()

  private fun refreshSessionPriority(
    transitionState: Int = PlaybackState.STATE_SKIPPING_TO_NEXT
  ) {
    if (!enabled) return
    mediaSession.setPlaybackState(buildPlaybackState(transitionState))
    handler.postDelayed(
      {
        if (enabled) {
          mediaSession.setPlaybackState(buildPlaybackState(currentPlaybackState))
          mediaSession.setActive(true)
        }
      },
      200L
    )
  }

  fun release() {
    enabled = false
    cancelPendingPress()
    handler.removeCallbacksAndMessages(null)
    runCatching {
      mediaSession.setActive(false)
      mediaSession.release()
    }.onFailure {
      Timber.tag(TAG).w(it, "Failed to release screen-off volume-key session")
    }
  }
}
