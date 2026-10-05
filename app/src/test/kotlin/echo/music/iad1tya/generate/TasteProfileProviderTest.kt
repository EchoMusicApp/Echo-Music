package echo.music.iad1tya.generate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import echo.music.iad1tya.constants.LastFMUsernameKey
import echo.music.iad1tya.db.daos.TasteProfileDao
import echo.music.iad1tya.db.entities.TasteProfileEntity
import echo.music.iad1tya.utils.lastfm.LastFmTasteApi
import echo.music.iad1tya.utils.lastfm.SimilarArtist
import echo.music.iad1tya.utils.lastfm.SimilarTrack
import echo.music.iad1tya.utils.lastfm.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTasteProfileDao : TasteProfileDao {
  var storedProfile: TasteProfileEntity? = null

  override suspend fun get(): TasteProfileEntity? = storedProfile

  override suspend fun upsert(profile: TasteProfileEntity) {
    storedProfile = profile
  }

  override suspend fun clear() {
    storedProfile = null
  }
}

class FakeLastFmTasteApi : LastFmTasteApi {
  var userTopArtistsCallCount = 0
  var userTopTracksCallCount = 0
  var userTopTagsCallCount = 0
  var shouldFail = false

  override suspend fun getSimilarTracks(artist: String, track: String): Result<List<SimilarTrack>> {
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(listOf(SimilarTrack("Similar Song", "Similar Artist", 0.9)))
  }

  override suspend fun getSimilarArtists(artist: String): Result<List<SimilarArtist>> {
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(listOf(SimilarArtist("Similar Artist", 0.8)))
  }

  override suspend fun getTopTags(artist: String): Result<List<Tag>> {
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(listOf(Tag("synthpop", 100)))
  }

  override suspend fun getUserTopArtists(user: String, limit: Int): Result<List<String>> {
    userTopArtistsCallCount++
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(listOf("The Weeknd", "Daft Punk", "Kavinsky"))
  }

  override suspend fun getUserTopTracks(user: String, limit: Int): Result<List<SimilarTrack>> {
    userTopTracksCallCount++
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(listOf(SimilarTrack("Starboy", "The Weeknd"), SimilarTrack("Nightcall", "Kavinsky")))
  }

  override suspend fun getUserTopTags(user: String, limit: Int): Result<List<Tag>> {
    userTopTagsCallCount++
    if (shouldFail) return Result.failure(RuntimeException("Network error"))
    return Result.success(listOf(Tag("synthwave", 80), Tag("electronic", 60)))
  }
}

class FakePreferencesDataStore(private var prefs: Preferences) : DataStore<Preferences> {
  override val data: Flow<Preferences> get() = flowOf(prefs)
  override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
    prefs = transform(prefs)
    return prefs
  }
}

class TasteProfileProviderTest {
  private lateinit var fakeDao: FakeTasteProfileDao
  private lateinit var fakeApi: FakeLastFmTasteApi
  private lateinit var fakeStatsDao: FakeSongPlayStatsDao
  private lateinit var fakeDataStore: FakePreferencesDataStore
  private lateinit var provider: TasteProfileProvider

  @Before
  fun setup() {
    fakeDao = FakeTasteProfileDao()
    fakeApi = FakeLastFmTasteApi()
    fakeStatsDao = FakeSongPlayStatsDao()
    fakeDataStore = FakePreferencesDataStore(mutablePreferencesOf(LastFMUsernameKey to "audioscrobbler_user"))
    provider = TasteProfileProvider(fakeDao, fakeApi, fakeDataStore, fakeStatsDao)
  }

  @Test
  fun testCacheHitWithinOneHourDoesNotFetchAgain() = runTest {
    // 1. First call fetches from API
    val firstProfile = provider.get()
    assertNotNull("First profile should not be null", firstProfile)
    assertEquals(1, fakeApi.userTopArtistsCallCount)
    assertEquals(1, fakeApi.userTopTracksCallCount)
    assertEquals(1, fakeApi.userTopTagsCallCount)

    // 2. Second call within TTL returns cached instance without network call
    val secondProfile = provider.get()
    assertNotNull(secondProfile)
    assertEquals(1, fakeApi.userTopArtistsCallCount) // Call count must remain 1
    assertEquals(firstProfile?.fetchedAtMillis, secondProfile?.fetchedAtMillis)
  }

  @Test
  fun testForceRefreshBypassesCache() = runTest {
    provider.get()
    assertEquals(1, fakeApi.userTopArtistsCallCount)

    provider.get(forceRefresh = true)
    assertEquals(2, fakeApi.userTopArtistsCallCount)
  }

  @Test
  fun testNetworkFailureReturnsStaleProfileFromDatabase() = runTest {
    // Pre-populate DAO with stale database entry
    fakeDao.storedProfile = TasteProfileEntity(
      id = 1,
      topArtistsJson = "[\"Radiohead\"]",
      topTracksJson = "[\"creep|radiohead\"]",
      topGenresJson = "[\"alt-rock\"]",
      confidence = 1.0f,
      updatedAtMillis = System.currentTimeMillis() - 7200_000L, // 2 hours old
    )

    // Simulate API failure
    fakeApi.shouldFail = true

    val profile = provider.get(forceRefresh = true)
    assertNotNull("Should return stale profile on API failure", profile)
    assertTrue("Should contain Radiohead from stale record", profile!!.topArtistNames.contains("radiohead"))
  }
}
