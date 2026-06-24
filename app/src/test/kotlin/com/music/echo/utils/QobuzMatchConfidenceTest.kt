package iad1tya.echo.music.utils

import iad1tya.echo.music.utils.qobuz.QobuzAlbum
import iad1tya.echo.music.utils.qobuz.QobuzPerformer
import iad1tya.echo.music.utils.qobuz.QobuzTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Qobuz lossless-matching scorer in YTPlayerUtils.
 *
 * These guard the fix for the silent "lossless -> Saavn/Opus" downgrades
 * (#540, #505, #487): a strong title + duration match must no longer be vetoed
 * just because the artist string is formatted differently across providers,
 * while wrong-song / wrong-length / wrong-language matches stay rejected.
 */
class QobuzMatchConfidenceTest {

    private fun track(
        title: String,
        artist: String?,
        durationSeconds: Int,
        streamable: Boolean = true,
        version: String? = null,
        albumTitle: String? = null,
    ) = QobuzTrack(
        id = 1L,
        title = title,
        duration = durationSeconds,
        performer = artist?.let { QobuzPerformer(name = it) },
        album = albumTitle?.let { QobuzAlbum(title = it) },
        version = version,
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
    fun missingArtistStillMatchesOnTitleAndDuration() {
        // Title-only fallback: a blank artist must not veto a strong match.
        val c = confidence("", "Hello", 295_000L, track("Hello", "Adele", 295))
        assertTrue("missing artist should not veto a strong title+duration match, was $c", c >= ACCEPT_THRESHOLD)
    }

    @Test
    fun unknownDurationStillMatchesOnTitleAndArtist() {
        val c = confidence("Coldplay", "Yellow", null, track("Yellow", "Coldplay", 0))
        assertTrue("unknown duration must stay neutral, was $c", c >= ACCEPT_THRESHOLD)
    }

    @Test
    fun titleMetadataNoiseDoesNotLowerMatch() {
        // Non-parenthesised noise ("Official Video") must be stripped before scoring.
        val c = confidence("Adele", "Hello Official Video", 295_000L, track("Hello", "Adele", 295))
        assertTrue("video metadata noise must not break the match, was $c", c >= 0.9f)
    }

    @Test
    fun wrongDurationIsHardRejected() {
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
    fun wrongLanguageVersionIsRejectedDespiteTitleAndDurationMatch() {
        // Same title + duration, but a conflicting language tag => different song.
        val c = confidence(
            queryArtist = "Anirudh (Tamil)",
            queryTitle = "Naa Ready",
            queryDuration = 200_000L,
            candidate = track("Naa Ready", "Anirudh", 200, version = "Telugu"),
        )
        assertEquals("wrong-language version must be rejected", 0f, c, 0f)
    }

    @Test
    fun sameLanguageVersionStillMatches() {
        val c = confidence(
            queryArtist = "Anirudh (Tamil)",
            queryTitle = "Naa Ready",
            queryDuration = 200_000L,
            candidate = track("Naa Ready", "Anirudh", 200, version = "Tamil"),
        )
        assertTrue("same-language match should pass, was $c", c >= ACCEPT_THRESHOLD)
    }

    @Test
    fun detectsLanguageFromNativeScript() {
        assertEquals("tamil", detectLanguageHint("வணக்கம்"))
        assertEquals("telugu", detectLanguageHint("నమస్తే"))
    }

    @Test
    fun detectsLanguageFromRomanisedTag() {
        assertEquals("telugu", detectLanguageHint("Some Song (Telugu Version)"))
    }

    @Test
    fun noLanguageWhenUndetermined() {
        assertNull(detectLanguageHint("Hello", "Adele"))
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

    @Test
    fun qobuzSearchTermsSupportTitleOnlyLookup() {
        val terms = qobuzSearchTerms("", "Hello")
        assertTrue("blank artist should still search by title", terms.contains("Hello"))
    }
}
