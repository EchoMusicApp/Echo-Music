package echo.music.iad1tya.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsProviderOrderTest {
  @Test
  fun orderedProviderNamesBlankUsesDefaultOrder() {
    assertEquals(
      LyricsProviderRegistry.getDefaultProviderOrder(),
      LyricsProviderRegistry.orderedProviderNames(""),
    )
  }

  @Test
  fun orderedProviderNamesPartialOrderKeepsSavedNamesThenAppendsMissing() {
    val order = LyricsProviderRegistry.orderedProviderNames("Kugou,LrcLib")
    assertEquals(
      listOf(
        "Kugou",
        "LrcLib",
        "YouLyPlus",
        "Paxsenix",
        "Unison",
        "BetterLyrics",
        "SimpMusic",
        "YouTubeSubtitle",
        "YouTubeMusic",
      ),
      order,
    )
  }

  @Test
  fun orderedProviderNamesDropsUnknownAndDuplicateNames() {
    val order = LyricsProviderRegistry.orderedProviderNames("LrcLib, Bogus,LrcLib")
    assertEquals(
      listOf(
        "LrcLib",
        "YouLyPlus",
        "Paxsenix",
        "Unison",
        "BetterLyrics",
        "SimpMusic",
        "Kugou",
        "YouTubeSubtitle",
        "YouTubeMusic",
      ),
      order,
    )
  }

  @Test
  fun chooseProviderWaitsWhileAnEarlierProviderHasNoOutcome() {
    val choice =
      LyricsProviderRegistry.chooseProvider(
        listOf("Kugou", "LrcLib"),
        mapOf("LrcLib" to ProviderOutcome.SYNCED),
      )
    assertEquals(ProviderChoice.Pending, choice)
  }

  @Test
  fun chooseProviderPrefersEarlierUnsyncedOverLaterSynced() {
    val choice =
      LyricsProviderRegistry.chooseProvider(
        listOf("Kugou", "LrcLib"),
        mapOf(
          "Kugou" to ProviderOutcome.UNSYNCED,
          "LrcLib" to ProviderOutcome.SYNCED,
        ),
      )
    assertEquals(ProviderChoice.Selected("Kugou"), choice)
  }

  @Test
  fun chooseProviderSelectsNextProviderOnlyAfterEarlierFailures() {
    val choice =
      LyricsProviderRegistry.chooseProvider(
        listOf("Kugou", "LrcLib"),
        mapOf(
          "Kugou" to ProviderOutcome.FAILED,
          "LrcLib" to ProviderOutcome.SYNCED,
        ),
      )
    assertEquals(ProviderChoice.Selected("LrcLib"), choice)
  }

  @Test
  fun chooseProviderReturnsNoneWhenEveryProviderFailed() {
    val choice =
      LyricsProviderRegistry.chooseProvider(
        listOf("Kugou", "LrcLib"),
        mapOf(
          "Kugou" to ProviderOutcome.FAILED,
          "LrcLib" to ProviderOutcome.FAILED,
        ),
      )
    assertEquals(ProviderChoice.None, choice)
  }
}
