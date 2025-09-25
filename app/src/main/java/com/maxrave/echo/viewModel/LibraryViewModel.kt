package iad1tya.echo.music.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import iad1tya.echo.music.R
import iad1tya.echo.music.common.Config
import iad1tya.echo.music.common.DownloadState
import iad1tya.echo.music.data.dataStore.DataStoreManager
import iad1tya.echo.music.data.db.entities.AlbumEntity
import iad1tya.echo.music.data.db.entities.LocalPlaylistEntity
import iad1tya.echo.music.data.db.entities.PairSongLocalPlaylist
import iad1tya.echo.music.data.db.entities.PlaylistEntity
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.data.model.searchResult.playlists.PlaylistsResult
import iad1tya.echo.music.data.type.PlaylistType
import iad1tya.echo.music.utils.LocalResource
import iad1tya.echo.music.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

@UnstableApi
class LibraryViewModel(
    private val application: Application,
) : BaseViewModel(application) {

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _listCanvasSong: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val listCanvasSong: StateFlow<LocalResource<List<SongEntity>>> get() = _listCanvasSong.asStateFlow()

    private val _downloadedSongs: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedSongs: StateFlow<LocalResource<List<SongEntity>>> get() = _downloadedSongs.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }


    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            mainRepository.getLibraryPlaylist().collect { data ->
//                    _listYouTubePlaylist.postValue(data?.reversed())
                _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeLoggedIn(): Boolean = runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE

    fun getPlaylistFavorite() {
        viewModelScope.launch {
            mainRepository.getLikedAlbums().collect { album ->
                val temp: MutableList<PlaylistType> = mutableListOf()
                temp.addAll(album)
                mainRepository.getLikedPlaylists().collect { playlist ->
                    temp.addAll(playlist)
                    val sortedList =
                        temp.sortedWith<PlaylistType>(
                            Comparator { p0, p1 ->
                                val timeP0: LocalDateTime? =
                                    when (p0) {
                                        is AlbumEntity -> p0.inLibrary
                                        is PlaylistEntity -> p0.inLibrary
                                        else -> null
                                    }
                                val timeP1: LocalDateTime? =
                                    when (p1) {
                                        is AlbumEntity -> p1.inLibrary
                                        is PlaylistEntity -> p1.inLibrary
                                        else -> null
                                    }
                                if (timeP0 == null || timeP1 == null) {
                                    return@Comparator if (timeP0 == null && timeP1 == null) {
                                        0
                                    } else if (timeP0 == null) {
                                        -1
                                    } else {
                                        1
                                    }
                                }
                                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                            },
                        )
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
        }
    }

    fun getFavoritePodcasts() {
        viewModelScope.launch {
            mainRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }
    }

    fun getCanvasSong() {
        _listCanvasSong.value = LocalResource.Loading()
        viewModelScope.launch {
            mainRepository.getCanvasSong(max = 5).collect { data ->
                _listCanvasSong.value = LocalResource.Success(data)
            }
        }
    }

    fun getLocalPlaylist() {
        _yourLocalPlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
//                    _listLocalPlaylist.postValue(values)
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
            }
        }
    }

    fun getDownloadedPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllDownloadedPlaylist().collect { values ->
                _downloadedPlaylist.value = LocalResource.Success(values)
            }
        }
    }

    fun getDownloadedSongs() {
        viewModelScope.launch {
            mainRepository.getDownloadedSongs().collect { values ->
                _downloadedSongs.value = LocalResource.Success(values?.reversed() ?: emptyList())
            }
        }
    }

    fun updateLikeStatus(
        videoId: String,
        likeStatus: Int,
    ) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
        }
    }

    fun createPlaylist(title: String) {
        Log.d("LibraryViewModel", "createPlaylist called with title: $title")
        viewModelScope.launch {
            try {
                val localPlaylistEntity = LocalPlaylistEntity(title = title)
                Log.d("LibraryViewModel", "Created LocalPlaylistEntity: $localPlaylistEntity")
                mainRepository.insertLocalPlaylist(localPlaylistEntity)
                Log.d("LibraryViewModel", "Successfully inserted playlist to database")
                getLocalPlaylist()
                Log.d("LibraryViewModel", "Refreshed local playlist list")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error in createPlaylist: ${e.message}", e)
            }
        }
    }

    fun updateLocalPlaylistTracks(
        list: List<String>,
        id: Long,
    ) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
    }

    fun addToYouTubePlaylist(
        localPlaylistId: Long,
        youtubePlaylistId: String,
        videoId: String,
    ) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.Synced,
                    )
                    Toast
                        .makeText(
                            getApplication(),
                            application.getString(R.string.added_to_youtube_playlist),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                    )
                    Toast.makeText(getApplication(), application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    fun deleteSong(videoId: String) {
        viewModelScope.launch {
            mainRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            mainRepository.resetTotalPlayTime(videoId)
        }
    }
}