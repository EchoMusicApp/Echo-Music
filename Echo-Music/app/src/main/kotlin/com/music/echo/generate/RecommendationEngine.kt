package echo.music.iad1tya.generate

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.daos.RecommendationExclusionDao
import echo.music.iad1tya.db.daos.SongPlayStatsDao
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import echo.music.iad1tya.utils.lastfm.LastFmTasteApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

const val SIMILAR_TRACK_BONUS = 2.0f
const val SIMILAR_ARTIST_BONUS = 1.0f
const val TAG_MATCH_BONUS = 1.5f
const val GENRE_MATCH_BONUS = 1.0f

fun scoreCandidateWithExternals(
  baseScore: Float,
  isSimilarTrack: Boolean = false,
  isSimilarArtist: Boolean = false,
  matchesTopGenre: Boolean = false,
  matchesGenrePreference: Boolean = false,
): Float {
  var score = baseScore
  if (isSimilarTrack) score += SIMILAR_TRACK_BONUS
  if (isSimilarArtist) score += SIMILAR_ARTIST_BONUS
  if (matchesTopGenre) score += TAG_MATCH_BONUS
  if (matchesGenrePreference) score += GENRE_MATCH_BONUS
  return score
}

@Singleton
open class RecommendationEngine @Inject constructor(
  private val songPlayStatsDao: SongPlayStatsDao,
  private val recommendationExclusionDao: RecommendationExclusionDao,
  private val tasteProfileProvider: TasteProfileProvider,
  private val lastFmTasteApi: LastFmTasteApi,
  private val genresRepository: GenresRepository? = null,
  private val database: MusicDatabase? = null,
) : LocalTasteEngine(songPlayStatsDao, recommendationExclusionDao, database) {
  var allowNullDatabaseForTesting: Boolean = false

  override suspend fun generate(count: Int): Result<String> = withContext(Dispatchers.IO) {
    try {
      val exclusions = recommendationExclusionDao.getAll().map { it.trackKey }.toSet()
      val timeOfDay = currentTimeOfDay()

      val now = System.currentTimeMillis()
      val thirtyDaysAgo = now - 30L * 86_400_000L

      val recentStats = songPlayStatsDao.mostPlayedSince(thirtyDaysAgo, limit = 100)
        .ifEmpty { songPlayStatsDao.recentlyPlayed(limit = 20) }

      if (recentStats.isEmpty()) {
        return@withContext Result.failure(IllegalStateException("No listening history available yet"))
      }

      val likedSongs = database?.likedSongsByRowIdAsc()?.firstOrNull().orEmpty()
      val likedKeys = likedSongs.map { song ->
        trackKeyOf(song.song.title, song.artists.joinToString(", ") { it.name })
      }.toSet()

      val eligibleSeeds = recentStats
        .filter { it.trackKey !in exclusions && applyCandidateFilter(it) }
        .sortedByDescending { scoreStats(it, likedKeys, timeOfDay) }

      if (eligibleSeeds.isEmpty()) {
        return@withContext Result.failure(IllegalStateException("All candidate tracks have been excluded"))
      }

      // 1. Fetch taste profile (cached 1hr) and top genres
      val tasteProfile = runCatching { tasteProfileProvider.get() }.getOrNull()
      val userTopGenres = mutableSetOf<String>()
      tasteProfile?.topGenres?.forEach { userTopGenres.add(it.lowercase()) }
      genresRepository?.let { repo ->
        runCatching {
          repo.topGenres(15).forEach { userTopGenres.add(it.genre.lowercase()) }
        }
      }

      // 2. Fetch external signals from Last.fm in parallel with seed expansion
      val topSeeds = eligibleSeeds.take(3)
      val similarTrackKeys = mutableSetOf<String>()
      val similarArtistNames = mutableSetOf<String>()
      val externalCandidates = mutableListOf<SongItem>()

      try {
        coroutineScope {
          val externalJobs = topSeeds.map { seed ->
            async {
              val similarTracksRes = lastFmTasteApi.getSimilarTracks(seed.artist, seed.title)
              val similarArtistsRes = lastFmTasteApi.getSimilarArtists(seed.artist)
              seed to Pair(similarTracksRes.getOrNull().orEmpty(), similarArtistsRes.getOrNull().orEmpty())
            }
          }
          val results = externalJobs.map { it.await() }
          for ((_, pair) in results) {
            val (tracks, artists) = pair
            for (t in tracks) {
              similarTrackKeys.add(trackKeyOf(t.name, t.artist))
            }
            for (a in artists) {
              similarArtistNames.add(a.name.trim().lowercase())
            }

            // Resolve top similar tracks on YouTube if not already excluded
            for (track in tracks.take(5)) {
              val key = trackKeyOf(track.name, track.artist)
              if (key in exclusions) continue

              // Search YouTube Music for playable item
              val searchResult = runCatching {
                YouTube.search(query = "${track.artist} ${track.name}", filter = YouTube.SearchFilter.FILTER_SONG).getOrNull()
              }.getOrNull()

              val songItem = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull()
              if (songItem != null) {
                externalCandidates.add(songItem)
              }
            }
          }
        }
      } catch (e: Exception) {
        Timber.d(e, "External signal expansion failed, falling back to local seeds")
      }

      // 3. Local YouTube related tracks expansion (Slice 1 path)
      val seedVideoIds = eligibleSeeds.mapNotNull { it.videoId }.distinct().take(5)
      val localCandidates = mutableListOf<SongItem>()

      for (videoId in seedVideoIds) {
        val nextResult = runCatching {
          YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
        }.getOrNull()

        nextResult?.items?.filterIsInstance<SongItem>()?.let { localCandidates.addAll(it) }

        nextResult?.relatedEndpoint?.let { endpoint ->
          runCatching {
            YouTube.related(endpoint).getOrNull()?.songs
          }.getOrNull()?.let { localCandidates.addAll(it) }
        }
      }

      // 4. Merge candidates and calculate composite scores
      data class CandidateWithScore(val song: SongItem, val score: Float)
      val seenKeys = mutableSetOf<String>()
      seenKeys.addAll(exclusions)

      // Cap combined candidates before scoring (e.g. count * 3)
      val maxCandidatesToScore = (count * 3).coerceAtLeast(30)
      val combinedCandidates = (externalCandidates + localCandidates)
        .distinctBy { it.id }
        .filter { songItem ->
          val artistName = songItem.artists.joinToString(", ") { it.name }.trim()
          val key = trackKeyOf(songItem.title, artistName)
          if (key in seenKeys) false else {
            seenKeys.add(key)
            true
          }
        }
        .take(maxCandidatesToScore)

      // Batch Room stats lookups instead of calling find for each candidate
      val candidateKeys = combinedCandidates.map { songItem ->
        val artistName = songItem.artists.joinToString(", ") { it.name }.trim()
        trackKeyOf(songItem.title, artistName)
      }
      val statsByKey: Map<String, SongPlayStatsEntity> = songPlayStatsDao.findByKeys(candidateKeys)
        .associateBy { it.trackKey }

      // Resolve genres once per distinct artist in parallel within a timeout
      val distinctArtists = combinedCandidates
        .mapNotNull { it.artists.firstOrNull()?.name?.trim()?.takeIf { a -> a.isNotBlank() } }
        .distinct()

      val genresByArtist: Map<String, List<String>> = if (genresRepository != null && distinctArtists.isNotEmpty()) {
        kotlinx.coroutines.withTimeoutOrNull(2000L) {
          coroutineScope {
            distinctArtists.map { artist ->
              async {
                val dummyKey = "track|${artist.lowercase()}"
                val genres = runCatching { genresRepository.genresForTrack(dummyKey) }.getOrDefault(emptyList())
                artist.lowercase() to genres
              }
            }.map { it.await() }.toMap()
          }
        } ?: emptyMap()
      } else {
        emptyMap()
      }

      val scoredCandidates = mutableListOf<CandidateWithScore>()

      for (songItem in combinedCandidates) {
        val artistName = songItem.artists.joinToString(", ") { it.name }.trim()
        val key = trackKeyOf(songItem.title, artistName)

        val localStats = statsByKey[key]
        val baseScore = if (localStats != null) {
          scoreStats(localStats, likedKeys, timeOfDay)
        } else {
          // Unplayed discovery candidate base score
          5.0f
        }

        val isSimilarTrack = key in similarTrackKeys
        val isSimilarArtist = artistName.lowercase() in similarArtistNames
        val leadArtist = songItem.artists.firstOrNull()?.name?.trim()?.lowercase().orEmpty()
        val candidateGenres = genresByArtist[leadArtist].orEmpty()

        val hasGenreIntersection = if (userTopGenres.isNotEmpty() && candidateGenres.isNotEmpty()) {
          candidateGenres.any { it.lowercase() in userTopGenres }
        } else {
          false
        }

        val matchesTopGenre = hasGenreIntersection
        val matchesGenrePref = if (userTopGenres.isNotEmpty()) {
          hasGenreIntersection || userTopGenres.any { g ->
            val regex = "\\b${Regex.escape(g)}\\b".toRegex(RegexOption.IGNORE_CASE)
            regex.containsMatchIn(songItem.title) || regex.containsMatchIn(artistName)
          }
        } else {
          false
        }

        val finalScore = scoreCandidateWithExternals(
          baseScore = baseScore,
          isSimilarTrack = isSimilarTrack,
          isSimilarArtist = isSimilarArtist,
          matchesTopGenre = matchesTopGenre,
          matchesGenrePreference = matchesGenrePref,
        )

        scoredCandidates.add(CandidateWithScore(songItem, finalScore))
      }

      val sortedSongs = scoredCandidates
        .sortedByDescending { it.score }
        .map { it.song }

      val finalSongs = if (sortedSongs.size >= 5) {
        sortedSongs.take(count)
      } else {
        // Fallback to local items if needed
        val localFallbackItems = eligibleSeeds
          .filter { it.videoId != null }
          .take(count)
          .map { stats ->
            SongItem(
              id = stats.videoId!!,
              title = stats.title,
              artists = listOf(com.music.innertube.models.Artist(name = stats.artist, id = null)),
              album = null,
              duration = null,
              thumbnail = stats.artworkUrl.orEmpty(),
            )
          }
        (sortedSongs + localFallbackItems).distinctBy { it.id }.take(count)
      }

      if (finalSongs.isEmpty()) {
        return@withContext super.generate(count)
      }

      val playlistId = "local_taste_${System.currentTimeMillis()}"
      val playlistName = "Taste Mix (${timeOfDay.replaceFirstChar { it.uppercase() }})"

      if (database == null && !allowNullDatabaseForTesting) {
        return@withContext Result.failure(IllegalStateException("Database not available to persist playlist"))
      }

      persistPlaylist(playlistId, playlistName, finalSongs)

      Result.success(playlistId)
    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) throw e
      Timber.e(e, "RecommendationEngine generation failed, attempting local fallback")
      super.generate(count)
    }
  }
}
