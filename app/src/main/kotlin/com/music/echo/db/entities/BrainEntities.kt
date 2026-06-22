package iad1tya.echo.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brain_activity_log")
data class BrainActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val reason: String,
    val timestamp: Long
)

@Entity(tableName = "play_event")
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackId: String,
    val startTime: Long,
    val durationMs: Long,
    val skipped: Boolean,
    val engaged: Boolean
)

@Entity(tableName = "taste_profile")
data class TasteProfileEntity(
    @PrimaryKey val id: Int = 1,
    val genres: String,
    val confidence: Float,
    val patternsFound: Int,
    val modelVersion: String,
    val updatedAt: Long
)
