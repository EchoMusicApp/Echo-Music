package echo.music.iad1tya.ui.player

// Copied from Thumbnail.kt (the rest of that file needs the player UI).
internal fun normalizeCanvasSongTitle(raw: String): String {
  val stripped =
    raw
      .replace(Regex("\\s*\\[[^]]*]"), "")
      .replace(
        Regex(
          "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
          RegexOption.IGNORE_CASE,
        ),
        "",
      )
      .replace(
        Regex(
          "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
          RegexOption.IGNORE_CASE,
        ),
        "",
      )
      .replace(
        Regex(
          "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
          RegexOption.IGNORE_CASE,
        ),
        "",
      )
      .replace(Regex("\\s+"), " ")
      .trim()

  return stripped.trim('-').replace(Regex("\\s+"), " ").trim()
}

internal fun normalizeCanvasArtistName(raw: String): String {
  val first =
    raw
      .split(
        Regex(
          "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
          RegexOption.IGNORE_CASE,
        ),
        limit = 2,
      )
      .firstOrNull()
      .orEmpty()

  return first.replace(Regex("\\s+"), " ").trim()
}

