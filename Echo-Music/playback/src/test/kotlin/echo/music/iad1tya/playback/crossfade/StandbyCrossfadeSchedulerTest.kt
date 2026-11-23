package echo.music.iad1tya.playback.crossfade

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class StandbyCrossfadeSchedulerTest {

  private lateinit var scheduler: StandbyCrossfadeSchedulerImpl
  private lateinit var primaryPlayer: ExoPlayer
  private lateinit var standbyPlayer: ExoPlayer

  private var standbyPlaybackState = Player.STATE_IDLE

  @Before
  fun setup() {
    scheduler = StandbyCrossfadeSchedulerImpl(
      scope = CoroutineScope(Dispatchers.Unconfined),
      preArmManager = AdaptivePreArmManager(baseLeadTimeMs = 20_000L)
    )

    primaryPlayer = Proxy.newProxyInstance(
      ExoPlayer::class.java.classLoader,
      arrayOf(ExoPlayer::class.java)
    ) { _, method, _ ->
      when (method.name) {
        "getPlaybackState" -> Player.STATE_READY
        else -> null
      }
    } as ExoPlayer

    standbyPlayer = Proxy.newProxyInstance(
      ExoPlayer::class.java.classLoader,
      arrayOf(ExoPlayer::class.java)
    ) { _, method, _ ->
      when (method.name) {
        "getPlaybackState" -> standbyPlaybackState
        else -> null
      }
    } as ExoPlayer
  }

  @Test
  fun testInitialStateIsIdle() {
    assertEquals(ArmedStatus.Idle, scheduler.armedState.value)
    assertFalse(scheduler.isCrossfading.value)
  }

  @Test
  fun testPreArmsWhenPositionWithinLeadTime() {
    val nextItem = MediaItem.Builder().setMediaId("next_123").build()
    scheduler.onQueueOrTrackChanged(primaryPlayer, nextItem, crossfadeDurationMs = 5_000L)

    var factoryCalled = false
    val factory = {
      factoryCalled = true
      standbyPlayer
    }

    // Duration is 100s, lead time is 20s -> trigger pre-arm at >= 80s
    scheduler.onPlaybackPositionTick(primaryPlayer, currentPositionMs = 70_000L, durationMs = 100_000L, playerFactory = factory)
    assertFalse(factoryCalled)

    // Advance to 82s (within 20s lead time)
    scheduler.onPlaybackPositionTick(primaryPlayer, currentPositionMs = 82_000L, durationMs = 100_000L, playerFactory = factory)
    assertTrue("Player factory should be called to pre-warm standby", factoryCalled)
    assertTrue(scheduler.armedState.value is ArmedStatus.Preparing)
  }

  @Test
  fun testTransitionsToArmedWhenStandbyPlayerIsReady() {
    val nextItem = MediaItem.Builder().setMediaId("next_123").build()
    scheduler.onQueueOrTrackChanged(primaryPlayer, nextItem, crossfadeDurationMs = 5_000L)

    standbyPlaybackState = Player.STATE_READY

    scheduler.onPlaybackPositionTick(primaryPlayer, currentPositionMs = 85_000L, durationMs = 100_000L) { standbyPlayer }

    // Notify standby readiness
    scheduler.onStandbyPlayerStateChanged(Player.STATE_READY)

    assertTrue("Status should be Armed once standby is STATE_READY", scheduler.armedState.value is ArmedStatus.Armed)
  }

  @Test
  fun testCancellationResetsToIdle() {
    val nextItem = MediaItem.Builder().setMediaId("next_123").build()
    scheduler.onQueueOrTrackChanged(primaryPlayer, nextItem, crossfadeDurationMs = 5_000L)
    scheduler.onPlaybackPositionTick(primaryPlayer, currentPositionMs = 85_000L, durationMs = 100_000L) { standbyPlayer }

    scheduler.cancelCrossfade()
    assertEquals(ArmedStatus.Idle, scheduler.armedState.value)
    assertFalse(scheduler.isCrossfading.value)
  }
}
