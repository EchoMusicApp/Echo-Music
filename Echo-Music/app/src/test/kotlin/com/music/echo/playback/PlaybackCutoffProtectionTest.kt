package com.music.echo.playback

import androidx.media3.common.C
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.CutoffDecision
import echo.music.iad1tya.playback.PlaybackCutoffGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCutoffProtectionTest {

  @Test
  fun testCutoffGuardInterceptionDecision() {
    val guard = PlaybackCutoffGuard(thresholdMs = 3000L, maxRetriesPerTrack = 2)
    val mediaId = "track_123"
    guard.onTrackChanged(mediaId)

    val canonicalDurationSeconds = 210 // 210s = 210,000ms
    val canonicalDurationMs = canonicalDurationSeconds * 1000L

    // Case 1: Premature cutoff at 150,000ms (discrepancy = 60,000ms > 3000ms)
    val cutoffDecision = guard.verifyTrackCompletion(
      currentPositionMs = 150000L,
      canonicalDurationMs = canonicalDurationMs
    )
    assertTrue("Should be RecoverPrematureCutoff", cutoffDecision is CutoffDecision.RecoverPrematureCutoff)
    val recovery = cutoffDecision as CutoffDecision.RecoverPrematureCutoff
    assertEquals(150000L, recovery.resumePositionMs)
    assertEquals(1, recovery.retryAttempt)

    // Case 2: Genuine end at 208,500ms (discrepancy = 1500ms <= 3000ms)
    val genuineDecision = guard.verifyTrackCompletion(
      currentPositionMs = 208500L,
      canonicalDurationMs = canonicalDurationMs
    )
    assertEquals(CutoffDecision.AllowEnd, genuineDecision)
  }

  @Test
  fun testCanonicalDurationPrecedence() {
    val metadata = MediaMetadata(
      id = "test_song",
      title = "Test Song",
      artists = emptyList(),
      duration = 245 // 245 seconds
    )

    // When player decoder reports C.TIME_UNSET (-1), canonical metadata provides 245,000ms
    assertEquals(245000L, PlaybackCutoffGuard.computeEffectiveDuration(metadata.duration, C.TIME_UNSET))

    // When player decoder reports truncated 15,000ms during initial buffer, canonical duration is preserved
    assertEquals(245000L, PlaybackCutoffGuard.computeEffectiveDuration(metadata.duration, 15000L))

    // When metadata has duration 0 (e.g. live stream), player duration is used
    val liveMetadata = metadata.copy(duration = 0)
    assertEquals(180000L, PlaybackCutoffGuard.computeEffectiveDuration(liveMetadata.duration, 180000L))
  }
}
