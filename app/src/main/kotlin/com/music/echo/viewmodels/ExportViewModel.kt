package echo.music.iad1tya.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import echo.music.iad1tya.export.ExportBatchState
import echo.music.iad1tya.export.ExportEngine
import echo.music.iad1tya.export.ExportWorker
import echo.music.iad1tya.export.ExportableSong
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ExportViewModel
@Inject
constructor(
  val exportEngine: ExportEngine,
) : ViewModel() {

  private val _eligibleSongs = MutableStateFlow<List<ExportableSong>>(emptyList())
  val eligibleSongs: StateFlow<List<ExportableSong>> = _eligibleSongs.asStateFlow()

  private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
  val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds.asStateFlow()

  val searchQuery = MutableStateFlow("")

  val batchState: StateFlow<ExportBatchState> = exportEngine.batchState

  val filteredSongs: StateFlow<List<ExportableSong>> =
    combine(_eligibleSongs, searchQuery) { songs, query ->
      if (query.isBlank()) {
        songs
      } else {
        val trimmed = query.trim().lowercase()
        songs.filter { song ->
          song.title.lowercase().contains(trimmed) ||
            song.artist.lowercase().contains(trimmed) ||
            song.album.lowercase().contains(trimmed)
        }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTotalSize: StateFlow<Long> =
    combine(_eligibleSongs, _selectedSongIds) { songs, selected ->
      val selectedMap = selected.toSet()
      songs.filter { it.id in selectedMap }.sumOf { it.sizeBytes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  val totalEligibleSize: StateFlow<Long> =
    _eligibleSongs.combine(MutableStateFlow(Unit)) { songs, _ ->
      songs.sumOf { it.sizeBytes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  init {
    loadEligibleSongs()
  }

  fun loadEligibleSongs() {
    viewModelScope.launch {
      val songs = exportEngine.getEligibleSongs()
      _eligibleSongs.value = songs
      _selectedSongIds.update { current ->
        val validIds = songs.map { it.id }.toSet()
        current.filter { it in validIds }.toSet()
      }
    }
  }

  fun toggleSelection(songId: String) {
    _selectedSongIds.update { current ->
      if (current.contains(songId)) {
        current - songId
      } else {
        current + songId
      }
    }
  }

  fun selectAll() {
    val visibleIds = filteredSongs.value.map { it.id }.toSet()
    _selectedSongIds.update { it + visibleIds }
  }

  fun deselectAll() {
    _selectedSongIds.value = emptySet()
  }

  fun startExportAll(context: Context) {
    val allIds = _eligibleSongs.value.map { it.id }
    if (allIds.isNotEmpty()) {
      ExportWorker.startBulkExport(context, allIds)
    }
  }

  fun startExportSelected(context: Context) {
    val selectedIds = _selectedSongIds.value.toList()
    if (selectedIds.isNotEmpty()) {
      ExportWorker.startBulkExport(context, selectedIds)
    }
  }

  fun retryFailed(context: Context) {
    val failedIds = batchState.value.failedSongIds
    if (failedIds.isNotEmpty()) {
      ExportWorker.startBulkExport(context, failedIds)
    }
  }

  fun cancelExport(context: Context) {
    exportEngine.cancelExport()
    ExportWorker.cancelBulkExport(context)
  }
}
