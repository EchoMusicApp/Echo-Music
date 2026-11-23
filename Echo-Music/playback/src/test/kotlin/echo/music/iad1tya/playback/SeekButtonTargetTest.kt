package echo.music.iad1tya.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekButtonTargetTest {
  @Test
  fun seeksInBothDirections() {
    assertEquals(35_000L, seekButtonTarget(25_000L, 60_000L, 10_000L))
    assertEquals(15_000L, seekButtonTarget(25_000L, 60_000L, -10_000L))
  }

  @Test
  fun clampsAtTrackBoundaries() {
    assertEquals(0L, seekButtonTarget(3_000L, 60_000L, -10_000L))
    assertEquals(60_000L, seekButtonTarget(55_000L, 60_000L, 10_000L))
    assertEquals(0L, seekButtonTarget(0L, 0L, 10_000L))
  }

  @Test
  fun handlesUnknownDurationAndInvalidPosition() {
    assertEquals(35_000L, seekButtonTarget(25_000L, -9223372036854775807L, 10_000L))
    assertEquals(0L, seekButtonTarget(-1L, -1L, -10_000L))
  }

  @Test
  fun avoidsOverflow() {
    assertEquals(Long.MAX_VALUE, seekButtonTarget(Long.MAX_VALUE - 5L, -1L, 10_000L))
  }
}
