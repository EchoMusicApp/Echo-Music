package echo.music.iad1tya.lyrics

import echo.music.iad1tya.constants.PreferredLyricsProvider

object LyricsProviderRegistry {
  private val providerMap =
    mapOf(
      "YouLyPlus" to YouLyPlusLyricsProvider,
      "Paxsenix" to PaxSenixLyricsProvider,
      "BetterLyrics" to BetterLyricsProvider,
      "Unison" to UnisonLyricsProvider,
      "SimpMusic" to SimpMusicLyricsProvider,
      "LrcLib" to LrcLibLyricsProvider,
      "Kugou" to KuGouLyricsProvider,
      "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
      "YouTubeMusic" to YouTubeLyricsProvider,
    )

  val providerNames = providerMap.keys.toList()

  fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

  fun orderedProviderNames(orderString: String): List<String> {
    if (orderString.isBlank()) return getDefaultProviderOrder()
    val saved = orderString.split(",").map { it.trim() }.filter { it in providerNames }.distinct()
    return saved + getDefaultProviderOrder().filter { it !in saved }
  }

  fun chooseProvider(
    order: List<String>,
    outcomes: Map<String, ProviderOutcome>,
  ): ProviderChoice {
    for (name in order) {
      when (outcomes[name]) {
        null -> return ProviderChoice.Pending
        ProviderOutcome.FAILED -> continue
        ProviderOutcome.SYNCED,
        ProviderOutcome.UNSYNCED -> return ProviderChoice.Selected(name)
      }
    }
    return ProviderChoice.None
  }

  fun deserializeProviderOrder(orderString: String): List<String> {
    if (orderString.isBlank()) return getDefaultProviderOrder()
    return orderString.split(",").map { it.trim() }.filter { it in providerNames }
  }

  fun serializeProviderOrder(providers: List<String>): String =
    providers.filter { it in providerNames }.joinToString(",")

  fun getDefaultProviderOrder(): List<String> =
    listOf(
      "YouLyPlus",
      "Paxsenix",
      "Unison",
      "BetterLyrics",
      "SimpMusic",
      "LrcLib",
      "Kugou",
      "YouTubeSubtitle",
      "YouTubeMusic",
    )

  fun getOrderedProviders(orderString: String): List<LyricsProvider> =
    deserializeProviderOrder(orderString).mapNotNull { getProviderByName(it) }

  fun getProviderNameForEnum(enum: PreferredLyricsProvider): String =
    when (enum) {
      PreferredLyricsProvider.LRCLIB -> "LrcLib"
      PreferredLyricsProvider.KUGOU -> "Kugou"
      PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
      PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
      PreferredLyricsProvider.YOULYPLUS -> "YouLyPlus"
      PreferredLyricsProvider.PAXSENIX -> "Paxsenix"
      PreferredLyricsProvider.UNISON -> "Unison"
    }

  fun getDisplayName(name: String): String =
    when (name) {
      "YouLyPlus" -> "YouLyPlus"
      "Paxsenix" -> "PaxSenix"
      "BetterLyrics" -> "Better Lyrics"
      "Unison" -> "Unison"
      "SimpMusic" -> "SimpMusic"
      "LrcLib" -> "LrcLib"
      "Kugou" -> "KuGou"
      "YouTubeSubtitle" -> "YouTube Subtitle"
      "YouTubeMusic" -> "YouTube Music"
      else -> name
    }
}

enum class ProviderOutcome {
  SYNCED,
  UNSYNCED,
  FAILED,
}

sealed interface ProviderChoice {
  data object Pending : ProviderChoice

  data class Selected(val name: String) : ProviderChoice

  data object None : ProviderChoice
}
