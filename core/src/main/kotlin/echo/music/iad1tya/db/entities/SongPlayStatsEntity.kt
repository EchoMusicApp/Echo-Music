package echo.music.iad1tya.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "song_play_stats")
data class SongPlayStatsEntity(
  @PrimaryKey val trackKey: String,
  val title: String = "",
  val artist: String = "",
  val videoId: String? = null,
  val artworkUrl: String? = null,
  val totalPlayTimeMs: Long = 0L,
  val playCount: Int = 0,
  val skipCount: Int = 0,
  val lastPlayedAtMillis: Long = 0L,
)
