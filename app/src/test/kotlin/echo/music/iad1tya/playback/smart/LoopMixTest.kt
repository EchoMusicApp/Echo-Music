package echo.music.iad1tya.playback.smart

import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopMixTest {

  private fun levels(
    duration: Double,
    quiet: List<Pair<Double, Double>> = emptyList(),
  ): List<CurveWindow> {
    val frames = (duration * LEVEL_FRAME_RATE).roundToInt()
    val values =
      FloatArray(frames) { index ->
        val t = index / LEVEL_FRAME_RATE
        if (quiet.any { (a, b) -> t >= a && t < b }) 0f else levelFromRms(0.3).toFloat()
      }
    return listOf(CurveWindow(0.0, LEVEL_FRAME_RATE, values))
  }

  private fun analysis(
    bpm: Double,
    duration: Double,
    vocals: List<CurveWindow> = emptyList(),
    quiet: List<Pair<Double, Double>> = emptyList(),
  ) =
    TrackAnalysis(
      status = TrackAnalysis.STATUS_READY,
      duration = duration,
      bpm = bpm,
      beatInterval = 60 / bpm,
      beatConfidence = 0.8,
      downbeats = listOf(0.5),
      firstBeat = 0.5,
      vocalWindows = vocals,
      levelWindows = levels(duration, quiet),
    )

  private fun vocals(
    from: Double,
    to: Double,
    level: Float,
    clean: List<Pair<Double, Double>> = emptyList(),
  ): CurveWindow {
    val fps = 10.0
    val values =
      FloatArray(((to - from) * fps).roundToInt()) { index ->
        val t = from + index / fps
        if (clean.any { (a, b) -> t >= a && t < b }) 0.05f else level
      }
    return CurveWindow(from, fps, values)
  }

  private val clear = emptyList<LoopMix.Span>()

  private fun plan(
    out: TrackAnalysis,
    inn: TrackAnalysis,
    sungOut: List<LoopMix.Span>? = clear,
    sungIn: List<LoopMix.Span>? = clear,
  ) = LoopMix.plan(out, out.duration, inn, inn.duration, 0.0, sungOut, sungIn)

  @Test
  fun loopsWholeBarsOutgoingNearItsEndIncomingAtItsStart() {
    val plan = plan(analysis(124.0, 200.0), analysis(124.0, 180.0))
    assertNotNull(plan)
    val nonNullPlan = plan!!
    val bar = 60.0 / 124 * 4
    for (pick in listOf(nonNullPlan.out, nonNullPlan.inn)) {
      val bars = (pick.loopStart - 0.5) / bar
      assertTrue(abs(bars - bars.roundToInt()) < 1e-6)
      assertEquals(LoopMix.LOOP_BARS * bar, pick.loopEnd - pick.loopStart, 1e-6)
    }
    assertTrue(nonNullPlan.out.loopEnd <= 199.5)
    assertTrue(nonNullPlan.out.loopStart > 200 - 60)
    assertEquals(0.5, nonNullPlan.inn.loopStart, 1e-6)
  }

  @Test
  fun prefersBarsTheVocalMaskCallsInstrumental() {
    val out = analysis(120.0, 200.0, listOf(vocals(130.0, 200.0, 0.8f, listOf(150.0 to 170.0))))
    val inn = analysis(120.0, 180.0, listOf(vocals(0.0, 45.0, 0.8f, listOf(20.0 to 35.0))))
    val plan = plan(out, inn, null, null)
    assertNotNull(plan)
    val nonNullPlan = plan!!
    assertTrue(nonNullPlan.out.loopStart >= 150)
    assertTrue(nonNullPlan.out.loopEnd <= 170 + 0.8)
    assertTrue(nonNullPlan.out.vocal <= LoopMix.MAX_SUNG_SHARE)
    assertTrue(nonNullPlan.inn.loopStart >= 20)
    assertTrue(nonNullPlan.inn.loopEnd <= 35 + 0.8)
    assertEquals(LoopMix.Check.MASK, nonNullPlan.out.checkedBy)
  }

  @Test
  fun skipsAQuietFadeOutForALoopThatGrooves() {
    val out = analysis(120.0, 200.0, quiet = listOf(185.0 to 200.0))
    val plan = plan(out, analysis(120.0, 180.0))
    assertNotNull(plan)
    assertTrue(plan!!.out.loopEnd <= 185)
  }
}
