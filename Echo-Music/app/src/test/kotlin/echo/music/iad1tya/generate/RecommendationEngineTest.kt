package echo.music.iad1tya.generate

import androidx.datastore.preferences.core.mutablePreferencesOf
import echo.music.iad1tya.constants.LastFMUsernameKey
import echo.music.iad1tya.db.entities.RecommendationExclusionEntity
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationEngineTest {
  private lateinit var fakeStatsDao: FakeSongPlayStatsDao
  private lateinit var fakeExclusionDao: FakeRecommendationExclusionDao
  private lateinit var fakeTasteDao: FakeTasteProfileDao
  private lateinit var fakeApi: FakeLastFmTasteApi
  private lateinit var fakeDataStore: FakePreferencesDataStore
  private lateinit var tasteProfileProvider: TasteProfileProvider
  private lateinit var engine: RecommendationEngine

  @Before
  fun setup() {
    fakeStatsDao = FakeSongPlayStatsDao()
    fakeExclusionDao = FakeRecommendationExclusionDao()
    fakeTasteDao = FakeTasteProfileDao()
    fakeApi = FakeLastFmTasteApi()
    fakeDataStore = FakePreferencesDataStore(mutablePreferencesOf(LastFMUsernameKey to "test_user"))
    tasteProfileProvider = TasteProfileProvider(fakeTasteDao, fakeApi, fakeDataStore, fakeStatsDao)
    engine = RecommendationEngine(
      songPlayStatsDao = fakeStatsDao,
      recommendationExclusionDao = fakeExclusionDao,
      tasteProfileProvider = tasteProfileProvider,
      lastFmTasteApi = fakeApi,
      database = null,
    ).apply {
      allowNullDatabaseForTesting = true
    }
  }

  @Test
  fun testNullDatabaseFailsWhenNotTestingAllowed() = runTest {
    fakeStatsDao.upsert(
      SongPlayStatsEntity(
        trackKey = "blinding lights|the weeknd",
        title = "Blinding Lights",
        artist = "The Weeknd",
        videoId = "seed_vid",
        totalPlayTimeMs = 300_000L,
        playCount = 10,
        skipCount = 0,
        lastPlayedAtMillis = System.currentTimeMillis(),
      )
    )

    val strictEngine = RecommendationEngine(
      songPlayStatsDao = fakeStatsDao,
      recommendationExclusionDao = fakeExclusionDao,
      tasteProfileProvider = tasteProfileProvider,
      lastFmTasteApi = fakeApi,
      database = null,
    )
    strictEngine.allowNullDatabaseForTesting = false

    val result = strictEngine.generate(10)
    assertTrue("Null database must fail when not explicitly allowed", result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Database not available") == true)
  }

  @Test
  fun testEmptyHistoryFailsGracefully() = runTest {
    val result = engine.generate(20)
    assertTrue("Empty history should fail", result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("No listening history") == true)
  }

  @Test
  fun testAllExcludedFailsGracefully() = runTest {
    fakeStatsDao.upsert(
      SongPlayStatsEntity(
        trackKey = "bad song|bad artist",
        title = "Bad Song",
        artist = "Bad Artist",
        videoId = "vid1",
        totalPlayTimeMs = 120_000L,
        playCount = 5,
        skipCount = 0,
        lastPlayedAtMillis = System.currentTimeMillis(),
      )
    )
    fakeExclusionDao.upsert(
      RecommendationExclusionEntity(
        trackKey = "bad song|bad artist",
        excludedAtMillis = System.currentTimeMillis(),
        trackName = "Bad Song",
        artistName = "Bad Artist",
      )
    )

    val result = engine.generate(20)
    assertTrue("All excluded should fail", result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("All candidate tracks have been excluded") == true)
  }

  @Test
  fun testRecommendationSucceedsWithLocalSeedsAndLastFmSignals() = runTest {
    fakeStatsDao.upsert(
      SongPlayStatsEntity(
        trackKey = "blinding lights|the weeknd",
        title = "Blinding Lights",
        artist = "The Weeknd",
        videoId = "seed_vid",
        totalPlayTimeMs = 300_000L,
        playCount = 10,
        skipCount = 0,
        lastPlayedAtMillis = System.currentTimeMillis(),
      )
    )

    val result = engine.generate(10)
    assertTrue("Generation should succeed", result.isSuccess)
    val playlistId = result.getOrNull()
    assertTrue("PlaylistId should start with local_taste_", playlistId?.startsWith("local_taste_") == true)
  }

  @Test
  fun testRecommendationDegradesToLocalOnlyOnLastFmFailure() = runTest {
    fakeStatsDao.upsert(
      SongPlayStatsEntity(
        trackKey = "blinding lights|the weeknd",
        title = "Blinding Lights",
        artist = "The Weeknd",
        videoId = "seed_vid",
        totalPlayTimeMs = 300_000L,
        playCount = 10,
        skipCount = 0,
        lastPlayedAtMillis = System.currentTimeMillis(),
      )
    )

    fakeApi.shouldFail = true

    val result = engine.generate(10)
    assertTrue("Generation should succeed even when Last.fm fails", result.isSuccess)
    val playlistId = result.getOrNull()
    assertTrue("PlaylistId should start with local_taste_", playlistId?.startsWith("local_taste_") == true)
  }
}
