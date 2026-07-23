package iad1tya.echo.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Low-shelf ducking processor used only during AutoMix DJ-blend crossfades, so two full
 * basslines don't sum into mud during the overlap. Not the user's parametric EQ chain.
 *
 * The shelf filter's coefficients are fixed once at configure time and never change again —
 * changing an IIR filter's coefficients on the fly while its delay-line memory carries state
 * from the old coefficients produces audible clicks ("zipper noise"). Instead, the filter runs
 * continuously (its own signal stays perfectly smooth) and the duck amount is a linear blend
 * between the dry and fully-filtered signal, ramped smoothly per-sample toward whatever mix the
 * crossfade loop last requested.
 */
@UnstableApi
class AutomixDuckAudioProcessor(
    private val shelfFrequencyHz: Double = 150.0,
    private val maxDuckDb: Double = -10.0,
) : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    /** Target blend, 0=dry (no duck) .. 1=fully filtered (maxDuckDb). Set from the fade loop. */
    @Volatile
    private var targetMix: Float = 0f

    /** Currently applied blend; eases toward targetMix a little every sample, never jumps. */
    private var currentMix: Float = 0f

    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0
    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
        // Time constant for the per-sample mix ease, independent of sample rate.
        private const val MIX_EASE_MS = 15.0
    }

    /** Duck toward [dbFraction] of maxDuckDb, where 0=no duck and 1=full duck. */
    fun setMix(fraction: Float) {
        targetMix = fraction.coerceIn(0f, 1f)
    }

    fun resetGain() {
        targetMix = 0f
    }

    private fun computeShelfCoefficients() {
        val A = sqrt(10.0.pow(maxDuckDb / 20.0))
        val omega = 2.0 * PI * shelfFrequencyHz / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val alpha = sinOmega / 2.0 * sqrt(2.0) // S=1 shelf slope
        val sqrtA = sqrt(A)
        val aPlusOne = A + 1.0
        val aMinusOne = A - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        var rb0 = A * (aPlusOne - aMinusOne * cosOmega + twoSqrtAAlpha)
        var rb1 = 2.0 * A * (aMinusOne - aPlusOne * cosOmega)
        var rb2 = A * (aPlusOne - aMinusOne * cosOmega - twoSqrtAAlpha)
        val ra0 = aPlusOne + aMinusOne * cosOmega + twoSqrtAAlpha
        var ra1 = -2.0 * (aMinusOne + aPlusOne * cosOmega)
        var ra2 = aPlusOne + aMinusOne * cosOmega - twoSqrtAAlpha

        rb0 /= ra0; rb1 /= ra0; rb2 /= ra0; ra1 /= ra0; ra2 /= ra0
        b0 = rb0; b1 = rb1; b2 = rb2; a1 = ra1; a2 = ra2
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        computeShelfCoefficients()
        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return

        if (outputBuffer.capacity() < inputSize) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        // Per-sample ease toward targetMix: at 44.1-48kHz this settles in a few ms, fast
        // enough to track the fade loop's ~100ms updates while never stepping abruptly.
        val easePerSample = (1000.0 / (MIX_EASE_MS * sampleRate)).toFloat().coerceIn(0f, 1f)

        val sampleCount = inputSize / 2
        when (channelCount) {
            1 -> repeat(sampleCount) {
                currentMix += (targetMix - currentMix) * easePerSample
                val input = inputBuffer.getShort().toDouble() / 32768.0
                val filtered = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
                x2L = x1L; x1L = input; y2L = y1L; y1L = filtered
                val output = input + (filtered - input) * currentMix
                outputBuffer.putShort((output * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
            }
            2 -> repeat(sampleCount / 2) {
                currentMix += (targetMix - currentMix) * easePerSample
                val inL = inputBuffer.getShort().toDouble() / 32768.0
                val inR = inputBuffer.getShort().toDouble() / 32768.0

                val filteredL = b0 * inL + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
                x2L = x1L; x1L = inL; y2L = y1L; y1L = filteredL

                val filteredR = b0 * inR + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
                x2R = x1R; x1R = inR; y2R = y1R; y1R = filteredR

                val outL = inL + (filteredL - inL) * currentMix
                val outR = inR + (filteredR - inR) * currentMix

                outputBuffer.putShort((outL * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                outputBuffer.putShort((outR * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
            }
            else -> repeat(sampleCount) {
                outputBuffer.putShort(inputBuffer.getShort())
            }
        }

        outputBuffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer.remaining() == 0

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        x1L = 0.0; x2L = 0.0; y1L = 0.0; y2L = 0.0
        x1R = 0.0; x2R = 0.0; y1R = 0.0; y2R = 0.0
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        inputBuffer = EMPTY_BUFFER
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        currentMix = 0f
        targetMix = 0f
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
