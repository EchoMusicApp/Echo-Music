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
