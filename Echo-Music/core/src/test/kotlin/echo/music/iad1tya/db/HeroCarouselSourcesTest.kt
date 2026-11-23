package echo.music.iad1tya.db

import androidx.room.Room
import echo.music.iad1tya.db.entities.Event
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.SongEntity
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class HeroCarouselSourcesTest {
  private lateinit var database: InternalDatabase
  private lateinit var dao: DatabaseDao
  private val now = LocalDateTime.of(2026, 10, 14, 12, 0)

  @Before
  fun setup() {
    database = Room.inMemoryDatabaseBuilder(
      RuntimeEnvironment.getApplication(), InternalDatabase::class.java,
    ).allowMainThreadQueries().build()
    dao = database.dao
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun recentSongsUsePlaybackTimeAndOnlyCountEachTrackOnce() = runBlocking {
    listOf("older", "newer", "unplayed").forEach {
      dao.insert(SongEntity(id = it, title = it))
    }
    dao.insert(Event(songId = "newer", timestamp = now, playTime = 1000))
    dao.insert(Event(songId = "older", timestamp = now.minusDays(1), playTime = 1000))
    dao.insert(Event(songId = "newer", timestamp = now.minusDays(2), playTime = 1000))

    assertEquals(listOf("newer", "older"), dao.recentlyPlayedSongs().first().map { it.id })
    assertEquals(listOf("newer"), dao.recentlyPlayedSongs(limit = 1).first().map { it.id })
  }

  @Test
  fun includesLikedLibraryAndPlaylistOnlySongsWithoutUnassociatedCachedTracks() = runBlocking {
    dao.insert(SongEntity(id = "liked", title = "Liked", liked = true))
    dao.insert(SongEntity(id = "library", title = "Library", inLibrary = now))
    dao.insert(SongEntity(id = "playlist-only", title = "Playlist only"))
    dao.insert(SongEntity(id = "cached", title = "Cached"))
    dao.insert(PlaylistEntity(id = "playlist", name = "Playlist"))
    dao.insert(PlaylistSongMap(playlistId = "playlist", songId = "playlist-only", position = 0))
    dao.insert(PlaylistSongMap(playlistId = "playlist", songId = "liked", position = 1))

    assertEquals(
      setOf("liked", "library", "playlist-only"),
      dao.carouselLibrarySongs().first().map { it.id }.toSet(),
    )
    assertEquals(3, dao.carouselLibrarySongs().first().size)
  }
}
