package echo.music.iad1tya.playback

import echo.music.iad1tya.constants.EnableDynamicIslandKey
import echo.music.iad1tya.constants.EnableOriginIslandKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicIslandManagerTest {

  @Test
  fun testPreferenceKeys() {
    assertEquals("enableDynamicIsland", EnableDynamicIslandKey.name)
    assertEquals("enableOriginIsland", EnableOriginIslandKey.name)
  }
}
