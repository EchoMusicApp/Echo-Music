package iad1tya.echo.music.recognition

import com.google.gson.annotations.SerializedName

data class ShazamRequestBody(
    @SerializedName("geolocation")
    val geolocation: Geolocation,
    @SerializedName("signature")
    val signature: Signature,
    @SerializedName("timestamp")
    val timestamp: Int,
    @SerializedName("timezone")
    val timezone: String
)

data class Geolocation(
    @SerializedName("altitude")
    val altitude: Double,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double
)

data class Signature(
    @SerializedName("samplems")
    val samplems: Int,
    @SerializedName("timestamp")
    val timestamp: Int,
    @SerializedName("uri")
    val uri: String
)

data class ShazamResponse(
    @SerializedName("track")
    val track: Track?
)

data class Track(
    @SerializedName("genres")
    val genres: Genres?,
    @SerializedName("images")
    val images: Images?,
    @SerializedName("sections")
    val sections: List<Section>?,
    @SerializedName("subtitle")
    val subtitle: String?,
    @SerializedName("title")
    val title: String?,
)

data class Genres(
    @SerializedName("primary")
    val primary: String?
)

data class Images(
    @SerializedName("background")
    val background: String?,
    @SerializedName("coverart")
    val coverart: String?,
    @SerializedName("coverarthq")
    val coverarthq: String?,
)

data class Section(
    @SerializedName("metadata")
    val metadata: List<Metadata>?,
    @SerializedName("text")
    val text: List<String>?,
    @SerializedName("type")
    val type: String?,
)

data class Metadata(
    @SerializedName("text")
    val text: String?,
    @SerializedName("title")
    val title: String?
)
