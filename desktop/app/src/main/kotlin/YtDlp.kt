package echo.desktop

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads a song's audio with the bundled yt-dlp (kept current by the YouTube community), which
 * handles YouTube's changing rules better than our own URL handling. Used first; the built-in
 * resolver is the fallback.
 */
object YtDlp {
    private val tools: File? = System.getProperty("compose.application.resources.dir")?.let { File(it, "tools") }
    private val dir = File(System.getProperty("java.io.tmpdir"), "echomusic-cache").apply { mkdirs() }

    suspend fun download(videoId: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val exe = File(tools ?: error("yt-dlp is not bundled (development run)"), "yt-dlp.exe")
                check(exe.exists()) { "yt-dlp.exe missing from the app folder" }
                val q = if (Settings.lowQuality) "low" else "high"
                fun existing() = dir.listFiles { f -> f.name.startsWith("$videoId-yt-$q.") && !f.name.endsWith(".part") && f.length() > 0 }?.firstOrNull()
                existing()?.let { return@runCatching it }

                val cmd =
                    mutableListOf(
                        exe.absolutePath, "-f", if (Settings.lowQuality) "worstaudio" else "bestaudio[ext=m4a]/bestaudio",
                        "--no-playlist", "--no-warnings", "--no-progress", "--newline",
                        "-o", File(dir, "$videoId-yt-$q.%(ext)s").absolutePath,
                    )
                File(tools, "deno.exe").takeIf { it.exists() }?.let { cmd += listOf("--js-runtimes", "deno:${it.absolutePath}") }
                cmd += "https://www.youtube.com/watch?v=$videoId"

                val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                val output = proc.inputStream.bufferedReader().readLines() // yt-dlp exits when done, so this ends too
                if (!proc.waitFor(120, TimeUnit.SECONDS)) {
                    proc.destroyForcibly()
                    error("yt-dlp timed out")
                }
                existing() ?: error("yt-dlp: " + output.takeLast(4).joinToString(" | ").ifBlank { "exit ${proc.exitValue()}" })
            }
        }
}
