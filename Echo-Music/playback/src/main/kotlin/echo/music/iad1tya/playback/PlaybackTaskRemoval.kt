package echo.music.iad1tya.playback

import androidx.media3.common.Player

fun shouldStopPlaybackOnTaskRemoval(
  stopOnTaskClear: Boolean,
  playWhenReady: Boolean,
  playbackState: Int,
): Boolean =
  stopOnTaskClear || !playWhenReady ||
    playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED
