package com.music.echo.spotify

import echo.music.iad1tya.ui.screens.intentBrowserFallbackUrl
import echo.music.iad1tya.ui.screens.isWebViewLoadableUrl
import echo.music.iad1tya.ui.screens.toSpotifyCookieOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpotifyLoginWebViewTest {

  @Test
  fun httpsAndHttpAreLoadableInWebView() {
    assertTrue("https://accounts.spotify.com/login".isWebViewLoadableUrl())
    assertTrue("http://accounts.google.com".isWebViewLoadableUrl())
    assertFalse("intent://accounts.google.com#Intent;scheme=https;end".isWebViewLoadableUrl())
    assertFalse("spotify://playlist/abc".isWebViewLoadableUrl())
  }

  @Test
  fun intentFallbackUsesBrowserUrl() {
    val intentUri =
      "intent://accounts.google.com/o/oauth2#Intent;scheme=https;" +
        "S.browser_fallback_url=https%3A%2F%2Faccounts.google.com%2Fo%2Foauth2;end"
    assertEquals(
      "https://accounts.google.com/o/oauth2",
      intentUri.intentBrowserFallbackUrl(),
    )
  }

  @Test
  fun cookieOriginOnlyAllowsSpotifyHosts() {
    assertEquals(
      "https://accounts.spotify.com",
      "https://accounts.spotify.com/login?continue=1".toSpotifyCookieOrigin(),
    )
    assertEquals(
      "https://open.spotify.com",
      "https://open.spotify.com/".toSpotifyCookieOrigin(),
    )
    assertNull("https://accounts.google.com".toSpotifyCookieOrigin())
  }
}
