package iad1tya.echo.music.recognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.PlayerConnection

@HiltViewModel
class MusicRecognitionViewModel @Inject constructor(
    private val repository: ShazamRepository
) : ViewModel() {
    private val audioRecorder = AudioRecorder(viewModelScope)
    
    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state = _state.asStateFlow()

    private val _identifiedTrack = MutableSharedFlow<Track?>()
    val identifiedTrack = _identifiedTrack.asSharedFlow()

    init {
        combine(audioRecorder.duration, audioRecorder.buffer) { duration, buffer -> duration to buffer }
            .sample(2000L) // Sample every 2 seconds
            .onEach(::process)
            .launchIn(viewModelScope)
    }

    fun startListening() {
        if (_state.value is RecognitionState.Listening) return
        _state.value = RecognitionState.Listening
        try {
            audioRecorder.start()
        } catch (e: Exception) {
            _state.value = RecognitionState.Error("Microphone error")
        }
    }

    fun stopListening() {
        audioRecorder.stop()
        if (_state.value is RecognitionState.Listening) {
            _state.value = RecognitionState.Idle
        }
    }

    fun playSong(track: Track, playerConnection: PlayerConnection) {
        val query = "${track.title} ${track.subtitle}"
        viewModelScope.launch {
            YouTube.searchSummary(query).onSuccess { page ->
                val song = page.summaries.flatMap { it.items }
                    .filterIsInstance<SongItem>()
                    .firstOrNull()
                
                if (song != null) {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = song.id),
                            song.toMediaMetadata()
                        )
                    )
                } else {
                    // Fallback to error or just log
                    _state.value = RecognitionState.Error("Song not found on YouTube Music")
                }
            }.onFailure {
                _state.value = RecognitionState.Error("Failed to play: ${it.message}")
            }
        }
    }

    suspend fun findSongOnYouTube(track: Track): String? {
        val query = "${track.title} ${track.subtitle}"
        return try {
            val result = YouTube.searchSummary(query).getOrNull()
            val song = result?.summaries?.flatMap { it.items }
                ?.filterIsInstance<SongItem>()
                ?.firstOrNull()
            song?.id?.let { "https://music.youtube.com/watch?v=$it" }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun process(data: Pair<Int, ByteArray>) {
        if (_state.value !is RecognitionState.Listening) return

        val (duration, buffer) = data
        if (buffer.isEmpty()) return
        
        // Wait for at least 3 seconds of audio
        if (duration < 3) return 

        if (duration > 12) {
             _state.value = RecognitionState.Error("Timeout: Could not identify")
             stopListening()
             return
        }

        try {
            val track = repository.identify(duration, buffer)
            if (track != null) {
                _identifiedTrack.emit(track)
                _state.value = RecognitionState.Success(track)
                stopListening()
            }
        } catch (e: Exception) {
            // Keep trying until timeout or manual stop
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
    }
}

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Listening : RecognitionState()
    data class Success(val track: Track) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}
