package echo.music.iad1tya.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.extraction.strategy.PoTokenProviderKind
import com.metrolist.innertubex.models.YouTubeLocale
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.utils.potoken.PoTokenGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * YouTube stream extraction through InnerTubeX's live-benchmarked client catalog and cipher tiers.
 */
object InnerTubeXResolver {

  private const val TAG = "BitChord"

  /** A stream InnerTubeX found, and the identity its media fetch has to wear. */
  class Extracted(
    val videoId: String,
    val url: String,
    val kbps: Int,
    val mimeType: String,
    val loudnessDb: Double?,
    val clientName: String,
    /** InnerTubeX's catalog id for the exact client variant; what exclusions key on. */
    val profileId: String,
    val headers: Map<String, String>,
  )

  private var prefs: SharedPreferences? = null
  private var poTokens: PoTokenGenerator? = null
  private var playerDir: File? = null
  private var warmup: Job? = null

  fun init(context: Context) {
    val app = context.applicationContext
    prefs = app.getSharedPreferences("innertubex_player_config", Context.MODE_PRIVATE)
    poTokens = PoTokenGenerator()
    playerDir = File(app.filesDir, "innertubex_players").apply { mkdirs() }
    scope.launch { warm(WARM_DELAY_MS) }
  }

  /**
   * Pays the cold costs before a track needs them: the player config, the EJS solver (8.7s in
   * QuickJS when zemer-cipher lacks the player's hash), and the BotGuard WebView behind WEB_REMIX's
   * PoToken. Measured cold, those put an age-restricted track 15-20s from first audio when paid on
   * the play path.
   */
  private fun warm(delayMs: Long) {
    warmup?.cancel()
    warmup =
      scope.launch {
        delay(delayMs)
        val start = SystemClock.elapsedRealtime()
        runCatching {
            innerTube.cookie = com.music.innertube.YouTube.cookie
            innerTube.visitorData = com.music.innertube.YouTube.visitorData
            innerTube.locale = YouTubeLocale(gl = "US", hl = "en")
            extractor.prewarm()
          }
          .onFailure { if (it is CancellationException) throw it }
          .onFailure { Timber.tag(TAG).w("InnerTubeX warm-up failed: ${it.message}") }
          .onSuccess {
            Timber.tag(TAG)
              .d(TAG, "InnerTubeX warmed in ${SystemClock.elapsedRealtime() - start}ms")
          }
      }
  }

  /**
   * EJS preprocessed players, kept across processes. Solving a player from scratch is the 8.7s
   * step; a preprocessed one is reused per player version, so only the first launch after YouTube
   * rotates its player pays it.
   */
  private fun readPlayer(key: String): String? =
    playerDir?.let { File(it, key) }?.takeIf { it.isFile }?.readText()

  private fun writePlayer(key: String, value: String?) {
    val dir = playerDir ?: return
    val file = File(dir, key)
    if (value == null) {
      file.delete()
      return
    }
    val tmp = File(dir, "$key.tmp")
    tmp.writeText(value)
    tmp.renameTo(file)
    // Players rotate every few days; only the newest are worth their megabytes.
    dir
      .listFiles()
      ?.filter { !it.name.endsWith(".tmp") }
      ?.sortedByDescending { it.lastModified() }
      ?.drop(KEPT_PLAYERS)
      ?.forEach { it.delete() }
  }

  /**
   * BotGuard PoTokens minted in a hidden WebView; what WEB_REMIX needs for age-restricted tracks.
   */
  private val tokenProvider =
    object : TokenProvider {
      override val capabilities =
        TokenProviderCapabilities(
          providers = setOf(PoTokenProviderKind.WEB_BOTGUARD),
          usesWebView = true,
        )

      override suspend fun getPoToken(
        videoId: String,
        visitorData: String,
        cookie: String?
      ): PoTokenResult? =
        poTokens?.getWebClientPoToken(videoId, visitorData)?.let { token ->
          PoTokenResult(
            playerRequestToken = token.playerRequestPoToken,
            streamingDataToken = token.streamingDataPoToken,
            visitorData = visitorData,
          )
        }

      override suspend fun close() {}
    }

  private val http =
    HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
          }
        )
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 20_000
      }
      expectSuccess = false
    }

  private val logger = InnerTubeLogger { event ->
    if (event.level == InnerTubeLogLevel.DEBUG && !BuildConfig.DEBUG) return@InnerTubeLogger
    val details =
      if (event.details.isEmpty()) ""
      else
        event.details.entries.joinToString(prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
    val line = "ITX ${event.tag}: ${event.message}$details"
    if (event.level == InnerTubeLogLevel.INFO || event.level == InnerTubeLogLevel.DEBUG)
      Timber.tag(TAG).d(line)
    else Timber.tag(TAG).w(line)
  }

  private val repository =
    object : PlayerConfigRepository {
      override val enabled: Boolean
        get() = prefs != null

      override val sourceUrl: String = PLAYER_CONFIG_URL
      override val defaultSourceUrl: String = PLAYER_CONFIG_URL
      override var cachedJson: String
        get() = prefs?.getString("json", "").orEmpty()
        set(value) {
          prefs?.edit()?.putString("json", value)?.apply()
        }

      override var cachedAtMs: Long
        get() = prefs?.getLong("cached_at_ms", 0L) ?: 0L
        set(value) {
          prefs?.edit()?.putLong("cached_at_ms", value)?.apply()
        }

      override var cachedSourceUrl: String
        get() = prefs?.getString("source_url", "").orEmpty()
        set(value) {
          prefs?.edit()?.putString("source_url", value)?.apply()
        }

      override var cachedEtag: String
        get() = prefs?.getString("etag", "").orEmpty()
        set(value) {
          prefs?.edit()?.putString("etag", value)?.apply()
        }
    }

  private val innerTube = InnerTube(http, logger = logger)
  private val remoteStore = RemotePlayerConfigStore(http, repository, logger)
  private val cipherService = YouTubeCipherService(http, remoteStore, logger)
  private val extractor =
    InnerTubeExtractor(
      configParser = YtConfigParserImpl(http, innerTube, remoteStore, logger),
      cipherService = cipherService,
      innerTube = innerTube,
      tokenProvider = tokenProvider,
      logger = logger,
    )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** What each minted URL was, so its fetch can be dressed and its refusal attributed. */
  private val minted = ConcurrentHashMap<String, Extracted>()

  /** Clients refused a track mid-playback, by videoId, and until when. */
  private val excluded = ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>()

  /**
   * InnerTubeX picks one format per ask and has no bitrate ceiling of its own: [maxKbps] only
   * chooses between its smallest and best rungs, and [requireM4a] asks for its best AAC-in-MP4.
   *
   * @return a stream for [videoId], or null when every client InnerTubeX would try is refused or
   *   [skipClients] has ruled it out.
   * @throws com.metrolist.innertubex.extraction.StreamResolveException with a reason when
   *   InnerTubeX exhausted its catalog.
   */
  suspend fun extract(
    videoId: String,
    maxKbps: Int,
    skipClients: Set<String> = emptySet(),
    requireM4a: Boolean = false,
  ): Extracted? {
    innerTube.cookie = com.music.innertube.YouTube.cookie
    innerTube.visitorData = com.music.innertube.YouTube.visitorData
    innerTube.locale = YouTubeLocale(gl = "US", hl = "en")
    val stream =
      extractor.extract(
        videoId = videoId,
        hints =
          ContentHints()
            .withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
        excludedClients = excludedFor(videoId) + skipClients,
        audioQuality =
          when {
            requireM4a -> AudioQuality.MP4
            maxKbps <= LOW_KBPS -> AudioQuality.LOW
            else -> AudioQuality.AUTO
          },
        clientPlaybackNonce = generateClientPlaybackNonce(),
      ) ?: return null
    check(stream.sabrBootstrap == null) { "SABR is not supported by this playback engine" }
    val mime = stream.mimeType.orEmpty()
    val extracted =
      Extracted(
        videoId = videoId,
        url = stream.audioUrl,
        kbps = (stream.bitrate ?: 0) / 1000,
        mimeType =
          if (stream.codecs.isNullOrBlank()) mime else "$mime; codecs=\"${stream.codecs}\"",
        loudnessDb = stream.loudnessDb,
        clientName = stream.clientName,
        profileId = stream.profileId,
        headers = stream.headers,
      )
    if (minted.size >= MAX_REMEMBERED) minted.clear()
    minted[extracted.url] = extracted
    return extracted
  }

  /** Headers the media fetch for [url] must carry, when InnerTubeX minted it. */
  fun headersFor(url: String): Map<String, String>? = minted[url]?.headers

  /**
   * Records that googlevideo refused [url] mid-playback.
   *
   * @return the videoId it was minted for, or null when InnerTubeX did not mint it.
   */
  fun onRefused(url: String): String? {
    val refused = minted.remove(url) ?: return null
    exclude(refused.videoId, refused.profileId)
    // A refused ciphered URL is the signal the remote cipher config may be stale; it has its own
    // cooldown.
    scope.launch { runCatching { cipherService.refreshAfterStreamRejection() } }
    return refused.videoId
  }

  fun exclude(videoId: String, profileId: String) {
    Timber.tag(TAG).w("InnerTubeX: $profileId refused $videoId; skipping it for this track")
    excluded.getOrPut(videoId) { ConcurrentHashMap() }[profileId] =
      SystemClock.elapsedRealtime() + EXCLUDE_MS
  }

  fun onSessionChanged() {
    excluded.clear()
    minted.clear()
    // A new cookie or visitor means a new player config and PoToken binding.
    if (prefs != null) warm(0)
  }

  private fun excludedFor(videoId: String): Set<String> {
    val entries = excluded[videoId] ?: return emptySet()
    val now = SystemClock.elapsedRealtime()
    entries.entries.removeAll { it.value <= now }
    return entries.keys.toSet()
  }

  private const val PLAYER_CONFIG_URL =
    "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
  private const val LOW_KBPS = 64
  private const val EXCLUDE_MS = 10 * 60 * 1000L
  private const val MAX_REMEMBERED = 64
  /** Off the cold-start path; the first tap on a track is rarely sooner. */
  private const val WARM_DELAY_MS = 2_000L
  private const val KEPT_PLAYERS = 3
}
