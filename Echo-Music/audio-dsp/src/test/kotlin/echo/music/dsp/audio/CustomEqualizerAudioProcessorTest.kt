package echo.music.dsp.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import echo.music.dsp.core.models.FilterType
import echo.music.dsp.core.models.ParametricEQ
import echo.music.dsp.core.models.ParametricEQBand
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class CustomEqualizerAudioProcessorTest {

  private lateinit var processor: CustomEqualizerAudioProcessor

  @Before
  fun setUp() {
    processor = CustomEqualizerAudioProcessor()
  }

  @Test
  fun testBypassWhenDisabled() {
    val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    processor.configure(format)
    assertFalse(processor.isEnabled())

    val inputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
    inputBuffer.putShort(1234.toShort())
    inputBuffer.putShort((-5678).toShort())
    inputBuffer.flip()

    processor.queueInput(inputBuffer)
    val output = processor.getOutput()

    assertEquals(1234.toShort(), output.getShort())
    assertEquals((-5678).toShort(), output.getShort())
  }

  @Test
  fun testPreampGainScaling() {
    val format = AudioProcessor.AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)
    processor.configure(format)

    // 6 dB preamp is approximately 2.0x linear gain
    val eq = ParametricEQ(preamp = 6.0206, bands = emptyList())
    processor.applyProfile(eq)
    assertTrue(processor.isEnabled())

    val inputBuffer = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
    inputBuffer.putShort(1000.toShort())
    inputBuffer.flip()

    processor.queueInput(inputBuffer)
    val output = processor.getOutput()

    val outSample = output.getShort().toInt()
    // 1000 * 10^(6.0206 / 20) ~ 2000
    assertTrue("Expected ~2000, got $outSample", outSample in 1990..2010)
  }

  @Test
  fun testPendingProfileAppliedOnConfigure() {
    val eq = ParametricEQ(
      preamp = 0.0,
      bands = listOf(
        ParametricEQBand(
          frequency = 1000.0,
          gain = 3.0,
          q = 1.41,
          filterType = FilterType.PK,
          enabled = true
        )
      )
    )

    processor.applyProfile(eq)
    // Initially not configured, so not yet active
    assertFalse(processor.isEnabled())

    val format = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
    processor.configure(format)

    // After configure, pending profile should be applied
    assertTrue(processor.isEnabled())
  }

  @Test
  fun testDisableRestoresBypass() {
    val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    processor.configure(format)

    val eq = ParametricEQ(preamp = -6.0, bands = emptyList())
    processor.applyProfile(eq)
    assertTrue(processor.isEnabled())

    processor.disable()
    assertFalse(processor.isEnabled())

    val inputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
    inputBuffer.putShort(3000.toShort())
    inputBuffer.putShort(3000.toShort())
    inputBuffer.flip()

    processor.queueInput(inputBuffer)
    val output = processor.getOutput()

    assertEquals(3000.toShort(), output.getShort())
    assertEquals(3000.toShort(), output.getShort())
  }
}
