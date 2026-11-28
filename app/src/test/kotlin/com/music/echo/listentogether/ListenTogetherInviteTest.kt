package echo.music.iad1tya.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListenTogetherInviteTest {
  @Test
  fun hostInvitePrefillsTheSameRoomCode() {
    val link = ListenTogetherInvite.link("AB12CD34")
    assertEquals("https://metroserverx.meowery.eu/listen?code=AB12CD34", link)
    assertEquals("AB12CD34", ListenTogetherInvite.roomCodeFromQr(link))
  }

  @Test
  fun acceptsExistingInviteFormatsAndNormalizesCodes() {
    for (payload in
      listOf(
        " ab12cd34 ",
        "echomusic://listen?room=ab12cd34",
        "https://metroserverx.meowery.eu/listen/ab12cd34",
        "https://metroserverx.meowery.eu/listen?code=%41B12CD34",
      )) {
      assertEquals(payload, "AB12CD34", ListenTogetherInvite.roomCodeFromQr(payload))
    }
  }

  @Test
  fun rejectsUnrelatedUrlsAndMalformedInvites() {
    for (payload in
      listOf(
        "https://example.com/listen?code=AB12CD34",
        "https://metroserverx.meowery.eu/unrelated?code=AB12CD34",
        "https://metroserverx.meowery.eu/listen?code=AB12CD34&room=ZZZZZZZZ",
        "https://metroserverx.meowery.eu/listen?code=AB12",
        "https://metroserverx.meowery.eu/listen?code=%",
        "file:///listen/AB12CD34",
        "AB12-CD34",
        "ßAB12CD",
      )) {
      assertNull(payload, ListenTogetherInvite.roomCodeFromQr(payload))
    }
  }
}
