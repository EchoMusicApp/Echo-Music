package echo.music.iad1tya.generate

import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationScoringTest {

  @Test
  fun testSimilarTrackBonusAppliesCorrectly() {
    val base = 10.0f
    val scored = scoreCandidateWithExternals(base, isSimilarTrack = true)
    assertEquals(12.0f, scored, 0.001f)
  }

  @Test
  fun testSimilarArtistBonusAppliesCorrectly() {
    val base = 10.0f
    val scored = scoreCandidateWithExternals(base, isSimilarArtist = true)
    assertEquals(11.0f, scored, 0.001f)
  }

  @Test
  fun testTagMatchBonusAppliesCorrectly() {
    val base = 10.0f
    val scored = scoreCandidateWithExternals(base, matchesTopGenre = true)
    assertEquals(11.5f, scored, 0.001f)
  }

  @Test
  fun testGenreMatchBonusAppliesCorrectly() {
    val base = 10.0f
    val scored = scoreCandidateWithExternals(base, matchesGenrePreference = true)
    assertEquals(11.0f, scored, 0.001f)
  }

  @Test
  fun testAllExternalBonusesAreAdditive() {
    val base = 10.0f
    val scored = scoreCandidateWithExternals(
      baseScore = base,
      isSimilarTrack = true,
      isSimilarArtist = true,
      matchesTopGenre = true,
    )
    // 10.0 + 2.0 (track) + 1.0 (artist) + 1.5 (tag) = 14.5
    assertEquals(14.5f, scored, 0.001f)
  }

  @Test
  fun testAllSlice3ExternalBonusesAreAdditive() {
    val base = 10.0f
    val scored = scoreCandidateWithExternals(
      baseScore = base,
      isSimilarTrack = true,
      isSimilarArtist = true,
      matchesTopGenre = true,
      matchesGenrePreference = true,
    )
    // 10.0 + 2.0 (track) + 1.0 (artist) + 1.5 (tag) + 1.0 (genre) = 15.5
    assertEquals(15.5f, scored, 0.001f)
  }

  @Test
  fun testDegradedModeScoresIdenticalToLocalScore() {
    val stats = SongPlayStatsEntity(
      trackKey = "song|artist",
      title = "Song",
      artist = "Artist",
      totalPlayTimeMs = 120_000L,
      playCount = 4,
      skipCount = 0,
      lastPlayedAtMillis = System.currentTimeMillis(),
    )
    val localScore = scoreStats(stats, likedKeys = emptySet(), timeOfDay = "afternoon")

    // With no external signals, external scoring must equal local score exactly
    val candidateScore = scoreCandidateWithExternals(
      baseScore = localScore,
      isSimilarTrack = false,
      isSimilarArtist = false,
      matchesTopGenre = false,
    )
    assertEquals(localScore, candidateScore, 0.001f)
  }
}
