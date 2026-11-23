package echo.music.iad1tya.playback.queues

import com.music.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioQueuePageFilterTest {
  private fun song(id: String) = SongItem(id, id, emptyList(), thumbnail = "")

  @Test
  fun repeatedSongsWithinAPageAreOnlyQueuedOnce() {
    val filter = RadioQueuePageFilter()

    assertEquals(listOf("a", "b"), filter.filter(listOf(song("a"), song("a"), song("b"))).map { it.id })
  }

  @Test
  fun continuationsCannotRequeueAlreadySeenTracks() {
    val filter = RadioQueuePageFilter()
    filter.filter(listOf(song("a"), song("b")))

    assertEquals(listOf("c"), filter.filter(listOf(song("b"), song("c"), song("a"))).map { it.id })
    assertTrue(filter.filter(listOf(song("a"), song("b"), song("c"))).isEmpty())
  }

  @Test
  fun aNewRadioSessionCanPlayTheSameTracksAgain() {
    val filter = RadioQueuePageFilter()
    filter.filter(listOf(song("a")))
    filter.reset()

    assertEquals(listOf("a"), filter.filter(listOf(song("a"))).map { it.id })
  }
}
