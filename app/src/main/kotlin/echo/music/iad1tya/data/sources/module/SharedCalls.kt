package echo.music.iad1tya.data.sources.module

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One in-flight-or-recent answer per key, shared by everyone who asks for it. */
internal class SharedCalls<T>(
  private val ttlMs: Long,
  private val scope: CoroutineScope,
  private val log: (String) -> Unit = {},
  private val now: () -> Long = System::currentTimeMillis,
) {
  private class Pending<T>(val work: Deferred<Result<T>>, val startedAtMs: Long)

  private val entries = ConcurrentHashMap<String, Pending<T>>()
  private val lock = Mutex()

  suspend fun get(
    key: String,
    describe: () -> String,
    produce: suspend () -> Result<T>,
  ): Result<T> {
    val entry = lock.withLock {
      val live = entries[key]?.takeIf { it.work.isActive || now() - it.startedAtMs < ttlMs }
      if (live != null) {
        log(describe() + if (live.work.isActive) " — ALREADY RUNNING" else " — CACHE HIT")
        live
      } else {
        if (entries.size >= MAX_ENTRIES) prune()
        Pending(scope.async { produce() }, now()).also { entries[key] = it }
      }
    }
    return entry.work.await().also { result ->
      if (result.isFailure) entries.remove(key, entry)
    }
  }

  fun prune() {
    val cutoff = now() - ttlMs
    entries.entries.removeIf { !it.value.work.isActive && it.value.startedAtMs < cutoff }
  }

  fun clear() {
    entries.clear()
  }

  companion object {
    private const val MAX_ENTRIES = 256
  }
}
