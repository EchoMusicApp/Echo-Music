/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package iad1tya.echo.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicMultiRowListItemRenderer(
    val title: Runs?,
    val subtitle: Runs?,
    val thumbnail: ThumbnailRenderer?,
    val onTap: NavigationEndpoint?,
    val playbackProgress: PlaybackProgress?,
    val displayStyle: String?,
    val menu: Menu?,
) {
    @Serializable
    data class PlaybackProgress(
        val value: Float? = null,
    )
}
