package echo.desktop

import com.sun.jna.NativeLibrary
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.binding.lib.LibC
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.factory.MediaPlayerFactory

data class PlayerState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val lengthMs: Long = 0,
    val error: String? = null,
)

/** Points JNA at the VLC engine shipped inside the app folder, if present (no VLC install needed). */
private object BundledVlc {
    fun configure() {
        val resources = System.getProperty("compose.application.resources.dir") ?: return
        val dir = File(resources, "vlc")
        if (!File(dir, "libvlc.dll").exists()) return
        NativeLibrary.addSearchPath("libvlc", dir.absolutePath)
        NativeLibrary.addSearchPath("libvlccore", dir.absolutePath)
        runCatching { LibC.INSTANCE._putenv("VLC_PLUGIN_PATH=" + File(dir, "plugins").absolutePath) }
    }
}

/** Thin wrapper around VLC (via vlcj). If VLC is not installed, [state] carries an error message. */
class VlcPlayer {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    /** VLC writes its own log here; useful when playback fails. */
    val logPath: String = File(System.getProperty("java.io.tmpdir"), "echomusic-vlc.log").absolutePath

    private var factory: MediaPlayerFactory? = null
    private val player: MediaPlayer? =
        try {
            BundledVlc.configure()
            val f =
                MediaPlayerFactory("--no-video", "--verbose=2", "--file-logging", "--logfile=$logPath")
            factory = f
            f.mediaPlayers().newMediaPlayer().also { mp ->
                mp.events().addMediaPlayerEventListener(
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
                                it.copy(
                                    isPlaying = false,
                                    error = "VLC could not play this stream. VLC log: $logPath",
                                )
                            }
                        }
                    }
                )
            }
        } catch (t: Throwable) {
            _state.value =
                PlayerState(
                    error =
                        "The audio engine (VLC) could not start. Install 64-bit VLC from videolan.org and restart. (${t.message})"
                )
            null
        }

    fun play(stream: ResolvedStream) {
        val mp = player ?: return
        _state.update { it.copy(positionMs = 0, lengthMs = 0, error = null) }
        mp.media().play(stream.url, ":http-user-agent=${stream.userAgent}")
    }

    fun togglePause() {
        player?.controls()?.pause()
    }

    fun seekTo(ms: Long) {
        player?.controls()?.setTime(ms)
    }

    fun setVolume(percent: Int) {
        player?.audio()?.setVolume(percent.coerceIn(0, 200))
    }

    fun release() {
        player?.release()
        factory?.release()
    }
}
