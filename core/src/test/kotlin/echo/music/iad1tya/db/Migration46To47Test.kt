package echo.music.iad1tya.db

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration46To47Test {
  private lateinit var db: SQLiteDatabase

  @Before
  fun setup() {
    db = SQLiteDatabase.create(null)

    db.execSQL("""
      CREATE TABLE song (
        id TEXT PRIMARY KEY NOT NULL,
        title TEXT NOT NULL,
        duration INTEGER NOT NULL,
        thumbnailUrl TEXT,
        albumId TEXT,
        albumName TEXT,
        explicit INTEGER NOT NULL DEFAULT 0,
        year INTEGER,
        date TEXT,
        isLocal INTEGER NOT NULL DEFAULT 0,
        hideFromQuickPicks INTEGER NOT NULL DEFAULT 0
      )
    """.trimIndent())

    db.execSQL("""
      CREATE TABLE artist (
        id TEXT PRIMARY KEY NOT NULL,
        name TEXT NOT NULL,
        isLocal INTEGER NOT NULL DEFAULT 0
      )
    """.trimIndent())

    db.execSQL("""
      CREATE TABLE song_artist_map (
        songId TEXT NOT NULL,
        artistId TEXT NOT NULL,
        position INTEGER NOT NULL,
        PRIMARY KEY(songId, artistId)
      )
    """.trimIndent())

    db.execSQL("""
      CREATE TABLE event (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        songId TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        playTime INTEGER NOT NULL
      )
    """.trimIndent())

    db.execSQL("""
      CREATE TABLE song_play_stats (
        trackKey TEXT PRIMARY KEY NOT NULL,
        title TEXT NOT NULL,
        artist TEXT NOT NULL,
        videoId TEXT,
        artworkUrl TEXT,
        totalPlayTimeMs INTEGER NOT NULL,
        playCount INTEGER NOT NULL,
        skipCount INTEGER NOT NULL,
        lastPlayedAtMillis INTEGER NOT NULL
      )
    """.trimIndent())
  }

  @After
  fun teardown() {
    db.close()
  }

  private fun runBackfill() {
    Migration46To47Spec().onPostMigrate(db.toSupportSQLiteDatabase())
  }

  @Test
  fun duplicateUploadsOfSameRecordingCollapseIntoOneRow() {
    db.execSQL("INSERT INTO song (id, title, duration) VALUES ('v1', 'Shape of You', 230)")
    db.execSQL("INSERT INTO song (id, title, duration) VALUES ('v2', 'Shape of You', 230)")
    db.execSQL("INSERT INTO artist (id, name) VALUES ('a1', 'Ed Sheeran')")
    db.execSQL("INSERT INTO song_artist_map (songId, artistId, position) VALUES ('v1', 'a1', 0)")
    db.execSQL("INSERT INTO song_artist_map (songId, artistId, position) VALUES ('v2', 'a1', 0)")

    db.execSQL("INSERT INTO event (songId, timestamp, playTime) VALUES ('v1', 1000, 30000)")
    db.execSQL("INSERT INTO event (songId, timestamp, playTime) VALUES ('v2', 2000, 45000)")

    runBackfill()

    db.rawQuery("SELECT * FROM song_play_stats", null).use { cursor ->
      assertEquals(1, cursor.count)
      assertTrue(cursor.moveToFirst())
      assertEquals("shape of you|ed sheeran", cursor.getString(cursor.getColumnIndexOrThrow("trackKey")))
      assertEquals("Shape of You", cursor.getString(cursor.getColumnIndexOrThrow("title")))
      assertEquals("Ed Sheeran", cursor.getString(cursor.getColumnIndexOrThrow("artist")))
      assertEquals(75000L, cursor.getLong(cursor.getColumnIndexOrThrow("totalPlayTimeMs")))
      assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("playCount")))
      assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("skipCount")))
      assertEquals(2000L, cursor.getLong(cursor.getColumnIndexOrThrow("lastPlayedAtMillis")))
    }
  }

  @Test
  fun sameTitleWithDifferentArtistsSeedsDistinctRows() {
    db.execSQL("INSERT INTO song (id, title, duration) VALUES ('v1', 'Hello', 200)")
    db.execSQL("INSERT INTO song (id, title, duration) VALUES ('v2', 'Hello', 250)")
    db.execSQL("INSERT INTO artist (id, name) VALUES ('a1', 'Adele')")
    db.execSQL("INSERT INTO artist (id, name) VALUES ('a2', 'Lionel Richie')")
    db.execSQL("INSERT INTO song_artist_map (songId, artistId, position) VALUES ('v1', 'a1', 0)")
    db.execSQL("INSERT INTO song_artist_map (songId, artistId, position) VALUES ('v2', 'a2', 0)")

    db.execSQL("INSERT INTO event (songId, timestamp, playTime) VALUES ('v1', 5000, 60000)")
    db.execSQL("INSERT INTO event (songId, timestamp, playTime) VALUES ('v2', 6000, 80000)")

    runBackfill()

    db.rawQuery("SELECT * FROM song_play_stats ORDER BY trackKey", null).use { cursor ->
      assertEquals(2, cursor.count)
      assertTrue(cursor.moveToFirst())
      assertEquals("hello|adele", cursor.getString(cursor.getColumnIndexOrThrow("trackKey")))
      assertTrue(cursor.moveToNext())
      assertEquals("hello|lionel richie", cursor.getString(cursor.getColumnIndexOrThrow("trackKey")))
    }
  }

  @Test
  fun songWithNoArtistMappingSeedsEmptyArtistRow() {
    db.execSQL("INSERT INTO song (id, title, duration) VALUES ('v1', 'Unknown Track', 180)")
    db.execSQL("INSERT INTO event (songId, timestamp, playTime) VALUES ('v1', 123456, 40000)")

    runBackfill()

    db.rawQuery("SELECT * FROM song_play_stats", null).use { cursor ->
      assertEquals(1, cursor.count)
      assertTrue(cursor.moveToFirst())
      assertEquals("unknown track|", cursor.getString(cursor.getColumnIndexOrThrow("trackKey")))
      assertEquals("Unknown Track", cursor.getString(cursor.getColumnIndexOrThrow("title")))
      assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("artist")))
      assertEquals(40000L, cursor.getLong(cursor.getColumnIndexOrThrow("totalPlayTimeMs")))
      assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("playCount")))
      assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("skipCount")))
      assertEquals(123456L, cursor.getLong(cursor.getColumnIndexOrThrow("lastPlayedAtMillis")))
    }
  }
}
