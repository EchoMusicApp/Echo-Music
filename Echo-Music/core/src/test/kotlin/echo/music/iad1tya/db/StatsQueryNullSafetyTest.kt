package echo.music.iad1tya.db

import androidx.room.Room
import echo.music.iad1tya.db.entities.Album
import echo.music.iad1tya.db.entities.AlbumEntity
import echo.music.iad1tya.db.entities.Event
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.db.entities.SongWithStats
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class StatsQueryNullSafetyTest {
  private lateinit var database: InternalDatabase
  private lateinit var dao: DatabaseDao
  private val now = 1_800_000_000_000L

  @Before
  fun setup() {
    database = Room.inMemoryDatabaseBuilder(
      RuntimeEnvironment.getApplication(),
      InternalDatabase::class.java,
    ).allowMainThreadQueries().build()
    dao = database.dao
  }

  @After
  fun teardown() {
    database.close()
  }

  private fun timestamp(millis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

  @Test
  fun mostPlayedSongsStats_handlesNullThumbnailUrlWithoutCrashing() = runBlocking {
    // Insert a local track with null thumbnailUrl
    dao.insert(SongEntity(id = "local_track_1", title = "Offline Audio", thumbnailUrl = null, isLocal = true))
    dao.insert(Event(songId = "local_track_1", timestamp = timestamp(now - 1_000), playTime = 45_000))

    // Query most played song stats
    val results = dao.mostPlayedSongsStats(fromTimeStamp = 0L, limit = 10, offset = 0, toTimeStamp = now).first()

    assertEquals(1, results.size)
    val stat = results.first()
    assertEquals("local_track_1", stat.id)
    assertEquals("Offline Audio", stat.title)
    assertNull(stat.thumbnailUrl)
    assertEquals(1, stat.songCountListened)
    assertEquals(45_000L, stat.timeListened)
  }

  @Test
  fun songWithStats_allowsNullValuesWithoutNpe() {
    val stat = SongWithStats(
      id = "song_null_thumb",
      title = "Test Title",
      artistName = null,
      thumbnailUrl = null,
      songCountListened = null,
      timeListened = null,
      isVideo = false,
    )
    assertNull(stat.thumbnailUrl)
    assertNull(stat.artistName)
    assertNull(stat.songCountListened)
    assertNull(stat.timeListened)
  }

  @Test
  fun album_allowsNullSongCountListenedWithoutNpe() {
    val album = Album(
      album = AlbumEntity(id = "album_1", title = "Album One", songCount = 5, duration = 1200),
      artists = emptyList(),
      songCountListened = null,
      timeListened = null,
    )
    assertNull(album.songCountListened)
    val fallbackCount = album.songCountListened ?: 0
    assertEquals(0, fallbackCount)
  }
}
