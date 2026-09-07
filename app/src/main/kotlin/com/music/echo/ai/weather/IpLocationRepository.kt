package echo.music.iad1tya.ai.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

data class IpLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val country: String? = null
)

object IpLocationRepository {
    private val client = OkHttpClient()

    suspend fun fetchIpLocation(): Result<IpLocation> = withContext(Dispatchers.IO) {
        // Primary provider: ipwho.is (HTTPS)
        try {
            val request = Request.Builder()
                .url("https://ipwho.is/")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body.string()
                    if (bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        if (json.optBoolean("success", false)) {
                            val lat = json.optDouble("latitude", Double.NaN)
                            val lon = json.optDouble("longitude", Double.NaN)
                            if (!lat.isNaN() && !lon.isNaN()) {
                                val city = if (json.has("city")) json.optString("city") else null
                                val country = if (json.has("country")) json.optString("country") else null
                                return@withContext Result.success(IpLocation(lat, lon, city, country))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e, "ipwho.is failed, trying fallback provider")
        }

        // Secondary fallback provider: ipapi.co (HTTPS)
        try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .addHeader("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body.string()
                    if (bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        val lat = json.optDouble("latitude", Double.NaN)
                        val lon = json.optDouble("longitude", Double.NaN)
                        if (!lat.isNaN() && !lon.isNaN()) {
                            val city = if (json.has("city")) json.optString("city") else null
                            val country = if (json.has("country_name")) json.optString("country_name") else null
                            return@withContext Result.success(IpLocation(lat, lon, city, country))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching IP location")
        }

        Result.failure(Exception("Could not determine location from IP."))
    }
}
