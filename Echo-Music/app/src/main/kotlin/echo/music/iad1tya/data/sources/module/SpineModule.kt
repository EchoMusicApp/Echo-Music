package echo.music.iad1tya.data.sources.module

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * One plugin descriptor as published in a module-source index.
 */
@Serializable
data class SpineModule(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("author") val author: String = "",
    @SerialName("version") val version: String = "",
    @SerialName("code") val code: Int = 0,
    @SerialName("type") val type: String = "MODULE",
    @SerialName("description") val description: String = "",
    @SerialName("tags") private val declaredTags: List<String> = emptyList(),
    @SerialName("labels") private val declaredLabels: List<String> = emptyList(),
    @SerialName("size") val size: Long = 0,
    @SerialName("sizeLabel") val sizeLabel: String = "",
    @SerialName("download") val download: String = "",
    @SerialName("logo") val logo: String? = null,
    @SerialName("icon") val icon: String? = null,
    @SerialName("lastUpdated") val lastUpdated: String = "",
    @SerialName("trusted") val trusted: Boolean = false,
    @SerialName("featured") val featured: Boolean = false,
    @SerialName("nsfw") val nsfw: Boolean = false,
    @SerialName("sources") val sources: List<SpineSource> = emptyList(),
) {
    val tags: List<String> get() = if (declaredTags.isNotEmpty()) declaredTags else declaredLabels

    val isLossless: Boolean
        get() = tags.any {
            it.uppercase(Locale.ROOT).contains("LOSSLESS") ||
                it.uppercase(Locale.ROOT).contains("HI-RES") ||
                it.uppercase(Locale.ROOT).contains("FLAC")
        }

    val hasHiRes: Boolean get() = tags.any { it.uppercase(Locale.ROOT).contains("HI-RES") }

    val isDolbyAtmos: Boolean
        get() = tags.any {
            it.uppercase(Locale.ROOT).contains("ATMOS") || it.uppercase(Locale.ROOT).contains("DOLBY")
        }
}

@Serializable
data class SpineSource(
    @SerialName("name") val name: String = "",
    @SerialName("lang") val lang: String = "all",
    @SerialName("id") val id: String = "",
    @SerialName("baseUrl") val baseUrl: String = ".",
)
