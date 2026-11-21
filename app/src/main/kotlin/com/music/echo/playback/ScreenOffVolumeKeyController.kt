package echo.music.iad1tya.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.VolumeProvider
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.core.content.IntentCompat
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
  private val onLongPressPreviousTrack: () -> Unit = onPreviousTrack,
) {
  companion object {
    private const val TAG = "ScreenOffVolumeKeys"
    private const val LONG_PRESS_TIMEOUT_MS = 500L

    /**
     * A VolumeProvider only receives directions, not key events, so an adjustment that is not
     * followed by a release (e.g. one issued by another app or an accessory) would otherwise
     * leave [pressedDirection] set and swallow every later press. Clear such stale presses.
     */
    private const val STALE_PRESS_TIMEOUT_MS = 5_000L
    private val PLAY_PAUSE_DOUBLE_TAP_TIMEOUT_MS =
      ViewConfiguration.getDoubleTapTimeout().toLong()
  }

  private val appContext = context.applicationContext
  private val powerManager = requireNotNull(appContext.getSystemService(PowerManager::class.java))
  private val handler = Handler(Looper.getMainLooper())

  private var enabled = false
  private var pressedDirection = 0
  private var longPressTriggered = false
  private var longPressRunnable: Runnable? = null
  private var stalePressRunnable: Runnable? = null
  private var currentPlaybackState = PlaybackState.STATE_NONE
  private var playPauseToggleGestureConsumed = false
  private var pendingPlayPauseToggle: Runnable? = null

  private val volumeProvider =
    object : VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 100, 50) {
      // MediaSession delivers this on the callback handler (the main looper), so handle it
      // inline: re-posting could let an overdue long-press timer run before the release.
      override fun onAdjustVolume(direction: Int) = handleVolumeAdjustment(direction)
    }

  private val mediaSession =
    MediaSession(appContext, "EchoMusicScreenOffVolumeKeys").apply {
      setCallback(
        object : MediaSession.Callback() {
          override fun onPlay() {
            cancelPendingPlayPauseToggle()
            onPlayRequested()
          }

          override fun onPause() {
            cancelPendingPlayPauseToggle()
            onPauseRequested()
          }

          override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
            val keyEvent =
              IntentCompat.getParcelableExtra(
                mediaButtonIntent,
                Intent.EXTRA_KEY_EVENT,
                KeyEvent::class.java,
              )
            val isPlayPauseToggle =
              keyEvent?.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyEvent?.keyCode == KeyEvent.KEYCODE_HEADSETHOOK

            if (!isPlayPauseToggle) {
              return super.onMediaButtonEvent(mediaButtonIntent)
            }

            if (keyEvent.action == KeyEvent.ACTION_UP && playPauseToggleGestureConsumed) {
              playPauseToggleGestureConsumed = false
              return true
            }

            if (keyEvent.action == KeyEvent.ACTION_DOWN && playPauseToggleGestureConsumed) {
              return true
            }

            if (
              currentPlaybackState == PlaybackState.STATE_BUFFERING ||
                currentPlaybackState == PlaybackState.STATE_PLAYING
            ) {
              if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                playPauseToggleGestureConsumed = true
                if (keyEvent.repeatCount == 0) {
                  if (pendingPlayPauseToggle != null) {
                    cancelPendingPlayPauseToggle()
                    onNextTrack()
                  } else {
                    schedulePlayPauseToggle()
                  }
                }
                return true
              }
            }

            return super.onMediaButtonEvent(mediaButtonIntent)
          }

          override fun onSkipToNext() = onNextTrack()

          override fun onSkipToPrevious() = onPreviousTrack()
        },
        handler
      )
    }

  fun update(
    playWhenReady: Boolean,
    playbackState: Int,
    refreshPriority: Boolean = false,
  ) {
    val playbackActive =
      playWhenReady &&
        (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
    if (!playWhenReady) cancelPendingPress()
    // Independent of `enabled`: a tap accepted before the screen turned on must still be
    // dropped if playback then pauses, ends or goes idle; READY/BUFFERING updates keep it.
    if (!playbackActive) cancelPendingPlayPauseToggle()

    val shouldIntercept = playbackActive && !powerManager.isInteractive
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
      playPauseToggleGestureConsumed = false
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

    // Ignore repeated adjustment callbacks generated while a key is held, but keep the
    // stale-press watchdog alive while they keep arriving.
    if (pressedDirection != 0) {
      if (direction == pressedDirection) scheduleStalePressReset()
      return
    }

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
            onLongPressPreviousTrack()
            refreshSessionPriority(PlaybackState.STATE_SKIPPING_TO_PREVIOUS)
          }
        }
      }
    longPressRunnable = runnable
    handler.postDelayed(runnable, LONG_PRESS_TIMEOUT_MS)
    scheduleStalePressReset()
  }

  private fun scheduleStalePressReset() {
    stalePressRunnable?.let(handler::removeCallbacks)
    val runnable =
      Runnable {
        stalePressRunnable = null
        if (pressedDirection != 0) {
          Timber.tag(TAG).d("No release for volume adjustment; clearing stale press")
          cancelPendingPress()
        }
      }
    stalePressRunnable = runnable
    handler.postDelayed(runnable, STALE_PRESS_TIMEOUT_MS)
  }

  private fun finishPress() {
    if (pressedDirection == 0) return
    longPressRunnable?.let(handler::removeCallbacks)
    longPressRunnable = null
    stalePressRunnable?.let(handler::removeCallbacks)
    stalePressRunnable = null

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
    stalePressRunnable?.let(handler::removeCallbacks)
    stalePressRunnable = null
    pressedDirection = 0
    longPressTriggered = false
  }

  private fun schedulePlayPauseToggle() {
    val runnable =
      Runnable {
        pendingPlayPauseToggle = null
        // Not gated on `enabled`: the tap was accepted while intercepting, and turning the
        // screen on before the double-tap timeout must not drop it.
        if (isPlaybackRequested()) {
          onPauseRequested()
        } else {
          onPlayRequested()
        }
      }
    pendingPlayPauseToggle = runnable
    handler.postDelayed(runnable, PLAY_PAUSE_DOUBLE_TAP_TIMEOUT_MS)
  }

  private fun cancelPendingPlayPauseToggle() {
    pendingPlayPauseToggle?.let(handler::removeCallbacks)
    pendingPlayPauseToggle = null
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
    cancelPendingPlayPauseToggle()
    playPauseToggleGestureConsumed = false
    handler.removeCallbacksAndMessages(null)
    runCatching {
      mediaSession.setActive(false)
      mediaSession.release()
    }.onFailure {
      Timber.tag(TAG).w(it, "Failed to release screen-off volume-key session")
    }
  }
}
