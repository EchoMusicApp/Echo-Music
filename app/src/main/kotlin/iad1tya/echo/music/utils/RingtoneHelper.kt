package iad1tya.echo.music.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.getSystemService
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.echo.innertube.YouTube
import iad1tya.echo.music.constants.AudioQuality
import iad1tya.echo.music.constants.PlayerStreamClient
import iad1tya.echo.music.constants.PlayerStreamClientKey
import iad1tya.echo.music.constants.PoTokenGvsKey
import iad1tya.echo.music.constants.PoTokenPlayerKey
import iad1tya.echo.music.constants.UseVisitorDataKey
import iad1tya.echo.music.constants.WebClientPoTokenEnabledKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

@UnstableApi
object RingtoneHelper {
    private const val TAG = "RingtoneHelper"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .addInterceptor { chain ->
            val request = chain.request()
            val clientParam = request.url.queryParameter("c")
            val ua = StreamClientUtils.resolveUserAgent(clientParam)
            val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)
            val builder = request.newBuilder().header("User-Agent", ua)
            originReferer.origin?.let { builder.header("Origin", it) }
            originReferer.referer?.let { builder.header("Referer", it) }
            chain.proceed(builder.build())
        }
        .build()

    /**
     * Step 1 — Download the song audio to a temp file, reporting progress 0.0–1.0.
     * Always fetches a fresh stream URL from YouTube and downloads via HTTP.
     *
     * @return the temp [File] containing raw audio, or null on failure.
     */
    suspend fun downloadAudio(
        context: Context,
        songId: String,
        @Suppress("UNUSED_PARAMETER") downloadCache: SimpleCache,
        @Suppress("UNUSED_PARAMETER") playerCache: SimpleCache,
        onProgress: suspend (Float, String) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Fetching stream URL…")
            val connectivityManager = context.getSystemService<ConnectivityManager>()!!
            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = songId,
                audioQuality = AudioQuality.AUTO,
                connectivityManager = connectivityManager,
                preferredStreamClient = context.dataStore[PlayerStreamClientKey]
                    ?.let { runCatching { PlayerStreamClient.valueOf(it) }.getOrNull() }
                    ?: PlayerStreamClient.ANDROID_VR,
                webClientPoTokenEnabled = context.dataStore.get(WebClientPoTokenEnabledKey, false),
                useVisitorData = context.dataStore.get(UseVisitorDataKey, false),
                manualGvsPoToken = context.dataStore.get(PoTokenGvsKey),
                manualPlayerPoToken = context.dataStore.get(PoTokenPlayerKey),
            ).getOrElse {
                Log.e(TAG, "Failed to get stream URL: ${it.message}")
                return@withContext null
            }

            val contentLength = playbackData.format.contentLength ?: -1L
            val streamUrl = "${playbackData.streamUrl}&range=0-${contentLength.takeIf { it > 0 } ?: 10_000_000}"

            onProgress(0.10f, "Downloading audio…")

            val tempFile = File(context.cacheDir, "${songId}_ringtone_src.tmp")
            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder().url(streamUrl).build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body ?: run {
                    Log.e(TAG, "Empty HTTP response body")
                    return@withContext null
                }
                val total = if (contentLength > 0) contentLength else body.contentLength()
                val buffer = ByteArray(8192)
                var downloaded = 0L

                FileOutputStream(tempFile).use { out ->
                    body.byteStream().use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val prog = 0.10f + (downloaded.toFloat() / total) * 0.88f
                                onProgress(prog.coerceIn(0f, 0.98f), "Downloading… ${(downloaded * 100 / total).coerceIn(0, 100)}%")
                            }
                        }
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                Log.e(TAG, "Download produced empty file")
                return@withContext null
            }

            onProgress(1f, "Download complete")
            Log.d(TAG, "Downloaded ${tempFile.length()} bytes")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "downloadAudio failed: ${e.message}", e)
            null
        }
    }

    /**
     * Step 2 — Transcode [startMs]..[endMs] of [audioFile] to AAC/m4a using Media3 Transformer,
     * save to the device Ringtones folder, then open the system ringtone picker.
     */
    suspend fun processAndSetRingtone(
        context: Context,
        audioFile: File,
        title: String,
        artist: String,
        startMs: Long,
        endMs: Long,
        onProgress: suspend (String) -> Unit,
    ): Boolean {
        try {
            onProgress("Trimming & encoding…")

            val outputFile = File(context.cacheDir, "${audioFile.nameWithoutExtension}_out.m4a").also {
                if (it.exists()) it.delete()
            }

            val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs.coerceAtLeast(0L))
            if (endMs != Long.MAX_VALUE) clippingBuilder.setEndPositionMs(endMs)

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(audioFile))
                .setClippingConfiguration(clippingBuilder.build())
                .build()

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)
                .build()

            val success = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val transformer = Transformer.Builder(context)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .build()

                    transformer.addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(true)
                        }
                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            Log.e(TAG, "Transformer error: ${exportException.message}", exportException)
                            if (cont.isActive) cont.resume(false)
                        }
                    })

                    try {
                        transformer.start(editedMediaItem, outputFile.absolutePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Transformer.start failed: ${e.message}", e)
                        if (cont.isActive) cont.resume(false)
                    }

                    cont.invokeOnCancellation { transformer.cancel() }
                }
            }

            audioFile.delete()

            if (!success || !outputFile.exists() || outputFile.length() == 0L) {
                outputFile.delete()
                return false
            }

            onProgress("Saving to Ringtones…")

            val sanitized = title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().take(50)
            val fileName  = "${sanitized}_ringtone.m4a"

            val ringtoneUri = withContext(Dispatchers.IO) {
                saveRingtone(context, outputFile, fileName, title, artist)
            }

            outputFile.delete()

            if (ringtoneUri == null) {
                Log.e(TAG, "saveRingtone returned null")
                return false
            }

            Log.d(TAG, "Ringtone saved: $ringtoneUri")
            onProgress("Opening ringtone picker…")

            withContext(Dispatchers.Main) {
                try {
                    context.startActivity(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Ringtone picker unavailable, opening Sound settings: ${e.message}")
                    try {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    } catch (_: Exception) {}
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "processAndSetRingtone failed: ${e.message}", e)
            return false
        }
    }

    // ─── MediaStore save ──────────────────────────────────────────────────────

    private fun saveRingtone(
        context: Context,
        file: File,
        fileName: String,
        title: String,
        artist: String,
    ): Uri? {
        val resolver = context.contentResolver

        resolver.delete(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
            arrayOf(fileName)
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES + "/")
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
                put(MediaStore.Audio.Media.IS_ALARM, 0)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv) ?: return null
            try {
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            } catch (e: Exception) {
                Log.e(TAG, "Write failed: ${e.message}")
                resolver.delete(uri, null, null)
                return null
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, fileName)
            file.copyTo(dest, overwrite = true)
            resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "${MediaStore.MediaColumns.DATA} = ?", arrayOf(dest.absolutePath))
            val cv = ContentValues().apply {
                @Suppress("DEPRECATION")
                put(MediaStore.MediaColumns.DATA, dest.absolutePath)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.SIZE, dest.length())
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
                put(MediaStore.Audio.Media.IS_ALARM, 0)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
            }
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv)
        }
    }
}
