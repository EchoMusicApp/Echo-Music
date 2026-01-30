package iad1tya.echo.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.AlbumEntity
import iad1tya.echo.music.db.entities.Artist
import iad1tya.echo.music.db.entities.Event
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.db.entities.SongEntity
import iad1tya.echo.music.ui.screens.OptionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

data class WrappedData(
    val topSongs: List<SongStats> = emptyList(),
    val topArtists: List<ArtistStats> = emptyList(),
    val totalTimeMillis: Long = 0,
    val totalSongsPlayed: Int = 0,
    val isLoading: Boolean = true
)

data class SongStats(
    val song: Song,
    val playCount: Int,
    val playTime: Long
)

data class ArtistStats(
    val artist: Artist,
    val playCount: Int,
    val playTime: Long
)

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrappedData())
    val uiState: StateFlow<WrappedData> = _uiState

    init {
        loadWrappedData()
    }

    private fun loadWrappedData() {
        viewModelScope.launch {
            // Define "Year" as roughly the last 365 days or since the beginning of the current year.
            // For a "Wrapped" feature, usually it's the current calendar year.
            val now = LocalDateTime.now()
            val startOfYear = LocalDateTime.of(now.year, 1, 1, 0, 0, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
            val endOfTime = now.toInstant(ZoneOffset.UTC).toEpochMilli()

            // Fetch Top Songs
            val topSongsFlow = database.mostPlayedSongsStats(
                fromTimeStamp = startOfYear,
                toTimeStamp = endOfTime,
                limit = 5
            )

            // Fetch Top Artists
            val topArtistsFlow = database.mostPlayedArtists(
                fromTimeStamp = startOfYear,
                toTimeStamp = endOfTime,
                limit = 5
            )
            
            combine(topSongsFlow, topArtistsFlow) { songs, artists ->
                val songsMapped = songs.map { 
                    val entity = SongEntity(
                        id = it.id,
                        title = it.title,
                        thumbnailUrl = it.thumbnailUrl
                    )
                    val songWrapper = Song(
                        song = entity,
                        artists = emptyList()
                    )
                    SongStats(songWrapper, it.songCountListened, it.timeListened ?: 0L) 
                }
                
                val artistsMapped = artists.map { 
                    ArtistStats(it, it.songCount, it.timeListened?.toLong() ?: 0L) 
                }

               WrappedData(
                   topSongs = songsMapped,
                   topArtists = artistsMapped,
                   isLoading = false
               )
            }.collect { data ->
                _uiState.value = data
            }
        }
        
        // Launch a separate coroutine to calculate true totals
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val startOfYear = LocalDateTime.of(now.year, 1, 1, 0, 0, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
            val endOfTime = now.toInstant(ZoneOffset.UTC).toEpochMilli()

             database.mostPlayedSongsStats(
                fromTimeStamp = startOfYear,
                toTimeStamp = endOfTime,
                limit = -1
            ).collect { allSongs ->
                val totalTime = allSongs.sumOf { (it.timeListened ?: 0L).toDouble() }.toLong()
                val totalCount = allSongs.sumOf { it.songCountListened }
                
                _uiState.value = _uiState.value.copy(
                    totalTimeMillis = totalTime,
                    totalSongsPlayed = totalCount
                )
            }
        }
    }
}
