package echo.music.iad1tya.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportTaskQueueTest {
  @Test
  fun completingOneSongDoesNotStopTheRemainingAlbumExports() = runTest {
    var idleCalls = 0
    val release = CompletableDeferred<Unit>()
    val exported = mutableListOf<String>()
    val queue = ExportTaskQueue(this, { idleCalls++ }, StandardTestDispatcher(testScheduler))
    queue.submit("first") { release.await(); exported.add("first") }
    queue.submit("second") { exported.add("second") }
    runCurrent()

    assertEquals(emptyList<String>(), exported)
    assertEquals(0, idleCalls)
    release.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf("first", "second"), exported)
    assertEquals(1, idleCalls)
  }

  @Test
  fun cancellationStopsRunningAndQueuedExports() = runTest {
    var idleCalls = 0
    var secondStarted = false
    val neverFinish = CompletableDeferred<Unit>()
    val queue = ExportTaskQueue(this, { idleCalls++ }, StandardTestDispatcher(testScheduler))
    queue.submit("first") { neverFinish.await() }
    queue.submit("second") { secondStarted = true }
    runCurrent()
    queue.cancelAll()
    advanceUntilIdle()

    assertFalse(secondStarted)
    assertEquals(1, idleCalls)
  }

  @Test
  fun cancellationBeforeExportsStartStillClearsTheQueue() = runTest {
    var idleCalls = 0
    var exports = 0
    val queue = ExportTaskQueue(this, { idleCalls++ }, StandardTestDispatcher(testScheduler))
    queue.submit("first") { exports++ }
    queue.submit("second") { exports++ }
    queue.cancelAll()
    advanceUntilIdle()

    assertEquals(0, exports)
    assertEquals(1, idleCalls)
    assertTrue(queue.isIdle)
  }

  @Test
  fun duplicateRequestsOnlyExportEachSongOnce() = runTest {
    var exports = 0
    val queue = ExportTaskQueue(this, {}, StandardTestDispatcher(testScheduler))
    queue.submit("same") { exports++ }
    assertFalse(queue.submit("same") { exports++ })
    advanceUntilIdle()

    assertEquals(1, exports)
  }
}
