package echo.music.iad1tya.playback.crossfade

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Computes constant acoustic power volume curves using equal-power sine/cosine.
 * Outgoing: cos(t * PI / 2)
 * Incoming: sin(t * PI / 2)
 *
 * Guaranteed: cos^2 + sin^2 = 1.0 (constant acoustic power, eliminating center volume dip).
 */
object EqualPowerCurve {

  data class Gains(val outgoingGain: Float, val incomingGain: Float)

  fun calculateGains(progress: Float): Gains {
    val safeProgress = if (progress.isFinite()) progress else 0.0f
    val clamped = safeProgress.coerceIn(0.0f, 1.0f)
    val angle = clamped * (PI.toFloat() / 2.0f)
    val outgoing = cos(angle)
    val incoming = sin(angle)
    return Gains(outgoingGain = outgoing, incomingGain = incoming)
  }
}
