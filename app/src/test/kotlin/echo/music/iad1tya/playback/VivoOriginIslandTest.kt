package echo.music.iad1tya.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VivoOriginIslandTest {

  @Test
  fun testProtocolConstants() {
    assertEquals("notification.superx.operation", VivoOriginIsland.EXTRA_SUPERX_OPERATION)
    assertEquals("notification.superx.showNotify", VivoOriginIsland.EXTRA_SUPERX_SHOW_NOTIFY)
    assertEquals("notification.superx.template", VivoOriginIsland.EXTRA_SUPERX_TEMPLATE)
    assertEquals("notification.superx.scene", VivoOriginIsland.EXTRA_SUPERX_SCENE)
    assertEquals("notification.superx.clickResp", VivoOriginIsland.EXTRA_SUPERX_CLICK_RESP)
    assertEquals("notification.superx.baseInfos", VivoOriginIsland.EXTRA_SUPERX_BASE_INFOS)

    assertEquals("notification.superx.baseInfos.title", VivoOriginIsland.EXTRA_BASE_INFO_TITLE)
    assertEquals("notification.superx.baseInfos.content", VivoOriginIsland.EXTRA_BASE_INFO_CONTENT)
    assertEquals("notification.superx.baseInfos.icon", VivoOriginIsland.EXTRA_BASE_INFO_ICON)

    assertEquals(0, VivoOriginIsland.OPERATION_CREATE)
    assertEquals(1, VivoOriginIsland.OPERATION_UPDATE)
    assertEquals(2, VivoOriginIsland.OPERATION_END)

    assertEquals(1, VivoOriginIsland.TEMPLATE_CAPSULE)
    assertEquals("MEDIA_PLAYBACK", VivoOriginIsland.SCENE_MEDIA_PLAYBACK)
  }

  @Test
  fun testReset() {
    VivoOriginIsland.isEnabled = false
    VivoOriginIsland.reset()
    assertEquals(VivoOriginIsland.isVivoDevice(), VivoOriginIsland.isEnabled)
  }

  @Test
  fun testToggleEnabled() {
    VivoOriginIsland.isEnabled = true
    assertTrue(VivoOriginIsland.isEnabled)
    VivoOriginIsland.isEnabled = false
    assertFalse(VivoOriginIsland.isEnabled)
  }
}
