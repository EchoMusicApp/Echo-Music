package echo.music.iad1tya.playback

import androidx.media3.exoplayer.audio.AudioSink
import echo.music.usbaudio.UsbAudioDriver
import echo.music.usbaudio.UsbDacAudioSink
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class UsbAudioServiceRoutingTest {
  @Test
  fun testSelectsUsbDacAudioSinkWhenBitPerfectActive() {
    val mockUsbSink = UsbDacAudioSink(UsbAudioDriver())
    val mockDefaultSink = Proxy.newProxyInstance(
      AudioSink::class.java.classLoader,
      arrayOf(AudioSink::class.java)
    ) { _, _, _ -> null } as AudioSink

    val sink = AudioSinkSelector.selectSink(
      isBitPerfectActive = true,
      usbDacAudioSink = mockUsbSink,
      defaultAudioSink = mockDefaultSink
    )
    assertTrue(sink is UsbDacAudioSink)
  }

  @Test
  fun testRoutingSinkSwitchesDynamically() {
    val mockUsbSink = UsbDacAudioSink(UsbAudioDriver())
    val mockDefaultSink = Proxy.newProxyInstance(
      AudioSink::class.java.classLoader,
      arrayOf(AudioSink::class.java)
    ) { _, _, _ -> null } as AudioSink

    var bitPerfect = false
    val routingSink = AudioSinkSelector.createRoutingSink(
      defaultAudioSink = mockDefaultSink,
      usbDacAudioSink = mockUsbSink,
      isBitPerfectActive = { bitPerfect }
    ) as RoutingAudioSink

    org.junit.Assert.assertSame(mockDefaultSink, routingSink.activeSink)

    bitPerfect = true
    routingSink.flush()
    org.junit.Assert.assertSame(mockUsbSink, routingSink.activeSink)
  }
}
