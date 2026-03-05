package iad1tya.echo.music.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object OdesliRepository {
    private val client = OkHttpClient()

    /**
     * Fetches the Odesli/Songlink page URL for the given song URL.
     * Returns the universal "all platforms" page URL, or null on failure.
     * Rate limit: 10 requests/min unauthenticated.
     */
    suspend fun getPageUrl(songUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedUrl = URLEncoder.encode(songUrl, "UTF-8")
            val request = Request.Builder()
                .url("https://api.song.link/v1-alpha.1/links?url=$encodedUrl&songIfSingle=true")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val pageUrl = json.optString("pageUrl")
                if (pageUrl.isNotEmpty()) pageUrl else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
