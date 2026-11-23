package com.music.echo.metadata.cleaner

import com.music.echo.metadata.models.CleanedMetadata
import com.music.echo.metadata.models.VersionInfo
import com.music.echo.metadata.models.VersionType

object MetadataCleaner {

  fun clean(rawTitle: String, rawArtist: String = ""): CleanedMetadata {
    val junkTags = mutableListOf<String>()
    val featuredArtists = mutableListOf<String>()
    var detectedVersion: VersionInfo? = null

    // Stage 1: Unicode and basic formatting sanitization
    var currentTitle = sanitizeUnicode(rawTitle)
    var currentArtist = sanitizeUnicode(rawArtist)

    // Stage 2: Title-Artist prefix extraction (e.g. "Artist - Title")
    val titleArtistSplit = splitArtistTitle(currentTitle, currentArtist)
    currentTitle = titleArtistSplit.title
    if (titleArtistSplit.artist.isNotBlank()) {
      currentArtist = titleArtistSplit.artist
    }

    // Stage 3: Extract bracket/trailing tags (Junk vs Version vs Feat)
    val extraction = extractTagsAndVersions(currentTitle)
    currentTitle = extraction.cleanedTitle
    junkTags.addAll(extraction.junkTags)
    if (detectedVersion == null) {
      detectedVersion = extraction.version
    }
    featuredArtists.addAll(extraction.featuredArtists)

    // Stage 4: Scrub channel junk suffixes from artist
    currentArtist = scrubChannelJunk(currentArtist)

    // Stage 5: Multi-artist delimiter split
    val artistSplit = splitArtists(currentArtist)
    val primaryArtist = artistSplit.first
    featuredArtists.addAll(artistSplit.second)

    // Normalize final cleanTitle
    currentTitle = normalizeTitle(currentTitle)

    return CleanedMetadata(
      rawTitle = rawTitle,
      rawArtist = rawArtist,
      cleanTitle = currentTitle,
      primaryArtist = primaryArtist,
      cleanArtist = currentArtist.ifBlank { primaryArtist },
      featuredArtists = featuredArtists.distinct().filter { it.isNotBlank() && !it.equals(primaryArtist, ignoreCase = true) },
      version = detectedVersion,
      junkTags = junkTags
    )
  }

  private fun sanitizeUnicode(input: String): String {
    var text = input
      .replace("&amp;", "&")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")
      .replace("&#39;", "'")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("[‘’´`]".toRegex(), "'")
      .replace("[“”]".toRegex(), "\"")

    // Remove emojis and symbols that aren't typical music punctuation
    text = text.replace("[\\p{So}\\p{Cn}]".toRegex(), "")
    return text.trim()
  }

  private data class TitleArtistSplit(val title: String, val artist: String)

  private fun splitArtistTitle(title: String, artist: String): TitleArtistSplit {
    // If title has " - ", see if left side is artist
    val parts = title.split(Regex("""\s+-\s+"""))
    if (parts.size >= 2) {
      val potentialArtist = parts[0].trim()
      val potentialTitle = parts.drop(1).joinToString(" - ").trim()

      if (potentialTitle.isNotBlank()) {
        val isRightSideVersionOrJunk =
          potentialTitle.startsWith("(") ||
          potentialTitle.startsWith("[") ||
          potentialTitle.startsWith("【") ||
          potentialTitle.startsWith("「") ||
          extractTagsAndVersions(potentialTitle).cleanedTitle.isBlank() ||
          CleanerRules.VERSION_PATTERNS.any { it.first.matches(potentialTitle) } ||
          CleanerRules.JUNK_PATTERNS.any { it.matches(potentialTitle) } ||
          potentialTitle.matches(Regex("""(?i)\b(official\s+video|official\s+music\s+video|official\s+audio|music\s+video|lyric\s+video|lyrics\s+video|official\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd)\b"""))

        if (artist.isNotBlank()) {
          if (!isRightSideVersionOrJunk && (
            potentialArtist.contains(artist, ignoreCase = true) ||
            artist.contains(potentialArtist, ignoreCase = true) ||
            artist.contains("Topic", ignoreCase = true) ||
            artist.contains("VEVO", ignoreCase = true)
          )) {
            return TitleArtistSplit(potentialTitle, potentialArtist)
          }
        } else if (!isRightSideVersionOrJunk) {
          // Both sides look like real names (Artist - Title)
          return TitleArtistSplit(potentialTitle, potentialArtist)
        }
      }
    }
    return TitleArtistSplit(title, artist)
  }

  private data class TagExtractionResult(
    val cleanedTitle: String,
    val junkTags: List<String>,
    val featuredArtists: List<String>,
    val version: VersionInfo?
  )

  private fun extractTagsAndVersions(title: String): TagExtractionResult {
    var workingTitle = title
    val junkTags = mutableListOf<String>()
    val featuredArtists = mutableListOf<String>()
    var version: VersionInfo? = null

    // Extract pipe junk or trailing separator junk like " | Official Visualizer 2024" or "// something"
    val pipeMatch = Regex("""\s*(?:\||//)\s*(.*)$""").find(workingTitle)
    if (pipeMatch != null) {
      val trailing = pipeMatch.groupValues[1].trim()
      val isJunk = CleanerRules.JUNK_PATTERNS.any { it.containsMatchIn(trailing) } ||
        trailing.contains(Regex("""\b(official|video|visualizer|audio|mv|pv|lyrics?|remaster|hd|4k)\b""", RegexOption.IGNORE_CASE))
      if (isJunk) {
        junkTags.add(trailing)
        workingTitle = workingTitle.substring(0, pipeMatch.range.first).trim()
      }
    }

    // Extract hashtags like #shorts
    val hashtagMatch = Regex("""\s*#\w+""").findAll(workingTitle)
    for (m in hashtagMatch) {
      junkTags.add(m.value.trim())
    }
    workingTitle = workingTitle.replace(Regex("""\s*#\w+"""), "").trim()

    // Match bracket expressions: (...), [...], 【...】, 「...」
    val bracketRegex = Regex("""(\(([^()]*)\)|\[([^\[\]]*)\]|【([^【】]*)】|「([^「」]*)」)""")
    val matches = bracketRegex.findAll(workingTitle).toList()

    for (match in matches) {
      val fullMatch = match.value
      val content = (match.groups[2]?.value ?: match.groups[3]?.value ?: match.groups[4]?.value ?: match.groups[5]?.value ?: "").trim()

      // Check if it's featuring: (feat. Artist) / (ft. Artist)
      val featMatch = Regex("""^(?:feat\.?|ft\.?|featuring)\s+(.+)$""", RegexOption.IGNORE_CASE).find(content)
      if (featMatch != null) {
        val artistsStr = featMatch.groupValues[1].trim()
        val splitFeat = splitArtists(artistsStr)
        featuredArtists.add(splitFeat.first)
        featuredArtists.addAll(splitFeat.second)
        workingTitle = workingTitle.replaceFirst(fullMatch, " ")
        continue
      }

      // Check if it's a version pattern
      var matchedVersion: VersionInfo? = null
      for ((pattern, versionType) in CleanerRules.VERSION_PATTERNS) {
        if (pattern.containsMatchIn(content)) {
          matchedVersion = VersionInfo(versionType, content)
          break
        }
      }

      if (matchedVersion != null) {
        if (version == null) {
          version = matchedVersion
        }
        workingTitle = workingTitle.replaceFirst(fullMatch, " ")
        continue
      }

      // Check if it's junk
      var isJunk = false
      for (junkPattern in CleanerRules.JUNK_PATTERNS) {
        if (junkPattern.containsMatchIn(fullMatch) || junkPattern.containsMatchIn(content)) {
          isJunk = true
          junkTags.add(content.ifBlank { fullMatch })
          break
        }
      }

      if (isJunk) {
        workingTitle = workingTitle.replaceFirst(fullMatch, " ")
      }
    }

    // Check for inline featuring not enclosed in brackets, e.g. "Song feat. Artist"
    val inlineFeatRegex = Regex("""(?:\s+|^)(?:feat\.?|ft\.?|featuring)\s+([^()\[\]\-|]+)""", RegexOption.IGNORE_CASE)
    val inlineFeatMatch = inlineFeatRegex.find(workingTitle)
    if (inlineFeatMatch != null) {
      val artistsStr = inlineFeatMatch.groupValues[1].trim()
      val splitFeat = splitArtists(artistsStr)
      featuredArtists.add(splitFeat.first)
      featuredArtists.addAll(splitFeat.second)
      workingTitle = workingTitle.removeRange(inlineFeatMatch.range).trim()
    }

    return TagExtractionResult(
      cleanedTitle = workingTitle.trim(),
      junkTags = junkTags,
      featuredArtists = featuredArtists,
      version = version
    )
  }

  private fun scrubChannelJunk(artist: String): String {
    var cleaned = artist.trim()
    if (cleaned.endsWith("- Topic", ignoreCase = true)) {
      cleaned = cleaned.substring(0, cleaned.length - 7).trim()
    } else if (cleaned.endsWith(" - Topic", ignoreCase = true)) {
      cleaned = cleaned.substring(0, cleaned.length - 8).trim()
    }

    if (cleaned.endsWith("VEVO", ignoreCase = true)) {
      val base = cleaned.substring(0, cleaned.length - 4).trim()
      cleaned = splitCamelCase(base)
    }

    if (cleaned.endsWith(" Records", ignoreCase = true)) {
      cleaned = cleaned.removeSuffix(" Records").removeSuffix(" records").trim()
    } else if (cleaned.endsWith(" Official", ignoreCase = true)) {
      cleaned = cleaned.removeSuffix(" Official").removeSuffix(" official").trim()
    }

    return cleaned.trim()
  }

  private fun splitCamelCase(text: String): String {
    if (text.contains(" ") || text.all { it.isUpperCase() } || text.all { it.isLowerCase() }) {
      return text
    }
    return text.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
  }

  private fun splitArtists(artistString: String): Pair<String, List<String>> {
    if (artistString.isBlank()) return "" to emptyList()

    var currentTokens = listOf(artistString)

    for (sep in CleanerRules.ARTIST_SEPARATORS) {
      val nextTokens = mutableListOf<String>()
      for (token in currentTokens) {
        val splits = token.split(sep)
        nextTokens.addAll(splits)
      }
      currentTokens = nextTokens
    }

    val cleanedArtists = currentTokens.map { it.trim() }.filter { it.isNotBlank() }
    return if (cleanedArtists.isEmpty()) {
      "" to emptyList()
    } else {
      val primary = cleanedArtists.first()
      val others = cleanedArtists.drop(1)
      primary to others
    }
  }

  private fun normalizeTitle(title: String): String {
    return title
      .replace(Regex("""\s+"""), " ")
      .trim()
      .trimEnd('-', '–', '—', '|', '/')
      .trim()
  }
}
