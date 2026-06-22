package iad1tya.echo.music.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iad1tya.echo.music.db.entities.BrainActivityLogEntity
import iad1tya.echo.music.db.entities.PlayEventEntity
import iad1tya.echo.music.db.entities.TasteProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EchoBrainDao {

    @Insert
    suspend fun insertActivityLog(log: BrainActivityLogEntity)

    @Query("SELECT * FROM brain_activity_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivityLogs(limit: Int): Flow<List<BrainActivityLogEntity>>

    @Insert
    suspend fun insertPlayEvent(event: PlayEventEntity)

    @Query("SELECT * FROM play_event ORDER BY startTime DESC")
    fun getAllPlayEvents(): Flow<List<PlayEventEntity>>

    @Query("SELECT COUNT(*) FROM play_event")
    fun getPlayEventCount(): Flow<Int>

    @Query("SELECT SUM(durationMs) FROM play_event")
    fun getTotalListeningTime(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTasteProfile(profile: TasteProfileEntity)

    @Query("SELECT * FROM taste_profile WHERE id = 1")
    fun getTasteProfile(): Flow<TasteProfileEntity?>
}
