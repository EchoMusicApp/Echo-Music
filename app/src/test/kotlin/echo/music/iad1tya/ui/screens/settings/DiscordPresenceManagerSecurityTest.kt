package echo.music.iad1tya.ui.screens.settings

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiscordPresenceManagerSecurityTest {

  @Test
  fun legacyTokenCannotStartDiscordPresenceOrUpdateIt() = runTest {
    val context = RuntimeEnvironment.getApplication()
    val token = "legacy-user-token"

    DiscordPresenceManager.start(
      context = context,
      token = token,
      songProvider = { null },
      positionProvider = { 0L },
      isPausedProvider = { true },
    )

    assertFalse(DiscordPresenceManager.isRunning())
    assertFalse(
      DiscordPresenceManager.updateNow(
        context = context,
        token = token,
        song = null,
        positionMs = 0L,
        isPaused = true,
      ),
    )
    assertFalse(DiscordPresenceManager.isRunning())
  }
}
