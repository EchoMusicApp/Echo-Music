package echo.music.iad1tya.ai.weather

import androidx.compose.runtime.Immutable

@Immutable
data class WeatherInfo(
    val temperature: Double,
    val feelsLike: Double,
    val condition: String,
    val conditionCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val isDay: Boolean,
    val weatherEmoji: String
) {
    val summaryBadgeText: String
        get() = "$weatherEmoji ${temperature.toInt()}°C • $condition • $humidity% Humidity"
}

sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(val data: WeatherInfo) : WeatherUiState
    data class Error(
        val message: String,
        val isPermissionDenied: Boolean = false,
        val isPermanentlyDenied: Boolean = false
    ) : WeatherUiState
}

fun mapWmoCodeToCondition(code: Int, isDay: Boolean): Pair<String, String> {
    return when (code) {
        0 -> if (isDay) "Sunny" to "☀️" else "Clear Sky" to "🌙"
        1 -> if (isDay) "Mostly Sunny" to "🌤️" else "Mostly Clear" to "🌙"
        2 -> "Partly Cloudy" to "⛅"
        3 -> "Overcast" to "☁️"
        45, 48 -> "Foggy & Mist" to "🌫️"
        51, 53, 55 -> "Drizzle" to "🌦️"
        56, 57 -> "Freezing Drizzle" to "🌧️"
        61 -> "Light Rain" to "🌧️"
        63 -> "Moderate Rain" to "🌧️"
        65 -> "Heavy Rain" to "🌧️"
        66, 67 -> "Freezing Rain" to "🌧️"
        71 -> "Light Snow" to "🌨️"
        73 -> "Moderate Snow" to "❄️"
        75 -> "Heavy Snow" to "❄️"
        77 -> "Snow Grains" to "❄️"
        80, 81, 82 -> "Rain Showers" to "🌦️"
        85, 86 -> "Snow Showers" to "🌨️"
        95 -> "Thunderstorm" to "⛈️"
        96, 99 -> "Heavy Thunderstorm" to "⛈️"
        else -> "Clear" to if (isDay) "☀️" else "🌙"
    }
}

fun WeatherInfo.toSnapshotString(): String {
    return "WEATHER_VIBE:${temperature.toInt()}°C • $condition|$weatherEmoji|$condition|$humidity|${temperature.toInt()}"
}

fun parseWeatherSnapshot(params: String?): WeatherInfo? {
    if (params == null || !params.startsWith("WEATHER_VIBE:")) return null
    return try {
        val payload = params.removePrefix("WEATHER_VIBE:")
        val parts = payload.split("|")
        if (parts.size >= 4) {
            val emoji = parts[1]
            val condition = parts[2]
            val humidity = parts[3].toIntOrNull() ?: 50
            val temp = parts.getOrNull(4)?.toDoubleOrNull() ?: parts[0].substringBefore("°").toDoubleOrNull() ?: 20.0
            WeatherInfo(
                temperature = temp,
                feelsLike = temp,
                condition = condition,
                conditionCode = 0,
                humidity = humidity,
                windSpeed = 0.0,
                isDay = true,
                weatherEmoji = emoji
            )
        } else null
    } catch (e: Exception) {
        null
    }
}
