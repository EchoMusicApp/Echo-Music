package com.music.jiosaavn

object SaavnMatcher {

    private const val VARIANT_PENALTY = -40

    private val nonAlphanumeric = Regex("[^a-z0-9]+")
    private val whitespace = Regex("\\s+")

    private val variantMarkerPhrases = listOf(
        "karaoke",
        "instrumental",
        "minus one",
        "cover",
        "remix",
        "lo-fi",
        "lofi",
        "sped up",
        "slowed",
        "reverb"
    )

    private val variantMarkers = variantMarkerPhrases.map { " ${normalize(it)} " }

    /**
     * Penalizes Saavn variant titles for issue #561 while preserving explicit variant searches.
     */
    fun variantPenalty(ytTitle: String, candidateName: String): Int {
        val normalizedYtTitle = searchable(ytTitle)
        val normalizedCandidateName = searchable(candidateName)
        var penalty = 0

        for (marker in variantMarkers) {
            if (!normalizedYtTitle.contains(marker) && normalizedCandidateName.contains(marker)) {
                penalty += VARIANT_PENALTY
            }
        }

        return penalty
    }

    private fun searchable(value: String): String = " ${normalize(value)} "

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(nonAlphanumeric, " ")
            .trim()
            .replace(whitespace, " ")
}
