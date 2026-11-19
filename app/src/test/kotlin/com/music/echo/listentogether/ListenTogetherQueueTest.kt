package echo.music.iad1tya.listentogether

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenTogetherQueueTest {
  @Test
  fun rebuildingAFullQueuePreservesTheSecondOccurrence() {
    val old = listOf("A", "B", "A", "C")
    val occurrence = queueOccurrence(old, 2, "A")
    val replacement = old + "D"
    assertEquals(2, activeQueueIndex(replacement, "A", occurrence))
    assertEquals("C", replacement[activeQueueIndex(replacement, "A", occurrence) + 1])
  }

  @Test
  fun activeFirstSnapshotKeepsLaterCopiesInTheUpcomingQueue() {
    val snapshot = listOf("A", "C", "A", "D")
    assertEquals(0, activeQueueIndex(snapshot, "A", occurrence = 0))
    assertEquals(2, activeQueueIndex(snapshot, "A", occurrence = 1))
  }

  @Test
  fun unmarkedSnapshotWithFullQueueStartsAtTheActiveItem() {
    assertEquals(1, incomingRoomQueueIndex(listOf("A", "B", "C"), "B", emptyList(), 0, false))
  }

  @Test
  fun snapshotKeepsKnownDuplicateOccurrenceAndRecognizesAnUpcomingSuffix() {
    val current = listOf("A", "B", "A", "C", "A")
    assertEquals(2, incomingRoomQueueIndex(current + "D", "A", current, 2, false))
    assertEquals(-1, incomingRoomQueueIndex(listOf("C", "A"), "A", current, 2, false))
    assertEquals(-1, incomingRoomQueueIndex(listOf("C", "A"), "A", emptyList(), 0, true))
  }
}
