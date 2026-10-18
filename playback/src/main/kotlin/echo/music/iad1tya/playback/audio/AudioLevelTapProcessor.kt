package echo.music.iad1tya.playback.audio

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Pass-through processor: the audio is copied unchanged, so playback is never interrupted or altered.
 * Along the way it measures loudness, three frequency bands and a simple bass-onset (beat) flag
 * with cheap one-pole filters (no FFT, no microphone/RECORD_AUDIO permission).
 */
@UnstableApi
@Suppress("DEPRECATION")
class AudioLevelTapProcessor : AudioProcessor {
  private var sampleRate = 0
  private var channelCount = 0
  private var active = false
  private var outputBuffer: ByteBuffer = EMPTY_BUFFER
  private var inputEnded = false

  // filter state
  private var lp200 = 0f
  private var lp2500 = 0f
  private var a200 = 0.03f
  private var a2500 = 0.3f

  // smoothed band values and beat tracking
  private var sLevel = 0f
  private var sBass = 0f
  private var sMid = 0f
  private var sTreble = 0f
  private var bassAvg = 0f
  private var lastBeatMs = 0L

  override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    sampleRate = inputAudioFormat.sampleRate
    channelCount = inputAudioFormat.channelCount
    // Only 16-bit PCM is analysed; any other format just passes through untouched.
    active = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && channelCount > 0 && sampleRate > 0
    if (active) {
      a200 = onePole(200f, sampleRate)
      a2500 = onePole(2500f, sampleRate)
    }
    return inputAudioFormat
  }

  private fun onePole(cutoffHz: Float, rate: Int): Float {
    val x = (2.0 * Math.PI * cutoffHz / rate).toFloat()
    return x / (x + 1f)
  }

  override fun isActive(): Boolean = true

  override fun queueInput(inputBuffer: ByteBuffer) {
    val size = inputBuffer.remaining()
    if (size == 0) {
      outputBuffer = EMPTY_BUFFER
      return
    }
    if (active) analyse(inputBuffer)
    val out = replaceOutputBuffer(size)
    out.put(inputBuffer)
    out.flip()
  }

  private fun analyse(buf: ByteBuffer) {
    val order = buf.order()
    buf.order(ByteOrder.LITTLE_ENDIAN)
    val base = buf.position()
    val frames = buf.remaining() / 2 / channelCount
    if (frames <= 0) {
      buf.order(order)
      return
    }
    var sumAll = 0f
    var sumBass = 0f
    var sumMid = 0f
    var sumTre = 0f
    var l200 = lp200
    var l2500 = lp2500
    var i = 0
    // Look at every 2nd frame at most — plenty for a visualiser and keeps the audio thread light.
    val step = if (frames > 1024) 2 else 1
    var counted = 0
    while (i < frames) {
      var mono = 0f
      var c = 0
      while (c < channelCount) {
        mono += buf.getShort(base + (i * channelCount + c) * 2) / 32768f
        c++
      }
      mono /= channelCount
      l200 += a200 * (mono - l200)
      l2500 += a2500 * (mono - l2500)
      val b = l200
      val m = l2500 - l200
      val t = mono - l2500
      sumAll += mono * mono
      sumBass += b * b
      sumMid += m * m
      sumTre += t * t
      counted++
      i += step
    }
    lp200 = l200
    lp2500 = l2500
    buf.order(order)
    if (counted == 0) return

    val rmsAll = sqrt(sumAll / counted)
    val rmsBass = sqrt(sumBass / counted)
    val rmsMid = sqrt(sumMid / counted)
    val rmsTre = sqrt(sumTre / counted)

    // Gentle compression so quiet and loud tracks both animate.
    val tLevel = comp(rmsAll * 3.2f)
    val tBass = comp(rmsBass * 4.0f)
    val tMid = comp(rmsMid * 5.0f)
    val tTre = comp(rmsTre * 9.0f)

    sLevel = follow(sLevel, tLevel)
    sBass = follow(sBass, tBass)
    sMid = follow(sMid, tMid)
    sTreble = follow(sTreble, tTre)

    val now = SystemClock.uptimeMillis()
    // Beat: bass jumps clearly above its own recent average, with a minimum gap of 230 ms.
    bassAvg += (tBass - bassAvg) * 0.02f
    if (tBass > 0.16f && tBass > bassAvg * 1.35f + 0.04f && sBass < tBass && now - lastBeatMs > 230) {
      lastBeatMs = now
      LiveAudioLevels.beatCount = LiveAudioLevels.beatCount + 1
    }

    LiveAudioLevels.level = sLevel
    LiveAudioLevels.bass = sBass
    LiveAudioLevels.mid = sMid
    LiveAudioLevels.treble = sTreble
    LiveAudioLevels.lastUpdateMs = now
  }

  private fun comp(x: Float): Float {
    val v = x.coerceIn(0f, 1.6f)
    return (v / (1f + 0.35f * v)).coerceIn(0f, 1f)
  }

  private fun follow(cur: Float, target: Float): Float =
    if (target > cur) cur + (target - cur) * 0.6f else cur + (target - cur) * 0.12f

  override fun queueEndOfStream() {
    inputEnded = true
  }

  override fun getOutput(): ByteBuffer {
    val o = outputBuffer
    outputBuffer = EMPTY_BUFFER
    return o
  }

  override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

  @Deprecated("Deprecated in AudioProcessor")
  override fun flush() {
    outputBuffer = EMPTY_BUFFER
    inputEnded = false
    lp200 = 0f
    lp2500 = 0f
    sLevel = 0f; sBass = 0f; sMid = 0f; sTreble = 0f
    LiveAudioLevels.clear()
  }

  @Deprecated("Deprecated in AudioProcessor")
  override fun reset() {
    flush()
    sampleRate = 0
    channelCount = 0
    active = false
  }

  private fun replaceOutputBuffer(size: Int): ByteBuffer {
    if (outputBuffer.capacity() < size) {
      outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
    } else {
      outputBuffer.clear()
    }
    return outputBuffer
  }

  companion object {
    private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
  }
}
