package com.music.innertube.pages

import com.music.echo.metadata.cleaner.ApiText
import com.music.innertube.models.Artist
import com.music.innertube.models.MusicMultiRowListItemRenderer
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.MusicTwoRowItemRenderer
import com.music.innertube.models.PODCAST_BROWSE_PREFIX
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.Run
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_PODCAST_EPISODE
import com.music.innertube.utils.parseTime

/**
 * Podcasts on YouTube Music are playlists (`PL…`) browsed as `MPSP` + playlist id, and their
 * episodes are regular videos. Shows are therefore mapped to [PlaylistItem] (with
 * [PlaylistItem.isPodcast]) and episodes to [SongItem], so the existing playlist screens, queues
 * and library saving work for them unchanged.
 */
object PodcastPage {
  /** The playlist id of a show from its `MPSP…` browse id. */
  fun showId(browseId: String): String = browseId.removePrefix(PODCAST_BROWSE_PREFIX)

  /** Browse id of a show, used as the episode's "artist" so it links back to the show. */
  fun showBrowseId(showId: String): String = PODCAST_BROWSE_PREFIX + showId

  fun isShowBrowseId(browseId: String?): Boolean =
    browseId?.startsWith(PODCAST_BROWSE_PREFIX) == true

  /**
   * An episode row. [show] is used when the row does not name its show, as on the show's own
   * page.
   */
  fun episode(renderer: MusicMultiRowListItemRenderer, show: Artist? = null): SongItem? {
    val watchEndpoint = renderer.onTap?.watchEndpoint ?: return null
    return SongItem(
      id = watchEndpoint.videoId ?: return null,
      title = renderer.title?.runs?.firstOrNull()?.text?.let(ApiText::clean) ?: return null,
      artists = listOfNotNull(renderer.secondTitle?.runs?.firstOrNull()?.toArtist() ?: show),
      duration =
        renderer.playbackProgress?.musicPlaybackProgressRenderer?.durationText?.runs?.let(
          ::parseDuration
        ),
      musicVideoType = MUSIC_VIDEO_TYPE_PODCAST_EPISODE,
      thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
      endpoint = watchEndpoint,
      // "271K views • 19 dic 2025": the date is the last part.
      publishedText = renderer.subtitle?.runs?.lastOrNull()?.text?.trim()?.takeIf { it.isNotEmpty() },
      playedPercent =
        renderer.playbackProgress?.musicPlaybackProgressRenderer?.playbackProgressPercentage,
      description =
        renderer.description?.runs?.joinToString("") { it.text }?.takeIf { it.isNotBlank() },
    )
  }

  /** A show card (Home shelves, related items). */
  fun show(renderer: MusicTwoRowItemRenderer): PlaylistItem? {
    val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
    if (!isShowBrowseId(browseId)) return null
    return PlaylistItem(
      id = showId(browseId),
      title = renderer.title.runs?.firstOrNull()?.text?.let(ApiText::clean) ?: return null,
      author = renderer.subtitle?.runs?.firstOrNull { it.navigationEndpoint != null }?.toArtist(),
      songCountText = null,
      thumbnail =
        renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
          ?: renderer.thumbnailRenderer.croppedSquareThumbnailRenderer?.getThumbnailUrl(),
      playEndpoint =
        renderer.thumbnailOverlay
          ?.musicItemThumbnailOverlayRenderer
          ?.content
          ?.musicPlayButtonRenderer
          ?.playNavigationEndpoint
          ?.watchPlaylistEndpoint,
      shuffleEndpoint = null,
      radioEndpoint = null,
      isPodcast = true,
    )
  }

  /** A show in search results: title, then the channel. */
  fun show(renderer: MusicResponsiveListItemRenderer): PlaylistItem? {
    val browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null
    if (!isShowBrowseId(browseId)) return null
    return PlaylistItem(
      id = showId(browseId),
      title =
        renderer.flexColumns
          .firstOrNull()
          ?.musicResponsiveListItemFlexColumnRenderer
          ?.text
          ?.runs
          ?.firstOrNull()
          ?.text
          ?.let(ApiText::clean) ?: return null,
      author =
        renderer.flexColumns
          .getOrNull(1)
          ?.musicResponsiveListItemFlexColumnRenderer
          ?.text
          ?.runs
          ?.firstOrNull { it.navigationEndpoint != null }
          ?.toArtist(),
      songCountText = null,
      thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
      playEndpoint =
        renderer.overlay
          ?.musicItemThumbnailOverlayRenderer
          ?.content
          ?.musicPlayButtonRenderer
          ?.playNavigationEndpoint
          ?.watchPlaylistEndpoint,
      shuffleEndpoint = null,
      radioEndpoint = null,
      isPodcast = true,
    )
  }

  /** An episode in search results: title, then "date • show". */
  fun episode(renderer: MusicResponsiveListItemRenderer): SongItem? {
    val watchEndpoint =
      renderer.overlay
        ?.musicItemThumbnailOverlayRenderer
        ?.content
        ?.musicPlayButtonRenderer
        ?.playNavigationEndpoint
        ?.watchEndpoint ?: return null
    if (renderer.musicVideoType != MUSIC_VIDEO_TYPE_PODCAST_EPISODE) return null
    val secondLine =
      renderer.flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        .orEmpty()
    return SongItem(
      id = watchEndpoint.videoId ?: return null,
      title =
        renderer.flexColumns
          .firstOrNull()
          ?.musicResponsiveListItemFlexColumnRenderer
          ?.text
          ?.runs
          ?.firstOrNull()
          ?.text
          ?.let(ApiText::clean) ?: return null,
      artists =
        listOfNotNull(
          secondLine.firstOrNull { isShowBrowseId(it.navigationEndpoint?.browseEndpoint?.browseId) }
            ?.toArtist()
        ),
      musicVideoType = MUSIC_VIDEO_TYPE_PODCAST_EPISODE,
      thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
      endpoint = watchEndpoint,
      // "31 mag 2021 • show": the date comes first.
      publishedText =
        secondLine.firstOrNull()?.takeIf { it.navigationEndpoint == null }?.text?.trim(),
    )
  }

  /**
   * Parses localized durations such as "23 min", "1 h 5 min", "1 hr 5 min" or "1:05:00"; returns
   * null when the text has no recognizable unit.
   */
  fun parseDuration(runs: List<Run>): Int? {
    val text = runs.joinToString("") { it.text }.replace("•", " ").trim()
    if (text.isEmpty()) return null
    if (text.contains(':')) return text.parseTime()
    var seconds = 0
    var matched = false
    DURATION_PART.findAll(text).forEach { match ->
      val value = match.groupValues[1].toIntOrNull() ?: return@forEach
      val unit = match.groupValues[2].lowercase()
      seconds +=
        when {
          unit.startsWith("h") || unit.startsWith("o") || unit.startsWith("st") -> value * 3600
          unit.startsWith("m") -> value * 60
          unit.startsWith("s") -> value
          else -> return@forEach
        }
      matched = true
    }
    return seconds.takeIf { matched && it > 0 }
  }

  private val DURATION_PART = Regex("""(\d+)\s*(\p{L}+)""")

  private fun Run.toArtist() =
    Artist(name = ApiText.clean(text), id = navigationEndpoint?.browseEndpoint?.browseId)
}
