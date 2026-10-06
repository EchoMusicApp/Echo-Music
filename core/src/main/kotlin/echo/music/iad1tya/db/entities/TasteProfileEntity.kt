package echo.music.iad1tya.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taste_profile")
data class TasteProfileEntity(
  @PrimaryKey val id: Int = 1,
  val topArtistsJson: String = "",
  val topTracksJson: String = "",
  val topGenresJson: String = "",
  val confidence: Float = 0f,
  val updatedAtMillis: Long = 0L,
)
