package echo.music.iad1tya.playback.crossfade

import kotlin.math.max
import kotlin.math.min

/**
 * Dynamically tracks network and media preparation duration to expand or contract
 * the pre-arm lead time window. Ensures the standby player is fully buffered and in
 * STATE_READY before the crossfade trigger instant arrives.
 */
class AdaptivePreArmManager(
  val baseLeadTimeMs: Long = 20_000L,
  val maxLeadTimeMs: Long = 35_000L,
  private val slowPrepThresholdMs: Long = 8_000L
) {
  init {
    require(baseLeadTimeMs > 0) { "baseLeadTimeMs must be positive" }
    require(slowPrepThresholdMs > 0) { "slowPrepThresholdMs must be positive" }
    require(maxLeadTimeMs >= baseLeadTimeMs) { "maxLeadTimeMs must be at least baseLeadTimeMs" }
  }

  @Volatile
  var currentLeadTimeMs: Long = baseLeadTimeMs
    private set

  private val recentPrepTimes = mutableListOf<Long>()

  @Synchronized
  fun recordPreparationDuration(durationMs: Long) {
    recentPrepTimes.add(durationMs)
    if (recentPrepTimes.size > 5) {
      recentPrepTimes.removeAt(0)
    }

    if (durationMs > slowPrepThresholdMs) {
      // Expand lead time proportionally
      val expansion = ((durationMs - slowPrepThresholdMs) * 1.5).toLong()
      currentLeadTimeMs = min(maxLeadTimeMs, max(currentLeadTimeMs, baseLeadTimeMs + expansion))
    } else {
      // Fast preparation: gradually decay towards baseLeadTimeMs
      val average = recentPrepTimes.average()
      if (average < slowPrepThresholdMs) {
        val decayed = currentLeadTimeMs - 2_000L
        currentLeadTimeMs = max(baseLeadTimeMs, decayed)
      }
    }
  }

  @Synchronized
  fun reset() {
    currentLeadTimeMs = baseLeadTimeMs
    recentPrepTimes.clear()
  }
}
