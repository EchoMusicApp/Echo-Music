package iad1tya.echo.music.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

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
                return@withContext
            }

            response.body?.byteStream()?.use { input ->
                context.contentResolver.openOutputStream(file.uri)?.use { output ->
                    input.copyTo(output)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download completed: $fileName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading local file")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
