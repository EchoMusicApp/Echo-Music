package iad1tya.echo.music.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.utils.RingtoneHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingtoneUiState(
    val showTrimmer: Boolean = false,
    val showProgress: Boolean = false,
    val targetSongId: String? = null,
    val targetSongTitle: String? = null,
    val targetSongArtist: String? = null,
    val targetSongDuration: Long = 0,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val isComplete: Boolean = false,
    val isSuccess: Boolean = false,
    val ringtoneUri: Uri? = null
)

class RingtoneViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RingtoneUiState())
    val uiState: StateFlow<RingtoneUiState> = _uiState.asStateFlow()

    fun showTrimmer(songId: String, title: String, artist: String, durationSeconds: Int) {
        _uiState.update {
            it.copy(
                showTrimmer = true,
                targetSongId = songId,
                targetSongTitle = title,
                targetSongArtist = artist,
                targetSongDuration = durationSeconds * 1000L
            )
        }
    }

    fun hideTrimmer() {
        _uiState.update { it.copy(showTrimmer = false) }
    }

    suspend fun getStreamUrl(context: Context, songId: String): String? {
        return RingtoneHelper.getStreamUrl(context, songId)
    }

    fun setAsRingtone(context: Context, startMs: Long, endMs: Long) {
        val state = _uiState.value
        val songId = state.targetSongId ?: return
        val title = state.targetSongTitle ?: "Unknown"
        val artist = state.targetSongArtist ?: "Unknown"

        hideTrimmer()

        _uiState.update {
            it.copy(
                showProgress = true,
                progress = 0f,
                statusMessage = "Starting...",
                isComplete = false,
                isSuccess = false,
                ringtoneUri = null
            )
        }

        viewModelScope.launch {
            RingtoneHelper.downloadAndTrimAsRingtone(
                context = context,
                songId = songId,
                title = title,
                artist = artist,
                startMs = startMs,
                endMs = endMs,
                onProgress = { progress, message ->
                    _uiState.update {
                        it.copy(progress = progress, statusMessage = message)
                    }
                },
                onComplete = { success, message, uri ->
                    _uiState.update {
                        it.copy(
                            isComplete = true,
                            isSuccess = success,
                            statusMessage = message,
                            ringtoneUri = uri
                        )
                    }
                }
            )
        }
    }

    fun dismissProgress() {
        _uiState.update { it.copy(showProgress = false, isComplete = false) }
    }

    fun openRingtoneSettings(context: Context) {
        RingtoneHelper.openRingtoneSettings(context, _uiState.value.ringtoneUri)
        dismissProgress()
    }

    fun hasSettingsPermission(context: Context): Boolean {
        return RingtoneHelper.hasSettingsPermission(context)
    }

    fun requestSettingsPermission(context: Context) {
        RingtoneHelper.requestSettingsPermission(context)
    }
}
