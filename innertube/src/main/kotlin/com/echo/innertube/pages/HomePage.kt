package com.echo.innertube.pages

import com.echo.innertube.models.Album
import com.echo.innertube.models.AlbumItem
import com.echo.innertube.models.Artist
import com.echo.innertube.models.ArtistItem
import com.echo.innertube.models.BrowseEndpoint
import com.echo.innertube.models.EpisodeItem
import com.echo.innertube.models.MusicCarouselShelfRenderer
import com.echo.innertube.models.MusicMultiRowListItemRenderer
import com.echo.innertube.models.MusicResponsiveListItemRenderer
import com.echo.innertube.models.MusicTwoRowItemRenderer
import com.echo.innertube.models.PlaylistItem
import com.echo.innertube.models.PodcastItem
import com.echo.innertube.models.SectionListRenderer
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.YTItem
import com.echo.innertube.models.oddElements
import com.echo.innertube.models.splitBySeparator
import com.echo.innertube.models.filterExplicit
import com.echo.innertube.models.filterVideoSongs
import com.echo.innertube.models.filterYoutubeShorts
import com.echo.innertube.utils.parseTime

data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    data class Chip(
        val title: String,
        val endpoint: BrowseEndpoint?,
        val deselectEndPoint: BrowseEndpoint?,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                return Chip(
                    title = renderer.chipCloudChipRenderer.text?.runs?.firstOrNull()?.text ?: return null,
                    endpoint = renderer.chipCloudChipRenderer.navigationEndpoint.browseEndpoint,
                    deselectEndPoint = renderer.chipCloudChipRenderer.onDeselectedCommand?.browseEndpoint,
                )
            }
        }
    }

    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: return null

                val items = mutableListOf<YTItem>()

                // Parse musicTwoRowItemRenderer items (songs, albums, playlists, artists, podcasts, episodes)
                renderer.contents.mapNotNull { it.musicTwoRowItemRenderer }
                    .mapNotNull { fromMusicTwoRowItemRenderer(it) }
                    .let { items.addAll(it) }

                // Parse musicMultiRowListItemRenderer items (podcast episodes)
                renderer.contents.mapNotNull { it.musicMultiRowListItemRenderer }
                    .mapNotNull { fromMusicMultiRowListItemRenderer(it) }
                    .let { items.addAll(it) }

                // Parse musicResponsiveListItemRenderer items (quick picks songs)
                renderer.contents.mapNotNull { it.musicResponsiveListItemRenderer }
                    .mapNotNull { fromMusicResponsiveListItemRenderer(it) }
                    .let { items.addAll(it) }

                if (items.isEmpty()) return null

                return Section(
                    title = title,
                    label = renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    items = items,
                )
            }

            private fun fromMusicMultiRowListItemRenderer(renderer: MusicMultiRowListItemRenderer): EpisodeItem? {
                val subtitleRuns = renderer.subtitle?.runs?.splitBySeparator()
                val menuItem = renderer.menu?.menuRenderer?.items?.find {
                    it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType?.startsWith("LIBRARY_") == true
                }?.toggleMenuServiceItemRenderer
                return EpisodeItem(
                    id = renderer.onTap?.watchEndpoint?.videoId ?: return null,
                    title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
                    author = null,
                    podcast = null,
                    duration = subtitleRuns?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                    publishDateText = subtitleRuns?.firstOrNull()?.firstOrNull()?.text,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit = false,
                    endpoint = renderer.onTap.watchEndpoint,
                    libraryAddToken = PageHelper.extractFeedbackToken(menuItem, "LIBRARY_ADD"),
                    libraryRemoveToken = PageHelper.extractFeedbackToken(menuItem, "LIBRARY_SAVED"),
                )
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
                if (!renderer.isSong) return null
                val secondaryLine = renderer.flexColumns
                    .getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.splitBySeparator()
                    ?: return null
                return SongItem(
                    id = renderer.playlistItemData?.videoId ?: return null,
                    title = renderer.flexColumns
                        .firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                    artists = secondaryLine.getOrNull(0)?.oddElements()?.map {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId,
                        )
                    } ?: return null,
                    album = secondaryLine.getOrNull(1)?.firstOrNull()
                        ?.takeIf { it.navigationEndpoint?.browseEndpoint != null }
                        ?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                            )
                        },
                    duration = secondaryLine.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit = renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
                    musicVideoType = renderer.musicVideoType,
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isPodcast -> {
                        PodcastItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = renderer.subtitle?.runs?.firstOrNull()?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                            episodeCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl(),
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        )
                    }

                    renderer.isEpisode -> {
                        val videoId = renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchEndpoint?.videoId ?: return null
                        val subtitleRuns = renderer.subtitle?.runs?.splitBySeparator()
                        val podcastRun = renderer.subtitle?.runs?.find {
                            it.navigationEndpoint?.browseEndpoint?.isPodcastEndpoint == true
                        }
                        val podcastAlbum = podcastRun?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null,
                            )
                        }
                        EpisodeItem(
                            id = videoId,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = subtitleRuns?.firstOrNull()?.firstOrNull()?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                            podcast = podcastAlbum,
                            duration = subtitleRuns?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                            publishDateText = subtitleRuns?.getOrNull(1)?.firstOrNull()?.text,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                            endpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchEndpoint,
                        )
                    }

                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs ?: return null
                        val (artistRuns, albumRuns) = subtitleRuns.partition { run ->
                            run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true
                        }
                        val artists = artistRuns.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                            )
                        }
                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = artists,
                            album = albumRuns.firstOrNull { run ->
                                run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true
                            }?.let { run ->
                                val endpoint = run.navigationEndpoint?.browseEndpoint ?: return null
                                Album(
                                    name = run.text,
                                    id = endpoint.browseId,
                                )
                            },
                            duration = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                            musicVideoType = renderer.musicVideoType,
                        )
                    }

                    renderer.isAlbum -> {
                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                            year = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                        )
                    }

                    renderer.isPlaylist -> {
                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.lastOrNull()?.text ?: return null,
                                id = null,
                            ),
                            songCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        )
                    }

                    else -> null
                }
            }
        }
    }

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(sections = sections.map {
                it.copy(items = it.items.filterExplicit())
            })
        } else this

    fun filterVideoSongs(enabled: Boolean = false) =
        if (enabled) {
            copy(sections = sections.map {
                it.copy(items = it.items.filterVideoSongs(true))
            })
        } else this

    fun filterYoutubeShorts(enabled: Boolean = false) =
        if (enabled) {
            copy(sections = sections.map {
                it.copy(items = it.items.filterYoutubeShorts(true))
            })
        } else this

}
