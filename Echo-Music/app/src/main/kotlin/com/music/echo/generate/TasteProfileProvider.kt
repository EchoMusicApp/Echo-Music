package echo.music.iad1tya.generate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import echo.music.iad1tya.constants.LastFMUsernameKey
import echo.music.iad1tya.db.daos.SongPlayStatsDao
import echo.music.iad1tya.db.daos.TasteProfileDao
import echo.music.iad1tya.db.entities.TasteProfileEntity
import echo.music.iad1tya.utils.lastfm.LastFmTasteApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

data class TasteProfile(
  val topArtistNames: List<String>,
  val topTrackKeys: List<String>,
  val topGenres: List<String>,
  val fetchedAtMillis: Long,
)

const val TASTE_PROFILE_TTL_MILLIS = 60L * 60 * 1000 // 1 hour

@Singleton
open class TasteProfileProvider @Inject constructor(
  private val tasteProfileDao: TasteProfileDao,
  private val lastFmTasteApi: LastFmTasteApi,
  private val dataStore: DataStore<Preferences>,
  private val songPlayStatsDao: SongPlayStatsDao,
) {
  private val mutex = Mutex()
  @Volatile private var inMemoryCache: TasteProfile? = null

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  open suspend fun get(forceRefresh: Boolean = false): TasteProfile? = withContext(Dispatchers.IO) {
    mutex.withLock {
      val now = System.currentTimeMillis()

      // 1. Check in-memory cache
      val memorySnapshot = inMemoryCache
      if (!forceRefresh && memorySnapshot != null && (now - memorySnapshot.fetchedAtMillis) < TASTE_PROFILE_TTL_MILLIS) {
        return@withLock memorySnapshot
      }

      // 2. Check Room database cache
      if (!forceRefresh) {
        try {
          val dbEntity = tasteProfileDao.get()
          if (dbEntity != null && (now - dbEntity.updatedAtMillis) < TASTE_PROFILE_TTL_MILLIS) {
            val profile = TasteProfile(
              topArtistNames = runCatching { json.decodeFromString<List<String>>(dbEntity.topArtistsJson) }.getOrDefault(emptyList()).map { it.lowercase() },
              topTrackKeys = runCatching { json.decodeFromString<List<String>>(dbEntity.topTracksJson) }.getOrDefault(emptyList()),
              topGenres = runCatching { json.decodeFromString<List<String>>(dbEntity.topGenresJson) }.getOrDefault(emptyList()).map { it.lowercase() },
              fetchedAtMillis = dbEntity.updatedAtMillis,
            )
            inMemoryCache = profile
            return@withLock profile
          }
        } catch (e: Exception) {
          Timber.d(e, "Error reading taste profile from database")
        }
      }

      // 3. Cache miss or expired: Fetch new profile
      val username = runCatching {
        dataStore.data.map { it[LastFMUsernameKey] }.first()
      }.getOrNull()?.trim().orEmpty()

      var fetchedArtists = emptyList<String>()
      var fetchedTracks = emptyList<String>()
      var fetchedGenres = emptyList<String>()
      var fetchSuccess = false

      if (username.isNotBlank()) {
        try {
          coroutineScope {
            val artistsDeferred = async { lastFmTasteApi.getUserTopArtists(username, limit = 30) }
            val tracksDeferred = async { lastFmTasteApi.getUserTopTracks(username, limit = 50) }
            val tagsDeferred = async { lastFmTasteApi.getUserTopTags(username, limit = 15) }

            val artistsResult = artistsDeferred.await()
            val tracksResult = tracksDeferred.await()
            val tagsResult = tagsDeferred.await()

            if (artistsResult.isSuccess || tracksResult.isSuccess || tagsResult.isSuccess) {
              fetchSuccess = true
              fetchedArtists = artistsResult.getOrNull().orEmpty().map { it.trim().lowercase() }
              fetchedTracks = tracksResult.getOrNull().orEmpty().map { trackKeyOf(it.name, it.artist) }
              fetchedGenres = tagsResult.getOrNull().orEmpty().map { it.name.trim().lowercase() }
            }
          }
        } catch (e: Exception) {
          Timber.d(e, "Last.fm taste fetch failed for $username")
        }
      }

      // If network fetch succeeded, construct and save profile
      if (fetchSuccess && (fetchedArtists.isNotEmpty() || fetchedTracks.isNotEmpty())) {
        val newProfile = TasteProfile(
          topArtistNames = fetchedArtists,
          topTrackKeys = fetchedTracks,
          topGenres = fetchedGenres,
          fetchedAtMillis = now,
        )
        inMemoryCache = newProfile
        try {
          tasteProfileDao.upsert(
            TasteProfileEntity(
              id = 1,
              topArtistsJson = json.encodeToString(newProfile.topArtistNames),
              topTracksJson = json.encodeToString(newProfile.topTrackKeys),
              topGenresJson = json.encodeToString(newProfile.topGenres),
              confidence = 1.0f,
              updatedAtMillis = now,
            )
          )
        } catch (e: Exception) {
          Timber.e(e, "Failed to persist taste profile to database")
        }
        return@withLock newProfile
      }

      // 4. On failure: Return stale cache if present
      val staleDbEntity = try { tasteProfileDao.get() } catch (e: Exception) { null }
      if (staleDbEntity != null) {
        val staleProfile = TasteProfile(
          topArtistNames = runCatching { json.decodeFromString<List<String>>(staleDbEntity.topArtistsJson) }.getOrDefault(emptyList()).map { it.lowercase() },
          topTrackKeys = runCatching { json.decodeFromString<List<String>>(staleDbEntity.topTracksJson) }.getOrDefault(emptyList()),
          topGenres = runCatching { json.decodeFromString<List<String>>(staleDbEntity.topGenresJson) }.getOrDefault(emptyList()).map { it.lowercase() },
          fetchedAtMillis = staleDbEntity.updatedAtMillis,
        )
        inMemoryCache = staleProfile
        return@withLock staleProfile
      }

      // 5. Fallback to local history snapshot
      val thirtyDaysAgo = now - 30L * 86_400_000L
      val localStats = songPlayStatsDao.mostPlayedSince(thirtyDaysAgo, limit = 50)
      if (localStats.isNotEmpty()) {
        val localProfile = TasteProfile(
          topArtistNames = localStats.map { it.artist.trim().lowercase() }.distinct(),
          topTrackKeys = localStats.map { it.trackKey },
          topGenres = emptyList(),
          fetchedAtMillis = now,
        )
        inMemoryCache = localProfile
        return@withLock localProfile
      }

      null
    }
  }

  suspend fun clear() = withContext(Dispatchers.IO) {
    mutex.withLock {
      inMemoryCache = null
      try {
        tasteProfileDao.clear()
      } catch (e: Exception) {
        Timber.e(e, "Failed to clear taste profile")
      }
    }
  }
}
