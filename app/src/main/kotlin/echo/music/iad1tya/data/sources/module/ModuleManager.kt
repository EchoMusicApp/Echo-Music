package echo.music.iad1tya.data.sources.module

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

internal fun redactModuleUrl(url: String): String = runCatching {
  val uri = java.net.URI(url)
  "${uri.scheme}://***${uri.rawPath.orEmpty()}"
}
  .getOrDefault("***")

/**
 * Fetches a module index, downloads and loads module JS, and calls the module's exported
 * search/stream functions.
 */
class ModuleManager(
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()
) {

  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  private val loadedModules = ConcurrentHashMap<String, LoadedModule>()

  data class LoadedModule(
    val module: SpineModule,
    val jsCode: String,
    val baseUrl: String,
  )

  private val calls = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private fun keyOf(vararg parts: String) = parts.joinToString("|") { "${it.length}:$it" }

  private val loads = SharedCalls<LoadedModule>(ttlMs = 0, scope = calls, log = ::logReuse)
  private val searches = SharedCalls<ModuleSearchResponse>(SEARCH_TTL_MS, calls, ::logReuse)
  private val streams = SharedCalls<ModuleStreamResponse>(STREAM_TTL_MS, calls, ::logReuse)

  private fun logReuse(line: String) = Timber.tag(TAG).d(line)

  private class CachedIndex(val modules: List<SpineModule>, val fetchedAtMs: Long)

  private val indexCache = mutableMapOf<String, CachedIndex>()
  private val indexLock = Mutex()

  suspend fun fetchIndex(sourceUrl: String): Result<List<SpineModule>> =
    withContext(Dispatchers.IO) {
      indexLock.withLock {
        val cached = indexCache[sourceUrl]
        if (cached != null && System.currentTimeMillis() - cached.fetchedAtMs < INDEX_TTL_MS) {
          Timber.tag(TAG)
            .d(
              "▶ fetchIndex(%s) — CACHE HIT (%d modules)",
              redactModuleUrl(sourceUrl),
              cached.modules.size,
            )
          return@withContext Result.success(cached.modules)
        }

        Timber.tag(TAG).d("▶ fetchIndex(%s)", redactModuleUrl(sourceUrl))
        runCatching {
          val request = Request.Builder().url(sourceUrl).build()
          httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
              throw Exception("HTTP ${resp.code} from ${redactModuleUrl(sourceUrl)}")
            }
            val body =
              resp.body?.string()
                ?: throw Exception("Empty body from ${redactModuleUrl(sourceUrl)}")
            val modules = ModuleIndex.parseModules(json, body)
            Timber.tag(TAG).d("  Parsed %d modules", modules.size)
            modules
          }
        }
          .onSuccess {
            indexCache[sourceUrl] = CachedIndex(it, System.currentTimeMillis())
          }
          .onFailure {
            Timber.tag(TAG)
              .e(it, "  ✗ fetchIndex FAILED for %s: %s", redactModuleUrl(sourceUrl), it.message)
          }
      }
    }

  suspend fun loadModule(
    module: SpineModule,
    resolveBaseUrl: suspend (String) -> String = { it },
  ): Result<LoadedModule> =
    loads.get(
      key = module.id,
      describe = { "▶ loadModule(${module.id})" },
    ) {
      loadModuleNow(module, resolveBaseUrl)
    }

  private suspend fun loadModuleNow(
    module: SpineModule,
    resolveBaseUrl: suspend (String) -> String,
  ): Result<LoadedModule> =
    withContext(Dispatchers.IO) {
      val cached = loadedModules[module.id]
      if (cached != null) {
        if (QuickJsExecutor.isLoaded(module.id)) {
          Timber.tag(TAG).d("▶ loadModule(%s) — CACHE HIT", module.id)
          return@withContext Result.success(cached)
        }
        val revived =
          QuickJsExecutor.loadModule(module.id, cached.jsCode, cached.baseUrl).map { cached }
        if (revived.isSuccess) return@withContext revived
        loadedModules.remove(module.id)
      }

      Timber.tag(TAG).d("▶ loadModule(%s) download=%s", module.id, module.download)
      runCatching {
        val downloadUrl =
          if (module.download.startsWith("http")) {
            module.download
          } else {
            val base = resolveBaseUrl(module.download)
            "$base/${module.download}"
          }

        Timber.tag(TAG).d("  Resolved download URL: %s", redactModuleUrl(downloadUrl))
        val request = Request.Builder().url(downloadUrl).build()
        val jsCode =
          httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
              throw Exception("HTTP ${resp.code} downloading module ${module.id}")
            }
            resp.body?.string() ?: throw Exception("Empty body for module ${module.id}")
          }
        val baseUrl = downloadUrl.substringBeforeLast("/")

        QuickJsExecutor.loadModule(module.id, jsCode, baseUrl).getOrThrow()

        val loaded = LoadedModule(module = module, jsCode = jsCode, baseUrl = baseUrl)
        loadedModules[module.id] = loaded
        Timber.tag(TAG)
          .d(
            "  ✓ Loaded module %s: %d chars, baseUrl=%s",
            module.id,
            jsCode.length,
            redactModuleUrl(baseUrl),
          )
        loaded
      }
        .onFailure {
          Timber.tag(TAG).e(it, "  ✗ loadModule FAILED for %s: %s", module.id, it.message)
        }
    }

  suspend fun searchTracks(
    loaded: LoadedModule,
    query: String,
    limit: Int = 50,
    settings: Map<String, String> = emptyMap(),
  ): Result<ModuleSearchResponse> =
    searches.get(
      key = keyOf(loaded.module.id, query, limit.toString(), contextArg(settings)),
      describe = { "▶ searchTracks() module=${loaded.module.id} query=\"$query\" limit=$limit" },
    ) {
      searchTracksNow(loaded, query, limit, settings)
    }

  private suspend fun searchTracksNow(
    loaded: LoadedModule,
    query: String,
    limit: Int,
    settings: Map<String, String>,
  ): Result<ModuleSearchResponse> =
    withContext(Dispatchers.IO) {
      val contextArg = contextArg(settings)
      Timber.tag(TAG)
        .d("▶ searchTracks() module=%s query=\"%s\" limit=%d", loaded.module.id, query, limit)
      runCatching {
        val result =
          QuickJsExecutor.callExport(
              moduleId = loaded.module.id,
              functionName = "searchTracks",
              args = listOf("\"$query\"", limit.toString(), contextArg),
            )
            .getOrThrow()
        json.decodeFromString<ModuleSearchResponse>(result).also {
          Timber.tag(TAG).d("  ✓ Parsed %d tracks (total=%d)", it.tracks.size, it.total)
        }
      }
        .onCancellation()
        .onFailure {
          Timber.tag(TAG)
            .e(
              it,
              "  ✗ searchTracks FAILED for %s query='%s': %s",
              loaded.module.id,
              query,
              it.message,
            )
        }
    }

  suspend fun getStreamUrl(
    loaded: LoadedModule,
    trackId: String,
    quality: String = "",
    settings: Map<String, String> = emptyMap(),
  ): Result<ModuleStreamResponse> =
    streams.get(
      key = keyOf(loaded.module.id, trackId, quality, contextArg(settings)),
      describe = {
        "▶ getStreamUrl() module=${loaded.module.id} trackId=$trackId quality=$quality"
      },
    ) {
      getStreamUrlNow(loaded, trackId, quality, settings)
    }

  private suspend fun getStreamUrlNow(
    loaded: LoadedModule,
    trackId: String,
    quality: String,
    settings: Map<String, String>,
  ): Result<ModuleStreamResponse> =
    withContext(Dispatchers.IO) {
      val contextArg = contextArg(settings)
      Timber.tag(TAG)
        .d("▶ getStreamUrl() module=%s trackId=%s quality=%s", loaded.module.id, trackId, quality)
      runCatching {
        val result =
          QuickJsExecutor.callExport(
              moduleId = loaded.module.id,
              functionName = "getTrackStreamUrl",
              args = listOf("\"$trackId\"", "\"$quality\"", contextArg),
            )
            .getOrThrow()
        json.decodeFromString<ModuleStreamResponse>(result).also {
          Timber.tag(TAG)
            .d(
              "  ✓ streamUrl=%s quality=%s",
              it.streamUrl?.take(100) ?: "<none>",
              it.track?.audioQuality,
            )
        }
      }
        .onCancellation()
        .onFailure {
          Timber.tag(TAG)
            .e(
              it,
              "  ✗ getStreamUrl FAILED for %s trackId=%s: %s",
              loaded.module.id,
              trackId,
              it.message,
            )
        }
    }

  private fun <T> Result<T>.onCancellation(): Result<T> = also {
    (exceptionOrNull() as? CancellationException)?.let { throw it }
  }

  private fun contextArg(settings: Map<String, String>): String =
    settings.entries.joinToString(
      separator = ",",
      prefix = "{settings:{",
      postfix = "}}",
    ) { (key, value) ->
      "\"$key\":{\"value\":\"$value\"}"
    }

  fun unloadModule(moduleId: String) {
    loadedModules.remove(moduleId)
    QuickJsExecutor.unload(moduleId)
  }

  fun unloadAll() {
    loadedModules.clear()
    loads.clear()
    searches.clear()
    streams.clear()
    QuickJsExecutor.unloadAll()
  }

  private companion object {
    const val TAG = "ModuleManager"
    const val INDEX_TTL_MS = 10 * 60 * 1000L
    const val SEARCH_TTL_MS = 10 * 60 * 1000L
    const val STREAM_TTL_MS = 5 * 60 * 1000L
  }
}
