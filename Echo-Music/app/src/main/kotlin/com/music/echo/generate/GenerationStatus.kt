package echo.music.iad1tya.generate

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class GenerationState {
  data object Idle : GenerationState()
  data class Running(val message: String) : GenerationState()
  data class Done(val playlistId: String) : GenerationState()
  data class Failed(val error: String) : GenerationState()
}

interface GenerationStatus {
  val state: StateFlow<GenerationState>
  fun start(message: String)
  fun update(message: String)
  fun succeed(playlistId: String)
  fun fail(error: String)
  fun cancel()
}

@Singleton
open class RealGenerationStatus @Inject constructor() : GenerationStatus {
  private val _state = MutableStateFlow<GenerationState>(GenerationState.Idle)
  override val state: StateFlow<GenerationState> = _state.asStateFlow()

  override fun start(message: String) {
    _state.value = GenerationState.Running(message)
  }

  override fun update(message: String) {
    if (_state.value is GenerationState.Running) {
      _state.value = GenerationState.Running(message)
    }
  }

  override fun succeed(playlistId: String) {
    _state.value = GenerationState.Done(playlistId)
  }

  override fun fail(error: String) {
    _state.value = GenerationState.Failed(error)
  }

  override fun cancel() {
    _state.value = GenerationState.Idle
  }
}
