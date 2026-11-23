package com.music.echo.metadata.models

data class CleanedMetadata(
  val rawTitle: String,
  val rawArtist: String,
  val cleanTitle: String,
  val primaryArtist: String,
  val cleanArtist: String = primaryArtist,
  val featuredArtists: List<String> = emptyList(),
  val version: VersionInfo? = null,
  val junkTags: List<String> = emptyList()
) {
  fun toDisplayTitle(): String {
    return if (version != null && version.type != VersionType.ORIGINAL) {
      "$cleanTitle (${version.description})"
    } else {
      cleanTitle
    }
  }

  fun toSearchQuery(): Pair<String, String> {
    return cleanTitle to cleanArtist
  }
}
