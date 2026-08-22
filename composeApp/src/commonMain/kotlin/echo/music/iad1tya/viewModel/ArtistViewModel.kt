package echo.music.iad1tya.viewModel

import androidx.lifecycle.viewModelScope
import echo.music.iad1tya.common.Config
import echo.music.iad1tya.domain.data.entities.ArtistEntity
import echo.music.iad1tya.domain.data.entities.SongEntity
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.data.model.browse.artist.Albums
import echo.music.iad1tya.domain.data.model.browse.artist.ArtistBrowse
import echo.music.iad1tya.domain.data.model.browse.artist.ArtistLogo
import echo.music.iad1tya.domain.data.model.browse.artist.Related
import echo.music.iad1tya.domain.data.model.browse.artist.ResultPlaylist
import echo.music.iad1tya.domain.data.model.browse.artist.Singles
import echo.music.iad1tya.domain.data.model.streams.YouTubeWatchEndpoint
import echo.music.iad1tya.domain.extension.now
import echo.music.iad1tya.domain.mediaservice.handler.PlaylistType
import echo.music.iad1tya.domain.mediaservice.handler.QueueData
import echo.music.iad1tya.domain.repository.ArtistRepository
import echo.music.iad1tya.domain.repository.LyricsCanvasRepository
import echo.music.iad1tya.domain.repository.SongRepository
import echo.music.iad1tya.domain.utils.Resource
import echo.music.iad1tya.extension.toArtistScreenData
import echo.music.iad1tya.viewModel.ArtistScreenState.Error
import echo.music.iad1tya.viewModel.ArtistScreenState.Loading
import echo.music.iad1tya.viewModel.ArtistScreenState.Success
import echo.music.iad1tya.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.radio
import echomusic.composeapp.generated.resources.shuffle

class ArtistViewModel(
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
) : BaseViewModel() {
    // It is dynamic and can be changed by the user, so separate it from the ArtistScreenData
    private var _canvasUrl: MutableStateFlow<Pair<String, SongEntity>?> = MutableStateFlow(null)
    var canvasUrl: StateFlow<Pair<String, SongEntity>?> = _canvasUrl

    // Artist name-logo image + accent color from the hidden catalog (immersive header).
    private val _artistLogo: MutableStateFlow<ArtistLogo?> = MutableStateFlow(null)
    val artistLogo: StateFlow<ArtistLogo?> = _artistLogo

    private var _followed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var followed: StateFlow<Boolean> = _followed

    private val _artistScreenState: MutableStateFlow<ArtistScreenState> = MutableStateFlow(Loading)
    val artistScreenState: StateFlow<ArtistScreenState> = _artistScreenState

    fun browseArtist(channelId: String) {
        _artistScreenState.value = Loading
        _canvasUrl.value = null
        _artistLogo.value = null
        _followed.value = false
        viewModelScope.launch {
            artistRepository.getArtistData(channelId).collect { browse ->
                val data = browse.data
                when (browse) {
                    is Resource.Success if (data != null) -> {
                        data.channelId?.let { channelId ->
                            insertArtist(
                                ArtistEntity(
                                    channelId,
                                    data.name,
                                    data.thumbnails
                                        ?.lastOrNull()
                                        ?.url,
                                ),
                            )
                        }
                        _artistScreenState.value =
                            Success(data.toArtistScreenData())
                        // Canvas comes ONLY from the single most-popular song: take the first
                        // popular result and use its canvas if it has one. If it doesn't,
                        // leave canvas null (already reset above) — no fallback to other songs.
                        data.songs?.results?.firstOrNull()?.let { topSong ->
                            val entity = songRepository.getSongById(topSong.videoId).firstOrNull()
                            val canvasUrl = entity?.canvasUrl
                            if (entity != null && canvasUrl != null) {
                                _canvasUrl.value = Pair(canvasUrl, entity)
                                log("CanvasUrl: $canvasUrl")
                            }
                        }
                    }

                    is Resource.Error ->
                        _artistScreenState.value = Error(browse.message ?: "Error")

                    else -> {
                        _artistScreenState.value = Error("Error")
                    }
                }
            }
        }
    }

    private suspend fun fetchAndCacheArtistLogo(
        channelId: String,
        artistName: String,
    ) {
        lyricsCanvasRepository.getArtistLogo(artistName).collectLatest { res ->
            if (res is Resource.Success) {
                val logo = res.data ?: return@collectLatest
                _artistLogo.value = logo
                artistRepository.updateArtistNameLogo(channelId, logo.logoUrl, logo.bgColorHex)
            }
        }
    }

    fun insertArtist(artist: ArtistEntity) {
        viewModelScope.launch {
            artistRepository.insertArtist(artist)
            artistRepository.updateArtistInLibrary(now(), artist.channelId)
            delay(100)
            artistRepository.getArtistById(artist.channelId).collect { artistEntity ->
                if (artistEntity != null) {
                    artist.thumbnails?.let {
                        artistRepository.updateArtistImage(artistEntity.channelId, it)
                    }
                    _followed.value = artistEntity.followed
                    log("insertArtist: ${artistEntity.followed}")
                    // Name-logo: reuse the cached one if present, else fetch + persist it.
                    val cachedLogoUrl = artistEntity.nameLogoUrl
                    if (cachedLogoUrl != null) {
                        _artistLogo.value =
                            ArtistLogo(
                                logoUrl = cachedLogoUrl,
                                bgColorHex = artistEntity.nameLogoColor,
                                width = 0,
                                height = 0,
                            )
                    } else {
                        launch { fetchAndCacheArtistLogo(artist.channelId, artist.name) }
                    }
                }
            }
        }
    }

    fun updateFollowed(
        followed: Int,
        channelId: String,
    ) {
        viewModelScope.launch {
            _followed.value = (followed == 1)
            artistRepository.updateFollowedStatus(channelId, followed)
            log("updateFollowed: ${_followed.value}")
        }
    }

    fun onRadioClick(endpoint: YouTubeWatchEndpoint) {
        viewModelScope.launch {
            songRepository.getRadioFromEndpoint(endpoint).collectLatest { res ->
                val data = res.data
                when (res) {
                    is Resource.Success if data != null && data.first.isNotEmpty() -> {
                        setQueueData(
                            QueueData.Data(
                                listTracks = data.first,
                                firstPlayedTrack = data.first.first(),
                                playlistId = endpoint.playlistId,
                                playlistName = "\"${artistScreenState.value.data.title}\" ${getString(Res.string.radio)}",
                                playlistType = PlaylistType.RADIO,
                                continuation = data.second,
                            ),
                        )
                        loadMediaItem(
                            data.first.first(),
                            Config.PLAYLIST_CLICK,
                            0,
                        )
                    }

                    else -> {
                        makeToast(res.message)
                    }
                }
            }
        }
    }

    fun onShuffleClick(endpoint: YouTubeWatchEndpoint) {
        viewModelScope.launch {
            songRepository.getRadioFromEndpoint(endpoint).collectLatest { res ->
                val data = res.data
                when (res) {
                    is Resource.Success if data != null && data.first.isNotEmpty() -> {
                        setQueueData(
                            QueueData.Data(
                                listTracks = data.first,
                                firstPlayedTrack = data.first.first(),
                                playlistId = endpoint.playlistId,
                                playlistName = "\"${artistScreenState.value.data.title}\" ${getString(Res.string.shuffle)}",
                                playlistType = PlaylistType.RADIO,
                                continuation = data.second,
                            ),
                        )
                        loadMediaItem(
                            data.first.first(),
                            Config.PLAYLIST_CLICK,
                            0,
                        )
                    }

                    else -> {
                        makeToast(res.message)
                    }
                }
            }
        }
    }
}

data class ArtistScreenData(
    val title: String? = null,
    val imageUrl: String? = null,
    val subscribers: String? = null,
    val playCount: String? = null,
    val isChannel: Boolean = false,
    val channelId: String? = null,
    val radioParam: YouTubeWatchEndpoint? = null,
    val shuffleParam: YouTubeWatchEndpoint? = null,
    val description: String? = null,
    val listSongParam: String? = null,
    val popularSongs: List<Track> = emptyList(),
    val singles: Singles? = null,
    val albums: Albums? = null,
    val video: ArtistBrowse.Videos? = null,
    val related: Related? = null,
    val featuredOn: List<ResultPlaylist> = emptyList(),
)

sealed class ArtistScreenState(
    val data: ArtistScreenData = ArtistScreenData(),
    val message: String? = null,
) {
    data object Loading : ArtistScreenState()

    class Success(
        data: ArtistScreenData,
    ) : ArtistScreenState(data)

    class Error(
        message: String,
    ) : ArtistScreenState(message = message)
}