package com.echo.innertube.pages

import com.echo.innertube.models.Album
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.Artist
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.MusicResponsiveListItemRenderer
import com.echo.innertube.models.MusicTwoRowItemRenderer
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.YTItem
import com.echo.innertube.models.oddElements
import com.echo.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
