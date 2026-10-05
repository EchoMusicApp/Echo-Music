package echo.music.iad1tya.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongPlayStatsDao {
  @Query("SELECT * FROM song_play_stats WHERE trackKey = :trackKey")
  suspend fun find(trackKey: String): SongPlayStatsEntity?

  @Query("SELECT * FROM song_play_stats WHERE trackKey IN (:trackKeys)")
  suspend fun findByKeys(trackKeys: List<String>): List<SongPlayStatsEntity>

  @Upsert
  suspend fun upsert(entity: SongPlayStatsEntity)

  @Query("SELECT * FROM song_play_stats WHERE lastPlayedAtMillis >= :sinceMillis ORDER BY totalPlayTimeMs DESC LIMIT :limit")
  suspend fun mostPlayedSince(sinceMillis: Long, limit: Int): List<SongPlayStatsEntity>

  @Query("SELECT * FROM song_play_stats ORDER BY lastPlayedAtMillis DESC LIMIT :limit")
  suspend fun recentlyPlayed(limit: Int): List<SongPlayStatsEntity>

  @Query("SELECT * FROM song_play_stats ORDER BY totalPlayTimeMs DESC LIMIT :limit")
  fun observeTopPlayed(limit: Int): Flow<List<SongPlayStatsEntity>>

  @Query("SELECT trackKey FROM song_play_stats")
  suspend fun getAllTrackKeys(): List<String>

  @Query("DELETE FROM song_play_stats")
  suspend fun clearAll()
}
