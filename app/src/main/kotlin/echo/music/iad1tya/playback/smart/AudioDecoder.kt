package echo.music.iad1tya.playback.smart

import android.content.Context
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/** Decodes a region of an audio track to mono/stereo float PCM using the platform MediaCodec. */
object AudioDecoder {

  private const val TAG = "AudioDecoder"
  private const val TIMEOUT_US = 10_000L

  /** Decoded mono PCM at the container's own sample rate. */
  data class Pcm(val samples: FloatArray, val sampleRate: Double)

  /** Decoded planar stereo PCM at the container's own sample rate. */
  data class StereoPcm(val left: FloatArray, val right: FloatArray, val sampleRate: Double)

  fun decodeRegion(
    source: MediaDataSource,
    startSeconds: Double,
    endSeconds: Double,
    abort: () -> Boolean = { false },
  ): Pair<Pcm, Double>? {
    val chunks = ArrayList<FloatArray>()
    val decoded =
      decodeRaw(
        { extractor -> extractor.setDataSource(source) },
        startSeconds,
        endSeconds,
        abort,
      ) { buffer, info, channels, _ ->
        chunks += toMono(buffer, info, channels)
      } ?: return null
    return Pcm(flatten(chunks), decoded.first) to decoded.second
  }

  fun decodeRegion(
    context: Context,
    uri: Uri,
    startSeconds: Double,
    endSeconds: Double,
    abort: () -> Boolean = { false },
  ): Pair<Pcm, Double>? {
    val chunks = ArrayList<FloatArray>()
    val decoded =
      decodeRaw(
        { extractor -> extractor.setDataSource(context, uri, null) },
        startSeconds,
        endSeconds,
        abort,
      ) { buffer, info, channels, _ ->
        chunks += toMono(buffer, info, channels)
      } ?: return null
    return Pcm(flatten(chunks), decoded.first) to decoded.second
  }

  fun decodeRegion(
    fd: FileDescriptor,
    startSeconds: Double,
    endSeconds: Double,
    abort: () -> Boolean = { false },
  ): Pair<Pcm, Double>? {
    val chunks = ArrayList<FloatArray>()
    val decoded =
      decodeRaw({ extractor -> extractor.setDataSource(fd) }, startSeconds, endSeconds, abort) {
        buffer,
        info,
        channels,
        _ ->
        chunks += toMono(buffer, info, channels)
      } ?: return null
    return Pcm(flatten(chunks), decoded.first) to decoded.second
  }

  fun decodeRegionStereo(
    source: MediaDataSource,
    startSeconds: Double,
    endSeconds: Double,
    abort: () -> Boolean = { false },
  ): Pair<StereoPcm, Double>? {
    val left = ArrayList<FloatArray>()
    val right = ArrayList<FloatArray>()
    val decoded =
      decodeRaw(
        { extractor -> extractor.setDataSource(source) },
        startSeconds,
        endSeconds,
        abort,
      ) { buffer, info, channels, _ ->
        toStereo(buffer, info, channels, left, right)
      } ?: return null
    return StereoPcm(flatten(left), flatten(right), decoded.first) to decoded.second
  }

  private fun flatten(chunks: List<FloatArray>): FloatArray {
    val samples = FloatArray(chunks.sumOf { it.size })
    var offset = 0
    for (chunk in chunks) {
      chunk.copyInto(samples, offset)
      offset += chunk.size
    }
    return samples
  }

  private fun decodeRaw(
    setSource: (MediaExtractor) -> Unit,
    startSeconds: Double,
    endSeconds: Double,
    abort: () -> Boolean,
    onBuffer: (ByteBuffer, MediaCodec.BufferInfo, Int, Int) -> Unit,
  ): Pair<Double, Double>? {
    if (endSeconds <= startSeconds) return null
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
      setSource(extractor)
      val trackIndex =
        (0 until extractor.trackCount).firstOrNull { index ->
          extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") ==
            true
        } ?: return null
      extractor.selectTrack(trackIndex)
      val format = extractor.getTrackFormat(trackIndex)
      val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

      val startUs = (startSeconds * 1_000_000).toLong()
      val endUs = (endSeconds * 1_000_000).toLong()
      extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

      codec =
        runCatching { MediaCodec.createDecoderByType(mime) }
          .onFailure { Log.w(TAG, "No decoder for $mime", it) }
          .getOrNull() ?: return null
      codec.configure(format, null, null, 0)
      codec.start()

      val bufferInfo = MediaCodec.BufferInfo()
      var outputChannels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
      var outputRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
      var actualStartSeconds = -1.0
      var sawFirstSample = false
      var inputDone = false
      var outputDone = false

      while (!outputDone) {
        if (abort()) return null
        var progressed = false
        while (!inputDone) {
          val inputIndex = codec.dequeueInputBuffer(0L)
          if (inputIndex < 0) break
          val inputBuffer = codec.getInputBuffer(inputIndex) ?: break
          progressed = true
          val sampleSize = extractor.readSampleData(inputBuffer, 0)
          val sampleTimeUs = extractor.sampleTime
          if (sampleSize < 0 || (sampleTimeUs in 0..Long.MAX_VALUE && sampleTimeUs > endUs)) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            inputDone = true
          } else {
            codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
            extractor.advance()
          }
        }

        var timeout = if (progressed) 0L else TIMEOUT_US
        while (!outputDone) {
          val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
          if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
          timeout = 0L
          when (outputIndex) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
              val newFormat = codec.outputFormat
              outputRate = newFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputRate
              outputChannels = newFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannels
            }
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
            else ->
              if (outputIndex >= 0) {
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                  outputDone = true
                }
                if (bufferInfo.size > 0) {
                  if (!sawFirstSample) {
                    actualStartSeconds = bufferInfo.presentationTimeUs / 1_000_000.0
                    sawFirstSample = true
                  }
                  codec.getOutputBuffer(outputIndex)?.let { output ->
                    onBuffer(output, bufferInfo, outputChannels, outputRate)
                  }
                  if (bufferInfo.presentationTimeUs > endUs) outputDone = true
                }
                codec.releaseOutputBuffer(outputIndex, false)
              }
          }
        }
      }

      if (!sawFirstSample || outputRate <= 0) return null
      return outputRate.toDouble() to actualStartSeconds
    } catch (error: Exception) {
      Log.w(TAG, "Region decode failed", error)
      return null
    } finally {
      runCatching { codec?.stop() }
      runCatching { codec?.release() }
      runCatching { extractor.release() }
    }
  }

  private fun toMono(buffer: ByteBuffer, info: MediaCodec.BufferInfo, channels: Int): FloatArray {
    val safeChannels = max(1, channels)
    val shorts =
      buffer
        .duplicate()
        .apply {
          order(ByteOrder.LITTLE_ENDIAN)
          position(info.offset)
          limit(info.offset + info.size)
        }
        .asShortBuffer()
    val frames = shorts.remaining() / safeChannels
    val mono = FloatArray(frames)
    val frame = ShortArray(safeChannels)
    for (index in 0 until frames) {
      shorts.get(frame, 0, safeChannels)
      var sum = 0
      for (value in frame) sum += value
      mono[index] = (sum / safeChannels.toFloat()) / 32768f
    }
    return mono
  }

  private fun toStereo(
    buffer: ByteBuffer,
    info: MediaCodec.BufferInfo,
    channels: Int,
    left: MutableList<FloatArray>,
    right: MutableList<FloatArray>,
  ) {
    val safeChannels = max(1, channels)
    val shorts =
      buffer
        .duplicate()
        .apply {
          order(ByteOrder.LITTLE_ENDIAN)
          position(info.offset)
          limit(info.offset + info.size)
        }
        .asShortBuffer()
    val frames = shorts.remaining() / safeChannels
    val leftChunk = FloatArray(frames)
    val rightChunk = FloatArray(frames)
    val frame = ShortArray(safeChannels)
    for (index in 0 until frames) {
      shorts.get(frame, 0, safeChannels)
      leftChunk[index] = frame[0] / 32768f
      rightChunk[index] = (if (safeChannels > 1) frame[1] else frame[0]) / 32768f
    }
    left += leftChunk
    right += rightChunk
  }

  private fun MediaFormat.intOrNull(key: String): Int? =
    if (containsKey(key)) getInteger(key) else null
}
