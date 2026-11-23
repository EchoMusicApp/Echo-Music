package com.music.lrclib

import com.music.echo.metadata.cleaner.MetadataCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcLibSanitizationTest {

  @Test
  fun testSearchQuerySanitization() {
    val query = MetadataCleaner.clean("Coldplay - Yellow (Official Video) [4K]", "Coldplay - Topic").toSearchQuery()
    assertEquals("Yellow", query.first)
    assertEquals("Coldplay", query.second)
  }

  @Test
  fun testRemixSearchQueryStripsRemixForSearch() {
    val query = MetadataCleaner.clean("Song (The Remix)", "Artist").toSearchQuery()
    assertEquals("Song", query.first)
    assertEquals("Artist", query.second)
  }
}
