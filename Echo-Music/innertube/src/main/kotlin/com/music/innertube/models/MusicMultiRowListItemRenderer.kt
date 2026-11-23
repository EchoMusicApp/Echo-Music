package com.music.innertube.models

import kotlinx.serialization.Serializable

/**
 * Podcast episode row: a 16:9 thumbnail, the episode title, its show ([secondTitle], only outside
 * the show's own page), a "views • date" subtitle, a description and the duration. Used in
 * [MusicCarouselShelfRenderer] (Home podcast shelves) and [MusicShelfRenderer] (show pages).
 */
@Serializable
data class MusicMultiRowListItemRenderer(
  val title: Runs?,
  val secondTitle: Runs? = null,
  val subtitle: Runs? = null,
  val description: Runs? = null,
  val thumbnail: ThumbnailRenderer?,
  val onTap: NavigationEndpoint?,
  val playbackProgress: PlaybackProgress? = null,
) {
  @Serializable
  data class PlaybackProgress(
    val musicPlaybackProgressRenderer: MusicPlaybackProgressRenderer?,
  ) {
    @Serializable
    data class MusicPlaybackProgressRenderer(
      val playbackProgressPercentage: Int? = null,
      val durationText: Runs? = null,
    )
  }
}
