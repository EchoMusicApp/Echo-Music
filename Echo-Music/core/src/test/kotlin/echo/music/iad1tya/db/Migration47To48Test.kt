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
class Migration47To48Test {
  private lateinit var db: SQLiteDatabase

  @Before
  fun setup() {
    db = SQLiteDatabase.create(null)

    // Pre-populate v47 schema tables
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

    db.execSQL("""
      CREATE TABLE recommendation_exclusions (
        trackKey TEXT PRIMARY KEY NOT NULL,
        excludedAtMillis INTEGER NOT NULL,
        trackName TEXT NOT NULL,
        artistName TEXT NOT NULL
      )
    """.trimIndent())

    // Insert sample v47 data
    db.execSQL("""
      INSERT INTO song_play_stats (trackKey, title, artist, videoId, artworkUrl, totalPlayTimeMs, playCount, skipCount, lastPlayedAtMillis)
      VALUES ('starboy|the weeknd', 'Starboy', 'The Weeknd', 'vid123', 'thumb.jpg', 180000, 3, 0, 1690000000)
    """.trimIndent())

    db.execSQL("""
      INSERT INTO recommendation_exclusions (trackKey, excludedAtMillis, trackName, artistName)
      VALUES ('bad song|artist x', 1690000000, 'Bad Song', 'Artist X')
    """.trimIndent())
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun testMigrationCreatesTasteProfileTableAndPreservesExistingData() {
    // Run migration 47 -> 48: Add taste_profile table
    db.execSQL("""
      CREATE TABLE IF NOT EXISTS taste_profile (
        id INTEGER NOT NULL PRIMARY KEY,
        topArtistsJson TEXT NOT NULL,
        topTracksJson TEXT NOT NULL,
        topGenresJson TEXT NOT NULL,
        confidence REAL NOT NULL,
        updatedAtMillis INTEGER NOT NULL
      )
    """.trimIndent())

    Migration47To48Spec().onPostMigrate(db.toSupportSQLiteDatabase())

    // 1. Verify taste_profile exists and can store data
    db.execSQL("""
      INSERT INTO taste_profile (id, topArtistsJson, topTracksJson, topGenresJson, confidence, updatedAtMillis)
      VALUES (1, '["The Weeknd","Daft Punk"]', '["starboy|the weeknd"]', '["synthpop","rnb"]', 0.95, 1700000000)
    """.trimIndent())

    val profileCursor = db.rawQuery("SELECT * FROM taste_profile WHERE id = 1", null)
    assertTrue("taste_profile row should exist", profileCursor.moveToFirst())
    assertEquals("[\"The Weeknd\",\"Daft Punk\"]", profileCursor.getString(profileCursor.getColumnIndexOrThrow("topArtistsJson")))
    assertEquals("[\"starboy|the weeknd\"]", profileCursor.getString(profileCursor.getColumnIndexOrThrow("topTracksJson")))
    assertEquals("[\"synthpop\",\"rnb\"]", profileCursor.getString(profileCursor.getColumnIndexOrThrow("topGenresJson")))
    assertEquals(0.95f, profileCursor.getFloat(profileCursor.getColumnIndexOrThrow("confidence")), 0.001f)
    assertEquals(1700000000L, profileCursor.getLong(profileCursor.getColumnIndexOrThrow("updatedAtMillis")))
    profileCursor.close()

    // 2. Verify song_play_stats data was preserved intact
    val statsCursor = db.rawQuery("SELECT * FROM song_play_stats WHERE trackKey = 'starboy|the weeknd'", null)
    assertTrue("song_play_stats row should be preserved", statsCursor.moveToFirst())
    assertEquals("Starboy", statsCursor.getString(statsCursor.getColumnIndexOrThrow("title")))
    assertEquals("The Weeknd", statsCursor.getString(statsCursor.getColumnIndexOrThrow("artist")))
    assertEquals(180000L, statsCursor.getLong(statsCursor.getColumnIndexOrThrow("totalPlayTimeMs")))
    assertEquals(3, statsCursor.getInt(statsCursor.getColumnIndexOrThrow("playCount")))
    statsCursor.close()

    // 3. Verify recommendation_exclusions data was preserved intact with trackName and artistName
    val exclCursor = db.rawQuery("SELECT * FROM recommendation_exclusions WHERE trackKey = 'bad song|artist x'", null)
    assertTrue("recommendation_exclusions row should be preserved", exclCursor.moveToFirst())
    assertEquals("Bad Song", exclCursor.getString(exclCursor.getColumnIndexOrThrow("trackName")))
    assertEquals("Artist X", exclCursor.getString(exclCursor.getColumnIndexOrThrow("artistName")))
    assertEquals(1690000000L, exclCursor.getLong(exclCursor.getColumnIndexOrThrow("excludedAtMillis")))
    exclCursor.close()
  }

  @Test
  fun testMigrationReplacesLegacyTasteProfileSchema() {
    // Seed legacy taste_profile schema from MIGRATION_37_38
    db.execSQL("""
      CREATE TABLE IF NOT EXISTS `taste_profile` (
        `id` INTEGER NOT NULL,
        `genres` TEXT NOT NULL,
        `confidence` REAL NOT NULL,
        `patternsFound` INTEGER NOT NULL,
        `modelVersion` TEXT NOT NULL,
        `updatedAt` INTEGER NOT NULL,
        PRIMARY KEY(`id`)
      )
    """.trimIndent())
    db.execSQL("""
      INSERT INTO `taste_profile` VALUES (1, 'pop,rock', 0.8, 5, 'v1', 1600000000)
    """.trimIndent())

    // Run migration spec
    Migration47To48Spec().onPostMigrate(db.toSupportSQLiteDatabase())

    // Verify taste_profile now has v48 columns
    db.execSQL("""
      INSERT INTO taste_profile (id, topArtistsJson, topTracksJson, topGenresJson, confidence, updatedAtMillis)
      VALUES (1, '["Artist"]', '["track|artist"]', '["pop"]', 0.9, 1700000000)
    """.trimIndent())

    db.rawQuery("SELECT * FROM taste_profile WHERE id = 1", null).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals("[\"Artist\"]", cursor.getString(cursor.getColumnIndexOrThrow("topArtistsJson")))
      assertEquals("[\"track|artist\"]", cursor.getString(cursor.getColumnIndexOrThrow("topTracksJson")))
      assertEquals("[\"pop\"]", cursor.getString(cursor.getColumnIndexOrThrow("topGenresJson")))
      assertEquals(0.9f, cursor.getFloat(cursor.getColumnIndexOrThrow("confidence")), 0.001f)
      assertEquals(1700000000L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAtMillis")))
    }
  }
}
