package echo.music.iad1tya.playback

sealed interface CutoffDecision {
  data object AllowEnd : CutoffDecision
  data class RecoverPrematureCutoff(val resumePositionMs: Long, val retryAttempt: Int) : CutoffDecision
}

class PlaybackCutoffGuard(
  private val thresholdMs: Long = 3000L,
  private val maxRetriesPerTrack: Int = 2
) {
  private var retryCount = 0
  private var currentMediaId: String? = null

  fun onTrackChanged(mediaId: String?) {
    currentMediaId = mediaId
    retryCount = 0
  }

  /**
   * Evaluates whether a STATE_ENDED transition is genuine or premature.
   * Returns CutoffDecision.AllowEnd if genuine, or CutoffDecision.RecoverPrematureCutoff if premature.
   */
  fun verifyTrackCompletion(
    currentPositionMs: Long,
    canonicalDurationMs: Long,
    playerDurationMs: Long = -1L
  ): CutoffDecision {
    // If decoder/player duration is known and positive, and the current position has reached it,
    // the stream has genuinely ended according to the media source container/demuxer.
    val safePosition = currentPositionMs.coerceAtLeast(0L)
    if (playerDurationMs > 0L) {
      val playerDiscrepancy = playerDurationMs - safePosition
      if (playerDiscrepancy <= thresholdMs) {
        return CutoffDecision.AllowEnd
      }
    }

    if (canonicalDurationMs <= 0L) return CutoffDecision.AllowEnd

    val discrepancy = canonicalDurationMs - safePosition
    return if (discrepancy > thresholdMs && retryCount < maxRetriesPerTrack) {
      retryCount++
      CutoffDecision.RecoverPrematureCutoff(
        resumePositionMs = safePosition,
        retryAttempt = retryCount
      )
    } else {
      CutoffDecision.AllowEnd
    }
  }

  fun getRetryCount(): Int = retryCount

  companion object {
    fun computeEffectiveDuration(canonicalDurationSeconds: Int?, playerDurationMs: Long): Long {
      val canonicalSeconds = canonicalDurationSeconds ?: 0
      return if (canonicalSeconds > 0) {
        canonicalSeconds * 1000L
      } else {
        if (playerDurationMs != androidx.media3.common.C.TIME_UNSET && playerDurationMs > 0L) playerDurationMs else 0L
      }
    }
  }
}
