package com.music.echo.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Which addon track replaces a YouTube song, on titles seen in real searches. */
@RunWith(RobolectricTestRunner::class)
class AddonMatchingTest {
  private val manager = ExtensionManager(RuntimeEnvironment.getApplication())

  private fun track(title: String, artist: String, duration: Int? = null) =
    AddonTrack(id = "$title|$artist", title = title, artist = artist, duration = duration)

  @Test
  fun videoTitleWithArtistAndLabels_matchesTheSongNotAShorterTitle() {
    val match =
      manager.selectBestMatch(
        listOf(
          track("Conta Su Di Me (feat. Caprice)", "Guè, Caprice", 221),
          track("Pequeno", "Guè", 157),
          track("Fattore Wow", "Marracash, J-AX, Guè", 196),
        ),
        title = "GUE' PEQUENO   CONTA SU DI ME OFFICIAL VIDEO",
        artist = "Guè",
        durationMs = 228_000L,
      )
    assertEquals("Conta Su Di Me (feat. Caprice)", match?.title)
  }

  @Test
  fun oneSharedWord_isAnotherSong() {
    val match =
      manager.selectBestMatch(
        listOf(track("Pequeno", "Guè", 157)),
        title = "GUE' PEQUENO   CONTA SU DI ME OFFICIAL VIDEO",
        artist = "Guè",
        durationMs = 228_000L,
      )
    assertNull(match)
  }

  @Test
  fun titleOnlySearch_needsTheWholeTitle() {
    val tracks = listOf(track("Stay", "Sam Smith"))
    assertNull(
      manager.selectBestMatch(tracks, "Stay With Me", "Sam Smith", null, requiredTitle = "Stay With Me")
    )
    assertEquals(
      "Stay With Me",
      manager
        .selectBestMatch(
          listOf(track("Stay With Me", "Sam Smith")),
          "Stay With Me",
          "Sam Smith",
          null,
          requiredTitle = "Stay With Me",
        )
        ?.title,
    )
  }

  @Test
  fun guestInTheRequest_isNotPartOfTheTitle() {
    val match =
      manager.selectBestMatch(
        listOf(track("Nel Back", "Rasty Kilo, Night Skinny, Guè")),
        title = "Nel Back (con Guè)",
        artist = "Rasty Kilo",
        durationMs = null,
      )
    assertEquals("Nel Back", match?.title)
  }

  @Test
  fun exactTitle_winsOverARemaster() {
    val match =
      manager.selectBestMatch(
        listOf(track("Time - 2023 Remaster", "Pink Floyd"), track("Time", "Pink Floyd")),
        title = "Time",
        artist = "Pink Floyd",
        durationMs = null,
      )
    assertEquals("Time", match?.title)
  }

  @Test
  fun liveTake_isNotServedForTheStudioSong() {
    val match =
      manager.selectBestMatch(
        listOf(track("Che Fantastica Storia E' La Vita - live", "Antonello Venditti")),
        title = "Che fantastica storia è la vita",
        artist = "Antonello Venditti",
        durationMs = null,
      )
    assertNull(match)
  }

  @Test
  fun sameTitle_matches() {
    val match =
      manager.selectBestMatch(
        listOf(track("Piccola stella senza cielo", "Ligabue")),
        title = "Piccola stella senza cielo",
        artist = "Ligabue",
        durationMs = null,
      )
    assertEquals("Piccola stella senza cielo", match?.title)
  }

  @Test
  fun renditionAfterAGuest_isKept() {
    val match =
      manager.selectBestMatch(
        listOf(track("Night Drive", "Artist")),
        title = "Night Drive feat. Guest - Live",
        artist = "Artist",
        durationMs = null,
      )
    assertNull(match)
  }

  @Test
  fun titleOnlySearch_ignoresTheGuest() {
    val match =
      manager.selectBestMatch(
        listOf(track("Night Drive", "Artist")),
        title = "Night Drive feat. Guest",
        artist = "Artist",
        durationMs = null,
        requiredTitle = "Night Drive feat. Guest",
      )
    assertEquals("Night Drive", match?.title)
  }

  @Test
  fun guests_tellVersionsOfATitleApart() {
    val tracks =
      listOf(
        track("Immortale (feat. Sfera Ebbasta)", "Guè, Sfera Ebbasta"),
        track("Immortale (feat. Nex Cassel)", "Guè, Nex Cassel"),
      )
    val match =
      manager.selectBestMatch(tracks, "Immortale (feat. Nex Cassel)", "Guè", durationMs = null)
    assertEquals("Immortale (feat. Nex Cassel)", match?.title)
  }

  @Test
  fun anotherGuest_isAnotherSong() {
    val match =
      manager.selectBestMatch(
        listOf(track("Immortale (feat. Sfera Ebbasta)", "Guè, Sfera Ebbasta")),
        title = "Immortale",
        artist = "Guè, Nex Cassel",
        durationMs = null,
      )
    assertNull(match)
  }

  @Test
  fun guestListedFirst_isStillTheSameSong() {
    val tracks =
      listOf(
        track("Immortale (feat. Nex Cassel)", "Guè, Nex Cassel"),
        track("Immortale (feat. Sfera Ebbasta)", "Guè, Sfera Ebbasta"),
      )
    val match =
      manager.selectBestMatch(tracks, "Immortale", "Sfera Ebbasta, Guè", durationMs = null)
    assertEquals("Immortale (feat. Sfera Ebbasta)", match?.title)
  }
}
