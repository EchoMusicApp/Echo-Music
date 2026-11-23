package echo.music.iad1tya.utils

import echo.music.iad1tya.db.entities.ArtistEntity
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickPickSongsTest {
  private fun song(id: String, hidden: Boolean = false) =
    Song(SongEntity(id = id, title = id, hideFromQuickPicks = hidden), emptyList())

  @Test
  fun fallbackSongsCannotRestoreHiddenSongs() {
    val result = filterQuickPickSongs(
      listOf(song("hidden", true), song("stale"), song("visible"), song("visible")),
      setOf("stale"), emptySet(), false, false,
    )

    assertEquals(listOf("visible"), result.map { it.id })
  }

  @Test
  fun changingHiddenSongIdsRemovesSongsFromTheAlreadyLoadedFeed() {
    val loaded = listOf(song("first"), song("second"))
    val initial = filterQuickPickSongs(loaded, emptySet(), emptySet(), false, false)
    val updated = filterQuickPickSongs(loaded, setOf("first"), emptySet(), false, false)

    assertEquals(listOf("first", "second"), initial.map { it.id })
    assertEquals(listOf("second"), updated.map { it.id })
  }

  @Test
  fun blockedArtistsAreExcludedFromLocalRecommendations() {
    val blocked = Song(
      SongEntity(id = "blocked", title = "Blocked"),
      listOf(ArtistEntity(id = "artist-id", name = "Blocked Artist")),
    )
    val result = filterQuickPickSongs(
      listOf(blocked, song("visible")), emptySet(), setOf("artist-id||Blocked Artist"), false, false,
    )

    assertEquals(listOf("visible"), result.map { it.id })
  }
}
