package echo.music.iad1tya.playback.smart

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionAudioAlignerCorrelationTest {

  @Test
  fun `fft correlation matches brute force over many random envelopes`() {
    val random = Random(42)
    repeat(30) { case ->
      val sourceSize = 320 + random.nextInt(400)
      val targetSize = 320 + random.nextInt(400)
      val source = FloatArray(sourceSize) { random.nextFloat() }
      val trueLag = random.nextInt(-100, 101)
      val target =
        FloatArray(targetSize) { index ->
          val sourceIndex = index - trueLag
          val base = if (sourceIndex in source.indices) source[sourceIndex] else random.nextFloat()
          (base + random.nextFloat() * 0.2f).coerceIn(0f, 1f)
        }

      val fft = VersionAudioAligner.crossCorrelate(source, target)
      val brute = bruteForce(source, target)

      assertEquals("floor differs in case $case", brute.floor, fft.floor)
      assertEquals("ceiling differs in case $case", brute.ceiling, fft.ceiling)
      assertEquals("best lag differs in case $case", brute.lagFrames, fft.lagFrames)
      assertTrue(
        "confidence differs in case $case: ${brute.confidence} vs ${fft.confidence}",
        abs(brute.confidence - fft.confidence) < 1e-5,
      )
    }
  }

  @Test
  fun `correlation finds a planted lag with high confidence`() {
    val random = Random(7)
    val source = FloatArray(800) { random.nextFloat() }
    val trueLag = 150
    val target =
      FloatArray(800) { index ->
        val sourceIndex = index - trueLag
        if (sourceIndex in source.indices) source[sourceIndex] else random.nextFloat() * 0.3f
      }

    val result = VersionAudioAligner.crossCorrelate(source, target)

    assertEquals(trueLag, result.lagFrames)
    assertTrue("expected high confidence, got ${result.confidence}", result.confidence > 0.9)
  }

  @Test
  fun `empty envelopes yield no measurable correlation`() {
    val result = VersionAudioAligner.crossCorrelate(FloatArray(0), FloatArray(500))
    assertEquals(0, result.lagFrames)
    assertEquals(0.0, result.confidence, 0.0)
  }

  private fun bruteForce(
    sourceEnv: FloatArray,
    targetEnv: FloatArray,
    hopMs: Double = 10.0,
    maxLagSec: Double = 40.0,
  ): VersionAudioAligner.Correlation {
    if (sourceEnv.isEmpty() || targetEnv.isEmpty()) {
      return VersionAudioAligner.Correlation(0, 0.0, 0, 0)
    }

    val maxLagFrames = (maxLagSec * 1000.0 / hopMs).toInt()
    val minOverlapFrames = (3.0 * 1000.0 / hopMs).toInt()

    var bestLag = 0
    var bestCorrelation = -1.0

    val minLag = max(-maxLagFrames, -(sourceEnv.size - minOverlapFrames))
    val maxLag = min(maxLagFrames, targetEnv.size - minOverlapFrames)

    for (lag in minLag..maxLag) {
      val srcStart = max(0, -lag)
      val tgtStart = max(0, lag)
      val overlapLen = min(sourceEnv.size - srcStart, targetEnv.size - tgtStart)
      if (overlapLen < minOverlapFrames) continue

      var sumSrc = 0.0
      var sumTgt = 0.0
      var sumSrcSq = 0.0
      var sumTgtSq = 0.0
      var sumProd = 0.0

      for (i in 0 until overlapLen) {
        val s = sourceEnv[srcStart + i].toDouble()
        val t = targetEnv[tgtStart + i].toDouble()
        sumSrc += s
        sumTgt += t
        sumSrcSq += s * s
        sumTgtSq += t * t
        sumProd += s * t
      }

      val num = sumProd - (sumSrc * sumTgt) / overlapLen
      val denSrc = sumSrcSq - (sumSrc * sumSrc) / overlapLen
      val denTgt = sumTgtSq - (sumTgt * sumTgt) / overlapLen

      val denom = sqrt(denSrc * denTgt)
      if (denom > 1e-6) {
        val r = num / denom
        if (r > bestCorrelation) {
          bestCorrelation = r
          bestLag = lag
        }
      }
    }

    return VersionAudioAligner.Correlation(bestLag, bestCorrelation, minLag, maxLag)
  }
}
