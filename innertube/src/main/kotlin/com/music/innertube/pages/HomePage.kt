package com.music.innertube.pages

import com.music.echo.metadata.cleaner.ApiText
import com.music.innertube.models.Album
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.MusicCarouselShelfRenderer
import com.music.innertube.models.MusicTwoRowItemRenderer
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SectionListRenderer
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterBlockedArtists
import com.music.innertube.models.filterDislikedSongs
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.oddElements

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
      fun fromChipCloudChipRenderer(
        renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip,
      ): Chip? {
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
    /** "Casuale" card leading the shelf, when YouTube ships one. */
    val shuffle: Shuffle? = null,
    /** Every item is a live stream, as in the "Listen together" stations. */
    val liveOnly: Boolean = false,
  ) {
    /**
     * The "Listen together" stations: live streams the app cannot join. Recognized by their items,
     * whatever the language: the title alone matched only in English, so in Italian ("Da ascoltare
     * insieme") they showed.
     */
    val isStationListenTogether: Boolean
      get() {
        if (liveOnly) return true
        val titleUpper = title.uppercase()
        val labelUpper = label?.uppercase().orEmpty()
        return titleUpper.contains("LISTEN TOGETHER") ||
          (labelUpper.contains("STATION") && titleUpper.contains("LISTEN TOGETHER")) ||
          (labelUpper.contains("LISTEN TOGETHER") && titleUpper.contains("STATION"))
      }

    /** Shuffle entry point for the shelf, e.g. for Automotive "play all". */
    data class Shuffle(val title: String, val thumbnail: String?)

    companion object {
      fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
        return Section(
          title =
            renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
              ?: return null,
          label =
            renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline
              ?.runs
              ?.firstOrNull()
              ?.text,
          thumbnail =
            renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail
              ?.musicThumbnailRenderer
              ?.getThumbnailUrl()
              ?: renderer.contents.firstNotNullOfOrNull {
                it.musicShuffleRenderer?.getThumbnailUrl()
              }
              ?: renderer.contents
                .mapNotNull { it.musicTwoRowItemRenderer }
                .mapNotNull { itemThumbnailUrl(it) }
                .firstOrNull()
              ?: renderer.contents.firstNotNullOfOrNull {
                it.musicMultiRowListItemRenderer
                  ?.thumbnail
                  ?.musicThumbnailRenderer
                  ?.getThumbnailUrl()
              },
          endpoint =
            renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton
              ?.buttonRenderer
              ?.navigationEndpoint
              ?.browseEndpoint,
          items =
            renderer.contents
              .mapNotNull { content ->
                content.musicTwoRowItemRenderer?.let { fromMusicTwoRowItemRenderer(it) }
                  ?: content.musicMultiRowListItemRenderer?.let { PodcastPage.episode(it) }
                  // Shelves of list rows ("Quick picks", often "Listen again"): they were dropped,
                  // whole sections with them. They are read as in the search results.
                  ?: content.musicResponsiveListItemRenderer?.let {
                    SearchSummaryPage.fromMusicResponsiveListItemRenderer(it)
                  }
              }
              .ifEmpty {
                return null
              },
          // A live stream carries a badge under its title that is not an inline one (the
          // explicit badge is): stations are live streams only.
          liveOnly =
            renderer.contents
              .filter { it.musicShuffleRenderer == null }
              .let { cards ->
                cards.isNotEmpty() &&
                  cards.all { content ->
                    // A live stream is a video with that badge: podcast cards carry badges of
                    // their own, and counting them hid the Podcast chip's shelves.
                    val card = content.musicTwoRowItemRenderer ?: return@all false
                    card.navigationEndpoint.watchEndpoint?.videoId != null &&
                      card.musicVideoType != "MUSIC_VIDEO_TYPE_PODCAST_EPISODE" &&
                      card.subtitleBadges?.any { it.musicInlineBadgeRenderer == null } == true
                  }
              },
          shuffle =
            renderer.contents.firstNotNullOfOrNull { content ->
              content.musicShuffleRenderer?.let { shuffleRenderer ->
                Shuffle(
                  title =
                    shuffleRenderer.title?.runs?.firstOrNull()?.text?.let(ApiText::clean)
                      ?: "Shuffle",
                  thumbnail = shuffleRenderer.getThumbnailUrl(),
                )
              }
            },
        )
      }

      private fun itemThumbnailUrl(renderer: MusicTwoRowItemRenderer): String? =
        renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
          ?: renderer.thumbnailRenderer.croppedSquareThumbnailRenderer?.getThumbnailUrl()

      private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
        PodcastPage.show(renderer)?.let {
          return it
        }
        PodcastPage.episode(renderer)?.let {
          return it
        }
        return when {
          renderer.isSong -> {
            val subtitleRuns = renderer.subtitle?.runs?.oddElements() ?: return null
            SongItem(
              id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
              title = renderer.title.runs?.firstOrNull()?.text?.let(ApiText::clean) ?: return null,
              artists =
                subtitleRuns
                  .filter { run ->
                    run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true ||
                      (run.navigationEndpoint?.browseEndpoint != null &&
                        run.navigationEndpoint.browseEndpoint.browseId.startsWith("MPREb_") != true)
                  }
                  .map { run ->
                    Artist(
                      name = ApiText.clean(run.text),
                      id = run.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                  }
                  .ifEmpty {
                    subtitleRuns.firstOrNull()?.let { run ->
                      listOf(Artist(name = ApiText.clean(run.text), id = null))
                    } ?: emptyList()
                  },
              album =
                subtitleRuns
                  .firstOrNull {
                    it.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true
                  }
                  ?.let {
                    Album(
                      name = ApiText.clean(it.text),
                      id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null,
                    )
                  },
              duration = null,
              thumbnail =
                renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
              explicit =
                renderer.subtitleBadges?.any {
                  it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } == true,
            )
          }
          renderer.isAlbum -> {
            AlbumItem(
              browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
              playlistId =
                renderer.thumbnailOverlay
                  ?.musicItemThumbnailOverlayRenderer
                  ?.content
                  ?.musicPlayButtonRenderer
                  ?.playNavigationEndpoint
                  ?.watchPlaylistEndpoint
                  ?.playlistId ?: return null,
              title = renderer.title.runs?.firstOrNull()?.text?.let(ApiText::clean) ?: return null,
              artists =
                renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                  Artist(
                    name = ApiText.clean(it.text),
                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                  )
                },
              year = null,
              thumbnail =
                renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
              explicit =
                renderer.subtitleBadges?.find {
                  it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
            )
          }
          renderer.isPlaylist -> {
            PlaylistItem(
              id =
                renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL")
                  ?: return null,
              title = renderer.title.runs?.firstOrNull()?.text?.let(ApiText::clean) ?: return null,
              author =
                Artist(
                  name =
                    renderer.subtitle?.runs?.firstOrNull()?.text?.let(ApiText::clean)
                      ?: return null,
                  id = null,
                ),
              songCountText = null,
              thumbnail =
                renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
              playEndpoint =
                renderer.thumbnailOverlay
                  ?.musicItemThumbnailOverlayRenderer
                  ?.content
                  ?.musicPlayButtonRenderer
                  ?.playNavigationEndpoint
                  ?.watchPlaylistEndpoint ?: return null,
              shuffleEndpoint =
                renderer.menu
                  ?.menuRenderer
                  ?.items
                  ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                  ?.menuNavigationItemRenderer
                  ?.navigationEndpoint
                  ?.watchPlaylistEndpoint ?: return null,
              radioEndpoint =
                renderer.menu.menuRenderer.items
                  .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                  ?.menuNavigationItemRenderer
                  ?.navigationEndpoint
                  ?.watchPlaylistEndpoint,
            )
          }
          renderer.isArtist -> {
            ArtistItem(
              id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
              title = renderer.title.runs?.lastOrNull()?.text?.let(ApiText::clean) ?: return null,
              thumbnail =
                renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
              shuffleEndpoint =
                renderer.menu
                  ?.menuRenderer
                  ?.items
                  ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                  ?.menuNavigationItemRenderer
                  ?.navigationEndpoint
                  ?.watchPlaylistEndpoint ?: return null,
              radioEndpoint =
                renderer.menu.menuRenderer.items
                  .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                  ?.menuNavigationItemRenderer
                  ?.navigationEndpoint
                  ?.watchPlaylistEndpoint ?: return null,
            )
          }
          else -> null
        }
      }
    }
  }

  fun filterBlockedArtists() =
    copy(sections = sections.map { it.copy(items = it.items.filterBlockedArtists()) })

  fun filterDislikedSongs() =
    copy(sections = sections.map { it.copy(items = it.items.filterDislikedSongs()) })

  fun filterExplicit(enabled: Boolean = true) =
    if (enabled) {
      copy(sections = sections.map { it.copy(items = it.items.filterExplicit()) })
    } else this

  fun filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
      copy(
        sections =
          sections.map { section -> section.copy(items = section.items.filterVideoSongs(true)) },
      )
    } else this
}
