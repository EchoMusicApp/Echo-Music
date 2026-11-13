package echo.music.iad1tya.data.sources.module

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.AsyncFunctionBinding
import com.dokar.quickjs.binding.FunctionBinding
import com.dokar.quickjs.binding.define
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

/** Sandboxed QuickJS engine pool for executing modular community JavaScript plugins. */
object QuickJsExecutor {

  private const val TAG = "QuickJsExecutor"
  private const val MAX_MODULES = 12
  private const val ENGINES_PER_MODULE = 3

  private class Pool(val jsCode: String, val fetchBase: String) {
    val free = Channel<QuickJs>(Channel.UNLIMITED)
    val lock = Mutex()
    var started = 0
    val made = mutableListOf<QuickJs>()
  }

  fun isLoaded(moduleId: String): Boolean = synchronized(engineLock) { pools.containsKey(moduleId) }

  private val syncHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private val engineLock = Any()
  private val pools = LinkedHashMap<String, Pool>(16, 0.75f, true)

  suspend fun loadModule(moduleId: String, jsCode: String, fetchBase: String = ""): Result<Unit> {
    val evicted =
      synchronized(engineLock) {
        if (pools.containsKey(moduleId)) {
          Timber.tag(TAG).d("QuickJsExecutor.loadModule(%s) — ENGINE CACHE HIT", moduleId)
          return Result.success(Unit)
        }
        buildList {
          while (pools.size >= MAX_MODULES) {
            val lruId = pools.keys.firstOrNull() ?: break
            Timber.tag(TAG).d("  Evicting LRU module engine: %s", lruId)
            pools.remove(lruId)?.let { addAll(it.made) }
          }
        }
      }
    evicted.forEach { runCatching { it.close() } }

    Timber.tag(TAG)
      .d(
        "QuickJsExecutor.loadModule(%s) fetchBase=%s jsCodeLength=%d",
        moduleId,
        fetchBase,
        jsCode.length,
      )
    return withContext(Dispatchers.Default) {
      runCatching { newEngine(jsCode, fetchBase) }
        .onSuccess { qjs ->
          val pool = Pool(jsCode, fetchBase)
          pool.started = 1
          pool.made += qjs
          pool.free.trySend(qjs)
          synchronized(engineLock) { pools[moduleId] = pool }
        }
        .onFailure {
          Timber.tag(TAG).e(it, "  ✗ loadModule FAILED for %s: %s", moduleId, it.message)
        }
        .map {}
    }
  }

  private suspend fun newEngine(jsCode: String, fetchBase: String): QuickJs {
    val qjs = QuickJs.create(Dispatchers.Default)
    qjs.maxStackSize = 512 * 1024L
    try {
      bindConsole(qjs)
      bindAsyncFetch(qjs, fetchBase)

      qjs.evaluate<String>(POLYFILLS)
      val cleanCode = preprocessModuleCode(jsCode)

      qjs.evaluate<String>(
        """
                var __spine_iife_error = null;
                var __spine_mod = (function() {
                    try {
                        var module = { exports: {} };
                        var exports = module.exports;
                        var self = {};
                        $cleanCode
                        if (module.exports && (module.exports.searchTracks || module.exports.getTrackStreamUrl)) {
                            return module.exports;
                        }
                        return {};
                    } catch(e) {
                        __spine_iife_error = e && e.message ? e.message : String(e);
                        return {};
                    }
                })();
                'ok'
                """
          .trimIndent()
      )

      val iifeError = qjs.evaluate<String>("__spine_iife_error || 'none'")
      if (iifeError != "none") throw IllegalStateException("Module init error: $iifeError")

      val keys = qjs.evaluate<String>("Object.keys(__spine_mod).join(', ')")
      Timber.tag(TAG).d("  Module exports: [%s]", keys)
      return qjs
    } catch (e: Throwable) {
      qjs.close()
      throw e
    }
  }

  private suspend fun acquire(moduleId: String, pool: Pool): QuickJs {
    pool.free.tryReceive().getOrNull()?.let {
      return it
    }

    val mine =
      pool.lock.withLock {
        if (pool.started >= ENGINES_PER_MODULE) return@withLock false
        pool.started++
        true
      }
    if (!mine) return pool.free.receive()

    return try {
      newEngine(pool.jsCode, pool.fetchBase).also { fresh ->
        pool.lock.withLock { pool.made += fresh }
      }
    } catch (e: Throwable) {
      pool.lock.withLock { pool.started-- }
      throw e
    }
  }

  suspend fun callExport(
    moduleId: String,
    functionName: String,
    args: List<String>,
  ): Result<String> {
    val pool =
      synchronized(engineLock) { pools[moduleId] }
        ?: return Result.failure(IllegalStateException("Module $moduleId is not loaded"))

    Timber.tag(TAG).d("QuickJsExecutor.callExport(%s, %s) args=%s", moduleId, functionName, args)
    return withContext(Dispatchers.Default) {
      val qjs =
        try {
          acquire(moduleId, pool)
        } catch (e: Throwable) {
          if (e is kotlinx.coroutines.CancellationException) throw e
          return@withContext Result.failure(e)
        }
      try {
        callOn(qjs, moduleId, functionName, args)
      } finally {
        pool.free.trySend(qjs)
      }
    }
  }

  private suspend fun callOn(
    qjs: QuickJs,
    moduleId: String,
    functionName: String,
    args: List<String>,
  ): Result<String> = run {
    runCatching {
      val hasFn = qjs.evaluate<String>("typeof __spine_mod['$functionName']")
      if (hasFn != "function") {
        throw IllegalStateException("$functionName is not a function on module $moduleId")
      }

      val argsStr = args.joinToString(",")

      qjs.evaluate<String>(
        """
                var __spine_resolved_json = undefined;
                (async function() {
                    var __fn = __spine_mod['$functionName'];
                    if (!__fn) {
                        __spine_resolved_json = JSON.stringify({ error: '$functionName not found' });
                        return;
                    }
                    try {
                        var r = await __fn($argsStr);
                        __spine_resolved_json = typeof r === 'string' ? r : JSON.stringify(r);
                    } catch(e) {
                        __spine_resolved_json = JSON.stringify({ error: e && e.message ? e.message : String(e) });
                    }
                })();
                """
          .trimIndent()
      )

      val rawResult = qjs.evaluate<String>("__spine_resolved_json")
      Timber.tag(TAG).d("  callExport result (%d chars)", rawResult.length)
      rawResult
    }
      .onFailure {
        Timber.tag(TAG)
          .e(it, "  ✗ callExport FAILED for %s.%s: %s", moduleId, functionName, it.message)
      }
  }

  fun unload(moduleId: String) {
    val closing = synchronized(engineLock) { pools.remove(moduleId) }?.made.orEmpty()
    closing.forEach { runCatching { it.close() } }
    Timber.tag(TAG).d("QuickJsExecutor.unload(%s)", moduleId)
  }

  fun unloadAll() {
    val all =
      synchronized(engineLock) {
        val values = pools.values.flatMap { it.made }
        pools.clear()
        values
      }
    all.forEach { runCatching { it.close() } }
    Timber.tag(TAG).d("QuickJsExecutor.unloadAll()")
  }

  private fun preprocessModuleCode(jsCode: String): String {
    val code = jsCode.trim()

    val exportPattern = Regex("""^export\s+const\s+\w+\s*=\s*`""")
    val exportMatch = exportPattern.find(code)
    if (exportMatch != null) {
      val contentStart = exportMatch.range.last + 1
      var i = contentStart
      while (i < code.length) {
        if (code[i] == '\\' && i + 1 < code.length) {
          i += 2
          continue
        }
        if (code[i] == '`') {
          return code.substring(contentStart, i).trim()
        }
        i++
      }
      Timber.tag(TAG).w("  Template literal not closed, falling through to regex preprocess")
    }

    var result = code
    result =
      result.replace(Regex("""\bexport\s+default\s+(?=function|class|const|let|var|async)"""), "")
    result = result.replace(Regex("""\bexport\s+(const|let|var|function|class|async)\b"""), "$1")
    result = result.replace(Regex("""\bexport\s*\{[^}]*\}\s*;?"""), "")
    return result
  }

  private fun bindConsole(qjs: QuickJs) {
    qjs.define("console") {
      function(
        "log",
        object : FunctionBinding<Unit> {
          override fun invoke(args: Array<Any?>) {
            Timber.tag(TAG).d("[JS] %s", args.joinToString(" ") { it?.toString() ?: "null" })
          }
        },
      )
      function(
        "error",
        object : FunctionBinding<Unit> {
          override fun invoke(args: Array<Any?>) {
            Timber.tag(TAG).e("[JS-ERR] %s", args.joinToString(" ") { it?.toString() ?: "null" })
          }
        },
      )
      function(
        "warn",
        object : FunctionBinding<Unit> {
          override fun invoke(args: Array<Any?>) {
            Timber.tag(TAG).w("[JS-WARN] %s", args.joinToString(" ") { it?.toString() ?: "null" })
          }
        },
      )
      function(
        "info",
        object : FunctionBinding<Unit> {
          override fun invoke(args: Array<Any?>) {
            Timber.tag(TAG).i("[JS-INFO] %s", args.joinToString(" ") { it?.toString() ?: "null" })
          }
        },
      )
    }
  }

  private suspend fun bindAsyncFetch(qjs: QuickJs, fetchBase: String) {
    qjs.define("__spine") {
      asyncFunction(
        "fetch",
        object : AsyncFunctionBinding<String> {
          override suspend fun invoke(args: Array<Any?>): String {
            val rawUrl =
              args[0]?.toString() ?: throw IllegalArgumentException("fetch requires a URL")
            val method = args[1]?.toString() ?: "GET"
            val headersJson = args[2]?.toString() ?: "{}"
            val body = args[3]?.toString()
            val url = resolveUrl(rawUrl, fetchBase)

            Timber.tag(TAG).d("  → fetch %s %s", method, redactModuleUrl(url))
            val (statusCode, responseBody) = fetchUrlSync(url, method, headersJson, body)
            Timber.tag(TAG).d("    HTTP %d (%d bytes)", statusCode, responseBody.length)

            val respObj =
              JSONObject().apply {
                put("status", statusCode)
                put("ok", statusCode in 200..299)
                put("body", responseBody)
              }
            return respObj.toString()
          }
        },
      )
      asyncFunction(
        "setTimeout",
        object : AsyncFunctionBinding<String> {
          override suspend fun invoke(args: Array<Any?>): String {
            val ms = args.getOrNull(1)?.toString()?.toLongOrNull() ?: 0L
            kotlinx.coroutines.delay(ms)
            return "0"
          }
        },
      )
      asyncFunction(
        "clearTimeout",
        object : AsyncFunctionBinding<String> {
          override suspend fun invoke(args: Array<Any?>): String = "ok"
        },
      )
    }

    qjs.evaluate<Unit>(
      """
      var fetch = async function(url, options) {
          var method = 'GET';
          var headers = '{}';
          var body = null;
          if (options) {
              method = options.method || 'GET';
              if (options.headers) {
                  if (typeof options.headers === 'string') {
                      headers = options.headers;
                  } else {
                      try { headers = JSON.stringify(options.headers); } catch(e) { headers = '{}'; }
                  }
              }
              if (options.body !== undefined && options.body !== null) {
                  body = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
              }
              if (options.signal && options.signal.aborted) {
                  throw new Error('Aborted');
              }
          }
          var raw = JSON.parse(await __spine.fetch(url, method, headers, body));
          var respBody = raw.body;
          return {
              ok: raw.ok,
              status: raw.status,
              statusText: raw.ok ? 'OK' : 'Error',
              json: function() { try { return JSON.parse(respBody); } catch(e) { throw new Error('Invalid JSON: ' + respBody.substring(0, 200)); } },
              text: function() { return respBody; },
              arrayBuffer: function() { throw new Error('Not implemented'); },
              clone: function() { return this; },
              headers: { get: function(k) { return null; } }
          };
      };

      var setTimeout = async function(fn, ms) {
          await __spine.setTimeout(null, ms || 0);
          if (typeof fn === 'function') fn();
          return 0;
      };
      var clearTimeout = function(id) {};
      """
        .trimIndent()
    )
  }

  private fun resolveUrl(url: String, base: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    if (base.isEmpty()) return url
    return if (url.startsWith("/")) {
      val scheme = base.substringBefore("://")
      val host = base.substringAfter("://").substringBefore("/")
      "$scheme://$host$url"
    } else {
      "$base/$url"
    }
  }

  private fun fetchUrlSync(
    url: String,
    method: String,
    headersJson: String,
    body: String?,
  ): Pair<Int, String> {
    val builder = Request.Builder().url(url)

    var hasUserAgent = false
    try {
      val headersObj = JSONObject(headersJson)
      for (key in headersObj.keys()) {
        val value = headersObj.optString(key, "")
        builder.header(key, value)
        if (key.equals("user-agent", ignoreCase = true)) hasUserAgent = true
      }
    } catch (_: Exception) {}

    if (!hasUserAgent) {
      builder.header(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
          "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
      )
    }

    when (method.uppercase(Locale.ROOT)) {
      "POST" ->
        builder.post((body ?: "").toRequestBody("application/json; charset=utf-8".toMediaType()))
      "PUT" ->
        builder.put((body ?: "").toRequestBody("application/json; charset=utf-8".toMediaType()))
      "DELETE" -> builder.delete()
      "HEAD" -> builder.head()
      else -> builder.get()
    }

    syncHttpClient.newCall(builder.build()).execute().use { response ->
      val responseBody = response.body?.string() ?: ""
      return response.code to responseBody
    }
  }

  private const val POLYFILLS =
    """
        if (typeof AbortController === 'undefined') {
            var AbortController = function() { this.signal = { aborted: false }; };
            AbortController.prototype.abort = function() { this.signal.aborted = true; };
        }

        if (typeof Object.assign !== 'function') {
            Object.assign = function(target) {
                if (target == null) throw new TypeError('Cannot convert undefined or null to object');
                var to = Object(target);
                for (var i = 1; i < arguments.length; i++) {
                    var source = arguments[i];
                    if (source != null) {
                        for (var key in source) {
                            if (Object.prototype.hasOwnProperty.call(source, key)) {
                                to[key] = source[key];
                            }
                        }
                    }
                }
                return to;
            };
        }

        if (typeof Promise.any !== 'function') {
            Promise.any = function(promises) {
                return new Promise(function(resolve, reject) {
                    var errors = [];
                    var remaining = promises.length;
                    if (remaining === 0) { reject(new AggregateError([], 'All promises were rejected')); return; }
                    promises.forEach(function(p, i) {
                        Promise.resolve(p).then(resolve, function(e) {
                            errors[i] = e;
                            remaining--;
                            if (remaining === 0) reject(new AggregateError(errors, 'All promises were rejected'));
                        });
                    });
                });
            };
        }

        if (typeof Promise.allSettled !== 'function') {
            Promise.allSettled = function(promises) {
                return Promise.all(promises.map(function(p) {
                    return Promise.resolve(p).then(
                        function(value) { return { status: 'fulfilled', value: value }; },
                        function(reason) { return { status: 'rejected', reason: reason }; }
                    );
                }));
            };
        }

        if (typeof AggregateError === 'undefined') {
            var AggregateError = function(errors, message) {
                this.errors = errors;
                this.message = message || '';
                this.name = 'AggregateError';
            };
            AggregateError.prototype = Object.create(Error.prototype);
        }

        if (typeof atob === 'undefined') {
            var atob = function(input) {
                var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                var str = String(input).replace(/=+${'$'}/, '');
                var output = '';
                for (var bc = 0, bs, buffer, idx = 0; buffer = str.charAt(idx++); ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer, bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0) {
                    buffer = chars.indexOf(buffer);
                }
                return output;
            };
        }

        if (typeof setTimeout === 'undefined') {
            var __spine_timers = {};
            var __spine_timer_id = 0;
            var setTimeout = function(fn, ms) {
                var id = ++__spine_timer_id;
                __spine_timers[id] = { fn: fn, ms: ms || 0 };
                return id;
            };
            var clearTimeout = function(id) {
                if (__spine_timers[id]) delete __spine_timers[id];
            };
        }
        if (typeof clearTimeout === 'undefined') {
            var clearTimeout = function(id) {
                if (typeof __spine_timers !== 'undefined' && __spine_timers[id]) delete __spine_timers[id];
            };
        }

        if (typeof URL === 'undefined') {
            var URL = function(url, base) {
                this.href = url;
                try {
                    var a = url.replace(/^[^:]+:/, 'http:');
                    var match = a.match(/^\/\/([^/]+)(\/.*)?$/);
                    if (match) {
                        this.hostname = match[1];
                        this.pathname = match[2] || '/';
                    } else {
                        var m2 = a.match(/^https?:\/\/([^/]+)(\/.*)?$/);
                        if (m2) { this.hostname = m2[1]; this.pathname = m2[2] || '/'; }
                    }
                } catch(e) {}
            };
        }
    """
}
