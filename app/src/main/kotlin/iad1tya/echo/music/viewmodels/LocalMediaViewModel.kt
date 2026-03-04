@file:OptIn(ExperimentalCoroutinesApi::class)

package iad1tya.echo.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.constants.SongSortDescendingKey
import iad1tya.echo.music.constants.SongSortType
import iad1tya.echo.music.constants.SongSortTypeKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.Album
import iad1tya.echo.music.db.entities.Artist
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.extensions.reversed
import iad1tya.echo.music.extensions.toEnum
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.scanners.LocalMediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LocalMediaTab {
    SONGS,
    ALBUMS,
    ARTISTS,
}

@HiltViewModel
class LocalMediaViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val scanner: LocalMediaScanner,
) : ViewModel() {

    val isScanning = scanner.isScanning.asStateFlow()
    val scannerProgress = scanner.scannerProgress.asStateFlow()

    val localSongCount: StateFlow<Int> = database.localSongsCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val allLocalSongs =
        context.dataStore.data
            .map {
                Pair(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    it[SongSortDescendingKey] ?: true
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.localSongs(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val localAlbums: StateFlow<List<Album>> = database.localAlbumsByNameAsc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val localArtists: StateFlow<List<Artist>> = database.localArtistsByNameAsc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentTab = MutableStateFlow(LocalMediaTab.SONGS)
    val currentTab = _currentTab.asStateFlow()

    fun setTab(tab: LocalMediaTab) {
        _currentTab.value = tab
    }

    fun startScan(
        scanPaths: List<String> = emptyList(),
        excludedPaths: List<String> = emptyList(),
        sensitivity: Int = 1,
        strictExt: Boolean = false,
        useFilenameAsTitle: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            scanner.scan(scanPaths, excludedPaths, sensitivity, strictExt, useFilenameAsTitle)
        }
    }
}
