package echo.music.iad1tya.db

import androidx.room.Room
import echo.music.iad1tya.db.entities.ArtistEntity
import echo.music.iad1tya.db.entities.Event
import echo.music.iad1tya.db.entities.RelatedSongMap
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongArtistMap
import echo.music.iad1tya.db.entities.SongEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class RecommendationRelationsTest {
  private lateinit var database: InternalDatabase
  private lateinit var dao: DatabaseDao
  private val now = 1_800_000_000_000L
  private val seed = 1_103_515_245

  @Before
  fun setup() {
    database = Room.inMemoryDatabaseBuilder(
      RuntimeEnvironment.getApplication(),
      InternalDatabase::class.java,
    ).allowMainThreadQueries().build()
    dao = database.dao
    dao.insert(SongEntity(id = "source", title = "Source"))
    dao.insert(Event(songId = "source", timestamp = timestamp(now - 1_000), playTime = 10_000))
    dao.insert(ArtistEntity(id = "artist", name = "Artist"))

    // Exceed LIMIT 100 so a nondeterministic statement reset changes the selected ID set.
    repeat(160) { index ->
      val id = "track_$index"
      dao.insert(SongEntity(id = id, title = id, hideFromQuickPicks = index >= 150))
      if (index % 7 != 0) {
        dao.insert(SongArtistMap(songId = id, artistId = "artist", position = 0))
      }
      dao.insert(RelatedSongMap(songId = "source", relatedSongId = id))
      dao.insert(Event(songId = id, timestamp = timestamp(now - 40L * DAY_MS), playTime = 100_000))
      dao.insert(Event(songId = id, timestamp = timestamp(now - 10L * DAY_MS), playTime = 1_000))
    }
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun quickPicksKeepTheSameIdsAndRelationsAcrossStatementResets() = runBlocking {
    repeat(8) {
      assertCandidates(dao.quickPicks(now, seed).first())
    }
    assertNotEquals(
      dao.quickPicks(now, seed).first().map { it.id },
      dao.quickPicks(now, 1_664_525).first().map { it.id },
    )
  }

  @Test
  fun forgottenFavoritesKeepTheSameIdsAndRelationsAcrossStatementResets() = runBlocking {
    repeat(8) {
      assertCandidates(dao.forgottenFavorites(now, seed).first())
    }
    assertNotEquals(
      dao.forgottenFavorites(now, seed).first().map { it.id },
      dao.forgottenFavorites(now, 1_664_525).first().map { it.id },
    )
  }

  @Test
  fun quickPicksFallbackSeedsAreStableWithoutPlaybackHistory() = runBlocking {
    database.runInTransaction {
      database.openHelper.writableDatabase.execSQL("DELETE FROM event")
      database.openHelper.writableDatabase.execSQL("DELETE FROM related_song_map")
      repeat(160) { index ->
        dao.insert(RelatedSongMap(songId = "track_$index", relatedSongId = "track_${(index + 1) % 160}"))
      }
    }
    val expectedIds = dao.quickPicks(now, seed).first().map { it.id }
    assertTrue(expectedIds.isNotEmpty())
    repeat(8) {
      val songs = dao.quickPicks(now, seed).first()
      assertEquals(expectedIds, songs.map { it.id })
      assertTrue(songs.none { it.song.hideFromQuickPicks })
    }
    assertNotEquals(expectedIds, dao.quickPicks(now, 1).first().map { it.id })
  }

  @Test
  fun defaultSeedsStillLoadCompleteRecommendations() = runBlocking {
    assertEquals(100, dao.quickPicks(now).first().size)
    assertEquals(100, dao.forgottenFavorites(now).first().size)
  }

  private fun assertCandidates(songs: List<Song>) {
    val expectedIds = (0 until 150)
      .sortedBy { ((it + 2L) * seed) and 2_147_483_647L }
      .take(100)
      .map { "track_$it" }
    assertEquals(expectedIds, songs.map { it.id })
    songs.forEach { song ->
      assertTrue(!song.song.hideFromQuickPicks)
      val index = song.id.removePrefix("track_").toInt()
      assertEquals(if (index % 7 == 0) emptyList() else listOf("artist"), song.artists.map { it.id })
    }
  }

  private fun timestamp(millis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

  private companion object {
    const val DAY_MS = 86_400_000L
  }
}
