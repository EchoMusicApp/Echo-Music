package com.music.innertube.pages

import com.music.innertube.models.Artist
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import com.music.innertube.models.MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer
import com.music.innertube.models.MusicResponsiveListItemRenderer.PlaylistItemData
import com.music.innertube.models.NavigationEndpoint
import com.music.innertube.models.Run
import com.music.innertube.models.Runs
import com.music.innertube.models.SongItem
import com.music.innertube.models.Thumbnail
import com.music.innertube.models.ThumbnailRenderer
import com.music.innertube.models.ThumbnailRenderer.MusicThumbnailRenderer
import com.music.innertube.models.Thumbnails
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * YouTube splits "Sabin Rai & The Pharaoh" into the runs `Sabin Rai` / ` & ` / `amp; The Pharaoh`
 * (verbatim from a live `youtubei/v1/search` response). The parser must not surface the `amp;`
 * fragment as an artist.
 */
class SearchPageAmpTest {

  private fun browseRun(text: String, browseId: String) =
    Run(
      text = text,
      navigationEndpoint = NavigationEndpoint(browseEndpoint = BrowseEndpoint(browseId)),
    )

  private fun songRenderer() =
    MusicResponsiveListItemRenderer(
      badges = null,
      fixedColumns = null,
      flexColumns =
        listOf(
          FlexColumn(
            MusicResponsiveListItemFlexColumnRenderer(
              Runs(listOf(Run(text = "Samjhanchu", navigationEndpoint = null)))
            )
          ),
          FlexColumn(
            MusicResponsiveListItemFlexColumnRenderer(
              Runs(
                listOf(
                  browseRun("Sabin Rai", "UCdwRx_HpCXEEs87u9-Ug7Rg"),
                  Run(text = " & ", navigationEndpoint = null),
                  browseRun("amp; The Pharaoh", "UCyCHmG9WYYWptTfqDucYA4Q"),
                  Run(text = " • ", navigationEndpoint = null),
                  browseRun("Dhanyavaad", "MPREb_9nNk6xXyZzA"),
                  Run(text = " • ", navigationEndpoint = null),
                  Run(text = "4:34", navigationEndpoint = null),
                )
              )
            )
          ),
        ),
      thumbnail =
        ThumbnailRenderer(
          musicThumbnailRenderer =
            MusicThumbnailRenderer(
              thumbnail = Thumbnails(listOf(Thumbnail("https://example.com/t.jpg", 544, 544))),
              thumbnailCrop = null,
              thumbnailScale = null,
            ),
          musicAnimatedThumbnailRenderer = null,
          croppedSquareThumbnailRenderer = null,
        ),
      menu = null,
      playlistItemData = PlaylistItemData(playlistSetVideoId = null, videoId = "vid123"),
      overlay = null,
      navigationEndpoint = null,
    )

  @Test
  fun testAmpFragmentIsNotSurfacedAsArtist() {
    val item = SearchPage.toYTItem(songRenderer()) as SongItem

    assertEquals(
      listOf("Sabin Rai", "The Pharaoh"),
      item.artists.map(Artist::name),
    )
    assertEquals(
      listOf("UCdwRx_HpCXEEs87u9-Ug7Rg", "UCyCHmG9WYYWptTfqDucYA4Q"),
      item.artists.map(Artist::id),
    )
  }
}
