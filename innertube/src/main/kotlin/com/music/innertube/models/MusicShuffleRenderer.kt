package com.music.innertube.models

import kotlinx.serialization.Serializable

/**
 * "Casuale" (shuffle) card inside a [MusicCarouselShelfRenderer].
 *
 * YouTube Music puts it in the shelf's `contents` alongside
 * `musicTwoRowItemRenderer` items, and the app's own home renderer only maps the
 * latter, so shelves that lead with a shuffle card rendered as an empty
 * section. Mapping it lets those shelves expose their songs too.
 */
@Serializable
data class MusicShuffleRenderer(
  val title: Runs?,
  val subtitle: Runs?,
  val thumbnail: ThumbnailRenderer?,
) {
  fun getThumbnailUrl(): String? =
    thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
        ?: thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
}