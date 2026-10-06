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
import echo.music.iad1tya.models.toMediaMetadata
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

internal const val MAX_SUGGESTIONS = 25
internal const val PLAY_WEIGHT = 2.0f
internal const val SKIP_PENALTY = 3.0f
internal const val LIKED_BONUS = 5.0f
internal const val RECENCY_BONUS = 1.5f
internal const val TIME_OF_DAY_BONUS = 1.3f

private val MORNING = 6..11
private val AFTERNOON = 12..17
private val EVENING = 18..21

internal fun currentTimeOfDay(hour: Int = LocalTime.now().hour): String = when (hour) {
  in MORNING -> "morning"
  in AFTERNOON -> "afternoon"
  in EVENING -> "evening"
  else -> "night"
}

internal fun scoreStats(
  stats: SongPlayStatsEntity,
  likedKeys: Set<String>,
  timeOfDay: String,
): Float {
  val isLiked = stats.trackKey in likedKeys
  val isRecent = System.currentTimeMillis() - stats.lastPlayedAtMillis < 7L * 86_400_000L

  val playMinutes = (stats.totalPlayTimeMs.coerceAtLeast(0L) / 60_000.0)
  val normalizedPlayTime = kotlin.math.ln(1.0 + playMinutes).toFloat()
  var score = normalizedPlayTime * PLAY_WEIGHT
  score -= stats.skipCount.toFloat() * SKIP_PENALTY
  if (isLiked) score += LIKED_BONUS
  if (isRecent) score += RECENCY_BONUS

  val hourBonus = when (timeOfDay) {
    "morning" -> if (isRecent) TIME_OF_DAY_BONUS else 1.0f
    "night" -> if (isLiked) TIME_OF_DAY_BONUS else 1.0f
    else -> 1.0f
  }
  score *= hourBonus
  return score.coerceAtLeast(0f)
}

internal fun applyCandidateFilter(stats: SongPlayStatsEntity): Boolean =
  stats.skipCount < 2 || stats.totalPlayTimeMs > 45_000L

@Singleton
open class LocalTasteEngine @Inject constructor(
  private val songPlayStatsDao: SongPlayStatsDao,
  private val recommendationExclusionDao: RecommendationExclusionDao,
  private val database: MusicDatabase? = null,
) {
  open suspend fun generate(count: Int = MAX_SUGGESTIONS): Result<String> = withContext(Dispatchers.IO) {
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

      val seedVideoIds = eligibleSeeds.mapNotNull { it.videoId }.distinct().take(5)

      val expandedSongs = mutableListOf<SongItem>()
      val seenKeys = mutableSetOf<String>()
      seenKeys.addAll(exclusions)

      for (videoId in seedVideoIds) {
        val nextResult = runCatching {
          YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
        }.getOrNull()

        val candidateSongs = mutableListOf<SongItem>()
        nextResult?.items?.filterIsInstance<SongItem>()?.let { candidateSongs.addAll(it) }

        nextResult?.relatedEndpoint?.let { endpoint ->
          runCatching {
            YouTube.related(endpoint).getOrNull()?.songs
          }.getOrNull()?.let { candidateSongs.addAll(it) }
        }

        for (songItem in candidateSongs) {
          val key = trackKeyOf(songItem.title, songItem.artists.joinToString(", ") { it.name })
          if (key !in seenKeys) {
            seenKeys.add(key)
            expandedSongs.add(songItem)
          }
        }
        if (expandedSongs.size >= count) break
      }

      val finalSongs = if (expandedSongs.size >= 5) {
        expandedSongs.take(count)
      } else {
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
        (expandedSongs + localFallbackItems).distinctBy { it.id }.take(count)
      }

      if (finalSongs.isEmpty()) {
        return@withContext Result.failure(IllegalStateException("Could not generate playlist from seeds"))
      }

      val playlistId = "local_taste_${System.currentTimeMillis()}"
      val playlistName = "Taste Mix (${timeOfDay.replaceFirstChar { it.uppercase() }})"

      persistPlaylist(playlistId, playlistName, finalSongs)

      Result.success(playlistId)
    } catch (e: Exception) {
      Timber.e(e, "LocalTasteEngine generation failed")
      Result.failure(e)
    }
  }

  protected suspend fun persistPlaylist(
    playlistId: String,
    playlistName: String,
    songs: List<SongItem>,
  ) {
    database?.withTransaction {
      insert(
        PlaylistEntity(
          id = playlistId,
          name = playlistName,
          browseId = null,
        )
      )

      songs.forEachIndexed { index, songItem ->
        insert(songItem.toMediaMetadata())
        insert(
          PlaylistSongMap(
            playlistId = playlistId,
            songId = songItem.id,
            position = index,
          )
        )
      }
    }
  }
}
