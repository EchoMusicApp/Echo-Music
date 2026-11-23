package echo.music.iad1tya.playback

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTaskRemovalTest {
  @Test
  fun clearingTheTaskKeepsRequestedBackgroundPlaybackDuringBuffering() {
    assertFalse(shouldStopPlaybackOnTaskRemoval(false, true, Player.STATE_BUFFERING))
    assertFalse(shouldStopPlaybackOnTaskRemoval(false, true, Player.STATE_READY))
  }

  @Test
  fun stopOnTaskClearStopsPlayingAndBufferingTracks() {
    assertTrue(shouldStopPlaybackOnTaskRemoval(true, true, Player.STATE_BUFFERING))
    assertTrue(shouldStopPlaybackOnTaskRemoval(true, true, Player.STATE_READY))
  }

  @Test
  fun inactivePlaybackDoesNotKeepAServiceAlive() {
    assertTrue(shouldStopPlaybackOnTaskRemoval(false, false, Player.STATE_READY))
    assertTrue(shouldStopPlaybackOnTaskRemoval(false, true, Player.STATE_IDLE))
    assertTrue(shouldStopPlaybackOnTaskRemoval(false, true, Player.STATE_ENDED))
  }
}
