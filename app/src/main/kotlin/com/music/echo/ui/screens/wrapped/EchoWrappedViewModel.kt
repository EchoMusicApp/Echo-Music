package com.music.echo.ui.screens.wrapped

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EchoWrappedViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  val database: MusicDatabase,
) : ViewModel() {

  private val _selectedRange = MutableStateFlow(WrappedTimeRange.THIS_YEAR)
  val selectedRange: StateFlow<WrappedTimeRange> = _selectedRange.asStateFlow()

  private val _uiState = MutableStateFlow<WrappedUiState>(WrappedUiState.Loading)
  val uiState: StateFlow<WrappedUiState> = _uiState.asStateFlow()

  private val _isPlaylistSaving = MutableStateFlow(false)
  val isPlaylistSaving: StateFlow<Boolean> = _isPlaylistSaving.asStateFlow()

  private val _playlistSaved = MutableStateFlow(false)
  val playlistSaved: StateFlow<Boolean> = _playlistSaved.asStateFlow()

  init {
    loadWrappedData(WrappedTimeRange.THIS_YEAR)
  }

  fun setTimeRange(range: WrappedTimeRange) {
    if (_selectedRange.value == range) return
    _selectedRange.value = range
    loadWrappedData(range)
  }

  fun loadWrappedData(range: WrappedTimeRange) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.value = WrappedUiState.Loading

      val now = LocalDateTime.now()
      val (fromTimeStamp, toTimeStamp, yearLabel) =
        when (range) {
          WrappedTimeRange.THIS_YEAR -> {
            val startOfYear =
              now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0)
            Triple(
              startOfYear.toInstant(ZoneOffset.UTC).toEpochMilli(),
              now.toInstant(ZoneOffset.UTC).toEpochMilli(),
              now.year.toString()
            )
          }
          WrappedTimeRange.LAST_YEAR -> {
            val lastYear = now.minusYears(1)
            val startOfYear =
              lastYear.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0)
            val endOfYear =
              lastYear.withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59).withSecond(59)
            Triple(
              startOfYear.toInstant(ZoneOffset.UTC).toEpochMilli(),
              endOfYear.toInstant(ZoneOffset.UTC).toEpochMilli(),
              lastYear.year.toString()
            )
          }
          WrappedTimeRange.ALL_TIME -> {
            Triple(
              0L,
              now.toInstant(ZoneOffset.UTC).toEpochMilli(),
              "All-Time"
            )
          }
        }

      val uniqueSongsCount =
        database.getUniqueSongCountInRange(fromTimeStamp, toTimeStamp).first()

      // If user doesn't have enough data for the selected period, check all time as fallback recommendation
      if (uniqueSongsCount < 3) {
        val allTimeSongs = database.getUniqueSongCountInRange(0L, Long.MAX_VALUE).first()
        if (range != WrappedTimeRange.ALL_TIME && allTimeSongs >= 3) {
          // Auto fallback to All Time if current year has insufficient listens
          _selectedRange.value = WrappedTimeRange.ALL_TIME
          loadWrappedData(WrappedTimeRange.ALL_TIME)
          return@launch
        }
        _uiState.value =
          WrappedUiState.InsufficientData(
            songsPlayed = uniqueSongsCount,
            minSongsRequired = 3
          )
        return@launch
      }

      val totalPlayTimeMs =
        database.getTotalPlayTimeInRange(fromTimeStamp, toTimeStamp).first() ?: 0L
      val uniqueArtistsCount =
        database.getUniqueArtistCountInRange(fromTimeStamp, toTimeStamp).first()
      val uniqueAlbumsCount =
        database.getUniqueAlbumCountInRange(fromTimeStamp, toTimeStamp).first()

      val topSongsStats =
        database
          .mostPlayedSongsStats(
            fromTimeStamp = fromTimeStamp,
            limit = 50,
            toTimeStamp = toTimeStamp
          )
          .first()

      val topSongs =
        database
          .mostPlayedSongs(
            fromTimeStamp = fromTimeStamp,
            limit = 50,
            toTimeStamp = toTimeStamp
          )
          .first()

      val topArtists =
        database
          .mostPlayedArtists(
            fromTimeStamp = fromTimeStamp,
            limit = 10,
            toTimeStamp = toTimeStamp
          )
          .first()

      val topAlbums =
        database
          .mostPlayedAlbums(
            fromTimeStamp = fromTimeStamp,
            limit = 10,
            toTimeStamp = toTimeStamp
          )
          .first()

      val topSong = topSongs.firstOrNull()
      val topSongStat = topSongsStats.firstOrNull { it.id == topSong?.id } ?: topSongsStats.firstOrNull()
      val topArtist = topArtists.firstOrNull()
      val topAlbum = topAlbums.firstOrNull()

      val totalMinutes = totalPlayTimeMs / 60000L
      val dominantVibe =
        when {
          totalMinutes > 5000L -> "Certified Audiophile"
          uniqueArtistsCount > 30 -> "Musical Nomad"
          uniqueSongsCount > 100 -> "Infinite Playlist Curator"
          topArtist != null && (topArtist.timeListened ?: 0) > (totalPlayTimeMs / 3) ->
            "Devoted Superfan"
          else -> "Sonic Explorer"
        }

      val wrappedData =
        WrappedData(
          timeRange = range,
          yearLabel = yearLabel,
          totalPlayTimeMs = totalPlayTimeMs,
          totalMinutes = totalMinutes,
          uniqueSongsCount = uniqueSongsCount,
          uniqueArtistsCount = uniqueArtistsCount,
          uniqueAlbumsCount = uniqueAlbumsCount,
          topSong = topSong,
          topSongStats = topSongStat,
          top5Songs = topSongs.take(5),
          top5SongsStats = topSongsStats.take(5),
          topArtist = topArtist,
          top5Artists = topArtists.take(5),
          topAlbum = topAlbum,
          top5Albums = topAlbums.take(5),
          dominantVibe = dominantVibe
        )

      _uiState.value = WrappedUiState.Ready(wrappedData)
    }
  }

  fun createWrappedPlaylist(data: WrappedData, onComplete: (Boolean) -> Unit) {
    if (_isPlaylistSaving.value) return
    _isPlaylistSaving.value = true

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val playlistName = "Your ${data.yearLabel} Wrapped"
        val playlistEntity =
          PlaylistEntity(
            name = playlistName,
            bookmarkedAt = LocalDateTime.now(),
            isLocal = true,
            isEditable = true
          )
        database.insert(playlistEntity)
        val playlistId = playlistEntity.id

        val songsToInsert =
          if (data.top5Songs.isNotEmpty()) {
            database.mostPlayedSongs(
              fromTimeStamp = if (data.timeRange == WrappedTimeRange.ALL_TIME) 0L else {
                LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).toInstant(ZoneOffset.UTC).toEpochMilli()
              },
              limit = 50
            ).first().ifEmpty { data.top5Songs }
          } else {
            emptyList()
          }

        songsToInsert.take(50).forEachIndexed { index, song ->
          database.insert(
            PlaylistSongMap(playlistId = playlistId, songId = song.id, position = index)
          )
        }

        _playlistSaved.value = true
        _isPlaylistSaving.value = false
        withContext(Dispatchers.Main) {
          onComplete(true)
        }
      } catch (e: Exception) {
        _isPlaylistSaving.value = false
        withContext(Dispatchers.Main) {
          onComplete(false)
        }
      }
    }
  }
}
