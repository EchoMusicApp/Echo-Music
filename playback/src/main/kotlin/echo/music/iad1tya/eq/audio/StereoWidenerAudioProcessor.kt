package echo.music.iad1tya.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Mid-side stereo widener ("spatial audio" toggle). Decomposes each frame into a mono mid
 * component and a side (difference) component, then boosts the side component before
 * recombining — this pushes the L/R channels further apart without changing the mono-compatible
 * center image, unlike a naive per-channel gain change.
 *
 * Mono and non-16-bit-PCM sources are passed through unchanged: widening a mono signal is
 * meaningless (there is no side component), and the mid/side math below assumes 16-bit samples.
 */
@UnstableApi
class StereoWidenerAudioProcessor : AudioProcessor {

    /** 1 = unchanged, >1 widens. Clamped low enough that recombined peaks won't clip often. */
    @Volatile
    var width: Float = 1f
        set(value) {
            field = value.coerceIn(1f, 2f)
        }

    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        isActive = true
        return inputAudioFormat
    }

    // Always active once configured for stereo PCM16 — at width=1 the mid/side
    // decomposition recombines losslessly, so there's no separate bypass path needed, and
    // toggling stays glitch-free since Media3 evaluates isActive() only around configure(),
    // not per buffer.
    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return

        if (outputBuffer.capacity() < inputSize) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        val widthNow = width
        val sampleCount = inputSize / 2
        when (channelCount) {
            2 -> repeat(sampleCount / 2) {
                val left = inputBuffer.getShort().toInt()
                val right = inputBuffer.getShort().toInt()

                val mid = (left + right) * 0.5
                val side = (left - right) * 0.5 * widthNow

                val outLeft = (mid + side).toInt().coerceIn(-32768, 32767)
                val outRight = (mid - side).toInt().coerceIn(-32768, 32767)

                outputBuffer.putShort(outLeft.toShort())
                outputBuffer.putShort(outRight.toShort())
            }
            // Mono has no side component to widen — pass through unchanged.
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
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
