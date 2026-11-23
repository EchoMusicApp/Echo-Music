package echo.music.iad1tya.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "recommendation_exclusions")
data class RecommendationExclusionEntity(
  @PrimaryKey val trackKey: String,
  val excludedAtMillis: Long,
  val trackName: String = "",
  val artistName: String = "",
)
