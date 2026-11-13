package echo.music.iad1tya.playback.smart

import kotlin.math.log10
import kotlin.math.pow

const val VOCAL_ACTIVE_THRESHOLD = 0.25

/** Stored offline analysis for one track, in that track's own timeline seconds. */
data class TrackAnalysis(
  val status: String = "",
  val trackId: String = "",
  val duration: Double = 0.0,
  val bpm: Double = 0.0,
  val beatInterval: Double = 0.0,
  val beatConfidence: Double = 0.0,
  val downbeats: List<Double> = emptyList(),
  val phraseBoundaries: List<Double> = emptyList(),
  val firstBeat: Double = 0.0,
  val key: String = "",
  val keyConfidence: Double = 0.0,
  val audibleStartTime: Double? = null,
  val pickupTime: Double? = null,
  val introEndTime: Double = 0.0,
  val contentEndTime: Double = 0.0,
  val outroStartTime: Double = 0.0,
  val mixInTime: Double = 0.0,
  val mixOutTime: Double = 0.0,
  val mixInCandidates: List<MixCandidate> = emptyList(),
  val mixOutCandidates: List<MixCandidate> = emptyList(),
  val energyCurve: List<EnergySample> = emptyList(),
  val lowEnergyCurve: List<EnergySample> = emptyList(),
  val vocalActivityMask: List<Double> = emptyList(),
  val vocalProbability: Double = 0.0,
  val headBeats: BeatSide? = null,
  val tailBeats: BeatSide? = null,
  val vocalWindows: List<CurveWindow> = emptyList(),
  val levelWindows: List<CurveWindow> = emptyList(),
) {
  val isUsable: Boolean
    get() = status == STATUS_READY && bpm > 0

  companion object {
    const val STATUS_READY = "ready"
  }
}

data class EnergySample(val time: Double, val energy: Double)

data class MixCandidate(val time: Double, val score: Double, val type: String)

data class BeatSide(
  val beats: List<Double>,
  val downbeats: List<Double>,
  val bpm: Double,
  val confidence: Double,
)

const val LEVEL_FRAME_RATE = 10.0

class CurveWindow(
  val startTime: Double,
  val frameRate: Double,
  val values: FloatArray,
) {
  val endTime: Double
    get() = startTime + values.size / frameRate
}

fun levelFromRms(rms: Double): Double =
  if (rms <= 1e-3) 0.0 else ((20 * log10(rms) + 60) / 60).coerceIn(0.0, 1.0)

fun rmsFromLevel(level: Double): Double = if (level <= 0) 0.0 else 10.0.pow((level * 60 - 60) / 20)
