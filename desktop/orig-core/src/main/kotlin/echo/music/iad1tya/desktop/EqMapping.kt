package echo.music.iad1tya.desktop

import echo.music.dsp.core.models.FilterType
import echo.music.dsp.core.models.ParametricEQ
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * VLC's equalizer has 10 fixed bands; the app's equalizer is parametric (up to 20 bands with frequency, gain, Q, type).
 * We add up the response of every band at VLC's 10 centre frequencies and give VLC those gains.
 * shortcut: a sharp narrow cut between two VLC bands is smoothed out; VLC limits gains to +-20 dB.
 */
object EqMapping {
  val VLC_FREQUENCIES = doubleArrayOf(60.0, 170.0, 310.0, 600.0, 1000.0, 3000.0, 6000.0, 12000.0, 14000.0, 16000.0)
  private const val FS = 48000.0

  /** (preamp dB, gain dB for each of the 10 VLC bands) */
  fun toVlc(eq: ParametricEQ): Pair<Float, FloatArray> {
    val gains = FloatArray(VLC_FREQUENCIES.size) { i ->
      eq.bands.filter { it.enabled }.sumOf { responseDb(it.filterType, it.frequency, it.gain, it.q, VLC_FREQUENCIES[i]) }.coerceIn(-20.0, 20.0).toFloat()
    }
    return eq.preamp.coerceIn(-20.0, 20.0).toFloat() to gains
  }

  /** Magnitude (dB) at frequency f of one RBJ-cookbook biquad. */
  internal fun responseDb(type: FilterType, f0: Double, gainDb: Double, q: Double, f: Double): Double {
    val a = 10.0.pow(gainDb / 40.0)
    val w0 = 2 * PI * f0 / FS
    val cw = cos(w0)
    val alpha = sin(w0) / (2 * q.coerceAtLeast(0.05))
    val s = 2 * sqrt(a) * alpha
    val b0: Double; val b1: Double; val b2: Double; val a0: Double; val a1: Double; val a2: Double
    when (type) {
      FilterType.PK -> { b0 = 1 + alpha * a; b1 = -2 * cw; b2 = 1 - alpha * a; a0 = 1 + alpha / a; a1 = -2 * cw; a2 = 1 - alpha / a }
      FilterType.LSC -> {
        b0 = a * ((a + 1) - (a - 1) * cw + s); b1 = 2 * a * ((a - 1) - (a + 1) * cw); b2 = a * ((a + 1) - (a - 1) * cw - s)
        a0 = (a + 1) + (a - 1) * cw + s; a1 = -2 * ((a - 1) + (a + 1) * cw); a2 = (a + 1) + (a - 1) * cw - s
      }
      FilterType.HSC -> {
        b0 = a * ((a + 1) + (a - 1) * cw + s); b1 = -2 * a * ((a - 1) + (a + 1) * cw); b2 = a * ((a + 1) + (a - 1) * cw - s)
        a0 = (a + 1) - (a - 1) * cw + s; a1 = 2 * ((a - 1) - (a + 1) * cw); a2 = (a + 1) - (a - 1) * cw - s
      }
      FilterType.LPQ -> { b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = b0; a0 = 1 + alpha; a1 = -2 * cw; a2 = 1 - alpha }
      FilterType.HPQ -> { b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = b0; a0 = 1 + alpha; a1 = -2 * cw; a2 = 1 - alpha }
    }
    val w = 2 * PI * f / FS
    val c1 = cos(w); val c2 = cos(2 * w)
    val num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * c1 + 2 * b0 * b2 * c2
    val den = a0 * a0 + a1 * a1 + a2 * a2 + 2 * (a0 * a1 + a1 * a2) * c1 + 2 * a0 * a2 * c2
    return 10 * log10(num / den)
  }
}
