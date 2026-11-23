package echo.music.iad1tya.utils

import echo.music.iad1tya.lyrics.LyricsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTimingTest {
  @Test
  fun zeroTimestampLyricsRemainReadableWithoutFollowingPlayback() {
    val lyrics = "[00:00.00]First line\n[00:00.00]Second line"

    assertFalse(LyricsUtils.hasSyncedLyrics(lyrics))
    assertEquals(listOf("First line", "Second line"), LyricsUtils.parseLyrics(lyrics).map { it.text })
  }

  @Test
  fun metadataTagsDoNotTurnPlainLyricsIntoSyncedLyrics() {
    assertFalse(LyricsUtils.hasSyncedLyrics("[ar:Artist]\nFirst line\nSecond line"))
  }

  @Test
  fun realTimingIsRecognizedAfterWhitespaceAndMetadata() {
    assertTrue(LyricsUtils.hasSyncedLyrics("\n[ar:Artist]\n[00:00.00]First line\n[00:12.50]Second line"))
  }
}
