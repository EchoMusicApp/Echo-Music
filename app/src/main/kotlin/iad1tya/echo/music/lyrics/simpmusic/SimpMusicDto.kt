package iad1tya.echo.music.lyrics.simpmusic

import com.google.gson.annotations.SerializedName

data class SimpMusicApiResponse(
    @SerializedName("data") val data: List<SimpMusicLyricItem>?,
    @SerializedName("success") val success: Boolean
)

data class SimpMusicLyricItem(
    @SerializedName("id") val id: String,
    @SerializedName("videoId") val videoId: String,
    @SerializedName("songTitle") val songTitle: String,
    @SerializedName("artistName") val artistName: String,
    @SerializedName("albumName") val albumName: String?,
    @SerializedName("durationSeconds") val durationSeconds: Int?,
    @SerializedName("plainLyric") val plainLyric: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?,
    @SerializedName("richSyncLyrics") val richSyncLyrics: String?,
    @SerializedName("vote") val vote: Int?,
    @SerializedName("contributor") val contributor: String?,
    @SerializedName("contributorEmail") val contributorEmail: String?
)
