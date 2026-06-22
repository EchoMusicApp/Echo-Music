package com.music.jiosaavn

import org.junit.Assert.*
import org.junit.Test

class SaavnMatcherTest {

    @Test
    fun cleanCandidateReturnsZeroAndKaraokeCandidateReturnsNegative() {
        assertEquals(0, SaavnMatcher.variantPenalty("Expectations", "Expectations"))
        assertTrue(SaavnMatcher.variantPenalty("Expectations", "Expectations (Karaoke Version)") < 0)
    }

    @Test
    fun penaltyIsSuppressedWhenYtTitleContainsVariant() {
        assertEquals(0, SaavnMatcher.variantPenalty("Song (Instrumental)", "Song (Instrumental)"))
    }

    @Test
    fun markerMatchingIsCaseInsensitive() {
        assertTrue(SaavnMatcher.variantPenalty("Song", "Song KARAOKE") < 0)
        assertTrue(SaavnMatcher.variantPenalty("Song", "Song Minus One") < 0)
        assertTrue(SaavnMatcher.variantPenalty("Song", "Song minus-one") < 0)
    }

    @Test
    fun markerAsParentheticalAndHyphenSuffixArePenalized() {
        assertTrue(SaavnMatcher.variantPenalty("Song", "Song (Remix)") < 0)
        assertTrue(SaavnMatcher.variantPenalty("Song", "Song - Remix") < 0)
    }

    @Test
    fun noMarkerCandidateReturnsExactlyZero() {
        assertEquals(0, SaavnMatcher.variantPenalty("Song", "Song"))
    }
}
