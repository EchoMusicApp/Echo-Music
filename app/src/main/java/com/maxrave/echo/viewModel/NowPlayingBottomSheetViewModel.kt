package iad1tya.echo.music.viewModel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import iad1tya.echo.kotlinytmusicscraper.models.WatchEndpoint
import iad1tya.echo.music.R
import iad1tya.echo.music.common.Config
import iad1tya.echo.music.common.DownloadState
import iad1tya.echo.music.data.dataStore.DataStoreManager.Settings.LRCLIB
import iad1tya.echo.music.data.dataStore.DataStoreManager.Settings.YOUTUBE
import iad1tya.echo.music.data.db.entities.LocalPlaylistEntity
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.data.db.entities.PairSongLocalPlaylist
import java.time.LocalDateTime
import iad1tya.echo.music.data.manager.LocalPlaylistManager
import iad1tya.echo.music.data.model.searchResult.songs.Album
import iad1tya.echo.music.data.model.searchResult.songs.Artist
import iad1tya.echo.music.extension.toTrack
import iad1tya.echo.music.service.PlaylistType
import iad1tya.echo.music.service.QueueData
import iad1tya.echo.music.service.SleepTimerState
import iad1tya.echo.music.service.test.download.DownloadUtils
import iad1tya.echo.music.utils.Resource
import iad1tya.echo.music.utils.collectLatestResource
import iad1tya.echo.music.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

@UnstableApi
class NowPlayingBottomSheetViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val downloadUtils: DownloadUtils by inject()
    private val localPlaylistManager: LocalPlaylistManager by inject()

    private val _uiState: MutableStateFlow<NowPlayingBottomSheetUIState> =
        MutableStateFlow(
            NowPlayingBottomSheetUIState(
                listLocalPlaylist = emptyList(),
                mainLyricsProvider = LRCLIB,
                sleepTimer =
                    SleepTimerState(
                        false,
                        0,
                    ),
            ),
        )
    val uiState: StateFlow<NowPlayingBottomSheetUIState> get() = _uiState.asStateFlow()

    private var getSongAsFlow: Job? = null
    
    fun refreshPlaylists() {
        viewModelScope.launch {
            try {
                Log.d("NowPlayingBottomSheetViewModel", "Refreshing playlists...")
                val playlists = mainRepository.getAllLocalPlaylists().first()
                _uiState.update { it.copy(listLocalPlaylist = playlists) }
                Log.d("NowPlayingBottomSheetViewModel", "Refreshed ${playlists.size} playlists")
            } catch (e: Exception) {
                Log.e("NowPlayingBottomSheetViewModel", "Error refreshing playlists: ${e.message}", e)
            }
        }
    }

    init {
        viewModelScope.launch {
            val sleepTimerJob =
                launch {
                    simpleMediaServiceHandler.sleepTimerState.collectLatest { sl ->
                        _uiState.update { it.copy(sleepTimer = sl) }
                    }
                }
            val listLocalPlaylistJob =
                launch {
                    mainRepository.getAllLocalPlaylists().collectLatest { list ->
                        _uiState.update { it.copy(listLocalPlaylist = list) }
                    }
                }
            val mainLyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.collectLatest { lyricsProvider ->
                        when (lyricsProvider) {
                            LRCLIB -> {
                                _uiState.update { it.copy(mainLyricsProvider = LRCLIB) }
                            }
                            YOUTUBE -> {
                                _uiState.update { it.copy(mainLyricsProvider = YOUTUBE) }
                            }
                            LRCLIB -> {
                                _uiState.update { it.copy(mainLyricsProvider = LRCLIB) }
                            }
                            else -> {
                                log("Unknown lyrics provider", Log.ERROR)
                            }
                        }
                    }
                }
            sleepTimerJob.join()
            listLocalPlaylistJob.join()
            mainLyricsProviderJob.join()
        }
    }

    fun setSongEntity(songEntity: SongEntity?) {
        val songOrNowPlaying = songEntity ?: (simpleMediaServiceHandler.nowPlayingState.value.songEntity ?: return)
        viewModelScope.launch {
            songOrNowPlaying.videoId.let {
                mainRepository.getSongById(it).singleOrNull().let { song ->
                    if (song != null) {
                        getSongEntityFlow(videoId = song.videoId)
                    } else {
                        mainRepository.insertSong(songOrNowPlaying).singleOrNull()?.let {
                            getSongEntityFlow(videoId = songOrNowPlaying.videoId)
                        }
                    }
                }
            }
        }
    }

    private fun getSongEntityFlow(videoId: String) {
        getSongAsFlow?.cancel()
        if (videoId.isEmpty()) return
        getSongAsFlow =
            viewModelScope.launch {
                mainRepository.getSongAsFlow(videoId).collectLatest { song ->
                    log("getSongEntityFlow: $song", Log.WARN)
                    if (song != null) {
                        _uiState.update {
                            it.copy(
                                songUIState =
                                    NowPlayingBottomSheetUIState.SongUIState(
                                        videoId = song.videoId,
                                        title = song.title,
                                        listArtists =
                                            song.artistName?.mapIndexed { i, name ->
                                                Artist(name = name, id = song.artistId?.getOrNull(i) ?: "")
                                            } ?: emptyList(),
                                        thumbnails = song.thumbnails,
                                        liked = song.liked,
                                        downloadState = song.downloadState,
                                        album =
                                            song.albumName?.let { name ->
                                                Album(name = name, id = song.albumId ?: "")
                                            },
                                    ),
                            )
                        }
                    }
                }
            }
    }

    fun onUIEvent(ev: NowPlayingBottomSheetUIEvent) {
        val songUIState = uiState.value.songUIState
        if (songUIState.videoId.isEmpty()) return
        viewModelScope.launch {
            when (ev) {
                is NowPlayingBottomSheetUIEvent.DeleteFromPlaylist -> {
                }
                is NowPlayingBottomSheetUIEvent.ToggleLike -> {
                    mainRepository.updateLikeStatus(
                        songUIState.videoId,
                        if (songUIState.liked) 0 else 1,
                    )
                }
                is NowPlayingBottomSheetUIEvent.Download -> {
                    when (songUIState.downloadState) {
                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            mainRepository.updateDownloadState(
                                videoId = songUIState.videoId,
                                downloadState = DownloadState.STATE_PREPARING,
                            )
                            downloadUtils.downloadTrack(
                                videoId = songUIState.videoId,
                                title = songUIState.title,
                                thumbnail = songUIState.thumbnails ?: "",
                            )
                            makeToast(getString(R.string.downloading))
                        }
                        DownloadState.STATE_PREPARING, DownloadState.STATE_DOWNLOADING -> {
                            downloadUtils.removeDownload(songUIState.videoId)
                            mainRepository.updateDownloadState(
                                songUIState.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED,
                            )
                            makeToast(getString(R.string.removed_download))
                        }
                        DownloadState.STATE_DOWNLOADED -> {
                            downloadUtils.removeDownload(songUIState.videoId)
                            mainRepository.updateDownloadState(
                                songUIState.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED,
                            )
                            makeToast(getString(R.string.removed_download))
                        }
                    }
                }
                is NowPlayingBottomSheetUIEvent.AddToPlaylist -> {
                    // NEW SIMPLE IMPLEMENTATION
                    viewModelScope.launch {
                        try {
                            Log.d("AddToPlaylist", "=== STARTING ADD TO PLAYLIST ===")
                            Log.d("AddToPlaylist", "Song ID: ${songUIState.videoId}")
                            Log.d("AddToPlaylist", "Playlist ID: ${ev.playlistId}")
                            
                            // Step 1: Get the song entity
                            val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull()
                            if (songEntity == null) {
                                Log.e("AddToPlaylist", "Song not found in database")
                                makeToast("Song not found")
                                return@launch
                            }
                            Log.d("AddToPlaylist", "Found song: ${songEntity.title}")
                            
                            // Step 2: Get all playlists and find the target one
                            val allPlaylists = mainRepository.getAllLocalPlaylists().first()
                            val playlist = allPlaylists.find { it.id == ev.playlistId }
                            if (playlist == null) {
                                Log.e("AddToPlaylist", "Playlist not found")
                                makeToast("Playlist not found")
                                return@launch
                            }
                            Log.d("AddToPlaylist", "Found playlist: ${playlist.title}")
                            
                            // Step 3: Check if song already exists
                            val existingTracks = playlist.tracks ?: emptyList()
                            if (existingTracks.contains(songUIState.videoId)) {
                                Log.d("AddToPlaylist", "Song already in playlist")
                                makeToast("Song already in playlist")
                                return@launch
                            }
                            
                            // Step 4: Add song to playlist tracks
                            val newTracks = existingTracks + songUIState.videoId
                            Log.d("AddToPlaylist", "New tracks list size: ${newTracks.size}")
                            
                            // Step 5: Update playlist tracks in database
                            mainRepository.updateLocalPlaylistTracks(newTracks, ev.playlistId)
                            
                            // Step 5.5: Update playlist thumbnail if it's the first song
                            if (existingTracks.isEmpty() && songEntity.thumbnails != null) {
                                Log.d("AddToPlaylist", "Setting playlist thumbnail to first song: ${songEntity.thumbnails}")
                                mainRepository.updateLocalPlaylistThumbnail(songEntity.thumbnails, ev.playlistId)
                            }
                            
                            // Step 6: Insert song-playlist pair
                            val pair = PairSongLocalPlaylist(
                                playlistId = ev.playlistId,
                                songId = songUIState.videoId,
                                position = existingTracks.size,
                                inPlaylist = LocalDateTime.now()
                            )
                            mainRepository.insertPairSongLocalPlaylist(pair)
                            
                            Log.d("AddToPlaylist", "Successfully added song to playlist!")
                            makeToast("Added to playlist: ${playlist.title}")
                            
                            // Refresh playlists to reflect the changes
                            refreshPlaylists()
                            
                        } catch (e: Exception) {
                            Log.e("AddToPlaylist", "Error: ${e.message}", e)
                            makeToast("Error: ${e.message}")
                        }
                    }
                }
                is NowPlayingBottomSheetUIEvent.CreatePlaylistAndAddSong -> {
                    // Create new playlist and add song to it
                    val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    
                    // Create the playlist first with thumbnail from the first song
                    val localPlaylistEntity = LocalPlaylistEntity(
                        title = ev.playlistName,
                        thumbnail = songEntity.thumbnails
                    )
                    mainRepository.insertLocalPlaylist(localPlaylistEntity)
                    
                    // Refresh playlists to get the newly created one
                    refreshPlaylists()
                    
                    // Get the created playlist ID
                    val createdPlaylist = mainRepository.getAllLocalPlaylists().first().find { it.title == ev.playlistName }
                    if (createdPlaylist != null) {
                        // Add song to the newly created playlist
                        localPlaylistManager.addTrackToLocalPlaylist(id = createdPlaylist.id, song = songEntity).collectLatestResource(
                            onSuccess = {
                                makeToast("Created playlist '${ev.playlistName}' and added song")
                                // Refresh playlists again after adding song
                                refreshPlaylists()
                            },
                            onError = {
                                makeToast(getString(R.string.error))
                            },
                        )
                    }
                }
                is NowPlayingBottomSheetUIEvent.PlayNext -> {
                    val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    simpleMediaServiceHandler.playNext(songEntity.toTrack())
                    makeToast(getString(R.string.play_next))
                }
                is NowPlayingBottomSheetUIEvent.AddToQueue -> {
                    val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    simpleMediaServiceHandler.loadMoreCatalog(arrayListOf(songEntity.toTrack()), isAddToQueue = true)
                    makeToast(getString(R.string.added_to_queue))
                }
                is NowPlayingBottomSheetUIEvent.ChangeLyricsProvider -> {
                    if (listOf(YOUTUBE, LRCLIB).contains(ev.lyricsProvider)) {
                        dataStoreManager.setLyricsProvider(ev.lyricsProvider)
                    } else {
                        return@launch
                    }
                }
                is NowPlayingBottomSheetUIEvent.SetSleepTimer -> {
                    if (ev.cancel) {
                        simpleMediaServiceHandler.sleepStop()
                        makeToast(getString(R.string.sleep_timer_off_done))
                    } else if (ev.minutes > 0) {
                        simpleMediaServiceHandler.sleepStart(ev.minutes)
                    }
                }
                is NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch -> {
                    dataStoreManager.setPlaybackSpeed(ev.speed)
                    dataStoreManager.setPitch(ev.pitch)
                }
                is NowPlayingBottomSheetUIEvent.Share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val url = "https://music.youtube.com/watch?v=${songUIState.videoId}"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                    val chooserIntent =
                        Intent.createChooser(shareIntent, getString(R.string.share_url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    application.startActivity(chooserIntent)
                }

                is NowPlayingBottomSheetUIEvent.StartRadio -> {
                    mainRepository
                        .getRadioArtist(
                            WatchEndpoint(
                                videoId = ev.videoId,
                                playlistId = "RDAMVM${ev.videoId}",
                            ),
                        ).collectLatest { res ->
                            val data = res.data
                            when (res) {
                                is Resource.Success if (data != null && data.first.isNotEmpty()) -> {
                                    setQueueData(
                                        QueueData(
                                            listTracks = data.first,
                                            firstPlayedTrack = data.first.first(),
                                            playlistId = "RDAMVM${ev.videoId}",
                                            playlistName = ev.name,
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
                                    makeToast(res.message ?: getString(R.string.error))
                                }
                            }
                        }
                }
            }
        }
    }
}

data class NowPlayingBottomSheetUIState(
    val songUIState: SongUIState = SongUIState(),
    val listLocalPlaylist: List<LocalPlaylistEntity>,
    val mainLyricsProvider: String,
    val sleepTimer: SleepTimerState,
) {
    data class SongUIState(
        val videoId: String = "",
        val title: String = "",
        val listArtists: List<Artist> = emptyList(),
        val thumbnails: String? = null,
        val liked: Boolean = false,
        val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
        val album: Album? = null,
    )
}

sealed class NowPlayingBottomSheetUIEvent {
    data class DeleteFromPlaylist(
        val videoId: String,
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data object ToggleLike : NowPlayingBottomSheetUIEvent()

    data object Download : NowPlayingBottomSheetUIEvent()

    data class AddToPlaylist(
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data class CreatePlaylistAndAddSong(
        val playlistName: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object PlayNext : NowPlayingBottomSheetUIEvent()

    data object AddToQueue : NowPlayingBottomSheetUIEvent()

    data class ChangeLyricsProvider(
        val lyricsProvider: String,
    ) : NowPlayingBottomSheetUIEvent()

    data class SetSleepTimer(
        val cancel: Boolean = false,
        val minutes: Int = 0,
    ) : NowPlayingBottomSheetUIEvent()

    data class ChangePlaybackSpeedPitch(
        val speed: Float,
        val pitch: Int,
    ) : NowPlayingBottomSheetUIEvent()

    data class StartRadio(
        val videoId: String,
        val name: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object Share : NowPlayingBottomSheetUIEvent()
}