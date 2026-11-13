package echo.music.iad1tya.playback.smart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BeatTrackerTest {

    @Test
    fun pickPeaksFindsLocalMaxima() {
        val logits = floatArrayOf(0f, 0.2f, 1.5f, 0.3f, 0f, 0f, 0f, 2.0f, 0.1f)
        val peaks = BeatTracker.pickPeaks(logits)
        assertEquals(2, peaks.size)
        assertTrue(abs(peaks[0] - 2.0) < 0.5)
        assertTrue(abs(peaks[1] - 7.0) < 0.5)
    }

    @Test
    fun tempoFromBeatsCalculatesBpmAccurately() {
        // 120 BPM = 0.5s per beat
        val beats = (0..15).map { it * 0.5 }
        val bpm = BeatTracker.tempoFromBeats(beats)
        assertEquals(120.0, bpm, 1e-3)
    }

    @Test
    fun gridConfidenceReflectsRegularSpacing() {
        val regularBeats = (0..15).map { it * 0.5 }
        val strongLogits = List(16) { 2.0 }
        val conf = BeatTracker.gridConfidence(regularBeats, strongLogits)
        assertTrue(conf > 0.8)
    }
}
