package com.music.innertube

import com.music.innertube.models.NavigationEndpoint
import com.music.innertube.models.Run
import com.music.innertube.models.Runs
import com.music.innertube.models.SectionListRenderer
import com.music.innertube.models.Tabs
import com.music.innertube.models.response.NextResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteSignalsTest {

  @Test
  fun testParseTasteSignalsExtractsGenresAndMoods() {
    val sampleChip = SectionListRenderer.Header.ChipCloudRenderer.Chip(
      chipCloudChipRenderer = SectionListRenderer.Header.ChipCloudRenderer.Chip.ChipCloudChipRenderer(
        isSelected = false,
        navigationEndpoint = NavigationEndpoint(),
        text = Runs(listOf(Run(text = "Chill Pop", navigationEndpoint = null), Run(text = "Workout Rock", navigationEndpoint = null))),
        uniqueId = "chip1",
      )
    )

    val sampleSectionList = SectionListRenderer(
      header = SectionListRenderer.Header(
        chipCloudRenderer = SectionListRenderer.Header.ChipCloudRenderer(
          chips = listOf(sampleChip)
        )
      ),
      contents = emptyList(),
      continuations = null,
    )

    val tab = Tabs.Tab(
      tabRenderer = Tabs.Tab.TabRenderer(
        title = "Related",
        content = Tabs.Tab.TabRenderer.Content(
          sectionListRenderer = sampleSectionList,
          musicQueueRenderer = null,
        ),
        endpoint = null,
      )
    )

    val response = NextResponse(
      contents = NextResponse.Contents(
        singleColumnMusicWatchNextResultsRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer(
          tabbedRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer(
            watchNextTabbedResultsRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer.WatchNextTabbedResultsRenderer(
              tabs = listOf(tab)
            )
          )
        ),
        twoColumnWatchNextResults = null,
      ),
      continuationContents = null,
      currentVideoEndpoint = null,
    )

    val signals = YouTube.parseTasteSignals("test_vid", response)
    assertEquals("test_vid", signals.videoId)
    assertTrue("Should extract pop and rock", signals.genres.contains("pop") && signals.genres.contains("rock"))
    assertTrue("Should extract chill and workout", signals.moodTags.contains("chill") && signals.moodTags.contains("workout"))
  }

  @Test
  fun testParseTasteSignalsHandlesEmptyResponse() {
    val response = NextResponse(
      contents = NextResponse.Contents(
        singleColumnMusicWatchNextResultsRenderer = null,
        twoColumnWatchNextResults = null,
      ),
      continuationContents = null,
      currentVideoEndpoint = null,
    )

    val signals = YouTube.parseTasteSignals("empty_vid", response)
    assertEquals("empty_vid", signals.videoId)
    assertTrue("Genres should be empty", signals.genres.isEmpty())
    assertTrue("MoodTags should be empty", signals.moodTags.isEmpty())
  }

  @Test
  fun testParseTasteSignalsDoesNotFalsePositiveOnSubstrings() {
    val sampleChip = SectionListRenderer.Header.ChipCloudRenderer.Chip(
      chipCloudChipRenderer = SectionListRenderer.Header.ChipCloudRenderer.Chip.ChipCloudChipRenderer(
        isSelected = false,
        navigationEndpoint = NavigationEndpoint(),
        text = Runs(listOf(
          Run(text = "Therapy Session", navigationEndpoint = null),
          Run(text = "Popular Music", navigationEndpoint = null),
          Run(text = "Warehouse Beats", navigationEndpoint = null)
        )),
        uniqueId = "chip2",
      )
    )

    val sampleSectionList = SectionListRenderer(
      header = SectionListRenderer.Header(
        chipCloudRenderer = SectionListRenderer.Header.ChipCloudRenderer(
          chips = listOf(sampleChip)
        )
      ),
      contents = emptyList(),
      continuations = null,
    )

    val tab = Tabs.Tab(
      tabRenderer = Tabs.Tab.TabRenderer(
        title = "Related",
        content = Tabs.Tab.TabRenderer.Content(
          sectionListRenderer = sampleSectionList,
          musicQueueRenderer = null,
        ),
        endpoint = null,
      )
    )

    val response = NextResponse(
      contents = NextResponse.Contents(
        singleColumnMusicWatchNextResultsRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer(
          tabbedRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer(
            watchNextTabbedResultsRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer.WatchNextTabbedResultsRenderer(
              tabs = listOf(tab)
            )
          )
        ),
        twoColumnWatchNextResults = null,
      ),
      continuationContents = null,
      currentVideoEndpoint = null,
    )

    val signals = YouTube.parseTasteSignals("sub_vid", response)
    // Should NOT falsely match rap from "therapy", pop from "popular", or house from "warehouse"
    assertTrue("Should not match rap from therapy", !signals.genres.contains("rap"))
    assertTrue("Should not match pop from popular", !signals.genres.contains("pop"))
    assertTrue("Should not match house from warehouse", !signals.genres.contains("house"))
  }
}

