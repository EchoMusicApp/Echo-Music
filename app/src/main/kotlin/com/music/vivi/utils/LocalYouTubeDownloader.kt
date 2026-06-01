package iad1tya.echo.music.utils

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

data class LocalYouTubeFormat(
    val formatId: String,
    val title: String,
    val subtitle: String,
    val mimeType: String,
    val fileExtension: String,
    val isAudioOnly: Boolean,
    val isVideoOnly: Boolean,
    val bitrateKbps: Double?,
)

object LocalYouTubeDownloader {
    private const val TAG = "LocalYouTubeDownloader"
    private const val UPDATE_PREFS = "local_ytdlp"
    private const val LAST_UPDATE_KEY = "last_update"
    private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L
    private val initializationMutex = Mutex()

    @Volatile
    private var initialized = false

    suspend fun fetchFormats(context: Context, videoId: String): Result<List<LocalYouTubeFormat>> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureInitialized(context)
                updateYtDlpIfNeeded(context)
                runCatching { fetchFormats(videoId) }
                    .recoverCatching {
                        Timber.tag(TAG).w(it, "Retrying format extraction after yt-dlp refresh")
                        updateYtDlpIfNeeded(context, force = true)
                        fetchFormats(videoId)
                    }
                    .getOrThrow()
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to fetch yt-dlp formats for $videoId")
            }
        }

    suspend fun download(
        context: Context,
        videoId: String,
        formatId: String,
        destinationDirUriString: String,
        fileName: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureInitialized(context)
                val tempDir = File(context.cacheDir, "local-ytdlp/${UUID.randomUUID()}")
                check(tempDir.mkdirs()) { "Could not create temporary download directory" }

                try {
                    val outputBaseName = fileName.substringBeforeLast('.').sanitizeFileName()
                    val request =
                        YoutubeDLRequest(watchUrl(videoId)).apply {
                            addOption("--no-playlist")
                            addOption("--no-mtime")
                            addOption("--no-warnings")
                            addOption("--concurrent-fragments", "4")
                            addOption("-f", formatId)
                            addOption("-P", tempDir.absolutePath)
                            addOption("-o", "$outputBaseName.%(ext)s")
                            addRequestHeaders()
                        }

                    YoutubeDL.getInstance()
                        .execute(
                            request,
                            "echo-download-$videoId-${UUID.randomUUID()}",
                            null,
                        )

                    val downloadedFile =
                        tempDir
                            .walkTopDown()
                            .firstOrNull { it.isFile && !it.name.endsWith(".part") }
                            ?: error("yt-dlp did not produce a downloadable file")

                    copyToDestination(
                        context = context,
                        downloadedFile = downloadedFile,
                        destinationDirUriString = destinationDirUriString,
                    )
                } finally {
                    tempDir.deleteRecursively()
                }
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to download yt-dlp format $formatId for $videoId")
            }
        }

    private suspend fun ensureInitialized(context: Context) {
        if (initialized) return
        initializationMutex.withLock {
            if (initialized) return
            YoutubeDL.init(context.applicationContext)
            initialized = true
        }
    }

    private fun fetchFormats(videoId: String): List<LocalYouTubeFormat> {
        val request =
            YoutubeDLRequest(watchUrl(videoId)).apply {
                addOption("--no-playlist")
                addOption("--dump-single-json")
                addOption("--no-warnings")
                addOption("-R", "1")
                addOption("--socket-timeout", "15")
                addRequestHeaders()
            }
        val response =
            YoutubeDL.getInstance()
                .execute(
                    request,
                    "echo-format-$videoId-${UUID.randomUUID()}",
                    null,
                )
        return parseFormats(response.out)
    }

    private fun updateYtDlpIfNeeded(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(LAST_UPDATE_KEY, 0L)
        if (!force && System.currentTimeMillis() < lastUpdate + UPDATE_INTERVAL_MS) return

        runCatching {
                YoutubeDL.getInstance()
                    .updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel.STABLE)
            }
            .onSuccess {
                prefs.edit().putLong(LAST_UPDATE_KEY, System.currentTimeMillis()).apply()
                Timber.tag(TAG).i("yt-dlp update status: $it")
            }
            .onFailure {
                Timber.tag(TAG).w(it, "Could not refresh yt-dlp; continuing with bundled extractor")
            }
    }

    private fun YoutubeDLRequest.addRequestHeaders() {
        addOption("--user-agent", YouTubeClient.USER_AGENT_WEB)
        YouTube.cookie?.takeIf { it.isNotBlank() }?.let {
            addOption("--add-header", "Cookie:$it")
        }
    }

    private fun parseFormats(jsonText: String): List<LocalYouTubeFormat> {
        val json = JSONObject(jsonText)
        val durationSeconds = json.optDouble("duration").takeUnless(Double::isNaN)
        val formats = json.optJSONArray("formats") ?: return emptyList()

        return buildList {
            for (index in 0 until formats.length()) {
                val format = formats.optJSONObject(index) ?: continue
                val formatId = format.optString("format_id").takeIf { it.isNotBlank() } ?: continue
                val extension = format.optString("ext").ifBlank { "bin" }
                val audioCodec = format.optString("acodec").ifBlank { "none" }
                val videoCodec = format.optString("vcodec").ifBlank { "none" }
                val isAudioOnly = audioCodec != "none" && videoCodec == "none"
                val isVideoOnly = videoCodec != "none" && audioCodec == "none"

                if (audioCodec == "none" && videoCodec == "none") continue
                if (extension.equals("mhtml", ignoreCase = true)) continue

                val bitrateKbps =
                    format.optNullableDouble("abr")
                        ?: format.optNullableDouble("tbr")
                val fileSize =
                    format.optNullableDouble("filesize")
                        ?: format.optNullableDouble("filesize_approx")
                        ?: if (durationSeconds != null && bitrateKbps != null) {
                            durationSeconds * bitrateKbps * 125
                        } else {
                            null
                        }

                val title =
                    format.optString("format").takeIf { it.isNotBlank() }
                        ?: buildString {
                            append(formatId)
                            format.optString("resolution").takeIf { it.isNotBlank() }?.let {
                                append(" - ")
                                append(it)
                            }
                            format.optString("format_note").takeIf { it.isNotBlank() }?.let {
                                append(" (")
                                append(it)
                                append(")")
                            }
                        }

                val type =
                    when {
                        isAudioOnly -> "Audio only"
                        isVideoOnly -> "Video only"
                        else -> "Video + audio"
                    }
                val codecs =
                    listOf(audioCodec, videoCodec)
                        .filter { it != "none" }
                        .joinToString(" + ") { it.substringBefore('.') }
                val subtitle =
                    listOfNotNull(
                            "YouTube",
                            type,
                            extension.uppercase(Locale.US),
                            codecs.takeIf { it.isNotBlank() },
                            bitrateKbps?.let { formatBitrate(it) },
                            fileSize?.let(::formatFileSize),
                        )
                        .joinToString(" | ")

                add(
                    LocalYouTubeFormat(
                        formatId = formatId,
                        title = title,
                        subtitle = subtitle,
                        mimeType = mimeType(extension, isAudioOnly),
                        fileExtension = extension,
                        isAudioOnly = isAudioOnly,
                        isVideoOnly = isVideoOnly,
                        bitrateKbps = bitrateKbps,
                    )
                )
            }
        }.sortedWith(
            compareBy<LocalYouTubeFormat> {
                    when {
                        it.isAudioOnly -> 0
                        !it.isVideoOnly -> 1
                        else -> 2
                    }
                }
                .thenByDescending { it.bitrateKbps ?: 0.0 }
                .thenBy { it.formatId }
        )
    }

    private fun copyToDestination(
        context: Context,
        downloadedFile: File,
        destinationDirUriString: String,
    ): String {
        val destinationUri = android.net.Uri.parse(destinationDirUriString)
        val destinationDir =
            DocumentFile.fromTreeUri(context, destinationUri)
                ?.takeIf { it.isDirectory && it.canWrite() }
                ?: error("Cannot write to the selected download destination")
        val destinationName = downloadedFile.name.sanitizeFileName()

        destinationDir.findFile(destinationName)?.delete()
        val destination =
            destinationDir.createFile(
                mimeType(downloadedFile.extension, isAudioOnly = false),
                destinationName,
            ) ?: error("Could not create the destination file")

        try {
            context.contentResolver.openOutputStream(destination.uri)?.use { output ->
                downloadedFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open the destination file")
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }

        return destinationName
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[\\\\/:*?\"<>|]"), "")
            .trim()
            .ifEmpty { "download" }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        optDouble(name).takeUnless(Double::isNaN)

    private fun formatBitrate(kbps: Double): String =
        if (kbps >= 1000) {
            String.format(Locale.US, "%.2f Mbps", kbps / 1000)
        } else {
            String.format(Locale.US, "%.0f kbps", kbps)
        }

    private fun formatFileSize(bytes: Double): String =
        when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024)
            else -> String.format(Locale.US, "%.0f B", bytes)
        }

    private fun mimeType(extension: String, isAudioOnly: Boolean): String =
        when (extension.lowercase(Locale.US)) {
            "m4a" -> "audio/mp4"
            "opus" -> "audio/ogg"
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "webm" -> if (isAudioOnly) "audio/webm" else "video/webm"
            else -> "application/octet-stream"
        }

    private fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"
}
