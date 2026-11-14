package echo.music.iad1tya.desktop

import android.content.Context
import android.widget.Toast
import kotlin.system.exitProcess

/** After a restore: say so, start the app again (same executable) and quit this instance. */
fun relaunchApp(context: Context) {
  Toast.makeText(context, "Restore complete. Restarting Echo Music…", Toast.LENGTH_LONG).show()
  Thread.sleep(1800)
  runCatching {
    val cmd = ProcessHandle.current().info().command().orElse(null)
    // packaged app: the .exe itself; a dev run (java) is not restarted automatically
    if (cmd != null && !cmd.endsWith("java.exe") && !cmd.endsWith("javaw.exe") && !cmd.endsWith("/java")) ProcessBuilder(cmd).start()
  }
  exitProcess(0)
}
