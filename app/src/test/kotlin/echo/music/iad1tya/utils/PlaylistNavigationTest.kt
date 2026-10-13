package echo.music.iad1tya.utils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistNavigationTest {
  @Test
  fun invalidOnlineIdsAreRejected() {
    listOf(null, "", " ", "\t\n", "null", " null ").forEach { id ->
      assertNull(validOnlinePlaylistId(id))
      assertNull(onlinePlaylistRoute(id))
    }
  }

  @Test
  fun normalOnlineIdsArePreserved() {
    listOf("PLabc_123-xyz", "OLAK5uy_album", "LM", "123", "NULL").forEach { id ->
      assertEquals(id, validOnlinePlaylistId(id))
      assertEquals("online_playlist/$id", onlinePlaylistRoute(id))
    }
  }

  @Test
  fun playlistUrlWithoutIdIsRejected() {
    listOf(
        "",
        "https://music.youtube.com/playlist",
        "https://music.youtube.com/playlist?other=PLabc",
        "https://music.youtube.com/playlist?list",
        "https://music.youtube.com/playlist?list=",
        "https://music.youtube.com/playlist#list=PLabc",
      )
      .forEach { assertNull(onlinePlaylistIdFromUrl(it)) }
  }

  @Test
  fun playlistUrlWithNullOrBlankIdIsRejected() {
    listOf("null", "%6Eull", "%20", "+", "%09", "%20null%20").forEach { id ->
      assertNull(onlinePlaylistIdFromUrl("https://music.youtube.com/playlist?list=$id"))
    }
  }

  @Test
  fun malformedPlaylistUrlIsRejected() {
    listOf("https://music.youtube.com/playlist?list=%", "not a URL?list=PLabc").forEach {
      assertNull(onlinePlaylistIdFromUrl(it))
    }
  }

  @Test
  fun normalPlaylistUrlPreservesCompleteId() {
    assertEquals(
      "PLabc_123-xyz",
      onlinePlaylistIdFromUrl(" https://music.youtube.com/playlist?si=share&list=PLabc_123-xyz ")
    )
    assertEquals(
      "PLabc_123-xyz",
      onlinePlaylistIdFromUrl("https://www.youtube.com/playlist?list=PLabc_123-xyz&si=share")
    )
  }

  @Test
  fun encodedPlaylistIdIsDecodedOnceAndSafelyEncodedInRoute() {
    val id = "PLabc/part?x=1&y=2#fragment +%2F"
    val url =
      "https://music.youtube.com/playlist?list=PLabc%2Fpart%3Fx%3D1%26y%3D2%23fragment%20%2B%252F"
    assertEquals(id, onlinePlaylistIdFromUrl(url))
    val route = onlinePlaylistRoute(onlinePlaylistIdFromUrl(url))!!
    assertEquals(
      "online_playlist/PLabc%2Fpart%3Fx%3D1%26y%3D2%23fragment%20%2B%252F",
      route
    )
    assertEquals(id, URLDecoder.decode(route.substringAfter('/'), StandardCharsets.UTF_8.name()))
  }

  @Test
  fun invalidRemoteBrowseIdFallsBackToLocalPlaylist() {
    listOf(null, "", " ", "null").forEach { browseId ->
      listOf(null, 0, 10).forEach { remoteSongCount ->
        assertEquals(
          "local_playlist/LPabcdefgh",
          libraryPlaylistRoute("LPabcdefgh", browseId, false, 0, remoteSongCount)
        )
      }
    }
  }

  @Test
  fun validRemotePlaylistKeepsExistingOnlineRouting() {
    listOf(null, 10).forEach { remoteSongCount ->
      assertEquals(
        "online_playlist/PLabc_123-xyz",
        libraryPlaylistRoute("LPabcdefgh", "PLabc_123-xyz", false, 0, remoteSongCount)
      )
    }
  }

  @Test
  fun editablePopulatedAndZeroRemoteCountPlaylistsStayLocal() {
    assertEquals(
      "local_playlist/LPabcdefgh",
      libraryPlaylistRoute("LPabcdefgh", "PLabc", true, 0, 10)
    )
    assertEquals(
      "local_playlist/LPabcdefgh",
      libraryPlaylistRoute("LPabcdefgh", "PLabc", false, 1, 10)
    )
    assertEquals(
      "local_playlist/LPabcdefgh",
      libraryPlaylistRoute("LPabcdefgh", "PLabc", false, 0, 0)
    )
  }
}
