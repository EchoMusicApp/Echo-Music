package echo.music.iad1tya.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import echo.music.iad1tya.db.daos.RecommendationExclusionDao
import echo.music.iad1tya.db.daos.SongPlayStatsDao
import echo.music.iad1tya.generate.LocalTasteEngine
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GenerateUiState {
  data object Idle : GenerateUiState
  data object EmptyHistory : GenerateUiState
  data object AllExcluded : GenerateUiState
  data class Generating(val message: String) : GenerateUiState
  data class Success(val playlistId: String) : GenerateUiState
  data class Error(val message: String) : GenerateUiState
}

@HiltViewModel
class GenerateViewModel @Inject constructor(
  private val localTasteEngine: LocalTasteEngine,
  private val songPlayStatsDao: SongPlayStatsDao,
  private val recommendationExclusionDao: RecommendationExclusionDao,
) : ViewModel() {

  private val _uiState = MutableStateFlow<GenerateUiState>(GenerateUiState.Idle)
  val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

  val statsCount = songPlayStatsDao.observeTopPlayed(1)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val exclusionCount = recommendationExclusionDao.observeAll()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private var generationJob: Job? = null

  fun generate() {
    if (_uiState.value is GenerateUiState.Generating) return

    generationJob?.cancel()
    generationJob = viewModelScope.launch {
      try {
        _uiState.value = GenerateUiState.Generating("Analyzing listening momentum...")
        val result = localTasteEngine.generate()
        result.fold(
          onSuccess = { playlistId ->
            _uiState.value = GenerateUiState.Success(playlistId)
          },
          onFailure = { error ->
            val message = error.message.orEmpty()
            if (message.contains("No listening history", ignoreCase = true)) {
              _uiState.value = GenerateUiState.EmptyHistory
            } else if (message.contains("excluded", ignoreCase = true)) {
              _uiState.value = GenerateUiState.AllExcluded
            } else {
              _uiState.value = GenerateUiState.Error(message.ifBlank { "Failed to generate playlist" })
            }
          }
        )
      } catch (e: Exception) {
        _uiState.value = GenerateUiState.Error(e.message ?: "Unexpected error")
      }
    }
  }

  fun cancelGeneration() {
    generationJob?.cancel()
    generationJob = null
    _uiState.value = GenerateUiState.Idle
  }

  fun clearExclusions() {
    viewModelScope.launch {
      recommendationExclusionDao.clear()
      if (_uiState.value is GenerateUiState.AllExcluded) {
        _uiState.value = GenerateUiState.Idle
      }
    }
  }

  fun resetState() {
    _uiState.value = GenerateUiState.Idle
  }
}
