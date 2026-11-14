package echo.music.iad1tya.playback

import java.io.File
import java.time.LocalTime

/** Tiny diagnostic log (~/.echomusic/playback.log): what started, restarted or ended playback, and who asked. */
object PlaybackLog {
  private val file by lazy { File(System.getProperty("user.home"), ".echomusic/playback.log").also { it.parentFile.mkdirs() } }
  @Synchronized fun log(msg: String, withStack: Boolean = false) {
    runCatching {
      if (file.length() > 300_000) file.delete()
      file.appendText("${LocalTime.now()} $msg\n" +
        if (withStack) Throwable().stackTrace.drop(2).take(8).joinToString("") { "    at $it\n" } else "")
    }
  }
}
