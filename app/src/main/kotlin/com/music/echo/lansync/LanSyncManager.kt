package echo.music.iad1tya.lansync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_PODCAST_EPISODE
import echo.music.iad1tya.constants.AccountChannelHandleKey
import echo.music.iad1tya.constants.AccountEmailKey
import echo.music.iad1tya.constants.AccountNameKey
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.SpotifyAccessTokenKey
import echo.music.iad1tya.constants.SpotifySpDcKey
import echo.music.iad1tya.constants.SpotifySpKeyKey
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.extensions.metadata
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.PlayerConnection
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.spotify.Spotify
import echo.music.iad1tya.spotify.SpotifyAuth
import echo.music.iad1tya.utils.PodcastProgress
import echo.music.iad1tya.utils.dataStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class LanPeer(
  val id: String,
  val name: String,
  val platform: String,
  val host: String,
  val port: Int,
  val lastSeenMs: Long = System.currentTimeMillis(),
) {
  val baseUrl: String
    get() = "http://$host:$port"
}

data class LanTrackDto(
  val videoId: String,
  val title: String,
  val artist: String,
  val album: String? = null,
  val durationSec: Int = 0,
  val thumbnail: String? = null,
  val isPodcast: Boolean = false,
)

data class LanEpisodeDto(
  val videoId: String,
  val positionMs: Long,
  val durationMs: Long = 0L,
  val finished: Boolean = false,
  val updatedAt: Long = 0L,
  val title: String? = null,
  val show: String? = null,
  val thumbnail: String? = null,
)

data class LanPeerState(
  val deviceId: String,
  val deviceName: String,
  val platform: String,
  val playing: Boolean,
  val volume: Float,
  val positionMs: Long,
  val durationMs: Long,
  val current: LanTrackDto?,
  val queue: List<LanTrackDto>,
  val index: Int,
  val source: String? = null,
  val shuffle: Boolean = false,
  val repeat: String = "off",
  val episodes: List<LanEpisodeDto> = emptyList(),
)

/**
 * Zero-server-cost LAN sync manager using Android NsdManager (_echomusic._tcp.) and an embedded
 * lightweight HTTP/1.1 JSON server for playback handoff, Echo Connect remote control, and podcast
 * progress synchronization with Echo Music Desktop.
 */
class LanSyncManager private constructor(private val appContext: Context) {

  companion object {
    private const val TAG = "LanSyncManager"
    private const val SERVICE_TYPE = "_echomusic._tcp."
    private const val PREFS_NAME = "lan_sync_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_PAIRED_PEER_ID = "paired_peer_id"
    private const val KEY_PAIRED_PEER_NAME = "paired_peer_name"
    private const val KEY_PAIR_TOKEN = "pair_token"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    @Volatile private var instance: LanSyncManager? = null

    fun getInstance(context: Context): LanSyncManager =
      instance
        ?: synchronized(this) {
          instance ?: LanSyncManager(context.applicationContext).also { instance = it }
        }
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(900, TimeUnit.MILLISECONDS)
      .readTimeout(4, TimeUnit.SECONDS)
      .writeTimeout(4, TimeUnit.SECONDS)
      .build()

  val deviceId: String by lazy {
    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.getString(KEY_DEVICE_ID, null)
      ?: UUID.randomUUID().toString().replace("-", "").take(12).also { id ->
        prefs.edit { putString(KEY_DEVICE_ID, id) }
      }
  }

  val deviceName: String
    get() = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android Device"

  @Volatile private var playerConnection: PlayerConnection? = null
  @Volatile private var database: MusicDatabase? = null

  data class PairingSession(
    val peer: LanPeer,
    val code: String,
    val expiresAtMs: Long,
    val secondsLeft: Int,
  )

  data class SyncDetails(
    val lastSyncAtMs: Long = 0L,
    val accountStatus: String = "Not synced yet",
    val localPlaylistsStatus: String = "Not synced yet",
    val spotifyPlaylistsStatus: String = "Not synced yet",
    val episodesStatus: String = "Not synced yet",
  )

  private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

  private val _pairedPeerId = MutableStateFlow<String?>(prefs.getString(KEY_PAIRED_PEER_ID, null))
  val pairedPeerId: StateFlow<String?> = _pairedPeerId.asStateFlow()

  private val _pairedPeerName =
    MutableStateFlow<String?>(prefs.getString(KEY_PAIRED_PEER_NAME, null))
  val pairedPeerName: StateFlow<String?> = _pairedPeerName.asStateFlow()

  private val _pairingSession = MutableStateFlow<PairingSession?>(null)
  val pairingSession: StateFlow<PairingSession?> = _pairingSession.asStateFlow()

  data class IncomingPairRequest(
    val peer: LanPeer,
    val expiresAtMs: Long,
    val busy: Boolean = false,
    val error: String? = null,
  )

  private val _incomingPairRequest = MutableStateFlow<IncomingPairRequest?>(null)
  val incomingPairRequest: StateFlow<IncomingPairRequest?> = _incomingPairRequest.asStateFlow()

  private val _syncBusy = MutableStateFlow(false)
  val syncBusy: StateFlow<Boolean> = _syncBusy.asStateFlow()

  private val _syncDetails = MutableStateFlow(SyncDetails())
  val syncDetails: StateFlow<SyncDetails> = _syncDetails.asStateFlow()

  private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
  val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

  private var pairingJob: Job? = null

  private val _peers = MutableStateFlow<List<LanPeer>>(emptyList())
  val peers: StateFlow<List<LanPeer>> = _peers.asStateFlow()

  private val _remoteStates = MutableStateFlow<Map<String, LanPeerState>>(emptyMap())
  val remoteStates: StateFlow<Map<String, LanPeerState>> = _remoteStates.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _activeRemotePeer = MutableStateFlow<LanPeer?>(null)
  val activeRemotePeer: StateFlow<LanPeer?> = _activeRemotePeer.asStateFlow()

  val isRemoteControlling: StateFlow<Boolean> =
    _activeRemotePeer.map { it != null }.stateIn(scope, SharingStarted.Eagerly, false)

  private val _remotePlaying = MutableStateFlow(false)
  val remotePlaying: StateFlow<Boolean> = _remotePlaying.asStateFlow()

  private val _remotePositionMs = MutableStateFlow(0L)
  val remotePositionMs: StateFlow<Long> = _remotePositionMs.asStateFlow()

  private val _remoteDurationMs = MutableStateFlow(0L)
  val remoteDurationMs: StateFlow<Long> = _remoteDurationMs.asStateFlow()

  private val _remoteMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
  val remoteMediaMetadata: StateFlow<MediaMetadata?> = _remoteMediaMetadata.asStateFlow()

  private val _remoteShuffle = MutableStateFlow(false)
  val remoteShuffle: StateFlow<Boolean> = _remoteShuffle.asStateFlow()

  private val _remoteRepeat = MutableStateFlow("off")
  val remoteRepeat: StateFlow<String> = _remoteRepeat.asStateFlow()

  private var observerJob: Job? = null
  private var remotePollJob: Job? = null
  @Volatile private var lastObservedTrackId: String? = null
  @Volatile private var suppressAutoMirrorUntilMs: Long = 0L
  @Volatile private var lastRemoteSeekMs: Long = 0L

  private val peerMap = ConcurrentHashMap<String, LanPeer>()
  private var nsdManager: NsdManager? = null
  private var multicastLock: WifiManager.MulticastLock? = null
  private var serverSocket: ServerSocket? = null
  private var serverJob: Job? = null
  private var syncLoopJob: Job? = null
  private var registrationListener: NsdManager.RegistrationListener? = null
  private var discoveryListener: NsdManager.DiscoveryListener? = null
  @Volatile private var running = false

  fun attachPlayerConnection(connection: PlayerConnection?) {
    if (connection != null) {
      database = connection.database
    }
    if (playerConnection === connection && observerJob?.isActive == true) return
    playerConnection = connection
    observerJob?.cancel()
    if (connection == null) return
    lastObservedTrackId = connection.mediaMetadata.value?.id
    observerJob = scope.launch {
      connection.mediaMetadata.collect { meta ->
        val trackId = meta?.id
        val prev = lastObservedTrackId
        lastObservedTrackId = trackId
        if (trackId.isNullOrBlank() || trackId == prev) return@collect
        val target = _activeRemotePeer.value ?: return@collect
        if (System.currentTimeMillis() < suppressAutoMirrorUntilMs) return@collect
        // Wait briefly for queueWindows to finish updating for the newly selected song.
        delay(180)
        if (
          _activeRemotePeer.value?.id == target.id &&
            System.currentTimeMillis() >= suppressAutoMirrorUntilMs
        ) {
          handoffToPeer(target)
        }
      }
    }
  }

  fun disconnectRemotePeer() {
    stopRemotePolling()
    _activeRemotePeer.value = null
    _remoteMediaMetadata.value = null
    _remotePlaying.value = false
    _remotePositionMs.value = 0L
    _remoteDurationMs.value = 0L
    _statusMessage.value = "Switched back to this phone"
  }

  fun attachAsController(peer: LanPeer) {
    _activeRemotePeer.value = peer
    suppressAutoMirrorUntilMs = System.currentTimeMillis() + 1800L
    scope.launch(Dispatchers.Main) { playerConnection?.player?.pause() }
    startRemotePolling(peer)
    _statusMessage.value = "Controlling ${peer.name}"
  }

  private fun attachAsControllerById(peerId: String) {
    val peer = peerMap[peerId] ?: _peers.value.firstOrNull { it.id == peerId } ?: return
    attachAsController(peer)
  }

  private fun startRemotePolling(peer: LanPeer) {
    remotePollJob?.cancel()
    remotePollJob = scope.launch {
      var tickCount = 0
      while (isActive && _activeRemotePeer.value?.id == peer.id) {
        if (tickCount % 4 == 0) {
          refreshPeerState(peer)?.let { applyRemoteState(it) }
        } else if (_remotePlaying.value && System.currentTimeMillis() - lastRemoteSeekMs > 1200L) {
          val dur = _remoteDurationMs.value
          val next = _remotePositionMs.value + 250L
          _remotePositionMs.value = if (dur > 0) next.coerceAtMost(dur) else next
        }
        tickCount++
        delay(250)
      }
    }
  }

  private fun stopRemotePolling() {
    remotePollJob?.cancel()
    remotePollJob = null
  }

  private fun applyRemoteState(st: LanPeerState) {
    if (_activeRemotePeer.value?.id != st.deviceId) return
    _remotePlaying.value = st.playing
    _remoteDurationMs.value =
      if (st.durationMs > 0) st.durationMs else ((st.current?.durationSec ?: 0) * 1000L)
    if (System.currentTimeMillis() - lastRemoteSeekMs > 1200L) {
      _remotePositionMs.value = st.positionMs.coerceAtLeast(0L)
    }
    _remoteShuffle.value = st.shuffle
    _remoteRepeat.value = st.repeat
    _remoteMediaMetadata.value = st.current?.let(::dtoToMediaMetadata)
  }

  @Synchronized
  fun start() {
    if (running) return
    running = true
    try {
      val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      multicastLock =
        wifi?.createMulticastLock("EchoMusicLanSync")?.apply {
          setReferenceCounted(false)
          acquire()
        }
    } catch (e: Exception) {
      Log.w(TAG, "MulticastLock acquire failed", e)
    }

    val ss =
      try {
        ServerSocket(45870)
      } catch (_: Exception) {
        ServerSocket(0)
      }
    serverSocket = ss
    val localPort = ss.localPort
    serverJob = scope.launch {
      while (isActive && running) {
        try {
          val client = ss.accept()
          launch { handleHttpClient(client) }
        } catch (_: Exception) {
          if (!running) break
        }
      }
    }

    registerNsdService(localPort)
    startDiscovery()

    syncLoopJob = scope.launch {
      while (isActive && running) {
        delay(300_000)
        if (_pairedPeerId.value != null) {
          forceSyncNow()
        } else {
          syncEpisodesWithAllPeers()
        }
      }
    }
    Log.d(TAG, "Started LAN Sync on port $localPort as $deviceName ($deviceId)")
  }

  @Synchronized
  fun stop() {
    if (!running) return
    running = false
    syncLoopJob?.cancel()
    syncLoopJob = null
    serverJob?.cancel()
    serverJob = null
    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    serverSocket = null

    val nsd = nsdManager
    registrationListener?.let {
      try {
        nsd?.unregisterService(it)
      } catch (_: Exception) {}
    }
    discoveryListener?.let {
      try {
        nsd?.stopServiceDiscovery(it)
      } catch (_: Exception) {}
    }
    registrationListener = null
    discoveryListener = null
    try {
      multicastLock?.takeIf { it.isHeld }?.release()
    } catch (_: Exception) {}
    multicastLock = null
    peerMap.clear()
    _peers.value = emptyList()
  }

  private fun registerNsdService(port: Int) {
    val nsd = (appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager) ?: return
    nsdManager = nsd
    val info =
      NsdServiceInfo().apply {
        serviceName = "EchoMusic-${deviceId.take(8)}"
        serviceType = SERVICE_TYPE
        setPort(port)
        setAttribute("id", deviceId)
        setAttribute("name", deviceName)
        setAttribute("platform", "android")
        localIPv4Address()?.let { setAttribute("ip", it) }
        setAttribute("version", "1")
      }
    val listener =
      object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
          Log.d(TAG, "Registered NSD service: ${serviceInfo.serviceName}")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
          Log.w(TAG, "NSD registration failed: $errorCode")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
      }
    registrationListener = listener
    try {
      nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    } catch (e: Exception) {
      Log.w(TAG, "registerService error", e)
    }
  }

  private fun startDiscovery() {
    val nsd = (appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager) ?: return
    nsdManager = nsd
    val listener =
      object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
          if (!serviceInfo.serviceType.contains("_echomusic")) return
          if (serviceInfo.serviceName.contains(deviceId.take(8))) return
          resolvePeer(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
          val removed =
            peerMap.entries.removeIf { (_, p) ->
              serviceInfo.serviceName.contains(p.id.take(8)) || p.name == serviceInfo.serviceName
            }
          if (removed) publishPeers()
        }

        override fun onDiscoveryStopped(serviceType: String) {}

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
          Log.w(TAG, "Discovery start failed: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
      }
    discoveryListener = listener
    try {
      nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    } catch (e: Exception) {
      Log.w(TAG, "discoverServices error", e)
    }
  }

  @Suppress("DEPRECATION")
  private fun resolvePeer(serviceInfo: NsdServiceInfo) {
    val nsd = nsdManager ?: return
    try {
      nsd.resolveService(
        serviceInfo,
        object : NsdManager.ResolveListener {
          override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "Resolve failed for ${info.serviceName}: $errorCode")
          }

          override fun onServiceResolved(resolved: NsdServiceInfo) {
            val host = resolved.host?.hostAddress ?: return
            val port = resolved.port
            if (port <= 0) return
            val attrs = resolved.attributes
            val id =
              attrs["id"]?.toString(StandardCharsets.UTF_8)?.takeIf { it.isNotBlank() }
                ?: resolved.serviceName
            if (id == deviceId) return
            val name =
              attrs["name"]?.toString(StandardCharsets.UTF_8)?.takeIf { it.isNotBlank() }
                ?: resolved.serviceName
            val platform =
              attrs["platform"]?.toString(StandardCharsets.UTF_8)?.takeIf { it.isNotBlank() }
                ?: "desktop"
            peerMap[id] =
              LanPeer(id = id, name = name, platform = platform, host = host, port = port)
            publishPeers()
            scope.launch { refreshPeerState(peerMap[id] ?: return@launch) }
          }
        },
      )
    } catch (e: Exception) {
      Log.w(TAG, "resolveService error", e)
    }
  }

  private fun publishPeers() {
    _peers.value = peerMap.values.sortedBy { it.name.lowercase() }
  }

  // --- Client RPCs ---

  private fun localIPv4Address(): String? {
    return try {
      java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.firstNotNullOfOrNull { iface ->
        if (!iface.isUp || iface.isLoopback) return@firstNotNullOfOrNull null
        iface.inetAddresses
          ?.toList()
          ?.firstOrNull { addr ->
            addr is java.net.Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress
          }
          ?.hostAddress
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun executeGetWithFallback(peer: LanPeer, path: String): String? {
    val urls =
      if (peer.host == "127.0.0.1") listOf("${peer.baseUrl}$path")
      else listOf("${peer.baseUrl}$path", "http://127.0.0.1:${peer.port}$path")
    for (url in urls) {
      try {
        val req = Request.Builder().url(url).get().build()
        httpClient.newCall(req).execute().use { resp ->
          if (resp.isSuccessful) return resp.body.string()
        }
      } catch (_: Exception) {}
    }
    return null
  }

  private fun executePostWithFallback(peer: LanPeer, path: String, jsonBody: String): String? {
    val urls =
      if (peer.host == "127.0.0.1") listOf("${peer.baseUrl}$path")
      else listOf("${peer.baseUrl}$path", "http://127.0.0.1:${peer.port}$path")
    var lastError: Exception? = null
    for (url in urls) {
      try {
        val req = Request.Builder().url(url).post(jsonBody.toRequestBody(JSON_MEDIA)).build()
        httpClient.newCall(req).execute().use { resp ->
          if (resp.isSuccessful) return resp.body.string()
        }
      } catch (e: Exception) {
        lastError = e
      }
    }
    if (lastError != null) throw lastError
    return null
  }

  suspend fun refreshPeerState(peer: LanPeer): LanPeerState? =
    withContext(Dispatchers.IO) {
      try {
        val body = executeGetWithFallback(peer, "/v1/state") ?: return@withContext null
        val st = parsePeerState(JSONObject(body))
        _remoteStates.value = _remoteStates.value + (peer.id to st)
        st
      } catch (e: Exception) {
        Log.w(TAG, "refreshPeerState failed for ${peer.name}", e)
        null
      }
    }

  fun sendControl(
    peer: LanPeer,
    action: String,
    positionMs: Long? = null,
    volume: Float? = null,
    index: Int? = null,
    shuffle: Boolean? = null,
    repeat: String? = null,
    controllerPeerId: String? = null,
  ) {
    scope.launch {
      try {
        val json =
          JSONObject().apply {
            put("action", action)
            if (positionMs != null) put("positionMs", positionMs)
            if (volume != null) put("volume", volume.toDouble())
            if (index != null) put("index", index)
            if (shuffle != null) put("shuffle", shuffle)
            if (repeat != null) put("repeat", repeat)
            if (!controllerPeerId.isNullOrBlank()) put("controllerPeerId", controllerPeerId)
          }
        val body = executePostWithFallback(peer, "/v1/control", json.toString())
        if (body != null) {
          val st = parsePeerState(JSONObject(body))
          _remoteStates.value = _remoteStates.value + (peer.id to st)
          applyRemoteState(st)
          _statusMessage.value = null
        }
      } catch (e: Exception) {
        Log.w(TAG, "sendControl failed", e)
        _statusMessage.value = "Control failed: ${e.message}"
      }
    }
  }

  fun togglePlayRemote() {
    val peer = _activeRemotePeer.value ?: return
    _remotePlaying.value = !_remotePlaying.value
    sendControl(peer, "toggle")
  }

  fun playRemote() {
    val peer = _activeRemotePeer.value ?: return
    _remotePlaying.value = true
    sendControl(peer, "play")
  }

  fun pauseRemote() {
    val peer = _activeRemotePeer.value ?: return
    _remotePlaying.value = false
    sendControl(peer, "pause")
  }

  fun seekRemote(positionMs: Long) {
    val peer = _activeRemotePeer.value ?: return
    val clamped = positionMs.coerceAtLeast(0L)
    lastRemoteSeekMs = System.currentTimeMillis()
    _remotePositionMs.value = clamped
    sendControl(peer, "seek", positionMs = clamped)
  }

  fun nextRemote() {
    val peer = _activeRemotePeer.value ?: return
    sendControl(peer, "next")
  }

  fun previousRemote() {
    val peer = _activeRemotePeer.value ?: return
    sendControl(peer, "previous")
  }

  fun setVolumeRemote(volume: Float) {
    val peer = _activeRemotePeer.value ?: return
    sendControl(peer, "volume", volume = volume)
  }

  fun setShuffleRemote(enabled: Boolean) {
    val peer = _activeRemotePeer.value ?: return
    _remoteShuffle.value = enabled
    sendControl(peer, "shuffle", shuffle = enabled)
  }

  fun setRepeatRemote(mode: String) {
    val peer = _activeRemotePeer.value ?: return
    _remoteRepeat.value = mode
    sendControl(peer, "repeat", repeat = mode)
  }

  fun playIndexRemote(index: Int) {
    val peer = _activeRemotePeer.value ?: return
    sendControl(peer, "play_index", index = index)
  }

  fun handoffToPeer(peer: LanPeer) {
    _activeRemotePeer.value = peer
    suppressAutoMirrorUntilMs = System.currentTimeMillis() + 1800L
    startRemotePolling(peer)
    scope.launch {
      val state = withContext(Dispatchers.Main) { buildLocalState() }
      lastObservedTrackId = state.current?.videoId
      if (state.current == null && state.queue.isEmpty()) {
        _statusMessage.value = "Connected to ${peer.name} (play a song to send)"
        return@launch
      }
      val items = state.queue.ifEmpty { listOfNotNull(state.current) }
      val payload =
        JSONObject().apply {
          put("items", JSONArray().apply { items.forEach { put(trackToJson(it)) } })
          put("index", state.index.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
          put("positionMs", state.positionMs)
          put("source", state.source ?: "android_handoff")
          put("playImmediately", true)
          put("episodes", JSONArray().apply { state.episodes.forEach { put(episodeToJson(it)) } })
        }
      withContext(Dispatchers.Main) { playerConnection?.player?.pause() }
      try {
        val body = executePostWithFallback(peer, "/v1/handoff", payload.toString())
        if (body != null) {
          val st = parsePeerState(JSONObject(body))
          _remoteStates.value = _remoteStates.value + (peer.id to st)
          applyRemoteState(st)
          _statusMessage.value = "Live Sync active on ${peer.name}"
        } else {
          _statusMessage.value = "Handoff failed"
        }
      } catch (e: Exception) {
        _statusMessage.value = "Handoff error: ${e.message}"
      }
    }
  }

  fun continueFromPeer(peer: LanPeer) {
    stopRemotePolling()
    _activeRemotePeer.value = null
    _remoteMediaMetadata.value = null
    _remotePlaying.value = false
    suppressAutoMirrorUntilMs = System.currentTimeMillis() + 2500L
    scope.launch {
      val st =
        refreshPeerState(peer)
          ?: run {
            _statusMessage.value = "Could not reach ${peer.name}"
            return@launch
          }
      val items = st.queue.ifEmpty { listOfNotNull(st.current) }
      if (items.isEmpty()) {
        _statusMessage.value = "${peer.name} has nothing playing"
        return@launch
      }
      // Pause remote device before resuming locally and tell it this phone is now active
      sendControl(peer, "pause", controllerPeerId = deviceId)
      withContext(Dispatchers.Main) {
        applyLocalHandoff(
          items = items,
          index = st.index,
          positionMs = st.positionMs,
          source = st.source,
          playImmediately = true,
          episodes = st.episodes,
        )
      }
      _statusMessage.value = "Continuing from ${peer.name}"
    }
  }

  fun syncEpisodesWithAllPeers() {
    val activePeers = _peers.value
    if (activePeers.isEmpty()) return
    val localEps = localEpisodes()
    scope.launch {
      for (peer in activePeers) {
        try {
          val payload =
            JSONObject().apply {
              put("episodes", JSONArray().apply { localEps.forEach { put(episodeToJson(it)) } })
            }
          val body =
            executePostWithFallback(peer, "/v1/episodes/sync", payload.toString()) ?: continue
          val arr = JSONObject(body).optJSONArray("episodes") ?: continue
          mergeRemoteEpisodes(parseEpisodes(arr))
        } catch (_: Exception) {}
      }
    }
  }

  // --- Embedded HTTP/1.1 Server ---

  private suspend fun handleHttpClient(socket: Socket) {
    try {
      socket.use { sock ->
        sock.soTimeout = 5000
        val input = BufferedInputStream(sock.getInputStream())
        val requestLine = readLine(input) ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return
        val method = parts[0]
        val path = parts[1].substringBefore("?")
        var contentLength = 0
        while (true) {
          val header = readLine(input) ?: break
          if (header.isEmpty()) break
          if (header.startsWith("Content-Length:", ignoreCase = true)) {
            contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
          }
        }
        val bodyBytes =
          if (contentLength > 0 && contentLength < 4 * 1024 * 1024) {
            ByteArray(contentLength).also { buf ->
              var read = 0
              while (read < contentLength) {
                val n = input.read(buf, read, contentLength - read)
                if (n <= 0) break
                read += n
              }
            }
          } else {
            ByteArray(0)
          }
        val bodyStr = String(bodyBytes, StandardCharsets.UTF_8)

        val responseJson: String =
          when {
            method == "GET" && path == "/v1/state" -> {
              val st = withContext(Dispatchers.Main) { buildLocalState() }
              stateToJson(st).toString()
            }
            method == "POST" && path == "/v1/control" -> {
              val obj = JSONObject(bodyStr)
              val st = withContext(Dispatchers.Main) { applyLocalControl(obj) }
              stateToJson(st).toString()
            }
            method == "POST" && path == "/v1/handoff" -> {
              val obj = JSONObject(bodyStr)
              val items = parseTracks(obj.optJSONArray("items"))
              val index = obj.optInt("index", 0)
              val positionMs = obj.optLong("positionMs", 0L)
              val source = obj.optString("source", "")
              val playImmediately = obj.optBoolean("playImmediately", true)
              val episodes = parseEpisodes(obj.optJSONArray("episodes"))
              val st =
                withContext(Dispatchers.Main) {
                  applyLocalHandoff(items, index, positionMs, source, playImmediately, episodes)
                }
              stateToJson(st).toString()
            }
            method == "POST" && path == "/v1/episodes/sync" -> {
              val arr = JSONObject(bodyStr).optJSONArray("episodes")
              mergeRemoteEpisodes(parseEpisodes(arr))
              val merged = localEpisodes()
              JSONObject()
                .apply {
                  put("episodes", JSONArray().apply { merged.forEach { put(episodeToJson(it)) } })
                }
                .toString()
            }
            method == "POST" && path == "/v1/pair/start" -> {
              val obj = JSONObject(bodyStr)
              val id = obj.optString("deviceId")
              val peer =
                peerMap[id]
                  ?: LanPeer(
                    id = id,
                    name = obj.optString("deviceName").ifBlank { "Echo Desktop" },
                    platform = obj.optString("platform", "desktop"),
                    host =
                      obj.optString("host").ifBlank {
                        sock.inetAddress?.hostAddress ?: "127.0.0.1"
                      },
                    port = obj.optInt("port", 45871),
                  )
              val exp = obj.optLong("expiresAtMs", System.currentTimeMillis() + 60_000L)
              _incomingPairRequest.value = IncomingPairRequest(peer = peer, expiresAtMs = exp)
              JSONObject().put("ok", true).toString()
            }
            method == "POST" && path == "/v1/pair/verify" -> {
              val obj = JSONObject(bodyStr)
              handlePairVerify(
                  peerId = obj.optString("deviceId"),
                  peerName = obj.optString("deviceName"),
                  code = obj.optString("code"),
                )
                .toString()
            }
            method == "POST" && path == "/v1/pair/unpair" -> {
              val obj = JSONObject(bodyStr)
              val id = obj.optString("deviceId")
              if (_pairedPeerId.value == null || _pairedPeerId.value == id) {
                clearPairingStorage("Unpaired by remote device")
              }
              JSONObject().put("ok", true).toString()
            }
            method == "POST" && path == "/v1/sync" -> {
              val incoming = JSONObject(bodyStr)
              applyIncomingSyncBundle(incoming)
              buildLocalSyncBundle().toString()
            }
            else -> {
              writeHttpResponse(sock, 404, "Not Found", """{"error":"not found"}""")
              return
            }
          }
        writeHttpResponse(sock, 200, "OK", responseJson)
      }
    } catch (e: Exception) {
      Log.d(TAG, "HTTP client closed/failed: ${e.message}")
    }
  }

  private fun readLine(input: BufferedInputStream): String? {
    val out = ByteArrayOutputStream()
    while (true) {
      val b = input.read()
      if (b == -1) return if (out.size() == 0) null else out.toString(StandardCharsets.UTF_8.name())
      if (b == '\n'.code) break
      if (b != '\r'.code) out.write(b)
    }
    return out.toString(StandardCharsets.UTF_8.name())
  }

  private fun writeHttpResponse(socket: Socket, code: Int, status: String, json: String) {
    val body = json.toByteArray(StandardCharsets.UTF_8)
    val header =
      "HTTP/1.1 $code $status\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: ${body.size}\r\n" +
        "Connection: close\r\n\r\n"
    val out = BufferedOutputStream(socket.getOutputStream())
    out.write(header.toByteArray(StandardCharsets.US_ASCII))
    out.write(body)
    out.flush()
  }

  // --- PlayerConnection & PodcastProgress Bridge ---

  private fun buildLocalState(): LanPeerState {
    val conn = playerConnection
    val player = conn?.player
    val meta = conn?.mediaMetadata?.value
    val windows = conn?.queueWindows?.value.orEmpty()
    val queueTracks = windows.mapNotNull { w ->
      val m = w.mediaItem.metadata ?: return@mapNotNull null
      metadataToDto(m)
    }
    val currentIndex =
      player?.currentMediaItemIndex?.coerceIn(0, (queueTracks.size - 1).coerceAtLeast(0)) ?: 0
    val repeatStr =
      when (player?.repeatMode) {
        Player.REPEAT_MODE_ONE -> "one"
        Player.REPEAT_MODE_ALL -> "all"
        else -> "off"
      }
    return LanPeerState(
      deviceId = deviceId,
      deviceName = deviceName,
      platform = "android",
      playing = player?.isPlaying == true,
      volume = player?.volume ?: 1f,
      positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
      durationMs = player?.duration?.takeIf { it > 0 } ?: ((meta?.duration ?: 0) * 1000L),
      current = meta?.let(::metadataToDto),
      queue = queueTracks,
      index = currentIndex,
      source = conn?.queueTitle?.value,
      shuffle = player?.shuffleModeEnabled == true,
      repeat = repeatStr,
      episodes = localEpisodes(),
    )
  }

  private fun applyLocalControl(obj: JSONObject): LanPeerState {
    val conn = playerConnection
    val player = conn?.player
    if (conn != null && player != null) {
      when (obj.optString("action")) {
        "play" -> {
          if (player.playbackState == Player.STATE_IDLE) player.prepare()
          player.play()
        }
        "pause" -> {
          player.pause()
          val ctrlId = obj.optString("controllerPeerId")
          if (ctrlId.isNotBlank()) {
            attachAsControllerById(ctrlId)
          }
        }
        "toggle" -> {
          if (player.isPlaying) player.pause()
          else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
          }
        }
        "next" -> conn.seekToNext()
        "previous" -> conn.seekToPrevious()
        "seek" -> conn.seekTo(obj.optLong("positionMs", 0L).coerceAtLeast(0L))
        "volume" -> {
          val v = obj.optDouble("volume", 1.0).toFloat()
          player.volume = if (v > 1f) (v / 100f).coerceIn(0f, 1f) else v.coerceIn(0f, 1f)
        }
        "shuffle" -> {
          player.shuffleModeEnabled = obj.optBoolean("shuffle", false)
        }
        "repeat" -> {
          player.repeatMode =
            when (obj.optString("repeat")) {
              "one" -> Player.REPEAT_MODE_ONE
              "all" -> Player.REPEAT_MODE_ALL
              else -> Player.REPEAT_MODE_OFF
            }
        }
        "play_index" -> {
          val idx = obj.optInt("index", 0)
          if (idx in 0 until player.mediaItemCount) {
            player.seekToDefaultPosition(idx)
            player.play()
          }
        }
      }
    }
    return buildLocalState()
  }

  private fun applyLocalHandoff(
    items: List<LanTrackDto>,
    index: Int,
    positionMs: Long,
    source: String?,
    playImmediately: Boolean,
    episodes: List<LanEpisodeDto>,
  ): LanPeerState {
    stopRemotePolling()
    _activeRemotePeer.value = null
    _remoteMediaMetadata.value = null
    _remotePlaying.value = false
    suppressAutoMirrorUntilMs = System.currentTimeMillis() + 2500L
    if (episodes.isNotEmpty()) {
      mergeRemoteEpisodes(episodes)
    }
    val conn = playerConnection
    if (conn != null && items.isNotEmpty()) {
      val mediaItems = items.map { dtoToMediaMetadata(it).toMediaItem() }
      val safeIndex = index.coerceIn(0, mediaItems.lastIndex)
      conn.playQueue(
        ListQueue(
          title = source?.takeIf { it.isNotBlank() } ?: "Echo Connect",
          items = mediaItems,
          startIndex = safeIndex,
          position = positionMs.coerceAtLeast(0L),
        ),
      )
      if (!playImmediately) {
        conn.player.pause()
      }
    }
    return buildLocalState()
  }

  private fun localEpisodes(): List<LanEpisodeDto> =
    PodcastProgress.entries.value.values.map { ep ->
      LanEpisodeDto(
        videoId = ep.id,
        positionMs = ep.positionMs,
        durationMs = ep.durationMs,
        finished = ep.finished,
        updatedAt = ep.updatedAt,
        title = ep.title,
        show = ep.show,
        thumbnail = ep.thumbnailUrl,
      )
    }

  private fun mergeRemoteEpisodes(remote: List<LanEpisodeDto>) {
    remote.forEach { ep ->
      PodcastProgress.mergeLanEpisode(
        id = ep.videoId,
        positionMs = ep.positionMs,
        durationMs = ep.durationMs,
        finished = ep.finished,
        updatedAt = ep.updatedAt,
        title = ep.title,
        show = ep.show,
        thumbnailUrl = ep.thumbnail,
      )
    }
  }

  // --- JSON Codecs ---

  private fun metadataToDto(m: MediaMetadata) =
    LanTrackDto(
      videoId = m.id,
      title = m.title,
      artist = m.artists.joinToString(", ") { it.name },
      album = m.album?.title,
      durationSec = m.duration,
      thumbnail = m.thumbnailUrl,
      isPodcast = m.musicVideoType == MUSIC_VIDEO_TYPE_PODCAST_EPISODE,
    )

  private fun dtoToMediaMetadata(dto: LanTrackDto) =
    MediaMetadata(
      id = dto.videoId,
      title = dto.title,
      artists = listOf(MediaMetadata.Artist(id = null, name = dto.artist)),
      duration = dto.durationSec,
      thumbnailUrl = dto.thumbnail,
      musicVideoType = if (dto.isPodcast) MUSIC_VIDEO_TYPE_PODCAST_EPISODE else null,
    )

  private fun trackToJson(t: LanTrackDto) =
    JSONObject().apply {
      put("videoId", t.videoId)
      put("title", t.title)
      put("artist", t.artist)
      t.album?.let { put("album", it) }
      put("durationSec", t.durationSec)
      t.thumbnail?.let { put("thumbnail", it) }
      put("isPodcast", t.isPodcast)
    }

  private fun episodeToJson(e: LanEpisodeDto) =
    JSONObject().apply {
      put("videoId", e.videoId)
      put("positionMs", e.positionMs)
      put("durationMs", e.durationMs)
      put("finished", e.finished)
      put("updatedAt", e.updatedAt)
      e.title?.let { put("title", it) }
      e.show?.let { put("show", it) }
      e.thumbnail?.let { put("thumbnail", it) }
    }

  private fun stateToJson(s: LanPeerState) =
    JSONObject().apply {
      put("deviceId", s.deviceId)
      put("deviceName", s.deviceName)
      put("platform", s.platform)
      put("playing", s.playing)
      put("volume", s.volume.toDouble())
      put("positionMs", s.positionMs)
      put("durationMs", s.durationMs)
      s.current?.let { put("current", trackToJson(it)) }
      put("queue", JSONArray().apply { s.queue.forEach { put(trackToJson(it)) } })
      put("index", s.index)
      s.source?.let { put("source", it) }
      put("shuffle", s.shuffle)
      put("repeat", s.repeat)
      put("episodes", JSONArray().apply { s.episodes.forEach { put(episodeToJson(it)) } })
      put("updatedAt", System.currentTimeMillis())
    }

  private fun parseTracks(arr: JSONArray?): List<LanTrackDto> {
    if (arr == null) return emptyList()
    return List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        LanTrackDto(
          videoId = o.optString("videoId"),
          title = o.optString("title"),
          artist = o.optString("artist"),
          album = o.optString("album").takeIf { it.isNotBlank() },
          durationSec = o.optInt("durationSec", 0),
          thumbnail = o.optString("thumbnail").takeIf { it.isNotBlank() },
          isPodcast = o.optBoolean("isPodcast", false),
        )
      }
      .filter { it.videoId.isNotBlank() }
  }

  private fun parseEpisodes(arr: JSONArray?): List<LanEpisodeDto> {
    if (arr == null) return emptyList()
    return List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        LanEpisodeDto(
          videoId = o.optString("videoId"),
          positionMs = o.optLong("positionMs", 0L),
          durationMs = o.optLong("durationMs", 0L),
          finished = o.optBoolean("finished", false),
          updatedAt = o.optLong("updatedAt", 0L),
          title = o.optString("title").takeIf { it.isNotBlank() },
          show = o.optString("show").takeIf { it.isNotBlank() },
          thumbnail = o.optString("thumbnail").takeIf { it.isNotBlank() },
        )
      }
      .filter { it.videoId.isNotBlank() }
  }

  private fun parsePeerState(o: JSONObject): LanPeerState {
    val curObj = o.optJSONObject("current")
    val current = curObj?.let {
      LanTrackDto(
        videoId = it.optString("videoId"),
        title = it.optString("title"),
        artist = it.optString("artist"),
        album = it.optString("album").takeIf { s -> s.isNotBlank() },
        durationSec = it.optInt("durationSec", 0),
        thumbnail = it.optString("thumbnail").takeIf { s -> s.isNotBlank() },
        isPodcast = it.optBoolean("isPodcast", false),
      )
    }
    return LanPeerState(
      deviceId = o.optString("deviceId"),
      deviceName = o.optString("deviceName"),
      platform = o.optString("platform", "desktop"),
      playing = o.optBoolean("playing", false),
      volume = o.optDouble("volume", 1.0).toFloat(),
      positionMs = o.optLong("positionMs", 0L),
      durationMs = o.optLong("durationMs", 0L),
      current = current,
      queue = parseTracks(o.optJSONArray("queue")),
      index = o.optInt("index", 0),
      source = o.optString("source").takeIf { it.isNotBlank() },
      shuffle = o.optBoolean("shuffle", false),
      repeat = o.optString("repeat", "off"),
      episodes = parseEpisodes(o.optJSONArray("episodes")),
    )
  }

  // --- 6-Digit Pairing & Full Account / Playlist / Spotify Sync ---

  fun startPairing(peer: LanPeer) {
    pairingJob?.cancel()
    pairingJob = scope.launch {
      while (isActive && _pairedPeerId.value != peer.id) {
        val code = Random.nextInt(100_000, 1_000_000).toString()
        val expiresAt = System.currentTimeMillis() + 60_000L
        _pairingSession.value =
          PairingSession(peer = peer, code = code, expiresAtMs = expiresAt, secondsLeft = 60)
        val payload =
          JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("platform", "android")
            localIPv4Address()?.let { put("host", it) }
            put("port", serverSocket?.localPort ?: 45870)
            put("expiresAtMs", expiresAt)
          }
        try {
          executePostWithFallback(peer, "/v1/pair/start", payload.toString())
        } catch (e: Exception) {
          _statusMessage.value = "Could not reach ${peer.name}: ${e.message}"
        }
        for (sec in 59 downTo 0) {
          delay(1000)
          val cur = _pairingSession.value ?: return@launch
          if (_pairedPeerId.value == peer.id) return@launch
          _pairingSession.value = cur.copy(secondsLeft = sec)
        }
      }
    }
  }

  fun cancelPairing() {
    pairingJob?.cancel()
    pairingJob = null
    _pairingSession.value = null
  }

  fun dismissIncomingPairRequest() {
    _incomingPairRequest.value = null
  }

  fun submitIncomingPairCode(code: String) {
    val req = _incomingPairRequest.value ?: return
    val trimmed = code.trim()
    if (trimmed.length != 6) {
      _incomingPairRequest.value = req.copy(error = "Enter the 6-digit code")
      return
    }
    _incomingPairRequest.value = req.copy(busy = true, error = null)
    scope.launch {
      try {
        val payload =
          JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("code", trimmed)
          }
        val body = executePostWithFallback(req.peer, "/v1/pair/verify", payload.toString())
        val resp = if (body != null) JSONObject(body) else null
        if (resp?.optBoolean("ok", false) == true) {
          val token = resp.optString("pairToken")
          prefs.edit {
            putString(KEY_PAIRED_PEER_ID, req.peer.id)
            putString(KEY_PAIRED_PEER_NAME, req.peer.name)
            putString(KEY_PAIR_TOKEN, token)
          }
          _pairedPeerId.value = req.peer.id
          _pairedPeerName.value = req.peer.name
          _incomingPairRequest.value = null
          _statusMessage.value = "Paired with ${req.peer.name}"
          appendSyncLog("Paired with ${req.peer.name}")
        } else {
          val err =
            resp?.optString("error")?.takeIf { it.isNotBlank() } ?: "Invalid or expired code"
          _incomingPairRequest.value = req.copy(busy = false, error = err)
        }
      } catch (e: Exception) {
        _incomingPairRequest.value = req.copy(busy = false, error = e.message ?: "Pairing failed")
      }
    }
  }

  fun unpair(peer: LanPeer?) {
    val target = peer ?: _peers.value.firstOrNull { it.id == _pairedPeerId.value }
    clearPairingStorage("Unpaired")
    if (target != null) {
      scope.launch {
        try {
          executePostWithFallback(
            target,
            "/v1/pair/unpair",
            JSONObject().put("deviceId", deviceId).toString(),
          )
        } catch (_: Exception) {}
      }
    }
  }

  private fun clearPairingStorage(reason: String) {
    prefs.edit {
      remove(KEY_PAIRED_PEER_ID)
      remove(KEY_PAIRED_PEER_NAME)
      remove(KEY_PAIR_TOKEN)
    }
    _pairedPeerId.value = null
    _pairedPeerName.value = null
    cancelPairing()
    _statusMessage.value = reason
    appendSyncLog(reason)
  }

  private fun handlePairVerify(peerId: String, peerName: String, code: String): JSONObject {
    val session = _pairingSession.value
    val now = System.currentTimeMillis()
    if (session == null || now > session.expiresAtMs) {
      return JSONObject()
        .put("ok", false)
        .put("error", "Code expired. Check your phone for the new code.")
    }
    if (code.trim() != session.code) {
      return JSONObject().put("ok", false).put("error", "Incorrect 6-digit code.")
    }
    val token = UUID.randomUUID().toString().replace("-", "")
    val boundId = peerId.ifBlank { session.peer.id }
    val boundName = peerName.ifBlank { session.peer.name }
    prefs.edit {
      putString(KEY_PAIRED_PEER_ID, boundId)
      putString(KEY_PAIRED_PEER_NAME, boundName)
      putString(KEY_PAIR_TOKEN, token)
    }
    _pairedPeerId.value = boundId
    _pairedPeerName.value = boundName
    cancelPairing()
    _statusMessage.value = "Paired with $boundName"
    appendSyncLog("Paired with $boundName")
    return JSONObject().put("ok", true).put("pairToken", token)
  }

  fun forceSyncNow(peerOverride: LanPeer? = null) {
    val target = peerOverride ?: _peers.value.firstOrNull { it.id == _pairedPeerId.value } ?: return
    if (_syncBusy.value) return
    _syncBusy.value = true
    scope.launch {
      try {
        val bundle = buildLocalSyncBundle()
        val body = executePostWithFallback(target, "/v1/sync", bundle.toString())
        if (body != null) {
          applyIncomingSyncBundle(JSONObject(body))
          _statusMessage.value = "Sync complete with ${target.name}"
        } else {
          _statusMessage.value = "Sync failed: empty response"
        }
      } catch (e: Exception) {
        _statusMessage.value = "Sync failed: ${e.message}"
        appendSyncLog("Sync failed: ${e.message}")
      } finally {
        _syncBusy.value = false
      }
    }
  }

  private suspend fun buildLocalSyncBundle(): JSONObject {
    val now = System.currentTimeMillis()
    val prefsData = appContext.dataStore.data.first()

    // 1. Google Account
    val cookie = prefsData[InnerTubeCookieKey].orEmpty()
    val acctName = prefsData[AccountNameKey].orEmpty()
    val acctEmail = prefsData[AccountEmailKey].orEmpty()
    val acctHandle = prefsData[AccountChannelHandleKey].orEmpty()
    val accountJson =
      if (cookie.isNotBlank()) {
        JSONObject().apply {
          put("cookie", cookie)
          put("name", acctName)
          put("email", acctEmail)
          put("channelHandle", acctHandle)
          put("updatedAtMs", now)
        }
      } else null

    // 2. Local Playlists from Room DB (non-YouTube / local-only, excluding system playlists)
    val localPlaylistsArr = JSONArray()
    val db = database
    var localCount = 0
    if (db != null) {
      val allPlaylists = runCatching {
        db.playlistsByCreateDateAsc().first()
      }
        .getOrDefault(emptyList())
      for (pl in allPlaylists) {
        val e = pl.playlist
        if (
          e.id == PlaylistEntity.LIKED_PLAYLIST_ID || e.id == PlaylistEntity.DOWNLOADED_PLAYLIST_ID
        ) {
          continue
        }
        if (!e.isLocal && !e.browseId.isNullOrBlank()) continue
        val songs = runCatching { db.playlistSongs(e.id).first() }.getOrDefault(emptyList())
        val tracksArr = JSONArray()
        for (ps in songs) {
          val s = ps.song.song
          tracksArr.put(
            trackToJson(
              LanTrackDto(
                videoId = s.id,
                title = s.title,
                artist = ps.song.artists.joinToString(", ") { it.name },
                album = ps.song.album?.title,
                durationSec = s.duration,
                thumbnail = s.thumbnailUrl,
              ),
            ),
          )
        }
        localPlaylistsArr.put(
          JSONObject().apply {
            put("syncId", "local:${e.id}")
            put("title", e.name)
            e.thumbnailUrl?.let { put("thumbnail", it) }
            put("isSpotify", false)
            put("updatedAtMs", now)
            put("tracks", tracksArr)
          },
        )
        localCount++
      }
    }

    // 3. Spotify Playlists (if user is signed into Spotify)
    val spotifyPlaylistsArr = JSONArray()
    var spotifyCount = 0
    val spDc = prefsData[SpotifySpDcKey].orEmpty()
    val spKey = prefsData[SpotifySpKeyKey].orEmpty()
    var spToken = prefsData[SpotifyAccessTokenKey].orEmpty()
    if (spToken.isBlank() && spDc.isNotBlank()) {
      spToken = SpotifyAuth.fetchAccessToken(spDc, spKey).getOrNull()?.accessToken.orEmpty()
    }
    if (spToken.isNotBlank()) {
      Spotify.accessToken = spToken
      val myPlaylists = runCatching {
        Spotify.myPlaylists(limit = 50, offset = 0).getOrNull()?.items
      }
        .getOrNull()
        .orEmpty()
      for (sp in myPlaylists) {
        val spTracks = runCatching {
          Spotify.playlistTracks(sp.id, limit = 100, offset = 0).getOrNull()?.items
        }
          .getOrNull()
          .orEmpty()
        val tracksArr = JSONArray()
        for (item in spTracks) {
          val tr = item.track ?: continue
          tracksArr.put(
            trackToJson(
              LanTrackDto(
                videoId = "",
                title = tr.name,
                artist = tr.artists.joinToString(", ") { it.name },
                album = tr.album?.name,
                durationSec = (tr.durationMs / 1000L).toInt(),
                thumbnail = tr.album?.images?.firstOrNull()?.url,
              ),
            ),
          )
        }
        spotifyPlaylistsArr.put(
          JSONObject().apply {
            put("syncId", "spotify:${sp.id}")
            put("title", sp.name)
            sp.images.firstOrNull()?.url?.let { put("thumbnail", it) }
            put("isSpotify", true)
            put("updatedAtMs", now)
            put("tracks", tracksArr)
          },
        )
        spotifyCount++
      }
    }

    val eps = localEpisodes()
    _syncDetails.value =
      _syncDetails.value.copy(
        lastSyncAtMs = now,
        accountStatus =
          if (cookie.isNotBlank())
            "Synced (${acctName.ifBlank { acctEmail.ifBlank { "Signed in" } }})"
          else "Not signed in",
        localPlaylistsStatus = "$localCount playlists sent",
        spotifyPlaylistsStatus =
          if (spToken.isNotBlank()) "$spotifyCount playlists sent" else "Spotify not connected",
        episodesStatus = "${eps.size} episodes",
      )

    return JSONObject().apply {
      put("senderId", deviceId)
      put("senderName", deviceName)
      put("sentAtMs", now)
      accountJson?.let { put("googleAccount", it) }
      put("localPlaylists", localPlaylistsArr)
      put("spotifyPlaylists", spotifyPlaylistsArr)
      put("episodes", JSONArray().apply { eps.forEach { put(episodeToJson(it)) } })
    }
  }

  private suspend fun applyIncomingSyncBundle(obj: JSONObject) {
    val now = System.currentTimeMillis()
    val acctObj = obj.optJSONObject("googleAccount")
    var acctMsg = _syncDetails.value.accountStatus
    if (acctObj != null) {
      val cookie = acctObj.optString("cookie")
      val name = acctObj.optString("name")
      val email = acctObj.optString("email")
      val handle = acctObj.optString("channelHandle")
      if (cookie.isNotBlank()) {
        val currentCookie = appContext.dataStore.data.first()[InnerTubeCookieKey].orEmpty()
        if (currentCookie.isBlank()) {
          appContext.dataStore.edit { p ->
            p[InnerTubeCookieKey] = cookie
            if (name.isNotBlank()) p[AccountNameKey] = name
            if (email.isNotBlank()) p[AccountEmailKey] = email
            if (handle.isNotBlank()) p[AccountChannelHandleKey] = handle
          }
          acctMsg = "Signed in (${name.ifBlank { email }})"
          appendSyncLog("Google account signed in from Desktop")
        } else {
          acctMsg = "Synced (${name.ifBlank { "Active" }})"
        }
      }
    }

    val localArr = obj.optJSONArray("localPlaylists")
    var mergedPlaylists = 0
    val db = database
    if (localArr != null && db != null) {
      val existing = runCatching { db.playlistsByCreateDateAsc().first() }.getOrDefault(emptyList())
      for (i in 0 until localArr.length()) {
        val plObj = localArr.optJSONObject(i) ?: continue
        val title = plObj.optString("title").trim()
        if (title.isBlank()) continue
        val match = existing.firstOrNull {
          (it.playlist.isLocal || it.playlist.browseId.isNullOrBlank()) &&
            it.playlist.name.equals(title, ignoreCase = true)
        }
        val playlistId =
          match?.playlist?.id
            ?: PlaylistEntity.generatePlaylistId().also { newId ->
              db.insert(
                PlaylistEntity(
                  id = newId,
                  name = title,
                  thumbnailUrl = plObj.optString("thumbnail").takeIf { it.isNotBlank() },
                  isLocal = true,
                ),
              )
            }
        val tracksArr = plObj.optJSONArray("tracks")
        if (tracksArr != null) {
          val curSongs = runCatching {
            db.playlistSongs(playlistId).first()
          }
            .getOrDefault(emptyList())
          val existingIds = curSongs.map { it.song.song.id }.toMutableSet()
          var pos = curSongs.size
          for (j in 0 until tracksArr.length()) {
            val t = tracksArr.optJSONObject(j) ?: continue
            val vid = t.optString("videoId")
            if (vid.isBlank() || existingIds.contains(vid)) continue
            db.insert(
              SongEntity(
                id = vid,
                title = t.optString("title"),
                duration = t.optInt("durationSec", 0),
                thumbnailUrl = t.optString("thumbnail").takeIf { it.isNotBlank() },
              ),
            )
            db.insert(PlaylistSongMap(playlistId = playlistId, songId = vid, position = pos++))
            existingIds.add(vid)
          }
        }
        mergedPlaylists++
      }
    }

    val eps = parseEpisodes(obj.optJSONArray("episodes"))
    if (eps.isNotEmpty()) {
      mergeRemoteEpisodes(eps)
    }

    _syncDetails.value =
      _syncDetails.value.copy(
        lastSyncAtMs = now,
        accountStatus = acctMsg,
        localPlaylistsStatus =
          if (mergedPlaylists > 0) "$mergedPlaylists playlists merged"
          else _syncDetails.value.localPlaylistsStatus,
        episodesStatus = "${localEpisodes().size} episodes synced",
      )
    appendSyncLog("Synced with ${obj.optString("senderName").ifBlank { "Desktop" }}")
  }

  private fun appendSyncLog(message: String) {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val line = "[${fmt.format(Date())}] $message"
    _syncLogs.value = (listOf(line) + _syncLogs.value).take(20)
  }
}
