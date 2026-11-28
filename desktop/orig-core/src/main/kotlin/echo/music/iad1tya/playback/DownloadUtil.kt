package echo.music.iad1tya.playback

import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.utils.InnerTubeXResolver
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Offline downloads: yt-dlp saves the audio of a song into ~/.echomusic/downloads/<id>.<ext>; the player (VlcBackend)
 * prefers that file over a stream. Hooks into the UI through DownloadService.handler, like the Android download service.
 * shortcut: two downloads at a time, no pause/resume (a stopped download starts over), audio only.
 */
class DownloadUtil {
  private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
  val downloads: StateFlow<Map<String, Download>> = _downloads
  fun getDownload(songId: String): Flow<Download?> = _downloads.map { it[songId] }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val slots = Semaphore(2)
  private val jobs = HashMap<String, Job>()
  private val processes = HashMap<String, Process>()
  @Volatile private var database: MusicDatabase? = null

  init {
    dir.mkdirs()
    // songs downloaded in an earlier session
    _downloads.value = dir.listFiles().orEmpty().filter { it.isFile && it.extension in AUDIO_EXTENSIONS }
      .associate { it.nameWithoutExtension to Download(it.nameWithoutExtension, Download.STATE_COMPLETED, it.length()) }
    DownloadService.handler = object : DownloadService.Handler {
      override fun add(request: DownloadRequest, foreground: Boolean) = enqueue(request)
      override fun remove(id: String) = this@DownloadUtil.remove(id)
      override fun removeAll() = _downloads.value.keys.toList().forEach { this@DownloadUtil.remove(it) }
    }
  }

  fun attach(db: MusicDatabase) { database = db }

  private fun set(id: String, state: Int, length: Long = 0) = _downloads.update { it + (id to Download(id, state, length)) }

  @Synchronized
  private fun enqueue(request: DownloadRequest) {
    val id = request.id
    if (!ID_OK.matches(id)) return // ids go to a command line and a file name
    val s = _downloads.value[id]?.state
    if (s == Download.STATE_COMPLETED || s == Download.STATE_QUEUED || s == Download.STATE_DOWNLOADING) return
    val title = String(request.data).ifBlank { "Unknown" }
    set(id, Download.STATE_QUEUED)
    jobs[id] = scope.launch {
      slots.withPermit {
        set(id, Download.STATE_DOWNLOADING)
        val file = runCatching { fetch(id) }.onFailure { logError(id, it) }.getOrNull()
        if (file == null) { if (_downloads.value[id]?.state != Download.STATE_REMOVING) set(id, Download.STATE_FAILED); return@withPermit }
        set(id, Download.STATE_COMPLETED, file.length())
        val now = LocalDateTime.now()
        database?.query { if (insert(SongEntity(id = id, title = title, dateDownload = now, isDownloaded = true)) == -1L) updateDownloadedInfo(id, true, now) }
      }
    }
  }

  private fun fetch(id: String): File? {
    val p = ProcessBuilder(
      InnerTubeXResolver.ytDlp(), "-f", "bestaudio[ext=m4a]/bestaudio", "--no-playlist", "--no-warnings", "--no-mtime",
      "-o", File(dir, "$id.%(ext)s").absolutePath, "https://music.youtube.com/watch?v=$id",
    ).redirectErrorStream(true).start()
    synchronized(this) { processes[id] = p }
    p.inputStream.readBytes() // drain so the process never blocks on a full pipe
    val done = p.waitFor(20, TimeUnit.MINUTES)
    synchronized(this) { processes.remove(id) }
    if (!done) { p.destroyForcibly(); return null }
    return if (p.exitValue() == 0) fileFor(id) else null
  }

  @Synchronized
  private fun remove(id: String) {
    set(id, Download.STATE_REMOVING)
    jobs.remove(id)?.cancel()
    processes.remove(id)?.destroyForcibly()
    dir.listFiles().orEmpty().filter { it.name.startsWith("$id.") }.forEach { it.delete() } // finished file and any .part leftovers
    _downloads.update { it - id }
    database?.query { updateDownloadedInfo(id, false, null) }
  }

  private fun logError(id: String, e: Throwable) {
    runCatching { File(System.getProperty("user.home"), ".echomusic/error.log").appendText("download failed for $id: ${e.stackTraceToString()}\n") }
  }

  companion object {
    private val AUDIO_EXTENSIONS = setOf("m4a", "webm", "opus", "mp3", "ogg", "aac")
    private val ID_OK = Regex("[A-Za-z0-9_-]{6,20}")
    val dir: File = File(System.getProperty("user.home"), ".echomusic/downloads")

    /** The saved audio of a finished download, if there is one. */
    fun fileFor(id: String): File? =
      dir.listFiles()?.firstOrNull { it.isFile && it.nameWithoutExtension == id && it.extension in AUDIO_EXTENSIONS && it.length() > 0 }

    fun computeDownloadState(songIds: Collection<String>?, downloads: Map<String, Download>): Int {
      if (songIds.isNullOrEmpty()) return Download.STATE_STOPPED
      return if (songIds.all { downloads[it]?.state == Download.STATE_COMPLETED }) Download.STATE_COMPLETED
      else if (songIds.any { downloads[it]?.state == Download.STATE_QUEUED || downloads[it]?.state == Download.STATE_DOWNLOADING }) Download.STATE_DOWNLOADING
      else Download.STATE_STOPPED
    }
    fun hasDownload(songId: String, downloads: Map<String, Download>): Boolean {
      val s = downloads[songId]?.state
      return s == Download.STATE_COMPLETED || s == Download.STATE_QUEUED || s == Download.STATE_DOWNLOADING
    }
  }
}
