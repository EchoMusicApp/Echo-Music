package echo.music.iad1tya.ui.screens.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.echo.spotify.SpotifyPlaybackResolver
import com.music.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import echo.music.iad1tya.spotify.Spotify
import echo.music.iad1tya.spotify.models.SpotifyTrack
import echo.music.iad1tya.spotifyimport.SpotifyImportRepository
import echo.music.iad1tya.utils.reportException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SpotifyPlaylistDetailUiState(
  val playlistId: String = "",
  val title: String = "",
  val coverUrl: String? = null,
  val tracks: List<SpotifyTrack> = emptyList(),
  val totalCount: Int = 0,
  val isLoading: Boolean = false,
  val resolvingTrackId: String? = null,
  val error: String? = null,
)

@HiltViewModel
class SpotifyPlaylistDetailViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val spotifyImportRepository: SpotifyImportRepository,
  private val spotifyPlaybackResolver: SpotifyPlaybackResolver,
) : ViewModel() {

  private val playlistId: String = savedStateHandle.get<String>("playlistId").orEmpty()
  private val initialTitle: String = savedStateHandle.get<String>("title") ?: "Playlist"
  private val initialCover: String? = savedStateHandle.get<String>("cover")?.takeIf { it.isNotBlank() }

  private val _uiState = MutableStateFlow(
    SpotifyPlaylistDetailUiState(
      playlistId = playlistId,
      title = initialTitle,
      coverUrl = initialCover,
      isLoading = true,
    )
  )
  val uiState: StateFlow<SpotifyPlaylistDetailUiState> = _uiState.asStateFlow()

  init {
    loadTracks()
  }

  fun refresh() {
    loadTracks()
  }

  fun loadTracks() {
    viewModelScope.launch {
      fetchTracks()
    }
  }

  suspend fun refreshAndAwait(): Boolean = fetchTracks()

  private suspend fun fetchTracks(): Boolean = withContext(Dispatchers.IO) {
    _uiState.update { it.copy(isLoading = true, error = null) }
    try {
      spotifyImportRepository.restoreSession()

      val tracksList = mutableListOf<SpotifyTrack>()
      var total = 0

      if (playlistId == "liked") {
        val result = Spotify.likedSongs(limit = 50, offset = 0)
        val page = result.getOrThrow()
        tracksList.addAll(page.items.map { it.track })
        total = page.total
      } else {
        val result = Spotify.playlistTracks(playlistId = playlistId, limit = 50, offset = 0)
        val page = result.getOrThrow()
        tracksList.addAll(page.items.mapNotNull { it.track })
        total = page.total
      }

      _uiState.update {
        it.copy(
          tracks = tracksList,
          totalCount = total,
          isLoading = false,
          error = null,
        )
      }
      true
    } catch (e: Exception) {
      reportException(e)
      _uiState.update {
        it.copy(
          isLoading = false,
          error = e.message ?: "Failed to load tracks",
        )
      }
      false
    }
  }

  fun playTrack(
    track: SpotifyTrack,
    onSuccess: (SongItem) -> Unit,
    onError: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(resolvingTrackId = track.id) }
      val resolved = withContext(Dispatchers.IO) {
        spotifyPlaybackResolver.resolveTrack(track)
      }
      _uiState.update { it.copy(resolvingTrackId = null) }

      if (resolved != null) {
        onSuccess(resolved)
      } else {
        onError()
      }
    }
  }
}
