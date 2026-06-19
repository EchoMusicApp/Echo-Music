package iad1tya.echo.music.utils

import iad1tya.echo.music.utils.qobuz.QobuzPerformer
import iad1tya.echo.music.utils.qobuz.QobuzTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Qobuz lossless-matching scorer in YTPlayerUtils.
 *
 * These guard the fix for the silent "lossless -> Saavn/Opus" downgrades
 * (#540, #505, #487): a strong title + duration match must no longer be vetoed
 * just because the artist string is formatted differently across providers,
 * while wrong-song / wrong-length matches (#499, #512) stay rejected.
 */
class QobuzMatchConfidenceTest {

    private fun track(
        title: String,
        artist: String?,
        durationSeconds: Int,
        streamable: Boolean = true,
    ) = QobuzTrack(
        id = 1L,
        title = title,
        duration = durationSeconds,
        performer = artist?.let { QobuzPerformer(name = it) },
        streamable = streamable,
    )

    private val ACCEPT_THRESHOLD = 0.5f // mirrors the >= 0.5f gate in tryLossless

    @Test
    fun perfectMatchScoresVeryHigh() {
        val c = confidence("Adele", "Hello", 295_000L, track("Hello", "Adele", 295))
        assertTrue("expected near-perfect score, was $c", c >= 0.9f)
    }

    @Test
    fun sameSongDifferentArtistFormattingStillMatches() {
        // Same recording, same length, but the provider artist strings share no
        // tokens (the exact situation that previously zeroed the product).
        val c = confidence(
            queryArtist = "Vishal-Shekhar",
            queryTitle = "Malang",
            queryDuration = 240_000L,
            candidate = track("Malang", "Siddharth Mahadevan", 240),
        )
        assertTrue("artist mismatch should not veto a strong title+duration match, was $c", c >= ACCEPT_THRESHOLD)
    }

    @Test
    fun unknownDurationStillMatchesOnTitleAndArtist() {
        val c = confidence("Coldplay", "Yellow", null, track("Yellow", "Coldplay", 0))
        assertTrue("unknown duration must stay neutral, was $c", c >= ACCEPT_THRESHOLD)
    }

    @Test
    fun wrongDurationIsHardRejected() {
        // Title + artist match perfectly but the candidate is ~60% longer
        // (e.g. an extended edit / unrelated track).
        val c = confidence("Adele", "Hello", 295_000L, track("Hello", "Adele", 470))
        assertEquals("large duration drift must be rejected", 0f, c, 0f)
    }

    @Test
    fun unrelatedTitleIsRejected() {
        val c = confidence("Adele", "Hello", 295_000L, track("Bohemian Rhapsody", "Queen", 295))
        assertEquals("no title overlap must be rejected", 0f, c, 0f)
    }

    @Test
    fun nonStreamableIsRejected() {
        val c = confidence("Adele", "Hello", 295_000L, track("Hello", "Adele", 295, streamable = false))
        assertEquals("non-streamable candidate must score 0", 0f, c, 0f)
    }

    @Test
    fun correctArtistOutranksWrongArtistOnTies() {
        val correct = confidence("Adele", "Hello", 295_000L, track("Hello", "Adele", 295))
        val wrong = confidence("Adele", "Hello", 295_000L, track("Hello", "Lionel Richie", 295))
        assertTrue("correct artist should rank above wrong artist ($correct vs $wrong)", correct > wrong)
    }

    @Test
    fun cleanSearchTermStripsNoise() {
        assertEquals("Hello", cleanSearchTerm("Hello (Official Video) [HD]"))
    }

    @Test
    fun qobuzSearchTermsIncludePrimaryArtistFallback() {
        val terms = qobuzSearchTerms("Adele, Someone", "Hello")
        assertEquals("first term is the raw query", "Adele, Someone Hello", terms.first())
        assertTrue("should include a primary-artist-only fallback term", terms.contains("Adele Hello"))
    }
}
