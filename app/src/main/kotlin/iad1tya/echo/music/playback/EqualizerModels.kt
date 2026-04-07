package iad1tya.echo.music.playback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.floor

@Serializable
data class EqProfile(
    val id: String,
    val name: String,
    val bandCenterFreqHz: List<Int> = emptyList(),
    val bandLevelsMb: List<Int> = emptyList(),
    val outputGainMb: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
)

@Serializable
data class EqProfilesPayload(
    @SerialName("profiles")
    val profiles: List<EqProfile> = emptyList(),
)

data class EqCapabilities(
    val bandCount: Int,
    val minBandLevelMb: Int,
    val maxBandLevelMb: Int,
    val centerFreqHz: List<Int>,
    val systemPresets: List<String>,
)

data class EqSettings(
    val enabled: Boolean,
    val bandLevelsMb: List<Int>,
    val outputGainEnabled: Boolean,
    val outputGainMb: Int,
    val bassBoostEnabled: Boolean,
    val bassBoostStrength: Int,
    val virtualizerEnabled: Boolean,
    val virtualizerStrength: Int,
)

internal object EqualizerJson {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}

internal fun decodeBandLevelsMb(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { EqualizerJson.json.decodeFromString<List<Int>>(raw) }.getOrNull() ?: emptyList()
}

internal fun encodeBandLevelsMb(levelsMb: List<Int>): String {
    return runCatching { EqualizerJson.json.encodeToString(levelsMb) }.getOrNull().orEmpty()
}

internal fun decodeProfilesPayload(raw: String?): EqProfilesPayload {
    if (raw.isNullOrBlank()) return EqProfilesPayload()
    return runCatching { EqualizerJson.json.decodeFromString<EqProfilesPayload>(raw) }.getOrNull() ?: EqProfilesPayload()
}

internal fun encodeProfilesPayload(payload: EqProfilesPayload): String {
    return runCatching { EqualizerJson.json.encodeToString(payload) }.getOrNull().orEmpty()
}

internal fun resampleLevelsByIndex(levelsMb: List<Int>, targetCount: Int): List<Int> {
    if (targetCount <= 0) return emptyList()
    if (levelsMb.isEmpty()) return List(targetCount) { 0 }
    if (levelsMb.size == targetCount) return levelsMb
    if (targetCount == 1) return listOf(levelsMb.sum() / levelsMb.size)

    val lastIndex = levelsMb.lastIndex.toFloat().coerceAtLeast(1f)
    return List(targetCount) { i ->
        val pos = i.toFloat() * lastIndex / (targetCount - 1).toFloat()
        val lo = floor(pos).toInt().coerceIn(0, levelsMb.lastIndex)
        val hi = ceil(pos).toInt().coerceIn(0, levelsMb.lastIndex)
        val t = (pos - lo.toFloat()).coerceIn(0f, 1f)
        val a = levelsMb[lo]
        val b = levelsMb[hi]
        (a + ((b - a) * t)).toInt()
    }
}