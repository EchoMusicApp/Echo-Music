package echo.music.iad1tya.generate

import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalTasteScoringTest {

  @Test
  fun trackKeyNormalizationCleansWhitespaceAndLowercases() {
    val key1 = trackKeyOf("  Starboy ", " The Weeknd ")
    val key2 = trackKeyOf("starboy", "the weeknd")
    val key3 = trackKeyOf("STARBOY", "THE WEEKND")

    assertEquals("starboy|the weeknd", key1)
    assertEquals(key2, key1)
    assertEquals(key3, key1)
  }

  @Test
  fun allWeightsAndBonusesContributeToScore() {
    val baseEntity = SongPlayStatsEntity(
      trackKey = "song|artist",
      title = "Song",
      artist = "Artist",
      totalPlayTimeMs = 1000L,
      playCount = 1,
      skipCount = 0,
      lastPlayedAtMillis = System.currentTimeMillis(),
    )

    // Base score: ln(1.0 + 1000/60000) * 2.0 (PLAY_WEIGHT) + 1.5 (RECENCY_BONUS)
    val expectedBase = (kotlin.math.ln(1.0 + 1000.0 / 60_000.0).toFloat() * PLAY_WEIGHT) + RECENCY_BONUS
    // In afternoon: hourBonus = 1.0
    val afternoonScore = scoreStats(baseEntity, emptySet(), "afternoon")
    assertEquals(expectedBase, afternoonScore, 0.01f)

    // With liked bonus: expectedBase + 5.0 (LIKED_BONUS)
    val likedScore = scoreStats(baseEntity, setOf("song|artist"), "afternoon")
    assertEquals(expectedBase + LIKED_BONUS, likedScore, 0.01f)

    // With skip penalty: skipCount = 2 -> expectedBase - (2 * 3.0)
    val skippedEntity = baseEntity.copy(skipCount = 2)
    val skippedScore = scoreStats(skippedEntity, emptySet(), "afternoon")
    assertEquals((expectedBase - 2 * SKIP_PENALTY).coerceAtLeast(0f), skippedScore, 0.01f)

    // Verify skip penalty is heaviest single penalty (-3.0 per skip)
    assertTrue(SKIP_PENALTY > PLAY_WEIGHT)
  }

  @Test
  fun timeOfDayBonusAppliesCorrectly() {
    val recentEntity = SongPlayStatsEntity(
      trackKey = "track|artist",
      title = "Track",
      artist = "Artist",
      totalPlayTimeMs = 100L,
      lastPlayedAtMillis = System.currentTimeMillis(),
    )

    // In morning, recent tracks get 1.3x
    val morningScore = scoreStats(recentEntity, emptySet(), "morning")
    val neutralScore = scoreStats(recentEntity, emptySet(), "afternoon")
    assertEquals(neutralScore * 1.3f, morningScore, 0.01f)

    // In night, liked tracks get 1.3x
    val likedEntity = recentEntity.copy(lastPlayedAtMillis = 0L)
    val nightLikedScore = scoreStats(likedEntity, setOf("track|artist"), "night")
    val afternoonLikedScore = scoreStats(likedEntity, setOf("track|artist"), "afternoon")
    assertEquals(afternoonLikedScore * 1.3f, nightLikedScore, 0.01f)
  }

  @Test
  fun timeOfDayBucketsMatchSpecification() {
    assertEquals("morning", currentTimeOfDay(6))
    assertEquals("morning", currentTimeOfDay(11))
    assertEquals("afternoon", currentTimeOfDay(12))
    assertEquals("afternoon", currentTimeOfDay(17))
    assertEquals("evening", currentTimeOfDay(18))
    assertEquals("evening", currentTimeOfDay(21))
    assertEquals("night", currentTimeOfDay(22))
    assertEquals("night", currentTimeOfDay(3))
  }

  @Test
  fun applyCandidateFilterTruthTable() {
    // 0 skips, low play time -> true (skipCount < 2)
    assertTrue(applyCandidateFilter(SongPlayStatsEntity(trackKey = "k", skipCount = 0, totalPlayTimeMs = 10_000L)))

    // 1 skip, low play time -> true (skipCount < 2)
    assertTrue(applyCandidateFilter(SongPlayStatsEntity(trackKey = "k", skipCount = 1, totalPlayTimeMs = 10_000L)))

    // 2 skips, low play time -> false (skipCount >= 2 and totalPlayTimeMs <= 45_000)
    assertFalse(applyCandidateFilter(SongPlayStatsEntity(trackKey = "k", skipCount = 2, totalPlayTimeMs = 10_000L)))

    // 2 skips, high play time -> true (totalPlayTimeMs > 45_000)
    assertTrue(applyCandidateFilter(SongPlayStatsEntity(trackKey = "k", skipCount = 2, totalPlayTimeMs = 50_000L)))

    // 5 skips, high play time -> true (totalPlayTimeMs > 45_000)
    assertTrue(applyCandidateFilter(SongPlayStatsEntity(trackKey = "k", skipCount = 5, totalPlayTimeMs = 60_000L)))
  }
}
