package echo.music.dsp.controller

import echo.music.dsp.core.models.SavedEQProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DspControllerTest {

  private lateinit var controller: DspController

  @Before
  fun setUp() {
    controller = DspControllerImpl()
  }

  @Test
  fun testInitialState() {
    assertNull(controller.activeProfile.value)
    assertFalse(controller.isEnabled.value)
    assertEquals(1.0f, controller.stereoWidth.value, 0.001f)
  }

  @Test
  fun testCreateAndReleaseProcessors() {
    val trio = controller.createProcessors()
    assertTrue(controller.isInitialized())
    assertEquals(3, controller.getAudioProcessors().size)

    controller.releaseProcessors(trio)
    assertFalse(controller.isInitialized())
  }

  @Test
  fun testApplyProfileAndDisable() {
    val trio = controller.createProcessors()
    val profile = SavedEQProfile(
      id = "test_profile",
      name = "Test Profile",
      preamp = -2.0,
      bands = emptyList()
    )

    val result = controller.applyProfile(profile)
    assertTrue(result.isSuccess)
    assertEquals("test_profile", controller.activeProfile.value?.id)
    assertTrue(controller.isEnabled.value)

    controller.disableEqualizer()
    assertNull(controller.activeProfile.value)
    assertFalse(controller.isEnabled.value)
  }

  @Test
  fun testStereoWidthUpdate() {
    val trio = controller.createProcessors()
    controller.setStereoWidth(1.6f)

    assertEquals(1.6f, controller.stereoWidth.value, 0.001f)
    assertEquals(1.6f, trio.stereoWidener.width, 0.001f)
  }
}
