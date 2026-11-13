package echo.music.iad1tya.desktop

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RELEASES = "https://api.github.com/repos/ev1lrag3d/testing_somethin/releases?per_page=15"

private data class Update(val version: Int, val setupUrl: String?, val page: String)

/** Newest native-vN release newer than the running build, or null (also when offline / running a dev build). */
private fun findUpdate(): Update? {
  val current = System.getProperty("echomusic.version")?.toIntOrNull() ?: return null
  val text = URL(RELEASES).openConnection().apply { setRequestProperty("Accept", "application/vnd.github+json"); connectTimeout = 8000; readTimeout = 8000 }
    .getInputStream().bufferedReader().readText()
  // shortcut: regex over the JSON instead of a parser; fine for these two fields
  val best = Regex("\"tag_name\"\\s*:\\s*\"native-v(\\d+)\"").findAll(text).map { it.groupValues[1].toInt() }.maxOrNull() ?: return null
  if (best <= current) return null
  val setup = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]*native-v$best/EchoMusic-Setup-v$best\\.exe)\"").find(text)?.groupValues?.get(1)
  return Update(best, setup, "https://github.com/ev1lrag3d/testing_somethin/releases/tag/native-v$best")
}

@Composable
fun UpdatePrompt(exitApp: () -> Unit) {
  var update by remember { mutableStateOf<Update?>(null) }
  var busy by remember { mutableStateOf(false) }
  var dismissed by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(Unit) { update = withContext(Dispatchers.IO) { runCatching { findUpdate() }.getOrNull() } }
  val u = update
  if (u == null || dismissed) return
  AlertDialog(
    onDismissRequest = { if (!busy) dismissed = true },
    title = { Text("Update available") },
    text = { Text(if (busy) "Downloading the update…" else "Echo Music v${u.version} is out. Update now? The app will close and the installer will take over.") },
    confirmButton = {
      TextButton(enabled = !busy, onClick = {
        scope.launch(Dispatchers.IO) {
          busy = true
          val ok = runCatching {
            val url = u.setupUrl ?: error("no installer attached")
            val f = File(System.getProperty("java.io.tmpdir"), "EchoMusic-Setup-v${u.version}.exe")
            URL(url).openStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            ProcessBuilder(f.absolutePath).start()
          }.isSuccess
          if (ok) exitApp() else { runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(u.page)) }; busy = false; dismissed = true }
        }
      }) { Text("Update now") }
    },
    dismissButton = { TextButton(enabled = !busy, onClick = { dismissed = true }) { Text("Later") } },
  )
}
