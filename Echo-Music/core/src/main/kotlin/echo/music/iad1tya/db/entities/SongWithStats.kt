package echo.music.iad1tya.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class SongWithStats(
  val id: String,
  val title: String,
  val artistName: String? = null,
  val thumbnailUrl: String? = null,
  val songCountListened: Int? = 0,
  val timeListened: Long? = 0L,
  val isVideo: Boolean = false,
)
