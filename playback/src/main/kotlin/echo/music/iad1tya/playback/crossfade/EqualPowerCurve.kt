package echo.music.iad1tya.playback.crossfade

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Constant-power gains for a two-track crossfade.
 *
 * At every progress point, outgoingGain² + incomingGain² is approximately 1,
 * avoiding the perceived loudness dip of a linear crossfade.
 */
object EqualPowerCurve {
  data class Gains(val outgoingGain: Float, val incomingGain: Float)

  fun calculateGains(progress: Float): Gains {
    val safeProgress = if (progress.isFinite()) progress else 0f
    val angle = safeProgress.coerceIn(0f, 1f) * (PI.toFloat() / 2f)
    return Gains(outgoingGain = cos(angle), incomingGain = sin(angle))
  }
}
