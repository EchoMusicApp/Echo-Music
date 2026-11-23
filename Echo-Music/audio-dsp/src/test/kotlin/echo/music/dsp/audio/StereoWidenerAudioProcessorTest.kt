package echo.music.dsp.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class StereoWidenerAudioProcessorTest {

  private lateinit var processor: StereoWidenerAudioProcessor

  @Before
  fun setUp() {
    processor = StereoWidenerAudioProcessor()
  }

  @Test
  fun testConfigurationStereoPcm16() {
    val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    val outputFormat = processor.configure(format)

    assertEquals(44100, outputFormat.sampleRate)
    assertEquals(2, outputFormat.channelCount)
    assertEquals(C.ENCODING_PCM_16BIT, outputFormat.encoding)
    assertTrue(processor.isActive)
  }

  @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
  fun testConfigurationThrowsOnUnsupportedChannels() {
    val invalidFormat = AudioProcessor.AudioFormat(44100, 6, C.ENCODING_PCM_16BIT)
    processor.configure(invalidFormat)
  }

  @Test
  fun testWidthClamping() {
    processor.width = 0.5f
    assertEquals(1.0f, processor.width, 0.001f)

    processor.width = 1.4f
    assertEquals(1.4f, processor.width, 0.001f)

    processor.width = 3.0f
    assertEquals(2.0f, processor.width, 0.001f)
  }

  @Test
  fun testLosslessAtWidthOne() {
    val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    processor.configure(format)
    processor.width = 1.0f

    val inputBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
    val inLeft: Short = 1000
    val inRight: Short = -500
    inputBuffer.putShort(inLeft)
    inputBuffer.putShort(inRight)
    inputBuffer.putShort(inLeft)
    inputBuffer.putShort(inRight)
    inputBuffer.flip()

    processor.queueInput(inputBuffer)
    val output = processor.getOutput()

    assertEquals(8, output.remaining())
    val outL1 = output.getShort()
    val outR1 = output.getShort()
    val outL2 = output.getShort()
    val outR2 = output.getShort()

    assertEquals(inLeft, outL1)
    assertEquals(inRight, outR1)
    assertEquals(inLeft, outL2)
    assertEquals(inRight, outR2)
  }

  @Test
  fun testWideningExpandsStereoDifference() {
    val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    processor.configure(format)
    processor.width = 1.5f

    val inputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
    inputBuffer.putShort(1000.toShort())
    inputBuffer.putShort(500.toShort())
    inputBuffer.flip()

    processor.queueInput(inputBuffer)
    val output = processor.getOutput()

    val outL = output.getShort()
    val outR = output.getShort()

    // mid = (1000 + 500) * 0.5 = 750
    // side = (1000 - 500) * 0.5 * 1.5 = 375
    // outL = 750 + 375 = 1125 > 1000
    // outR = 750 - 375 = 375 < 500
    assertEquals(1125.toShort(), outL)
    assertEquals(375.toShort(), outR)
  }

  @Test
  fun testFlushAndReset() {
    val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    processor.configure(format)
    assertTrue(processor.isActive)

    @Suppress("DEPRECATION")
    processor.flush()
    assertFalse(processor.isEnded)

    processor.reset()
    assertFalse(processor.isActive)
  }
}
