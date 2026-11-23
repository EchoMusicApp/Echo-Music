package com.music.echo.metadata.cleaner

import com.music.echo.metadata.models.VersionType

object CleanerRules {

  val JUNK_PATTERNS = listOf(
    Regex("""\s*\(.*?\b(official\s+music\s+video|official\s+video|official\s+audio|music\s+video|lyric\s+video|lyrics\s+video|official\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd)\b.*?\)""", RegexOption.IGNORE_CASE),
    Regex("""\s*\[.*?\b(official\s+music\s+video|official\s+video|official\s+audio|music\s+video|lyric\s+video|lyrics\s+video|official\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd)\b.*?\]""", RegexOption.IGNORE_CASE),
    Regex("""\s*【.*?\b(official|mv|pv|video|audio|lyrics)\b.*?】""", RegexOption.IGNORE_CASE),
    Regex("""\s*「.*?\b(official|mv|pv|video|audio|lyrics)\b.*?」""", RegexOption.IGNORE_CASE),
    Regex("""\s*\|.*$"""),
    Regex("""\s*//.*$"""),
    Regex("""\s*#\w+"""),
    Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE)
  )

  val FEATURING_PATTERNS = listOf(
    Regex("""\s*\(feat\.?\s+([^()]+)\)""", RegexOption.IGNORE_CASE),
    Regex("""\s*\(ft\.?\s+([^()]+)\)""", RegexOption.IGNORE_CASE),
    Regex("""\s*\[feat\.?\s+([^\[\]]+)\]""", RegexOption.IGNORE_CASE),
    Regex("""\s*\[ft\.?\s+([^\[\]]+)\]""", RegexOption.IGNORE_CASE),
    Regex("""\s*\b(?:feat\.?|ft\.?|featuring)\s+([^()\[\]\-|]+)""", RegexOption.IGNORE_CASE)
  )

  val VERSION_PATTERNS = listOf(
    Pair(Regex("""\b(remix|flip|bootleg|vip|re-edit|club mix|dub)\b""", RegexOption.IGNORE_CASE), VersionType.REMIX),
    Pair(Regex("""\b(live|session|unplugged|in concert|live at [a-zA-Z0-9\s]+)\b""", RegexOption.IGNORE_CASE), VersionType.LIVE),
    Pair(Regex("""\b(acoustic|orchestral|symphonic)\b""", RegexOption.IGNORE_CASE), VersionType.ACOUSTIC),
    Pair(Regex("""\b(remaster|remastered|deluxe|anniversary)\b""", RegexOption.IGNORE_CASE), VersionType.REMASTER),
    Pair(Regex("""\b(instrumental|karaoke)\b""", RegexOption.IGNORE_CASE), VersionType.INSTRUMENTAL),
    Pair(Regex("""\b(cover)\b""", RegexOption.IGNORE_CASE), VersionType.COVER),
    Pair(Regex("""\b(radio edit|edit)\b""", RegexOption.IGNORE_CASE), VersionType.RADIO_EDIT),
    Pair(Regex("""\b(extended|extended mix)\b""", RegexOption.IGNORE_CASE), VersionType.EXTENDED),
    Pair(Regex("""\b(slowed|slowed \+ reverb|reverb)\b""", RegexOption.IGNORE_CASE), VersionType.SLOWED),
    Pair(Regex("""\b(sped up|nightcore)\b""", RegexOption.IGNORE_CASE), VersionType.SPED_UP)
  )

  val ARTIST_SEPARATORS = listOf(
    " & ",
    ", ",
    " feat. ",
    " feat ",
    " ft. ",
    " ft ",
    " featuring ",
    " and ",
    " x ",
    " X "
  )

  val CHANNEL_JUNK_SUFFIXES = listOf(
    Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE),
    Regex("""VEVO$""", RegexOption.IGNORE_CASE),
    Regex("""\s+Records$""", RegexOption.IGNORE_CASE),
    Regex("""\s+Official$""", RegexOption.IGNORE_CASE)
  )
}
