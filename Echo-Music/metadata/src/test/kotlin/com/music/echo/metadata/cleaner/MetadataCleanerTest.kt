package com.music.echo.metadata.cleaner

import com.music.echo.metadata.models.VersionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataCleanerTest {

  @Test
  fun testOfficialMusicVideoAndQualityStripped() {
    val result = MetadataCleaner.clean("Alan Walker - Faded (Official Music Video) [4K]", "Alan Walker")
    assertEquals("Faded", result.cleanTitle)
    assertEquals("Alan Walker", result.primaryArtist)
    assertNull(result.version)
    assertTrue(result.junkTags.any { it.contains("Official Music Video", ignoreCase = true) })
  }

  @Test
  fun testRemixPreservedInVersionInfo() {
    val result = MetadataCleaner.clean("Illenium - Crawl Outta Love (The Glitch Mob Remix) [Official Audio]", "Illenium")
    assertEquals("Crawl Outta Love", result.cleanTitle)
    assertEquals("Illenium", result.primaryArtist)
    assertNotNull(result.version)
    assertEquals(VersionType.REMIX, result.version?.type)
    assertEquals("The Glitch Mob Remix", result.version?.description)
    assertEquals("Crawl Outta Love (The Glitch Mob Remix)", result.toDisplayTitle())
    assertEquals("Crawl Outta Love", result.toSearchQuery().first)
  }

  @Test
  fun testFeaturingArtistsExtractedFromTitle() {
    val result = MetadataCleaner.clean("Major Lazer - Lean On (feat. MØ) (Official Lyric Video)", "Major Lazer")
    assertEquals("Lean On", result.cleanTitle)
    assertEquals("Major Lazer", result.primaryArtist)
    assertTrue("Should contain MØ in featured artists", result.featuredArtists.contains("MØ"))
  }

  @Test
  fun testTopicChannelCleaned() {
    val result = MetadataCleaner.clean("Yellow", "Coldplay - Topic")
    assertEquals("Yellow", result.cleanTitle)
    assertEquals("Coldplay", result.primaryArtist)
  }

  @Test
  fun testVevoChannelCleaned() {
    val result = MetadataCleaner.clean("Blank Space", "TaylorSwiftVEVO")
    assertEquals("Blank Space", result.cleanTitle)
    assertEquals("Taylor Swift", result.primaryArtist)
  }

  @Test
  fun testCjkBracketsCleaned() {
    val result = MetadataCleaner.clean("RADWIMPS - 前前前世 【Official MV】", "RADWIMPS")
    assertEquals("前前前世", result.cleanTitle)
    assertEquals("RADWIMPS", result.primaryArtist)
  }

  @Test
  fun testTrailingPipeAndStatusCleaned() {
    val result = MetadataCleaner.clean("Song Name | Official Visualizer 2024", "Artist")
    assertEquals("Song Name", result.cleanTitle)
    assertEquals("Artist", result.primaryArtist)
  }

  @Test
  fun testLivePerformanceVersionPreserved() {
    val result = MetadataCleaner.clean("Hotel California (Live on MTV 1994) [HD]", "Eagles")
    assertEquals("Hotel California", result.cleanTitle)
    assertEquals(VersionType.LIVE, result.version?.type)
    assertEquals("Live on MTV 1994", result.version?.description)
  }

  @Test
  fun testAcousticVersionPreserved() {
    val result = MetadataCleaner.clean("Take On Me (Acoustic Version)", "a-ha")
    assertEquals("Take On Me", result.cleanTitle)
    assertEquals(VersionType.ACOUSTIC, result.version?.type)
  }

  @Test
  fun testMultiArtistSeparatorsInArtistField() {
    val result = MetadataCleaner.clean("Title", "Calvin Harris & Dua Lipa")
    assertEquals("Calvin Harris", result.primaryArtist)
    assertEquals("Calvin Harris & Dua Lipa", result.cleanArtist)
    assertTrue("Should include Dua Lipa in featured artists", result.featuredArtists.contains("Dua Lipa"))
    val (queryTitle, queryArtist) = result.toSearchQuery()
    assertEquals("Title", queryTitle)
    assertEquals("Calvin Harris & Dua Lipa", queryArtist)
  }

  @Test
  fun testSoloTitleWithDashInParensDoesNotCorruptArtist() {
    val result = MetadataCleaner.clean("Alone - (Official Video)", "Marshmello")
    assertEquals("Alone", result.cleanTitle)
    assertEquals("Marshmello", result.primaryArtist)
  }

  @Test
  fun testArtistDashJunkDoesNotSplitJunkIntoTitle() {
    val result = MetadataCleaner.clean("Alan Walker - Official Music Video", "Alan Walker - Topic")
    assertEquals("Alan Walker", result.primaryArtist)
    assertTrue("Title should not be set to junk", result.cleanTitle != "Official Music Video")
  }


  @Test
  fun testHtmlEntitiesAndSmartQuotes() {
    val result = MetadataCleaner.clean("Rock &amp; Roll &#39;King&#39; “Edition”", "Artist")
    assertEquals("Rock & Roll 'King' \"Edition\"", result.cleanTitle)
  }

  @Test
  fun testHashtagStripped() {
    val result = MetadataCleaner.clean("Best Song Ever #shorts #trending", "Artist")
    assertEquals("Best Song Ever", result.cleanTitle)
    assertTrue(result.junkTags.contains("#shorts"))
  }

  @Test
  fun testMultipleFeaturingArtistsInTitleAndArtistField() {
    val result = MetadataCleaner.clean("Major Lazer & DJ Snake - Lean On feat. MØ", "")
    assertEquals("Lean On", result.cleanTitle)
    assertEquals("Major Lazer", result.primaryArtist)
    assertTrue(result.featuredArtists.contains("DJ Snake"))
    assertTrue(result.featuredArtists.contains("MØ"))
  }

  @Test
  fun testSlowedAndReverbVersion() {
    val result = MetadataCleaner.clean("After Dark (Slowed + Reverb)", "Mr. Kitty")
    assertEquals("After Dark", result.cleanTitle)
    assertEquals(VersionType.SLOWED, result.version?.type)
    assertEquals("Slowed + Reverb", result.version?.description)
  }

  @Test
  fun testBandNamesWithConjunctionsArePreservedInSearchQuery() {
    val simon = MetadataCleaner.clean("The Sound of Silence", "Simon & Garfunkel")
    assertEquals("The Sound of Silence", simon.cleanTitle)
    assertEquals("Simon & Garfunkel", simon.toSearchQuery().second)

    val earth = MetadataCleaner.clean("September", "Earth, Wind & Fire")
    assertEquals("September", earth.cleanTitle)
    assertEquals("Earth, Wind & Fire", earth.toSearchQuery().second)

    val florence = MetadataCleaner.clean("Dog Days Are Over", "Florence and the Machine")
    assertEquals("Dog Days Are Over", florence.cleanTitle)
    assertEquals("Florence and the Machine", florence.toSearchQuery().second)
  }
}
