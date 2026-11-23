package echo.music.domain.models

data class Song(
  val id: String,
  val title: String,
  val artists: List<ArtistRef>,
  val album: AlbumRef?,
  val durationSeconds: Int,
  val thumbnailUrl: String?,
  val isExplicit: Boolean = false,
  val isLocal: Boolean = false,
  val liked: Boolean = false,
  val totalPlayTimeMs: Long = 0L
)

data class ArtistRef(
  val id: String,
  val name: String
)

data class AlbumRef(
  val id: String,
  val title: String
)
