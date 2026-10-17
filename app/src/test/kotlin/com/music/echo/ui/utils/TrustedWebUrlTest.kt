package echo.music.iad1tya.ui.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedWebUrlTest {
  @Test
  fun acceptsOnlyTrustedHttpsHosts() {
    assertTrue(isTrustedHttpsUrl("https://music.youtube.com/watch", "music.youtube.com"))
    assertTrue(isTrustedHttpsUrl("https://canary.discord.com/app", "discord.com"))
    assertFalse(isTrustedHttpsUrl("http://music.youtube.com", "music.youtube.com"))
    assertFalse(isTrustedHttpsUrl("https://music.youtube.com.evil.example", "music.youtube.com"))
    assertFalse(isTrustedHttpsUrl("https://evildiscord.com/app", "discord.com"))
  }
}
