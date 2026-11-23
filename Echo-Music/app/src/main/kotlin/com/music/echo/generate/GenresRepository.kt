package echo.music.iad1tya.generate

import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.utils.lastfm.LastFmTasteApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

import echo.music.iad1tya.db.daos.SongPlayStatsDao

data class GenreWeight(
  val genre: String,
  val weight: Float,
)

private data class CachedTrackGenres(
  val genres: List<String>,
  val timestamp: Long,
)

const val GENRE_CACHE_TTL_MILLIS = 60L * 60 * 1000 // 1 hour

@Singleton
open class GenresRepository @Inject constructor(
  private val database: MusicDatabase? = null,
  private val tasteApi: LastFmTasteApi,
  private val tasteProfileProvider: TasteProfileProvider,
  private val songPlayStatsDao: SongPlayStatsDao? = null,
) {
  private val statsDao: SongPlayStatsDao?
    get() = songPlayStatsDao ?: database?.songPlayStatsDao

  private val trackGenresCache = ConcurrentHashMap<String, CachedTrackGenres>()

  open suspend fun topGenres(limit: Int = 10): List<GenreWeight> = withContext(Dispatchers.IO) {
    val genreScores = mutableMapOf<String, Float>()

    // 1. Incorporate top genres from cached TasteProfile snapshot
    try {
      val profile = tasteProfileProvider.get()
      if (profile != null && profile.topGenres.isNotEmpty()) {
        profile.topGenres.forEachIndexed { index, genre ->
          val weight = (profile.topGenres.size - index) * 1000f
          genreScores[genre.lowercase()] = genreScores.getOrDefault(genre.lowercase(), 0f) + weight
        }
      }
    } catch (e: Exception) {
      Timber.d(e, "Error reading taste profile for top genres")
    }

    // 2. Sample top played tracks from database listening history
    try {
      val topTracks = statsDao?.mostPlayedSince(0L, limit = 15).orEmpty()
      if (topTracks.isNotEmpty()) {
        coroutineScope {
          val genreDeferreds = topTracks.map { stats ->
            async {
              val genres = withTimeoutOrNull(2000L) {
                genresForTrack(stats.trackKey)
              }.orEmpty()
              stats to genres
            }
          }
          val results = genreDeferreds.map { it.await() }
          for ((stats, genres) in results) {
            val trackWeight = stats.totalPlayTimeMs.toFloat().coerceAtLeast(10f)
            for (genre in genres) {
              val key = genre.lowercase()
              genreScores[key] = genreScores.getOrDefault(key, 0f) + trackWeight
            }
          }
        }
      }
    } catch (e: Exception) {
      Timber.d(e, "Error aggregating genres from play stats")
    }

    genreScores.entries
      .sortedByDescending { it.value }
      .take(limit)
      .map { GenreWeight(genre = it.key, weight = it.value) }
  }

  open suspend fun genresForTrack(trackKey: String): List<String> = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    trackGenresCache[trackKey]?.let { cached ->
      if (now - cached.timestamp < GENRE_CACHE_TTL_MILLIS) {
        return@withContext cached.genres
      }
    }

    val parts = trackKey.split("|")
    val artist = if (parts.size >= 2) parts[1].trim() else ""
    if (artist.isBlank()) {
      return@withContext emptyList()
    }

    val tagsResult = try {
      tasteApi.getTopTags(artist)
    } catch (e: Exception) {
      Timber.d(e, "Failed to get tags for trackKey: $trackKey")
      null
    }

    if (tagsResult != null && tagsResult.isSuccess) {
      val fetched = tagsResult.getOrNull().orEmpty().map { it.name.trim().lowercase() }.distinct()
      trackGenresCache[trackKey] = CachedTrackGenres(genres = fetched, timestamp = now)
      fetched
    } else {
      if (tagsResult != null && tagsResult.isFailure) {
        Timber.d(tagsResult.exceptionOrNull(), "Failed to get tags for trackKey: $trackKey")
      }
      emptyList()
    }
  }

  fun clearCache() {
    trackGenresCache.clear()
  }
}
