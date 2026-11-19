package echo.music.iad1tya.listentogether

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherPlaybackEchoTest {
  private val track = TrackInfo(id = "A", title = "Track", artist = "Artist", duration = 60)

  @Test
  fun queueAddOnlyMatchesAnEchoWithTheSameInsertionMode() {
    val addNext =
      PlaybackActionPayload(PlaybackActions.QUEUE_ADD, trackInfo = track, insertNext = true)
    val append = addNext.copy(insertNext = false)
    val appendWithoutFlag = append.copy(insertNext = null)

    assertFalse(addNext.matchesLegacyEcho(append))
    assertFalse(addNext.matchesLegacyEcho(appendWithoutFlag))
    assertFalse(append.matchesLegacyEcho(addNext))
    assertTrue(addNext.matchesLegacyEcho(addNext.copy()))
    assertTrue(append.matchesLegacyEcho(appendWithoutFlag))
    assertTrue(appendWithoutFlag.matchesLegacyEcho(append))
  }

  @Test
  fun queueSyncOnlyMatchesAnEchoWithTheSameEntriesAndReplacementMode() {
    val replace =
      PlaybackActionPayload(PlaybackActions.SYNC_QUEUE, queue = listOf(track), replaceQueue = true)

    assertFalse(replace.matchesLegacyEcho(replace.copy(replaceQueue = false)))
    assertFalse(replace.copy(replaceQueue = false).matchesLegacyEcho(replace))
    assertFalse(replace.matchesLegacyEcho(replace.copy(queue = emptyList())))
    assertTrue(replace.matchesLegacyEcho(replace.copy()))
  }
}
