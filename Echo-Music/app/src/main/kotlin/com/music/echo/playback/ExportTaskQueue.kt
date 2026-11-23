package echo.music.iad1tya.playback

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ExportTaskQueue(
  private val scope: CoroutineScope,
  private val onIdle: () -> Unit,
  private val completionDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
  private val jobs = ConcurrentHashMap<String, Job>()
  private val slots = Semaphore(1)
  val isIdle: Boolean get() = jobs.isEmpty()

  fun submit(songId: String, export: suspend () -> Unit): Boolean {
    val job = scope.launch(start = CoroutineStart.LAZY) {
      slots.withPermit { export() }
    }
    if (jobs.putIfAbsent(songId, job) != null) {
      job.cancel()
      return false
    }
    job.invokeOnCompletion {
      scope.launch(NonCancellable + completionDispatcher) {
        if (jobs.remove(songId, job) && jobs.isEmpty()) onIdle()
      }
    }
    job.start()
    return true
  }

  fun cancelAll() {
    jobs.values.forEach { it.cancel() }
  }
}
