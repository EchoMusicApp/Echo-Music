package com.music.echo.metadata.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CleanedMetadataTest {

  @Test
  fun testDisplayTitleWithVersion() {
    val meta = CleanedMetadata(
      rawTitle = "Faded (Live at Wembley)",
      rawArtist = "Alan Walker",
      cleanTitle = "Faded",
      primaryArtist = "Alan Walker",
      featuredArtists = emptyList(),
      version = VersionInfo(VersionType.LIVE, "Live at Wembley"),
      junkTags = emptyList()
    )
    assertEquals("Faded (Live at Wembley)", meta.toDisplayTitle())
  }

  @Test
  fun testDisplayTitleWithoutVersion() {
    val meta = CleanedMetadata(
      rawTitle = "Faded [Official Video]",
      rawArtist = "Alan Walker",
      cleanTitle = "Faded",
      primaryArtist = "Alan Walker",
      featuredArtists = emptyList(),
      version = null,
      junkTags = listOf("Official Video")
    )
    assertEquals("Faded", meta.toDisplayTitle())
  }

  @Test
  fun testSearchQueryTuple() {
    val meta = CleanedMetadata(
      rawTitle = "Faded (Remix) feat. Someone",
      rawArtist = "Alan Walker",
      cleanTitle = "Faded",
      primaryArtist = "Alan Walker",
      featuredArtists = listOf("Someone"),
      version = VersionInfo(VersionType.REMIX, "Remix"),
      junkTags = emptyList()
    )
    val (searchTitle, searchArtist) = meta.toSearchQuery()
    assertEquals("Faded", searchTitle)
    assertEquals("Alan Walker", searchArtist)
  }
}
