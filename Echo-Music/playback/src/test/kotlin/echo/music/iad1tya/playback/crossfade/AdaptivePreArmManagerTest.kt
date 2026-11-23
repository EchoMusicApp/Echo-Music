package echo.music.iad1tya.playback.crossfade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePreArmManagerTest {

  @Test
  fun testInitialLeadTime() {
    val manager = AdaptivePreArmManager(baseLeadTimeMs = 20_000L, maxLeadTimeMs = 35_000L)
    assertEquals(20_000L, manager.currentLeadTimeMs)
  }

  @Test
  fun testExpandsLeadTimeOnSlowPreparation() {
    val manager = AdaptivePreArmManager(baseLeadTimeMs = 20_000L, maxLeadTimeMs = 35_000L)
    // Record a slow prep of 9 seconds
    manager.recordPreparationDuration(9_000L)
    assertTrue("Lead time should expand when prep takes 9s", manager.currentLeadTimeMs > 20_000L)

    // Record another slow prep
    manager.recordPreparationDuration(12_000L)
    assertTrue("Lead time should expand further", manager.currentLeadTimeMs >= 25_000L)
  }

  @Test
  fun testCapsAtMaxLeadTime() {
    val manager = AdaptivePreArmManager(baseLeadTimeMs = 20_000L, maxLeadTimeMs = 35_000L)
    manager.recordPreparationDuration(30_000L)
    manager.recordPreparationDuration(30_000L)
    assertEquals(35_000L, manager.currentLeadTimeMs)
  }

  @Test
  fun testGraduallyContractsOnFastPreparation() {
    val manager = AdaptivePreArmManager(baseLeadTimeMs = 20_000L, maxLeadTimeMs = 35_000L)
    manager.recordPreparationDuration(10_000L)
    val expanded = manager.currentLeadTimeMs

    // Subsequent very fast preps
    manager.recordPreparationDuration(1_000L)
    manager.recordPreparationDuration(1_000L)
    manager.recordPreparationDuration(1_000L)
    assertTrue("Lead time should contract towards base", manager.currentLeadTimeMs < expanded)
  }
}
