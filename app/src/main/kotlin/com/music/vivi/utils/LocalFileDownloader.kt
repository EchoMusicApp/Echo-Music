package iad1tya.echo.music.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object LocalFileDownloader {
    private val client = OkHttpClient()

    suspend fun download(
        context: Context,
        url: String,
        destinationDirUriString: String,
        fileName: String,
        mimeType: String,
        userAgent: String? = null
    ) = withContext(Dispatchers.IO) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "local_downloads"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notificationId = fileName.hashCode()
        
        try {
            val destinationUri = Uri.parse(destinationDirUriString)
            val dir = DocumentFile.fromTreeUri(context, destinationUri)
            
            if (dir == null || !dir.isDirectory || !dir.canWrite()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Cannot write to selected destination. Please choose a valid directory.", Toast.LENGTH_LONG).show()
                }
                return@withContext
            }

            val existingFile = dir.findFile(fileName)
            val file = existingFile ?: dir.createFile(mimeType, fileName)
            
            if (file == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create file.", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
            }
            
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(fileName)
                .setContentText("Downloading...")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(100, 0, true)

            notificationManager.notify(notificationId, notificationBuilder.build())

            val requestBuilder = Request.Builder().url(url)
            if (userAgent != null) {
                requestBuilder.header("User-Agent", userAgent)
            }
            requestBuilder.header("Range", "bytes=0-")
            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${response.code}", Toast.LENGTH_SHORT).show()
                }
                notificationManager.cancel(notificationId)
                return@withContext
            }

            response.body?.let { body ->
                val contentLength = body.contentLength()
                body.byteStream().use { input ->
                    context.contentResolver.openOutputStream(file.uri)?.use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        var lastUpdate = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            val currentTime = System.currentTimeMillis()
                            if (contentLength > 0 && currentTime - lastUpdate > 1000) {
                                val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                notificationBuilder.setProgress(100, progress, false)
                                    .setContentText("Downloading ($progress%)")
                                notificationManager.notify(notificationId, notificationBuilder.build())
                                lastUpdate = currentTime
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download completed: $fileName", Toast.LENGTH_SHORT).show()
            }
            
            notificationBuilder.setContentText("Download complete")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: Exception) {
            Timber.e(e, "Error downloading local file")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            notificationManager.cancel(notificationId)
        }
    }
}
