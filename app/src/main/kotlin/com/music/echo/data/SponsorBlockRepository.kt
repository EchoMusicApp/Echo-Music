package echo.music.iad1tya.data

import echo.music.iad1tya.models.SponsorBlockSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SponsorBlockRepository @Inject constructor() {
    private val client = OkHttpClient.Builder().build()
    private val cache = mutableMapOf<String, List<SponsorBlockSegment>>()

    suspend fun getSkipSegments(videoId: String): List<SponsorBlockSegment> = withContext(Dispatchers.IO) {
        if (cache.containsKey(videoId)) {
            return@withContext cache[videoId] ?: emptyList()
        }

        try {
            val categories = listOf("sponsor", "intro", "outro", "interaction", "selfpromo", "music_offtopic")
            val categoriesParam = categories.joinToString(separator = "%22,%22", prefix = "[%22", postfix = "%22]")
            val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$categoriesParam"
            
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code != 404) {
                    Timber.w("SponsorBlock API failed for video $videoId with code ${response.code}")
                }
                return@withContext emptyList()
            }

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(responseBody)
            
            val segments = mutableListOf<SponsorBlockSegment>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val segmentArray = obj.getJSONArray("segment")
                if (segmentArray.length() == 2) {
                    segments.add(
                        SponsorBlockSegment(
                            segment = listOf(segmentArray.getDouble(0).toFloat(), segmentArray.getDouble(1).toFloat()),
                            UUID = obj.optString("UUID"),
                            category = obj.optString("category"),
                            actionType = obj.optString("actionType"),
                            locked = obj.optInt("locked", 0),
                            votes = obj.optInt("votes", 0),
                            description = obj.optString("description")
                        )
                    )
                }
            }
            
            cache[videoId] = segments
            segments
        } catch (e: Exception) {
            Timber.e(e, "Error fetching SponsorBlock segments for $videoId")
            emptyList()
        }
    }
}
