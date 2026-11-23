package echo.music.iad1tya.generate

import androidx.datastore.preferences.core.mutablePreferencesOf
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import echo.music.iad1tya.utils.lastfm.LastFmTasteApi
import echo.music.iad1tya.utils.lastfm.SimilarArtist
import echo.music.iad1tya.utils.lastfm.SimilarTrack
import echo.music.iad1tya.utils.lastfm.Tag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeGenresTasteApi : LastFmTasteApi {
  var topTagsCallCount = 0
  var shouldFail = false
  val tagsByArtist = mutableMapOf<String, List<Tag>>()

  override suspend fun getSimilarTracks(artist: String, track: String): Result<List<SimilarTrack>> =
    Result.success(emptyList())

  override suspend fun getSimilarArtists(artist: String): Result<List<SimilarArtist>> =
    Result.success(emptyList())

  override suspend fun getTopTags(artist: String): Result<List<Tag>> {
    topTagsCallCount++
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(tagsByArtist[artist.lowercase()] ?: listOf(Tag("rock", 100), Tag("alternative", 80)))
  }

  override suspend fun getUserTopArtists(user: String, limit: Int): Result<List<String>> =
    Result.success(emptyList())

  override suspend fun getUserTopTracks(user: String, limit: Int): Result<List<SimilarTrack>> =
    Result.success(emptyList())

  override suspend fun getUserTopTags(user: String, limit: Int): Result<List<Tag>> =
    Result.success(emptyList())
}

private class FakeTasteProfileProvider(
  private val profile: TasteProfile? = null,
) : TasteProfileProvider(
  tasteProfileDao = FakeTasteProfileDao(),
  lastFmTasteApi = FakeGenresTasteApi(),
  dataStore = FakePreferencesDataStore(mutablePreferencesOf()),
  songPlayStatsDao = FakeSongPlayStatsDao(),
) {
  override suspend fun get(forceRefresh: Boolean): TasteProfile? = profile
}

class GenresRepositoryTest {

  private lateinit var fakeApi: FakeGenresTasteApi
  private lateinit var fakeStatsDao: FakeSongPlayStatsDao
  private lateinit var repository: GenresRepository

  @Before
  fun setup() {
    fakeApi = FakeGenresTasteApi()
    fakeStatsDao = FakeSongPlayStatsDao()
    val fakeProvider = FakeTasteProfileProvider(
      TasteProfile(
        topArtistNames = listOf("radiohead"),
        topTrackKeys = listOf("creep|radiohead"),
        topGenres = listOf("rock", "indie"),
        fetchedAtMillis = System.currentTimeMillis(),
      )
    )
    repository = GenresRepository(
      database = null,
      tasteApi = fakeApi,
      tasteProfileProvider = fakeProvider,
      songPlayStatsDao = fakeStatsDao,
    )
  }

  @Test
  fun testGenresForTrackFetchesAndCachesWithinTTL() = runTest {
    fakeApi.tagsByArtist["radiohead"] = listOf(Tag("art rock", 100), Tag("experimental", 90))

    // 1. First fetch hits API
    val firstResult = repository.genresForTrack("creep|radiohead")
    assertEquals(1, fakeApi.topTagsCallCount)
    assertTrue(firstResult.contains("art rock"))
    assertTrue(firstResult.contains("experimental"))

    // 2. Second fetch within TTL hits memory cache without extra API call
    val secondResult = repository.genresForTrack("creep|radiohead")
    assertEquals(1, fakeApi.topTagsCallCount)
    assertEquals(firstResult, secondResult)
  }

  @Test
  fun testTopGenresAggregatesWeightsFromTasteProfileAndPlayStats() = runTest {
    fakeStatsDao.upsert(
      SongPlayStatsEntity(
        trackKey = "karma police|radiohead",
        title = "Karma Police",
        artist = "Radiohead",
        totalPlayTimeMs = 120_000L,
        playCount = 10,
        skipCount = 0,
        lastPlayedAtMillis = System.currentTimeMillis(),
      )
    )

    val top = repository.topGenres(5)
    assertTrue("Should return non-empty top genres", top.isNotEmpty())
    val genreNames = top.map { it.genre }
    assertTrue("Should contain rock from taste profile", genreNames.contains("rock"))
    assertTrue("Should contain indie from taste profile", genreNames.contains("indie"))
  }

  @Test
  fun testGenresForTrackGracefulFallbackOnFailure() = runTest {
    fakeApi.shouldFail = true
    val result = repository.genresForTrack("song|failing_artist")
    assertTrue("Should return empty list on failure without crashing", result.isEmpty())

    // Ensure failed lookup was NOT cached: subsequent call after API recovers should succeed
    fakeApi.shouldFail = false
    fakeApi.tagsByArtist["failing_artist"] = listOf(Tag("jazz", 100))
    val retryResult = repository.genresForTrack("song|failing_artist")
    assertTrue("Should fetch tags after recovery if not cached", retryResult.contains("jazz"))
  }
}
