package echo.music.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongModelTest {

  @Test
  fun testSongCreationAndProperties() {
    val artist = ArtistRef("art_1", "Artist One")
    val album = AlbumRef("alb_1", "Album One")
    val song = Song(
      id = "s_1",
      title = "Track Title",
      artists = listOf(artist),
      album = album,
      durationSeconds = 180,
      thumbnailUrl = "https://example.com/thumb.jpg",
      isExplicit = false,
      isLocal = true,
      liked = true,
      totalPlayTimeMs = 50000L
    )

    assertEquals("s_1", song.id)
    assertEquals("Track Title", song.title)
    assertEquals(1, song.artists.size)
    assertEquals("Artist One", song.artists[0].name)
    assertEquals("Album One", song.album?.title)
    assertEquals(180, song.durationSeconds)
    assertTrue(song.isLocal)
    assertTrue(song.liked)
    assertFalse(song.isExplicit)
    assertEquals(50000L, song.totalPlayTimeMs)
  }

  @Test
  fun testPlaylistCreation() {
    val playlist = Playlist(
      id = "pl_1",
      name = "Favorites",
      songCount = 42,
      thumbnailUrl = "https://example.com/playlist.jpg"
    )

    assertEquals("pl_1", playlist.id)
    assertEquals("Favorites", playlist.name)
    assertEquals(42, playlist.songCount)
    assertEquals("https://example.com/playlist.jpg", playlist.thumbnailUrl)
  }
}
