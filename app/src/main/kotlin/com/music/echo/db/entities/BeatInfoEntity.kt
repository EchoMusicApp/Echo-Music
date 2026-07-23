package iad1tya.echo.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beat_info")
data class BeatInfoEntity(
    @PrimaryKey val songId: String,
    val bpm: Float,
    val firstBeatOffsetMs: Long,
    val confidence: Float,
    val analyzedAt: Long = System.currentTimeMillis(),
    /** Where the incoming track should start: first sustained-energy downbeat past the intro. */
    val mixInPointMs: Long? = null,
    /** Where the outgoing track's body ends (outro begins); transition starts here. */
    val mixOutPointMs: Long? = null,
    /** 0=C, 1=C#, ... 11=B. Null when the track's chroma signal was too weak to call a key. */
    val keyPitchClass: Int? = null,
    val keyIsMinor: Boolean? = null,
)
