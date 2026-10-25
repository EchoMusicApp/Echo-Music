package com.music.echo.ui.screens.wrapped

import androidx.compose.runtime.Immutable
import echo.music.iad1tya.db.entities.Album
import echo.music.iad1tya.db.entities.Artist
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongWithStats

enum class WrappedTimeRange(val label: String) {
  THIS_YEAR("This Year"),
  LAST_YEAR("Last Year"),
  ALL_TIME("All Time")
}

@Immutable
data class WrappedData(
  val timeRange: WrappedTimeRange,
  val yearLabel: String,
  val totalPlayTimeMs: Long,
  val totalMinutes: Long = totalPlayTimeMs / 60000L,
  val uniqueSongsCount: Int,
  val uniqueArtistsCount: Int,
  val uniqueAlbumsCount: Int,
  val topSong: Song?,
  val topSongStats: SongWithStats?,
  val top5Songs: List<Song>,
  val top5SongsStats: List<SongWithStats>,
  val topArtist: Artist?,
  val top5Artists: List<Artist>,
  val topAlbum: Album?,
  val top5Albums: List<Album>,
  val dominantVibe: String = "Diverse Explorer"
)

sealed interface WrappedUiState {
  object Loading : WrappedUiState
  data class InsufficientData(
    val songsPlayed: Int,
    val minSongsRequired: Int = 3
  ) : WrappedUiState
  data class Ready(val data: WrappedData) : WrappedUiState
}
