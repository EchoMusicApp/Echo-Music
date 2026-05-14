/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class AddItemYouTubePlaylistResponse(
    val status: String,
    val playlistEditResults: List<PlaylistEditResult>
) {
    @Serializable
    data class PlaylistEditResult(
        val playlistEditVideoAddedResultData: PlaylistEditVideoAddedResultData,
    ) {
        @Serializable
        data class PlaylistEditVideoAddedResultData(
            val setVideoId: String,
            val videoId: String
        )
    }
}
