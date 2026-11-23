package echo.music.core.mapping

import echo.music.iad1tya.db.entities.AlbumEntity
import echo.music.iad1tya.db.entities.ArtistEntity
import echo.music.iad1tya.db.entities.Playlist as DbPlaylist
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.Song as DbSong
import echo.music.iad1tya.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMappersTest {

  @Test
  fun testSongEntityToDomainMapping() {
    val entity = SongEntity(
      id = "song_123",
      title = "Midnight City",
      duration = 243,
      thumbnailUrl = "https://example.com/art.jpg",
      albumId = "alb_456",
      albumName = "Hurry Up",
      explicit = true,
      liked = true,
      totalPlayTime = 120000L,
      isLocal = false
    )

    val domainSong = entity.toDomain()

    assertEquals("song_123", domainSong.id)
    assertEquals("Midnight City", domainSong.title)
    assertEquals(243, domainSong.durationSeconds)
    assertEquals("https://example.com/art.jpg", domainSong.thumbnailUrl)
    assertEquals("alb_456", domainSong.album?.id)
    assertEquals("Hurry Up", domainSong.album?.title)
    assertTrue(domainSong.isExplicit)
    assertTrue(domainSong.liked)
    assertFalse(domainSong.isLocal)
    assertEquals(120000L, domainSong.totalPlayTimeMs)
  }

  @Test
  fun testDbSongWithRelationsToDomainMapping() {
    val entity = SongEntity(
      id = "song_rel",
      title = "Get Lucky",
      duration = 248,
      liked = false
    )
    val artists = listOf(
      ArtistEntity(id = "art_1", name = "Daft Punk"),
      ArtistEntity(id = "art_2", name = "Pharrell Williams")
    )
    val album = AlbumEntity(id = "alb_1", title = "Random Access Memories", songCount = 13, duration = 4440)

    val dbSong = DbSong(
      song = entity,
      artists = artists,
      album = album
    )

    val domainSong = dbSong.toDomain()

    assertEquals("song_rel", domainSong.id)
    assertEquals("Get Lucky", domainSong.title)
    assertEquals(2, domainSong.artists.size)
    assertEquals("Daft Punk", domainSong.artists[0].name)
    assertEquals("Pharrell Williams", domainSong.artists[1].name)
    assertEquals("Random Access Memories", domainSong.album?.title)
  }

  @Test
  fun testPlaylistEntityAndDbPlaylistMapping() {
    val entity = PlaylistEntity(
      id = "pl_1",
      name = "Electronic Mix",
      thumbnailUrl = "https://example.com/pl.jpg"
    )

    val domainFromEntity = entity.toDomain(songCount = 15)
    assertEquals("pl_1", domainFromEntity.id)
    assertEquals("Electronic Mix", domainFromEntity.name)
    assertEquals(15, domainFromEntity.songCount)
    assertEquals("https://example.com/pl.jpg", domainFromEntity.thumbnailUrl)

    val dbPlaylist = DbPlaylist(
      playlist = entity,
      songCount = 20,
      songThumbnails = listOf("https://example.com/thumb1.jpg", null)
    )

    val domainFromDb = dbPlaylist.toDomain()
    assertEquals("pl_1", domainFromDb.id)
    assertEquals(20, domainFromDb.songCount)
    assertEquals("https://example.com/pl.jpg", domainFromDb.thumbnailUrl)
  }
}
