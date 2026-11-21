package echo.music.iad1tya.generate

import echo.music.iad1tya.db.daos.SongPlayStatsDao
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

fun trackKeyOf(title: String, artist: String): String =
  "${title.trim()}|${artist.trim()}".lowercase()

@Singleton
class SongPlayStatsRepository @Inject constructor(
  private val songPlayStatsDao: SongPlayStatsDao,
) {
  private val mutex = Mutex()

  suspend fun recordListenedMs(
    title: String,
    artist: String,
    videoId: String?,
    artworkUrl: String?,
    listenedMs: Long,
    completed: Boolean,
  ) = withContext(Dispatchers.IO) {
    if (title.isBlank()) return@withContext
    val key = trackKeyOf(title, artist)
    try {
      mutex.withLock {
        val existing = songPlayStatsDao.find(key)
        val now = System.currentTimeMillis()
        val updated = if (existing != null) {
          existing.copy(
            videoId = videoId ?: existing.videoId,
            artworkUrl = artworkUrl ?: existing.artworkUrl,
            totalPlayTimeMs = existing.totalPlayTimeMs + listenedMs,
            playCount = if (completed) existing.playCount + 1 else existing.playCount,
            lastPlayedAtMillis = now,
          )
        } else {
          SongPlayStatsEntity(
            trackKey = key,
            title = title,
            artist = artist,
            videoId = videoId,
            artworkUrl = artworkUrl,
            totalPlayTimeMs = listenedMs,
            playCount = if (completed) 1 else 0,
            skipCount = 0,
            lastPlayedAtMillis = now,
          )
        }
        songPlayStatsDao.upsert(updated)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.e(e, "Failed to record listened ms for $key")
    }
  }

  /**
   * Eligible offline-cache candidates: at least two completed plays overall, recent activity,
   * and fewer skips than completed plays.
   */
  suspend fun automaticDownloadCandidates(
    sinceMillis: Long,
    limit: Int,
  ): List<SongPlayStatsEntity> = withContext(Dispatchers.IO) {
    try {
      songPlayStatsDao.mostPlayedSince(sinceMillis, limit.coerceAtLeast(1))
        .asSequence()
        .filter {
          it.playCount >= 2 &&
            it.skipCount < it.playCount &&
            !it.videoId.isNullOrBlank() &&
            it.title.isNotBlank()
        }
        .sortedWith(
          compareByDescending<SongPlayStatsEntity> { it.playCount }
            .thenByDescending { it.totalPlayTimeMs }
        )
        .toList()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.e(e, "Failed to load automatic-download candidates")
      emptyList()
    }
  }

  suspend fun recordSkip(
    title: String,
    artist: String,
    videoId: String?,
    artworkUrl: String?,
    listenedMs: Long,
  ) = withContext(Dispatchers.IO) {
    if (title.isBlank()) return@withContext
    val key = trackKeyOf(title, artist)
    try {
      mutex.withLock {
        val existing = songPlayStatsDao.find(key)
        val now = System.currentTimeMillis()
        val updated = if (existing != null) {
          existing.copy(
            videoId = videoId ?: existing.videoId,
            artworkUrl = artworkUrl ?: existing.artworkUrl,
            totalPlayTimeMs = existing.totalPlayTimeMs + listenedMs,
            skipCount = existing.skipCount + 1,
            lastPlayedAtMillis = now,
          )
        } else {
          SongPlayStatsEntity(
            trackKey = key,
            title = title,
            artist = artist,
            videoId = videoId,
            artworkUrl = artworkUrl,
            totalPlayTimeMs = listenedMs,
            playCount = 0,
            skipCount = 1,
            lastPlayedAtMillis = now,
          )
        }
        songPlayStatsDao.upsert(updated)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.e(e, "Failed to record skip for $key")
    }
  }
}
