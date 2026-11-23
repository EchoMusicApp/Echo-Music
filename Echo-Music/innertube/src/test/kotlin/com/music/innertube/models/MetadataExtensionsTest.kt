package com.music.innertube.models

import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataExtensionsTest {

  @Test
  fun testSongItemCleanedMetadataExtension() {
    val songItem = SongItem(
      id = "test_123",
      title = "Faded (Official Music Video)",
      artists = listOf(Artist(name = "Alan Walker", id = "artist_1")),
      thumbnail = "https://example.com/thumb.jpg"
    )

    assertEquals("Faded", songItem.cleanedMetadata.cleanTitle)
    assertEquals("Alan Walker", songItem.cleanArtistName)
    assertEquals("Faded", songItem.displayTitle)
  }
}
