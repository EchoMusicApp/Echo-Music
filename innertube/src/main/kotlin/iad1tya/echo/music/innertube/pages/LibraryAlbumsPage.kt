/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.innertube.pages

import iad1tya.echo.music.innertube.models.Album
import iad1tya.echo.music.innertube.models.AlbumItem
import iad1tya.echo.music.innertube.models.Artist
import iad1tya.echo.music.innertube.models.ArtistItem
import iad1tya.echo.music.innertube.models.MusicResponsiveListItemRenderer
import iad1tya.echo.music.innertube.models.MusicTwoRowItemRenderer
import iad1tya.echo.music.innertube.models.PlaylistItem
import iad1tya.echo.music.innertube.models.SongItem
import iad1tya.echo.music.innertube.models.YTItem
import iad1tya.echo.music.innertube.models.oddElements
import iad1tya.echo.music.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
            val playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                ?.watchPlaylistEndpoint?.playlistId
                ?: renderer.menu?.menuRenderer?.items?.firstOrNull()
                    ?.menuNavigationItemRenderer?.navigationEndpoint
                    ?.watchPlaylistEndpoint?.playlistId
                ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" }

            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
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
