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
        // Primary provider: ip-api.com
        try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    if (json.optString("status") == "success") {
                        val lat = json.optDouble("lat", Double.NaN)
                        val lon = json.optDouble("lon", Double.NaN)
                        if (!lat.isNaN() && !lon.isNaN()) {
                            val city = if (json.has("city")) json.optString("city") else null
                            val country = if (json.has("country")) json.optString("country") else null
                            return@withContext Result.success(IpLocation(lat, lon, city, country))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e, "ip-api.com failed, trying fallback provider")
        }

        // Secondary fallback provider: ipapi.co
        try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .addHeader("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
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
        } catch (e: Exception) {
            Timber.e(e, "Error fetching IP location")
        }

        Result.failure(Exception("Could not determine location from IP."))
    }
}
