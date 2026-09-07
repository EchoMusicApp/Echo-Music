package echo.music.iad1tya.ai.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

object WeatherRepository {
    private val client = OkHttpClient()

    suspend fun fetchWeather(latitude: Double, longitude: Double): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Weather service error: ${response.code}"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty weather response"))
            val json = JSONObject(responseBody)
            val current = json.optJSONObject("current") ?: return@withContext Result.failure(Exception("Invalid weather data"))

            val temp = current.optDouble("temperature_2m", 20.0)
            val feelsLike = current.optDouble("apparent_temperature", temp)
            val humidity = current.optInt("relative_humidity_2m", 50)
            val isDayCode = current.optInt("is_day", 1)
            val isDay = isDayCode == 1
            val weatherCode = current.optInt("weather_code", 0)
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)

            val (condition, emoji) = mapWmoCodeToCondition(weatherCode, isDay)

            val weatherInfo = WeatherInfo(
                temperature = temp,
                feelsLike = feelsLike,
                condition = condition,
                conditionCode = weatherCode,
                humidity = humidity,
                windSpeed = windSpeed,
                isDay = isDay,
                weatherEmoji = emoji
            )

            Result.success(weatherInfo)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching weather")
            Result.failure(e)
        }
    }
}
