package echo.music.iad1tya.utils.cipher

import android.content.Context
import android.util.Base64
import com.music.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Owns the player-config table at runtime: bundled asset as the offline default, overlaid
 * by the same JSON fetched from the remote source so rotated players are fixed without
 * an APK update. Parsing/validation is delegated to [PlayerConfigParser]; only validated
 * payloads ever replace the in-memory map or touch the disk cache.
 */
object PlayerConfigStore {
    private const val TAG = "echomusic_CipherConfig"
    private const val ASSET_NAME = "player_configs.json"

    private val REMOTE_URL by lazy {
        val encoded = "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL0VjaG9NdXNpY0FwcC9FY2hvLU11c2ljL21haW4vYXBwL3NyYy9tYWluL2Fzc2V0cy9wbGF5ZXJfY29uZmlncy5qc29u"
        String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    private const val REFRESH_TTL_MS = 6 * 60 * 60 * 1000L
    private const val FORCE_REFRESH_COOLDOWN_MS = 5 * 60 * 1000L

    private const val CACHE_FILE = "configs_remote.json"
    private const val META_FILE = "configs_remote.meta"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var bundledConfigs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    @Volatile
    private var mergedConfigs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    @Volatile
    var configEpoch: Int = 0
        private set

    @Volatile
    private var lastForcedAttemptMs = 0L

    @Volatile
    private var lastRejectionAttemptMs = 0L

    internal fun forcedCooldownActive(now: Long) = withinWindow(now, lastForcedAttemptMs, FORCE_REFRESH_COOLDOWN_MS)
    internal fun rejectionCooldownActive(now: Long) = withinWindow(now, lastRejectionAttemptMs, FORCE_REFRESH_COOLDOWN_MS)

    private fun withinWindow(now: Long, previousMs: Long, ttl: Long): Boolean {
        val elapsed = now - previousMs
        return elapsed in 0..<ttl
    }

    internal fun armForcedCooldownForTest(ms: Long) { lastForcedAttemptMs = ms }
    internal fun armRejectionCooldownForTest(ms: Long) { lastRejectionAttemptMs = ms }

    @Volatile
    private var lastAttemptReachedServer = false

    private val refreshMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { YouTube.proxy?.let { proxy(it) } }
            .build()
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext

        bundledConfigs = when (val result = parseSource("bundled asset") { loadBundledJson(context) }) {
            null -> emptyMap()
            else -> result
        }
        if (bundledConfigs.isEmpty()) {
            Timber.tag(TAG).e("Bundled $ASSET_NAME missing or invalid — config table starts empty")
        } else {
            Timber.tag(TAG).d("Loaded bundled configs (${bundledConfigs.size} hashes)")
        }

        applyCachedOverlay()
    }

    internal fun applyCachedOverlay() {
        val cached = parseSource("cached remote copy") { cacheFile()?.takeIf { it.exists() }?.readText() }
        mergedConfigs = if (cached != null) {
            Timber.tag(TAG).d("Overlaying cached remote configs (${cached.size} hashes)")
            PlayerConfigParser.merge(bundledConfigs, cached)
        } else {
            cacheFile()?.delete()
            metaFile()?.delete()
            bundledConfigs
        }
    }

    fun scheduleStartupRefresh() {
        scope.launch {
            try {
                refreshIfStale()
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Startup config refresh failed: ${e.message}")
            }
        }
    }

    fun get(hash: String): FunctionNameExtractor.HardcodedPlayerConfig? {
        val configs = mergedConfigs
        if (configs.isEmpty()) {
            Timber.tag(TAG).w("Config table is empty (initialize not called or bundled asset broken)")
        }
        return configs[hash]
    }

    fun knownHashes(): Set<String> = mergedConfigs.keys

    internal fun setTableForTest(configs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>) {
        mergedConfigs = configs
    }

    suspend fun forceRefresh(missingHash: String): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            if (mergedConfigs.containsKey(missingHash)) {
                Timber.tag(TAG).d("forceRefresh: $missingHash arrived via concurrent refresh")
                return@withLock true
            }

            val now = System.currentTimeMillis()
            if (forcedCooldownActive(now)) {
                Timber.tag(TAG).d("forceRefresh skipped (cooldown)")
                return@withLock false
            }
            lastForcedAttemptMs = now
            fetchAndApplyResetting { lastForcedAttemptMs = 0L }
            mergedConfigs.containsKey(missingHash)
        }
    }

    suspend fun refreshAfterStreamRejection(): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val now = System.currentTimeMillis()
            if (rejectionCooldownActive(now)) {
                Timber.tag(TAG).d("refreshAfterStreamRejection skipped (cooldown)")
                return@withLock false
            }
            lastRejectionAttemptMs = now
            fetchAndApplyResetting { lastRejectionAttemptMs = 0L }
        }
    }

    suspend fun manualRefresh(): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val now = System.currentTimeMillis()
            lastForcedAttemptMs = now
            fetchAndApplyResetting { lastForcedAttemptMs = 0L }
            lastAttemptReachedServer
        }
    }

    private fun fetchAndApplyResetting(resetCooldown: () -> Unit): Boolean {
        val changed = fetchAndApply()
        if (!lastAttemptReachedServer) resetCooldown()
        return changed
    }

    private suspend fun refreshIfStale() {
        val lastFetchMs = readMeta()?.second ?: 0L
        if (withinWindow(System.currentTimeMillis(), lastFetchMs, REFRESH_TTL_MS)) {
            Timber.tag(TAG).d("Remote configs fresh (fetched ${System.currentTimeMillis() - lastFetchMs} ms ago)")
            return
        }
        withContext(Dispatchers.IO) {
            refreshMutex.withLock { fetchAndApply() }
        }
    }

    private fun fetchAndApply(): Boolean {
        lastAttemptReachedServer = false
        try {
            val etag = readMeta()?.first
            val request = Request.Builder()
                .url(REMOTE_URL)
                .header("User-Agent", "Mozilla/5.0")
                .apply { if (!etag.isNullOrEmpty()) header("If-None-Match", etag) }
                .build()

            httpClient.newCall(request).execute().use { response ->
                lastAttemptReachedServer = true
                if (response.code == 304) {
                    Timber.tag(TAG).d("Remote configs unchanged (304)")
                    writeMeta(etag.orEmpty(), System.currentTimeMillis())
                    return false
                }
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("Remote config fetch HTTP ${response.code} — keeping previous configs")
                    return false
                }

                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    Timber.tag(TAG).w("Remote config fetch returned empty body — keeping previous configs")
                    return false
                }

                val remote = when (val result = PlayerConfigParser.parse(body)) {
                    is PlayerConfigParser.ParseResult.Failure -> {
                        Timber.tag(TAG).w("Remote configs rejected: ${result.reason} — keeping previous configs")
                        return false
                    }
                    is PlayerConfigParser.ParseResult.Success -> {
                        if (result.skippedEntries.isNotEmpty()) {
                            Timber.tag(TAG).w("Remote configs: skipped invalid entries ${result.skippedEntries}")
                        }
                        result.configs
                    }
                }

                return applyRemote(remote, body, response.header("ETag").orEmpty())
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Remote config fetch failed: ${e.message} — keeping previous configs")
            return false
        }
    }

    internal fun applyRemote(
        remote: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>,
        body: String,
        etag: String,
    ): Boolean {
        val merged = PlayerConfigParser.merge(bundledConfigs, remote)
        val changed = merged != mergedConfigs
        mergedConfigs = merged
        if (changed) configEpoch++
        Timber.tag(TAG).d("Remote configs applied (${remote.size} hashes, merged=${merged.size}, changed=$changed, epoch=$configEpoch)")

        try {
            cacheFile()?.let { writeAtomic(it, body) }
            writeMeta(etag, System.currentTimeMillis())
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not persist remote configs (kept in memory): ${e.message}")
        }
        return changed
    }

    private fun parseSource(
        label: String,
        read: () -> String?,
    ): Map<String, FunctionNameExtractor.HardcodedPlayerConfig>? {
        val text = try {
            read() ?: return null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not read $label: ${e.message}")
            return null
        }
        return when (val result = PlayerConfigParser.parse(text)) {
            is PlayerConfigParser.ParseResult.Failure -> {
                Timber.tag(TAG).w("Rejected $label: ${result.reason}")
                null
            }
            is PlayerConfigParser.ParseResult.Success -> {
                if (result.skippedEntries.isNotEmpty()) {
                    Timber.tag(TAG).w("$label: skipped invalid entries ${result.skippedEntries}")
                }
                result.configs
            }
        }
    }

    private fun loadBundledJson(context: Context): String? =
        try {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }

    internal var cacheDirForTest: File? = null

    private fun cacheDir(): File? {
        cacheDirForTest?.let { return it.apply { if (!exists()) mkdirs() } }
        val context = appContext ?: return null
        return File(context.filesDir, "cipher_cache").apply { if (!exists()) mkdirs() }
    }

    private fun cacheFile(): File? = cacheDir()?.let { File(it, CACHE_FILE) }

    private fun metaFile(): File? = cacheDir()?.let { File(it, META_FILE) }

    fun getLastFetchTimeMs(): Long {
        return readMeta()?.second ?: 0L
    }

    private fun readMeta(): Pair<String, Long>? {
        return try {
            val file = metaFile()?.takeIf { it.exists() } ?: return null
            val lines = file.readText().split("\n")
            if (lines.size < 2) return null
            val lastFetchMs = lines[1].toLongOrNull() ?: return null
            lines[0] to lastFetchMs
        } catch (e: Exception) {
            null
        }
    }

    private fun writeMeta(etag: String, lastFetchMs: Long) {
        try {
            metaFile()?.let { writeAtomic(it, "$etag\n$lastFetchMs") }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not write config meta: ${e.message}")
        }
    }

    internal fun writeAtomic(file: File, content: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(file)) {
            file.writeText(content)
            tmp.delete()
        }
    }
}
