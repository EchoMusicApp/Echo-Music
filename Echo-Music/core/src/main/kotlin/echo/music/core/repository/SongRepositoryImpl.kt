package echo.music.core.repository

import echo.music.core.mapping.toDomain
import echo.music.domain.models.Song as DomainSong
import echo.music.domain.repositories.SongRepository
import echo.music.iad1tya.db.MusicDatabase
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SongRepositoryImpl @Inject constructor(
  private val database: MusicDatabase
) : SongRepository {

  override fun getSong(id: String): Flow<DomainSong?> {
    return database.song(id).map { it?.toDomain() }
  }

  override fun getLikedSongs(): Flow<List<DomainSong>> {
    return database.likedSongsByRowIdAsc().map { list ->
      list.map { it.toDomain() }
    }
  }

  override fun getRecentlyPlayed(limit: Int): Flow<List<DomainSong>> {
    return database.events().map { events ->
      events.map { it.song.toDomain() }.distinctBy { it.id }.take(limit)
    }
  }

  override suspend fun updateLiked(id: String, isLiked: Boolean) {
    val likedDate = if (isLiked) LocalDateTime.now() else null
    database.query {
      updateLikedStatus(id, isLiked, likedDate)
    }
  }

  override suspend fun incrementPlayTime(id: String, durationMs: Long) {
    database.query {
      incrementTotalPlayTime(id, durationMs)
    }
  }
}
