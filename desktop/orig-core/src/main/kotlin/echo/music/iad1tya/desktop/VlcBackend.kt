package echo.music.iad1tya.desktop

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import com.sun.jna.NativeLibrary
import echo.music.iad1tya.playback.AudioBackend
import echo.music.iad1tya.utils.InnerTubeXResolver
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.binding.lib.LibC
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/** Where the packaged app keeps VLC and yt-dlp (compose.application.resources.dir), or null when run from Gradle. */
object Bundled {
  val dir: File? = System.getProperty("compose.application.resources.dir")?.let { File(it) }
  fun configureVlc() {
    val vlc = File(dir ?: return, "vlc")
    if (!File(vlc, "libvlc.dll").exists()) return
    NativeLibrary.addSearchPath("libvlc", vlc.absolutePath)
    NativeLibrary.addSearchPath("libvlccore", vlc.absolutePath)
    runCatching { LibC.INSTANCE._putenv("VLC_PLUGIN_PATH=" + File(vlc, "plugins").absolutePath) }
    runCatching { LibC.INSTANCE._putenv("PATH=" + vlc.absolutePath + ";" + (System.getenv("PATH") ?: "")) }
  }
}

/**
 * Real sound for the desktop QueuePlayer: yt-dlp finds the stream URL, VLC plays it.
 * Callbacks go back to the Swing UI thread because QueuePlayer is single-threaded.
 * shortcut: no gapless/crossfade, no preloading of the next song, no local-file items except plain file paths.
 */
class VlcBackend : AudioBackend {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var job: Job? = null
  @Volatile private var length = C.TIME_UNSET
  @Volatile private var pendingStart = 0L
  @Volatile private var startPaused = false
  @Volatile private var started = false // VLC has reported "playing" for the current load
  @Volatile private var generation = 0
  override var callbacks: AudioBackend.Callbacks? = null

  /** Local songs are stored as plain paths or as file: URIs. */
  private fun localFile(id: String): File? =
    echo.music.iad1tya.playback.DownloadUtil.fileFor(id) // a song saved for offline listening
      ?: runCatching { if (id.startsWith("file:")) File(java.net.URI(id)) else File(id) }.getOrNull()?.takeIf { it.isFile }

  private fun ui(block: () -> Unit) = SwingUtilities.invokeLater(block)

  private val mp: MediaPlayer? = try {
    Bundled.configureVlc()
    MediaPlayerFactory("--no-video", "--verbose=1").mediaPlayers().newMediaPlayer().also { p ->
      p.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun lengthChanged(m: MediaPlayer, l: Long) { length = l }
        override fun playing(m: MediaPlayer) {
          started = true
          // never call LibVLC from its own event thread: hand the seek/pause to the player's executor
          val seek = pendingStart; val pause = startPaused
          pendingStart = 0; startPaused = false
          if (seek > 0 || pause) m.submit { if (seek > 0) m.controls().setTime(seek); if (pause) m.controls().setPause(true) }
          val l = length
          ui { callbacks?.onReady(l) }
        }
        override fun finished(m: MediaPlayer) { ui { callbacks?.onEnded() } }
        override fun error(m: MediaPlayer) { fail("VLC could not play this stream") }
      })
    }
  } catch (t: Throwable) {
    System.err.println("VLC could not start: $t")
    null
  }

  private fun fail(msg: String) = ui {
    callbacks?.onError(PlaybackException(msg, null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
  }

  override fun load(item: MediaItem, startPositionMs: Long, playWhenReady: Boolean) {
    val p = mp ?: return fail("The audio engine (VLC) could not start")
    job?.cancel()
    p.controls().stop() // silence the previous song at once; the new stream can take a few seconds to resolve
    val gen = ++generation
    length = C.TIME_UNSET
    pendingStart = startPositionMs
    started = false
    startPaused = !playWhenReady
    ui { callbacks?.onBuffering() }
    job = scope.launch {
      val id = item.mediaId
      val local = localFile(id)
      if (local != null) { p.media().play(local.absolutePath); return@launch }
      val res = runCatching { InnerTubeXResolver.extract(id, 320) }
      val ex = res.getOrNull()
      res.exceptionOrNull()?.let { System.err.println("stream lookup failed for $id: $it"); runCatching { File(System.getProperty("user.home"), ".echomusic/error.log").also { f -> f.parentFile.mkdirs() }.appendText("stream lookup failed for $id: ${it.stackTraceToString()}\n") } }
      if (gen != generation) return@launch
      if (ex == null) return@launch fail("Could not get a stream: " + (res.exceptionOrNull()?.message ?: "yt-dlp returned nothing"))
      val ua = ex.headers["User-Agent"] ?: "Mozilla/5.0"
      p.media().play(ex.url, ":http-user-agent=$ua")
    }
  }

  override fun prefetch(item: MediaItem) {
    val id = item.mediaId
    if (localFile(id) != null) return
    scope.launch { runCatching { InnerTubeXResolver.extract(id, 320) } } // fills the resolver's cache
  }

  // before VLC reports "playing" the request is only recorded (and applied then); after it, VLC is told directly
  override fun play() { startPaused = false; mp?.controls()?.play() }
  override fun pause() { if (started) mp?.controls()?.setPause(true) else startPaused = true }
  override fun stop() { generation++; job?.cancel(); mp?.controls()?.stop() }
  override fun seekTo(positionMs: Long) { mp?.controls()?.setTime(positionMs) }
  override fun setVolume(volume: Float) { mp?.audio()?.setVolume((volume * 100).toInt().coerceIn(0, 200)) }
  override fun setSpeed(speed: Float) { mp?.controls()?.setRate(speed) }
  override val positionMs: Long get() = mp?.status()?.time()?.coerceAtLeast(0) ?: 0L
  override val durationMs: Long get() = length
  override val bufferedMs: Long get() = positionMs
}
