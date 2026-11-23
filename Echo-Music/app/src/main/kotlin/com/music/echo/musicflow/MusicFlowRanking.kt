package echo.music.iad1tya.musicflow

import echo.music.iad1tya.generate.trackKeyOf

data class MusicFlowRankingConfig(
  val artistSpacing: Int = 2,
  val likedArtistBoost: Float = 1.0f,
  val lessLikeThisPenalty: Float = 2.5f,
  val positionDecay: Float = 0.02f,
  val sourceWeights: Map<MusicFlowSource, Float> =
    mapOf(
      MusicFlowSource.HISTORY to 0.6f,
      MusicFlowSource.LIKED to 0.8f,
      MusicFlowSource.QUICK_PICKS to 0.5f,
      MusicFlowSource.CATALOG to 0.2f,
      MusicFlowSource.OFFLINE to 0.4f,
    ),
)

data class MusicFlowCandidateFilter(
  val seenSongIds: Set<String> = emptySet(),
  val seenTrackKeys: Set<String> = emptySet(),
  val excludedTrackKeys: Set<String> = emptySet(),
  val hiddenSongIds: Set<String> = emptySet(),
  val recentSongIds: Set<String> = emptySet(),
)

val MusicFlowItem.trackKey: String
  get() = trackKeyOf(song.title, song.artists.joinToString(", ") { it.name })

fun filterMusicFlowCandidates(
  candidates: List<MusicFlowItem>,
  filter: MusicFlowCandidateFilter,
): List<MusicFlowItem> {
  val batchIds = mutableSetOf<String>()
  val batchKeys = mutableSetOf<String>()
  return candidates.filter { item ->
    val key = item.trackKey
    item.songId.isNotBlank() &&
      item.songId !in filter.seenSongIds &&
      key !in filter.seenTrackKeys &&
      key !in filter.excludedTrackKeys &&
      item.songId !in filter.hiddenSongIds &&
      item.songId !in filter.recentSongIds &&
      batchIds.add(item.songId) &&
      batchKeys.add(key)
  }
}

/**
 * Scores candidates by source weight, liked-artist affinity and "less like this" feedback, then
 * reorders greedily so the same primary artist never lands within [MusicFlowRankingConfig.artistSpacing]
 * pages of itself, including across the boundary with the already visible [feedTailArtists].
 */
fun rankMusicFlowCandidates(
  candidates: List<MusicFlowItem>,
  feedTailArtists: List<String?> = emptyList(),
  likedArtists: Set<String> = emptySet(),
  lessLikeArtists: Set<String> = emptySet(),
  config: MusicFlowRankingConfig = MusicFlowRankingConfig(),
): List<MusicFlowItem> {
  val scored =
    candidates
      .mapIndexed { index, item ->
        val artist = item.primaryArtistKey
        var score = config.sourceWeights[item.source] ?: 0f
        score -= index * config.positionDecay
        if (artist != null && artist in likedArtists) score += config.likedArtistBoost
        if (artist != null && artist in lessLikeArtists) score -= config.lessLikeThisPenalty
        item to score
      }
      .sortedByDescending { it.second }
      .map { it.first }
      .toMutableList()

  val recent = ArrayDeque(feedTailArtists.takeLast(config.artistSpacing))
  val result = ArrayList<MusicFlowItem>(scored.size)
  while (scored.isNotEmpty()) {
    val pickIndex =
      scored.indexOfFirst { it.primaryArtistKey == null || it.primaryArtistKey !in recent }
        .takeIf { it >= 0 } ?: 0
    val picked = scored.removeAt(pickIndex)
    result.add(picked)
    recent.addLast(picked.primaryArtistKey)
    while (recent.size > config.artistSpacing) recent.removeFirst()
  }
  return result
}
