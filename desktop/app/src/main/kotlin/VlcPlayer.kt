package echo.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

data class PlayerState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val lengthMs: Long = 0,
    val error: String? = null,
)

/** Thin wrapper around VLC (via vlcj). If VLC is not installed, [state] carries an error message. */
class VlcPlayer {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val component: AudioPlayerComponent? =
        try {
            AudioPlayerComponent().also { c ->
                c.mediaPlayer().events().addMediaPlayerEventListener(
                    object : MediaPlayerEventAdapter() {
                        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                            _state.update { it.copy(positionMs = newTime) }
                        }

                        override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                            _state.update { it.copy(lengthMs = newLength) }
                        }

                        override fun playing(mediaPlayer: MediaPlayer) {
                            _state.update { it.copy(isPlaying = true, error = null) }
                        }

                        override fun paused(mediaPlayer: MediaPlayer) {
                            _state.update { it.copy(isPlaying = false) }
                        }

                        override fun stopped(mediaPlayer: MediaPlayer) {
                            _state.update { it.copy(isPlaying = false) }
                        }

                        override fun finished(mediaPlayer: MediaPlayer) {
                            _state.update { it.copy(isPlaying = false, positionMs = 0) }
                        }

                        override fun error(mediaPlayer: MediaPlayer) {
                            _state.update {
                                it.copy(isPlaying = false, error = "VLC could not play this stream")
                            }
                        }
                    }
                )
            }
        } catch (t: Throwable) {
            _state.value =
                PlayerState(
                    error =
                        "VLC was not found. Install 64-bit VLC from videolan.org, then restart this app. (${t.message})"
                )
            null
        }

    fun play(stream: ResolvedStream) {
        val c = component ?: return
        _state.update { it.copy(positionMs = 0, lengthMs = 0, error = null) }
        c.mediaPlayer().media().play(stream.url, ":http-user-agent=${stream.userAgent}")
    }

    fun togglePause() {
        val c = component ?: return
        c.mediaPlayer().controls().pause()
    }

    fun seekTo(ms: Long) {
        component?.mediaPlayer()?.controls()?.setTime(ms)
    }

    fun setVolume(percent: Int) {
        component?.mediaPlayer()?.audio()?.setVolume(percent.coerceIn(0, 200))
    }

    fun release() {
        component?.release()
    }
}
