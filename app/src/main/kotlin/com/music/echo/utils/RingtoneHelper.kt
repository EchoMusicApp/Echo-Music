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
import android.provider.Settings
import androidx.core.content.getSystemService
import iad1tya.echo.music.constants.AudioQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File

object RingtoneHelper {

    suspend fun getStreamUrl(context: Context, songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val connectivityManager = context.getSystemService<ConnectivityManager>()!!
            val audioQuality = AudioQuality.OPUS

            val result = YTPlayerUtils.playerResponseForPlayback(
                videoId = songId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
            )
            result.getOrNull()?.streamUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAndTrimAsRingtone(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float, String) -> Unit,
        onComplete: (Boolean, String, Uri?) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Getting audio stream...")

            val streamUrl = getStreamUrl(context, songId)
            if (streamUrl == null) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed to get audio stream", null)
                }
                return@withContext
            }

            onProgress(0.1f, "Fetching audio...")

            val tempFile = File(context.cacheDir, "temp_ringtone_source_$songId")

            val connection = java.net.URL(streamUrl).openConnection()
            connection.connect()
            val contentLength = connection.contentLength.toLong()

            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = 0.1f + (totalBytesRead.toFloat() / contentLength) * 0.4f
                            withContext(Dispatchers.Main) {
                                onProgress(progress, "Downloading... ${(progress * 100).toInt()}%")
                            }
                        }
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed to prepare source file", null)
                }
                return@withContext
            }

            onProgress(0.6f, "Processing audio...")

            val trimmedFile = File(context.cacheDir, "trimmed_ringtone_$songId.m4a")
            if (trimmedFile.exists()) trimmedFile.delete()

            val success = trimAudio(context, tempFile, trimmedFile, startMs, endMs)

            if (!success || !trimmedFile.exists() || trimmedFile.length() == 0L) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed to process audio or output is empty", null)
                }
                return@withContext
            }

            onProgress(0.85f, "Saving ringtone...")

            val fileName = "${title.replace(Regex("[^a-zA-Z0-9\\s]"), "")}_trimmed_$songId.m4a"

            val ringtoneUri: Uri = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                        put(MediaStore.Audio.Media.IS_ALARM, true)
                        put(MediaStore.Audio.Media.TITLE, "$title (Ringtone)")
                        put(MediaStore.Audio.Media.ARTIST, artist)
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: throw Exception("Failed to create MediaStore entry")

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        trimmedFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    uri
                } else {
                    val ringtonesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                    if (!ringtonesDir.exists()) ringtonesDir.mkdirs()

                    val file = File(ringtonesDir, fileName)
                    trimmedFile.copyTo(file, overwrite = true)

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DATA, file.absolutePath)
                        put(MediaStore.Audio.Media.TITLE, "$title (Ringtone)")
                        put(MediaStore.Audio.Media.ARTIST, artist)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                        put(MediaStore.Audio.Media.IS_ALARM, true)
                    }

                    context.contentResolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: Uri.fromFile(file)
                }
            } finally {
                tempFile.delete()
                trimmedFile.delete()
            }

            onProgress(0.95f, "Song saved to Ringtones...")

            withContext(Dispatchers.Main) {
                onProgress(1f, "Done!")
                onComplete(true, "\"$title\" added to system ringtones. Please select it from settings.", ringtoneUri)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete(false, "Error: ${e.message}", null)
            }
        }
    }

    private suspend fun trimAudio(
        context: Context,
        inputFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            inputFile.copyTo(outputFile, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openRingtoneSettings(context: Context, ringtoneUri: Uri? = null) {
        try {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone")
                if (ringtoneUri != null) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun hasSettingsPermission(context: Context): Boolean {
        return true
    }

    fun requestSettingsPermission(context: Context) {
        // Do nothing, permission is no longer requested
    }
}
