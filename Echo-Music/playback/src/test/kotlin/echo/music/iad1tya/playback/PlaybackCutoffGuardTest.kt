package echo.music.iad1tya.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackCutoffGuardTest {

  private lateinit var guard: PlaybackCutoffGuard

  @Before
  fun setUp() {
    guard = PlaybackCutoffGuard(thresholdMs = 3000L, maxRetriesPerTrack = 2)
  }

  @Test
  fun testAllowEndWhenDiscrepancyIsSmall() {
    guard.onTrackChanged("song_1")
    // Canonical 200,000ms (3m20s), current position 198,500ms (1.5s difference <= 3000ms)
    val decision = guard.verifyTrackCompletion(currentPositionMs = 198500L, canonicalDurationMs = 200000L)
    assertEquals(CutoffDecision.AllowEnd, decision)
  }

  @Test
  fun testAllowEndWhenPlayerDurationMatchesCurrentPosition() {
    guard.onTrackChanged("song_with_longer_metadata")
    // Metadata says 210,000ms (e.g. YouTube video length or metadata inaccuracy),
    // but actual audio stream decoder duration is 195,000ms, and current position is 194,000ms (1s difference <= 3000ms).
    val decision = guard.verifyTrackCompletion(
      currentPositionMs = 194000L,
      canonicalDurationMs = 210000L,
      playerDurationMs = 195000L
    )
    assertEquals(CutoffDecision.AllowEnd, decision)
  }

  @Test
  fun testAllowEndWhenDurationUnknownOrNonPositive() {
    guard.onTrackChanged("song_live")
    assertEquals(CutoffDecision.AllowEnd, guard.verifyTrackCompletion(1000L, -1L))
    assertEquals(CutoffDecision.AllowEnd, guard.verifyTrackCompletion(1000L, 0L))
  }

  @Test
  fun testRecoverPrematureCutoffWhenDiscrepancyLarge() {
    guard.onTrackChanged("song_2")
    // Canonical 200,000ms, current position 150,000ms (50s discrepancy > 3000ms)
    val decision = guard.verifyTrackCompletion(currentPositionMs = 150000L, canonicalDurationMs = 200000L)
    assertTrue(decision is CutoffDecision.RecoverPrematureCutoff)
    val recovery = decision as CutoffDecision.RecoverPrematureCutoff
    assertEquals(150000L, recovery.resumePositionMs)
    assertEquals(1, recovery.retryAttempt)
  }

  @Test
  fun testRetryBudgetExhaustionCapsAtMaxRetries() {
    guard.onTrackChanged("song_broken")
    // First cutoff
    val first = guard.verifyTrackCompletion(100000L, 200000L)
    assertTrue(first is CutoffDecision.RecoverPrematureCutoff)
    assertEquals(1, (first as CutoffDecision.RecoverPrematureCutoff).retryAttempt)

    // Second cutoff
    val second = guard.verifyTrackCompletion(120000L, 200000L)
    assertTrue(second is CutoffDecision.RecoverPrematureCutoff)
    assertEquals(2, (second as CutoffDecision.RecoverPrematureCutoff).retryAttempt)

    // Third cutoff: maxRetriesPerTrack (2) reached, must allow end to avoid infinite loop
    val third = guard.verifyTrackCompletion(125000L, 200000L)
    assertEquals(CutoffDecision.AllowEnd, third)
  }

  @Test
  fun testTrackChangeResetsRetryCount() {
    guard.onTrackChanged("song_1")
    guard.verifyTrackCompletion(100000L, 200000L)
    guard.verifyTrackCompletion(100000L, 200000L)

    // New track
    guard.onTrackChanged("song_2")
    val decision = guard.verifyTrackCompletion(100000L, 200000L)
    assertTrue(decision is CutoffDecision.RecoverPrematureCutoff)
    assertEquals(1, (decision as CutoffDecision.RecoverPrematureCutoff).retryAttempt)
  }
}
