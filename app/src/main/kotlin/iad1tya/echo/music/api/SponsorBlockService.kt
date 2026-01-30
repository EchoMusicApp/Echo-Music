package iad1tya.echo.music.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object SponsorBlockService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class Segment(
        val category: String,
        val start: Float,
        val end: Float,
        val uuid: String
    )

    private const val BASE_URL = "https://sponsor.ajay.app/api/skipSegments"

    suspend fun getSkipSegments(videoId: String): List<Segment> = withContext(Dispatchers.IO) {
        val categories = "[\"sponsor\",\"intro\",\"outro\",\"interaction\",\"selfpromo\",\"music_offtopic\"]"
        val url = "$BASE_URL?videoID=$videoId&categories=$categories"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.code == 404) return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(body)
            val segments = mutableListOf<Segment>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                segments.add(
                    Segment(
                        category = obj.getString("category"),
                        start = obj.getJSONArray("segment").getDouble(0).toFloat(),
                        end = obj.getJSONArray("segment").getDouble(1).toFloat(),
                        uuid = obj.getString("UUID")
                    )
                )
            }
            segments
        } catch (e: Exception) {
            emptyList()
        }
    }
}
