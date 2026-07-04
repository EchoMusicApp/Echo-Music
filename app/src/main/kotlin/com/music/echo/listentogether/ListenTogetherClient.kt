

package iad1tya.echo.music.listentogether

import android.util.Base64
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.edit
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.ListenTogetherAutoApprovalKey
import iad1tya.echo.music.constants.ListenTogetherIsHostKey
import iad1tya.echo.music.constants.ListenTogetherRoomCodeKey
import iad1tya.echo.music.constants.ListenTogetherServerUrlKey
import iad1tya.echo.music.constants.ListenTogetherSessionTimestampKey
import iad1tya.echo.music.constants.ListenTogetherSessionTokenKey
import iad1tya.echo.music.constants.ListenTogetherUserIdKey
import iad1tya.echo.music.utils.NetworkConnectivityObserver
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}


enum class RoomRole {
    HOST,
    GUEST,
    NONE
}


data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String,
    val details: String? = null
)

enum class LogLevel {
    INFO,
    WARNING,
    ERROR,
    DEBUG
}


sealed class PendingAction {
    data class CreateRoom(val username: String) : PendingAction()
    data class JoinRoom(val roomCode: String, val username: String) : PendingAction()
}


sealed class ListenTogetherEvent {
    
    data class Connected(val userId: String) : ListenTogetherEvent()
    data object Disconnected : ListenTogetherEvent()
    data class ConnectionError(val error: String) : ListenTogetherEvent()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ListenTogetherEvent()
    
    
    data class RoomCreated(val roomCode: String, val userId: String) : ListenTogetherEvent()
    data class JoinRequestReceived(val userId: String, val username: String) : ListenTogetherEvent()
    data class JoinApproved(val roomCode: String, val userId: String, val state: RoomState) : ListenTogetherEvent()
    data class JoinRejected(val reason: String) : ListenTogetherEvent()
    data class UserJoined(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserLeft(val userId: String, val username: String) : ListenTogetherEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : ListenTogetherEvent()
    data class Kicked(val reason: String) : ListenTogetherEvent()
    data class Reconnected(val roomCode: String, val userId: String, val state: RoomState, val isHost: Boolean) : ListenTogetherEvent()
    data class UserReconnected(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserDisconnected(val userId: String, val username: String) : ListenTogetherEvent()
    
    
    data class PlaybackSync(val action: PlaybackActionPayload) : ListenTogetherEvent()
    data class BufferWait(val trackId: String, val waitingFor: List<String>) : ListenTogetherEvent()
    data class BufferComplete(val trackId: String) : ListenTogetherEvent()
    data class SyncStateReceived(val state: SyncStatePayload) : ListenTogetherEvent()

    
    data class ServerError(val code: String, val message: String) : ListenTogetherEvent()

    
    data class ChatMessageReceived(val payload: ChatMessagePayload) : ListenTogetherEvent()
    
    
    data class LocalSuggestionApproved(val payload: SuggestionReceivedPayload) : ListenTogetherEvent()
}


@Singleton
class ListenTogetherClient @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ListenTogether"
        private val DEFAULT_SERVER_URL = ListenTogetherServers.defaultServerUrl
        private const val MAX_RECONNECT_ATTEMPTS = 15  
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L  
        private const val MAX_RECONNECT_DELAY_MS = 120000L  
        private const val PING_INTERVAL_MS = 25000L
        private const val MAX_LOG_ENTRIES = 500
        private const val SESSION_GRACE_PERIOD_MS = 10 * 60 * 1000L  

        
        private const val NOTIFICATION_CHANNEL_ID = "listen_together_channel"
        const val ACTION_APPROVE_JOIN = "iad1tya.echo.music.LISTEN_TOGETHER_APPROVE_JOIN"
        const val ACTION_REJECT_JOIN = "iad1tya.echo.music.LISTEN_TOGETHER_REJECT_JOIN"
        const val ACTION_APPROVE_SUGGESTION = "iad1tya.echo.music.LISTEN_TOGETHER_APPROVE_SUGGESTION"
        const val ACTION_REJECT_SUGGESTION = "iad1tya.echo.music.LISTEN_TOGETHER_REJECT_SUGGESTION"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_SUGGESTION_ID = "extra_suggestion_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        @Volatile
        private var instance: ListenTogetherClient? = null
        
        fun getInstance(): ListenTogetherClient? = instance
        
        fun setInstance(client: ListenTogetherClient) {
            instance = client
        }
    }
    
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(RoomRole.NONE)
    val role: StateFlow<RoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<JoinRequestPayload>>(emptyList())
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = _pendingJoinRequests.asStateFlow()

    private val _bufferingUsers = MutableStateFlow<List<String>>(emptyList())
    val bufferingUsers: StateFlow<List<String>> = _bufferingUsers.asStateFlow()

    
    private val _pendingSuggestions = MutableStateFlow<List<SuggestionReceivedPayload>>(emptyList())
    val pendingSuggestions: StateFlow<List<SuggestionReceivedPayload>> = _pendingSuggestions.asStateFlow()

    
    private val _blockedUsernames = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsernames: StateFlow<Set<String>> = _blockedUsernames.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    
    private val _events = MutableSharedFlow<ListenTogetherEvent>()
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()
    
    private val _rtt = MutableStateFlow(0L)
    val rtt: StateFlow<Long> = _rtt.asStateFlow()
    
    init {
        setInstance(this)
        ensureNotificationChannel()
        
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            loadPersistedSession()
            observeNetworkChanges()
        }
    }

    
    private fun observeNetworkChanges() {
        scope.launch {
            try {
                val observer = connectivityObserver ?: return@launch
                observer.networkStatus.collect { available: Boolean ->
                    val previous = isNetworkAvailable
                    isNetworkAvailable = available
                    
                    if (available && !previous) {
                        log(LogLevel.INFO, "Network restored, checking if reconnection needed")
                        
                        if (_connectionState.value == ConnectionState.ERROR || 
                            _connectionState.value == ConnectionState.DISCONNECTED) {
                            
                            if (sessionToken != null || _roomState.value != null || pendingAction != null) {
                                log(LogLevel.INFO, "Network restored, triggering reconnection")
                                reconnectAttempts = 0 
                                connect()
                            }
                        }
                    } else if (!available && previous) {
                        log(LogLevel.WARNING, "Network lost")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error observing network changes")
            }
        }
    }
    
    
    private fun loadPersistedSession() {
        try {
            val token = context.dataStore.get(ListenTogetherSessionTokenKey, "")
            val roomCode = context.dataStore.get(ListenTogetherRoomCodeKey, "")
            val userId = context.dataStore.get(ListenTogetherUserIdKey, "")
            val isHost = context.dataStore.get(ListenTogetherIsHostKey, false)
            val timestamp = context.dataStore.get(ListenTogetherSessionTimestampKey, 0L)
            
            
            if (token.isNotEmpty() && roomCode.isNotEmpty() && 
                (System.currentTimeMillis() - timestamp < SESSION_GRACE_PERIOD_MS)) {
                sessionToken = token
                storedRoomCode = roomCode
                _userId.value = userId.ifEmpty { null }
                wasHost = isHost
                sessionStartTime = timestamp
                log(LogLevel.INFO, "Loaded persisted session", "Room: $roomCode, Host: $isHost")
            } else if (token.isNotEmpty()) {
                log(LogLevel.WARNING, "Session expired", "Age: ${System.currentTimeMillis() - timestamp}ms")
                clearPersistedSession()
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to load persisted session", e.message)
        }
        
        
        loadBlockedUsernames()
    }
    
    
    private fun loadBlockedUsernames() {
        try {
            val blockedJson = context.dataStore.get(iad1tya.echo.music.constants.ListenTogetherBlockedUsersKey, "")
            val blockedList = if (blockedJson.isNotEmpty()) {
                json.decodeFromString<List<String>>(blockedJson)
            } else {
                emptyList()
            }
            _blockedUsernames.value = blockedList.toSet()
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to load blocked usernames", e.message)
            _blockedUsernames.value = emptySet()
        }
    }
    
    
    private suspend fun saveBlockedUsernames() {
        try {
            val blockedJson = json.encodeToString(_blockedUsernames.value.toList())
            context.dataStore.edit { preferences ->
                preferences[iad1tya.echo.music.constants.ListenTogetherBlockedUsersKey] = blockedJson
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to save blocked usernames", e.message)
        }
    }
    
    
    private fun savePersistedSession() {
        try {
            scope.launch {
                context.dataStore.edit { preferences ->
                    if (sessionToken != null) {
                        preferences[ListenTogetherSessionTokenKey] = sessionToken!!
                        preferences[ListenTogetherRoomCodeKey] = storedRoomCode ?: ""
                        preferences[ListenTogetherUserIdKey] = _userId.value ?: ""
                        preferences[ListenTogetherIsHostKey] = wasHost
                        preferences[ListenTogetherSessionTimestampKey] = System.currentTimeMillis()
                    }
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to save persisted session", e.message)
        }
    }
    
    
    private fun clearPersistedSession() {
        try {
            scope.launch {
                context.dataStore.edit { preferences ->
                    preferences.remove(ListenTogetherSessionTokenKey)
                    preferences.remove(ListenTogetherRoomCodeKey)
                    preferences.remove(ListenTogetherUserIdKey)
                    preferences.remove(ListenTogetherIsHostKey)
                    preferences.remove(ListenTogetherSessionTimestampKey)
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to clear persisted session", e.message)
        }
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    
    
    private val codec = MessageCodec(MessageFormat.JSON, false)

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var pingSentTime: Long = 0L
    private var reconnectAttempts = 0
    
    
    @Volatile private var sessionToken: String? = null
    @Volatile private var storedUsername: String? = null
    @Volatile private var storedRoomCode: String? = null
    private var wasHost: Boolean = false
    private var sessionStartTime: Long = 0


    @Volatile private var pendingAction: PendingAction? = null
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    
    private val joinRequestNotifications = ConcurrentHashMap<String, Int>()


    private val suggestionNotifications = ConcurrentHashMap<String, Int>()

    
    private val connectivityObserver: NetworkConnectivityObserver? by lazy {
        try {
            NetworkConnectivityObserver(context)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to create NetworkConnectivityObserver")
            null
        }
    }
    private var isNetworkAvailable = try { 
        connectivityObserver?.isCurrentlyConnected() ?: true 
    } catch (e: Exception) { 
        true 
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private fun getServerUrl(): String {
        return context.dataStore.get(ListenTogetherServerUrlKey, DEFAULT_SERVER_URL)
    }
    
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RECONNECT_DELAY_MS * (2 shl (minOf(attempt - 1, 4)))
        val cappedDelay = minOf(exponentialDelay, MAX_RECONNECT_DELAY_MS)
        
        val jitter = (cappedDelay * 0.2 * Math.random()).toLong()
        return cappedDelay + jitter
    }

    private fun log(level: LogLevel, message: String, details: String? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val entry = LogEntry(timestamp, level, message, details)
        
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_ENTRIES)
        
        when (level) {
            LogLevel.ERROR -> Timber.tag(TAG).e("$message ${details ?: ""}")
            LogLevel.WARNING -> Timber.tag(TAG).w("$message ${details ?: ""}")
            LogLevel.DEBUG -> Timber.tag(TAG).d("$message ${details ?: ""}")
            LogLevel.INFO -> Timber.tag(TAG).i("$message ${details ?: ""}")
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED || 
            _connectionState.value == ConnectionState.CONNECTING) {
            log(LogLevel.WARNING, "Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        val serverUrl = getServerUrl()
        log(LogLevel.INFO, "Connecting to server", serverUrl)

        
        codec.format = MessageFormat.JSON
        codec.compressionEnabled = false

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                log(LogLevel.INFO, "Connected to server")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                startPingJob()
                
                
                if (sessionToken != null && storedRoomCode != null) {
                    log(LogLevel.INFO, "Attempting to reconnect to previous session", "Room: $storedRoomCode")
                    sendMessage(MessageTypes.RECONNECT, ReconnectPayload(sessionToken!!))
                } else {
                    
                    executePendingAction()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                
                handleMessage(text.toByteArray())
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                
                handleMessage(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                log(LogLevel.INFO, "Server closing connection", "Code: $code, Reason: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                log(LogLevel.INFO, "Connection closed", "Code: $code, Reason: $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                log(LogLevel.ERROR, "Connection failure", t.message)
                handleConnectionFailure(t)
            }
        })
    }
    
    private fun executePendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        
        when (action) {
            is PendingAction.CreateRoom -> {
                log(LogLevel.INFO, "Executing pending create room", action.username)
                sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(action.username))
            }
            is PendingAction.JoinRoom -> {
                log(LogLevel.INFO, "Executing pending join room", "${action.roomCode} as ${action.username}")
                sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(action.roomCode.uppercase(), action.username))
            }
        }
    }

    
    fun disconnect() {
        log(LogLevel.INFO, "Disconnecting from server")
        releaseWakeLock() 
        pingJob?.cancel()
        pingJob = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        
        
        sessionToken = null
        storedRoomCode = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = RoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        
        clearPersistedSession()
        reconnectAttempts = 0
        
        scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(PING_INTERVAL_MS)
                pingSentTime = System.currentTimeMillis()
                sendMessageNoPayload(MessageTypes.PING)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = context.getSystemService<PowerManager>()
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "echomusic:ListenTogether"
            )
        }
        if (wakeLock?.isHeld == false) {
            
            
            wakeLock?.acquire(10 * 60 * 1000L)
            log(LogLevel.DEBUG, "Wake lock acquired")
        }
    }
    
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            log(LogLevel.DEBUG, "Wake lock released")
        }
    }

    private fun ensureNotificationChannel() {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            val existing = nm?.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.listen_together_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = context.getString(R.string.listen_together_notification_channel_desc)
                nm?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            log(LogLevel.WARNING, "Failed to create notification channel", e.message)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showJoinRequestNotification(payload: JoinRequestPayload) {
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        
        
        joinRequestNotifications[payload.userId] = notifId

        val approveIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_APPROVE_JOIN
            putExtra(EXTRA_USER_ID, payload.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val rejectIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_REJECT_JOIN
            putExtra(EXTRA_USER_ID, payload.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePI = PendingIntent.getBroadcast(context, payload.userId.hashCode(), approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rejectPI = PendingIntent.getBroadcast(context, payload.userId.hashCode().inv(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val content = context.getString(R.string.listen_together_join_request_notification, payload.username)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.share)
            .setContentTitle(context.getString(R.string.listen_together))
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.approve), approvePI)
            .addAction(0, context.getString(R.string.reject), rejectPI)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showSuggestionNotification(payload: SuggestionReceivedPayload) {
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        
        
        suggestionNotifications[payload.suggestionId] = notifId

        val approveIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_APPROVE_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, payload.suggestionId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val rejectIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_REJECT_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, payload.suggestionId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePI = PendingIntent.getBroadcast(context, payload.suggestionId.hashCode(), approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rejectPI = PendingIntent.getBroadcast(context, payload.suggestionId.hashCode().inv(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val content = context.getString(R.string.listen_together_suggestion_received, payload.fromUsername, payload.trackInfo.title)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.share)
            .setContentTitle(context.getString(R.string.listen_together))
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.approve), approvePI)
            .addAction(0, context.getString(R.string.reject), rejectPI)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    private fun handleDisconnect() {
        pingJob?.cancel()
        pingJob = null
        
        
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        
        if (sessionToken != null && _roomState.value != null) {
            log(LogLevel.INFO, "Connection lost, will attempt to reconnect")
            handleConnectionFailure(Exception("Connection lost"))
        } else {
            scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
        }
    }

    private fun handleConnectionFailure(t: Throwable) {
        pingJob?.cancel()
        pingJob = null
        
        
        val shouldReconnect = sessionToken != null || _roomState.value != null || pendingAction != null
        
        if (!isNetworkAvailable) {
            log(LogLevel.WARNING, "Connection failure, waiting for network", t.message)
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && shouldReconnect) {
            reconnectAttempts++
            _connectionState.value = ConnectionState.RECONNECTING
            
            val delayMs = calculateBackoffDelay(reconnectAttempts)
            val delaySeconds = delayMs / 1000
            
            log(LogLevel.INFO, "Attempting reconnect", 
                "Attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS, waiting ${delaySeconds}s, reason: ${t.message}")
            
            scope.launch {
                _events.emit(ListenTogetherEvent.Reconnecting(reconnectAttempts, MAX_RECONNECT_ATTEMPTS))
                delay(delayMs)
                
                
                if (_connectionState.value == ConnectionState.RECONNECTING || _connectionState.value == ConnectionState.DISCONNECTED) {
                    log(LogLevel.INFO, "Reconnecting after backoff", "Delay was ${delaySeconds}s")
                    connect()
                }
            }
        } else {
            _connectionState.value = ConnectionState.ERROR
            
            
            if (sessionToken != null) {
                log(LogLevel.ERROR, "Reconnection failed", 
                    "Max attempts reached, but session preserved for manual reconnect")
                scope.launch { 
                    _events.emit(ListenTogetherEvent.ConnectionError(
                        "Connection failed after $MAX_RECONNECT_ATTEMPTS attempts. ${t.message ?: "Unknown error"}"
                    ))
                }
            } else {
                
                sessionToken = null
                storedRoomCode = null
                storedUsername = null
                _roomState.value = null
                _role.value = RoomRole.NONE
                clearPersistedSession()
                
                scope.launch { 
                    _events.emit(ListenTogetherEvent.ConnectionError(t.message ?: "Unknown error"))
                }
            }
        }
    }

    private fun handleMessage(data: ByteArray) {
        log(LogLevel.DEBUG, "Received message", "${data.size} bytes")
        
        try {
            
            val detectedFormat = MessageCodec.detectMessageFormat(data)
            if (detectedFormat == MessageFormat.PROTOBUF && codec.format == MessageFormat.JSON) {
                codec.format = MessageFormat.PROTOBUF
                codec.compressionEnabled = true
                log(LogLevel.INFO, "Upgraded to Protobuf", "with compression")
            }
            
            
            val (msgType, payloadBytes) = codec.decode(data)
            
            when (msgType) {
                MessageTypes.ROOM_CREATED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? RoomCreatedPayload ?: return
                    _userId.value = payload.userId
                    _role.value = RoomRole.HOST
                    sessionToken = payload.sessionToken
                    storedRoomCode = payload.roomCode
                    wasHost = true
                    sessionStartTime = System.currentTimeMillis()
                    
                    _roomState.value = RoomState(
                        roomCode = payload.roomCode,
                        hostId = payload.userId,
                        users = listOf(UserInfo(payload.userId, storedUsername ?: "", true)),
                        isPlaying = false,
                        position = 0,
                        lastUpdate = System.currentTimeMillis(),
                        volume = 1f
                    )
                    
                    
                    savePersistedSession()
                    
                    acquireWakeLock() 
                    log(LogLevel.INFO, "Room created", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.RoomCreated(payload.roomCode, payload.userId)) }
                    
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.listen_together_room_created, payload.roomCode),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
                MessageTypes.JOIN_REQUEST -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? JoinRequestPayload ?: return
                    
                    
                    if (isUserBlocked(payload.username)) {
                        log(LogLevel.INFO, "Join request from blocked user ignored", "User: ${payload.username}")
                        
                        rejectJoin(payload.userId, "You are blocked")
                        return
                    }

                    _pendingJoinRequests.value += payload
                    log(LogLevel.INFO, "Join request received", "User: ${payload.username}")
                    
                    
                    val autoApprovalEnabled = context.dataStore.get(ListenTogetherAutoApprovalKey, false)
                    
                    if (_role.value == RoomRole.HOST) {
                        if (autoApprovalEnabled) {
                            
                            log(LogLevel.INFO, "Auto-approving join request", "User: ${payload.username}")
                            approveJoin(payload.userId)
                        } else {
                            
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                showJoinRequestNotification(payload)
                            }
                        }
                    }
                    scope.launch { _events.emit(ListenTogetherEvent.JoinRequestReceived(payload.userId, payload.username)) }
                }
                
                MessageTypes.JOIN_APPROVED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? JoinApprovedPayload ?: return
                    _userId.value = payload.userId
                    _role.value = RoomRole.GUEST
                    sessionToken = payload.sessionToken
                    storedRoomCode = payload.roomCode
                    wasHost = false
                    sessionStartTime = System.currentTimeMillis()
                    
                    _roomState.value = payload.state
                    
                    
                    savePersistedSession()
                    
                    acquireWakeLock() 
                    log(LogLevel.INFO, "Joined room", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.JoinApproved(payload.roomCode, payload.userId, payload.state)) }
                }
                
                MessageTypes.JOIN_REJECTED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? JoinRejectedPayload ?: return
                    log(LogLevel.WARNING, "Join rejected", payload.reason)
                    scope.launch { _events.emit(ListenTogetherEvent.JoinRejected(payload.reason)) }
                }
                
                MessageTypes.USER_JOINED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? UserJoinedPayload ?: return
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users + UserInfo(payload.userId, payload.username, false)
                    )
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != payload.userId }
                    
                    
                    joinRequestNotifications.remove(payload.userId)?.let { notifId ->
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                    
                    log(LogLevel.INFO, "User joined", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserJoined(payload.userId, payload.username)) }
                }
                
                MessageTypes.USER_LEFT -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? UserLeftPayload ?: return
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.filter { it.userId != payload.userId }
                    )
                    log(LogLevel.INFO, "User left", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserLeft(payload.userId, payload.username)) }
                }
                
                MessageTypes.HOST_CHANGED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? HostChangedPayload ?: return
                    _roomState.value = _roomState.value?.copy(
                        hostId = payload.newHostId,
                        users = _roomState.value!!.users.map { 
                            it.copy(isHost = it.userId == payload.newHostId)
                        }
                    )
                    if (payload.newHostId == _userId.value) {
                        _role.value = RoomRole.HOST
                    } else if (_role.value == RoomRole.HOST) {
                        
                        _role.value = RoomRole.GUEST
                    }
                    log(LogLevel.INFO, "Host changed", "New host: ${payload.newHostName}")
                    scope.launch { _events.emit(ListenTogetherEvent.HostChanged(payload.newHostId, payload.newHostName)) }
                }
                
                MessageTypes.KICKED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? KickedPayload ?: return
                    log(LogLevel.WARNING, "Kicked from room", payload.reason)
                    releaseWakeLock() 
                    sessionToken = null
                    _roomState.value = null
                    _role.value = RoomRole.NONE
                    scope.launch { _events.emit(ListenTogetherEvent.Kicked(payload.reason)) }
                }
                
                MessageTypes.SYNC_PLAYBACK -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? PlaybackActionPayload ?: return
                    log(LogLevel.DEBUG, "Playback sync", "Action: ${payload.action}")
                    
                    
                    when (payload.action) {
                        PlaybackActions.PLAY -> {
                            _roomState.value = _roomState.value?.copy(
                                isPlaying = true,
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.PAUSE -> {
                            _roomState.value = _roomState.value?.copy(
                                isPlaying = false,
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.SEEK -> {
                            _roomState.value = _roomState.value?.copy(
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.CHANGE_TRACK -> {
                            _roomState.value = _roomState.value?.copy(
                                currentTrack = payload.trackInfo,
                                isPlaying = false,
                                position = 0
                            )
                        }
                        PlaybackActions.QUEUE_ADD -> {
                            val ti = payload.trackInfo
                            if (ti != null) {
                                val currentQueue = _roomState.value?.queue ?: emptyList()
                                _roomState.value = _roomState.value?.copy(
                                    queue = if (payload.insertNext == true) listOf(ti) + currentQueue else currentQueue + ti
                                )
                            }
                        }
                        PlaybackActions.QUEUE_REMOVE -> {
                            val id = payload.trackId
                            if (!id.isNullOrEmpty()) {
                                val currentQueue = _roomState.value?.queue ?: emptyList()
                                _roomState.value = _roomState.value?.copy(
                                    queue = currentQueue.filter { it.id != id }
                                )
                            }
                        }
                        PlaybackActions.QUEUE_CLEAR -> {
                            _roomState.value = _roomState.value?.copy(queue = emptyList())
                        }
                        PlaybackActions.SET_VOLUME -> {
                            val vol = payload.volume
                            if (vol != null) {
                                _roomState.value = _roomState.value?.copy(volume = vol.coerceIn(0f, 1f))
                            }
                        }
                    }
                    
                    scope.launch { _events.emit(ListenTogetherEvent.PlaybackSync(payload)) }
                }
                
                MessageTypes.BUFFER_WAIT -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? BufferWaitPayload ?: return
                    _bufferingUsers.value = payload.waitingFor
                    log(LogLevel.DEBUG, "Waiting for buffering", "Users: ${payload.waitingFor.size}")
                    scope.launch { _events.emit(ListenTogetherEvent.BufferWait(payload.trackId, payload.waitingFor)) }
                }
                
                MessageTypes.BUFFER_COMPLETE -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? BufferCompletePayload ?: return
                    _bufferingUsers.value = emptyList()
                    log(LogLevel.INFO, "All users buffered", "Track: ${payload.trackId}")
                    scope.launch { _events.emit(ListenTogetherEvent.BufferComplete(payload.trackId)) }
                }
                
                MessageTypes.SYNC_STATE -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? SyncStatePayload ?: return
                    log(LogLevel.INFO, "Sync state received", "Playing: ${payload.isPlaying}, Position: ${payload.position}")
                    scope.launch { _events.emit(ListenTogetherEvent.SyncStateReceived(payload)) }
                }
                
                MessageTypes.SUGGESTION_RECEIVED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? SuggestionReceivedPayload ?: return
                    
                    if (_role.value == RoomRole.HOST) {
                        
                        if (isUserBlocked(payload.fromUsername)) {
                            log(LogLevel.INFO, "Suggestion from blocked user ignored", "User: ${payload.fromUsername}")
                            return
                        }

                        _pendingSuggestions.value += payload
                        log(LogLevel.INFO, "Suggestion received", "${payload.fromUsername}: ${payload.trackInfo.title}")
                        
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${payload.fromUsername} suggested: ${payload.trackInfo.title}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            showSuggestionNotification(payload)
                        }
                    }
                }

                MessageTypes.SUGGESTION_APPROVED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? SuggestionApprovedPayload ?: return
                    log(LogLevel.INFO, "Suggestion approved", payload.trackInfo.title)
                    
                    
                    suggestionNotifications.remove(payload.suggestionId)?.let { notifId ->
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                    
                    
                }

                MessageTypes.SUGGESTION_REJECTED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? SuggestionRejectedPayload ?: return
                    log(LogLevel.WARNING, "Suggestion rejected", payload.reason ?: "")
                    
                    
                    suggestionNotifications.remove(payload.suggestionId)?.let { notifId ->
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                    
                    
                }
                
                MessageTypes.ERROR -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? ErrorPayload ?: return
                    log(LogLevel.ERROR, "Server error", "${payload.code}: ${payload.message}")
                    
                    
                    when (payload.code) {
                        "session_not_found" -> {
                            
                            if (storedRoomCode != null && storedUsername != null && !wasHost) {
                                log(LogLevel.WARNING, "Session expired on server", 
                                    "Attempting automatic rejoin to room: $storedRoomCode")
                                
                                scope.launch {
                                    delay(500) 
                                    joinRoom(storedRoomCode!!, storedUsername!!)
                                }
                            } else if (storedRoomCode != null && storedUsername != null) {
                                
                                log(LogLevel.WARNING, "Host session expired", 
                                    "Room: $storedRoomCode - manual intervention may be needed")
                                clearPersistedSession()
                                sessionToken = null
                            } else {
                                clearPersistedSession()
                                sessionToken = null
                            }
                        }
                        else -> {}
                    }
                    
                    scope.launch { _events.emit(ListenTogetherEvent.ServerError(payload.code, payload.message)) }
                }
                
                MessageTypes.PONG -> {
                    if (pingSentTime > 0) {
                        val currentRtt = System.currentTimeMillis() - pingSentTime
                        _rtt.value = currentRtt
                        pingSentTime = 0
                        log(LogLevel.DEBUG, "Pong received", "RTT: ${currentRtt}ms")
                    } else {
                        log(LogLevel.DEBUG, "Pong received")
                    }
                }
                
                MessageTypes.RECONNECTED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? ReconnectedPayload ?: return
                    _userId.value = payload.userId
                    _role.value = if (payload.isHost) RoomRole.HOST else RoomRole.GUEST
                    _roomState.value = payload.state
                    
                    
                    wasHost = payload.isHost
                    sessionStartTime = System.currentTimeMillis()
                    savePersistedSession()
                    
                    
                    reconnectAttempts = 0
                    
                    acquireWakeLock() 
                    log(LogLevel.INFO, "Successfully reconnected to room", 
                        "Code: ${payload.roomCode}, isHost: ${payload.isHost}, attempt was $reconnectAttempts")
                    scope.launch { _events.emit(ListenTogetherEvent.Reconnected(payload.roomCode, payload.userId, payload.state, payload.isHost)) }
                }
                
                MessageTypes.USER_RECONNECTED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? UserReconnectedPayload ?: return
                    
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.map { user ->
                            if (user.userId == payload.userId) user.copy(isConnected = true) else user
                        }
                    )
                    log(LogLevel.INFO, "User reconnected", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserReconnected(payload.userId, payload.username)) }
                }
                
                MessageTypes.USER_DISCONNECTED -> {
                    val payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? UserDisconnectedPayload ?: return
                    
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.map { user ->
                            if (user.userId == payload.userId) user.copy(isConnected = false) else user
                        }
                    )
                    log(LogLevel.INFO, "User temporarily disconnected", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserDisconnected(payload.userId, payload.username)) }
                }

                MessageTypes.CHAT -> {
                    var payload = codec.decodePayload(msgType, payloadBytes, detectedFormat) as? ChatMessagePayload ?: return
                    
                    
                    if (payload.message.startsWith("\u200B[RPLY:")) {
                        try {
                            val endIdx = payload.message.indexOf("]\u200B")
                            if (endIdx != -1) {
                                val encoded = payload.message.substring(7, endIdx)
                                val decoded = String(Base64.decode(encoded, Base64.NO_WRAP))
                                val parts = decoded.split("|", limit = 2)
                                if (parts.size == 2) {
                                    val replyTo = RepliedMessage(parts[0], parts[1])
                                    val actualMessage = payload.message.substring(endIdx + 2)
                                    payload = payload.copy(message = actualMessage, replyTo = replyTo)
                                }
                            }
                        } catch (e: Exception) {
                            log(LogLevel.WARNING, "Failed to decode embedded reply", e.message)
                        }
                    }
                    
                    log(LogLevel.INFO, "Chat message received", "From: ${payload.username}")
                    scope.launch { _events.emit(ListenTogetherEvent.ChatMessageReceived(payload)) }
                }

                else -> {
                    log(LogLevel.WARNING, "Unknown message type", msgType)
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error parsing message", e.message)
        }
    }

    private inline fun <reified T> sendMessage(type: String, payload: T?) {
        try {
            val data = codec.encode(type, payload)
            log(LogLevel.DEBUG, "Sending message", "$type (${codec.format.name})")
            
            val success = webSocket?.send(okio.ByteString.of(*data)) ?: false
            if (!success) {
                log(LogLevel.ERROR, "Failed to send message", type)
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error encoding message", "$type: ${e.message}")
        }
    }
    
    private fun sendMessageNoPayload(type: String) {
        sendMessage<Unit>(type, null)
    }

    

    
    fun createRoom(username: String) {
        
        clearPersistedSession()
        sessionToken = null
        storedRoomCode = null
        wasHost = false
        
        storedUsername = username
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(username))
        } else {
            log(LogLevel.INFO, "Not connected, queueing create room action")
            pendingAction = PendingAction.CreateRoom(username)
            if (_connectionState.value == ConnectionState.DISCONNECTED || 
                _connectionState.value == ConnectionState.ERROR) {
                connect()
            }
            
        }
    }

    
    fun joinRoom(roomCode: String, username: String) {
        
        clearPersistedSession()
        sessionToken = null
        storedRoomCode = null
        wasHost = false

        storedUsername = username
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(roomCode.uppercase(), username))
        } else {
            log(LogLevel.INFO, "Not connected, queueing join room action")
            pendingAction = PendingAction.JoinRoom(roomCode, username)
            if (_connectionState.value == ConnectionState.DISCONNECTED || 
                _connectionState.value == ConnectionState.ERROR) {
                connect()
            }
            
        }
    }

    
    fun leaveRoom() {
        sendMessageNoPayload(MessageTypes.LEAVE_ROOM)
        
        
        sessionToken = null
        storedRoomCode = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = RoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        
        clearPersistedSession()
        
        releaseWakeLock()
    }

    
    fun approveJoin(userId: String) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot approve join", "Not host")
            return
        }
        sendMessage(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(userId))
        
        
        joinRequestNotifications.remove(userId)?.let { notifId ->
            NotificationManagerCompat.from(context).cancel(notifId)
        }
    }

    
    fun rejectJoin(userId: String, reason: String? = null) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot reject join", "Not host")
            return
        }
        sendMessage(MessageTypes.REJECT_JOIN, RejectJoinPayload(userId, reason))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
        
        
        joinRequestNotifications.remove(userId)?.let { notifId ->
            NotificationManagerCompat.from(context).cancel(notifId)
        }
    }

    
    fun kickUser(userId: String, reason: String? = null) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot kick user", "Not host")
            return
        }
        sendMessage(MessageTypes.KICK_USER, KickUserPayload(userId, reason))
    }

    
    fun transferHost(newHostId: String) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot transfer host", "Not host")
            return
        }
        sendMessage(MessageTypes.TRANSFER_HOST, TransferHostPayload(newHostId))
    }

    
    fun sendPlaybackAction(
        action: String, 
        trackId: String? = null, 
        position: Long? = null, 
        trackInfo: TrackInfo? = null, 
        insertNext: Boolean? = null, 
        queue: List<TrackInfo>? = null,
        queueTitle: String? = null,
        volume: Float? = null
    ) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot control playback", "Not host")
            return
        }
        sendMessage(
            MessageTypes.PLAYBACK_ACTION,
            PlaybackActionPayload(action, trackId, position, trackInfo, insertNext, queue, queueTitle, volume)
        )
    }

    
    fun sendChatMessage(message: String, replyTo: RepliedMessage? = null) {
        if (!isInRoom) {
            log(LogLevel.ERROR, "Cannot send chat message", "Not in room")
            return
        }
        
        
        val finalMessage = if (replyTo != null) {
            val metadata = "${replyTo.username}|${replyTo.message}"
            val encoded = Base64.encodeToString(metadata.toByteArray(), Base64.NO_WRAP)
            "\u200B[RPLY:$encoded]\u200B$message"
        } else {
            message
        }
        
        sendMessage(MessageTypes.CHAT, ChatPayload(finalMessage, replyTo))
    }

    
    fun sendBufferReady(trackId: String) {
        sendMessage(MessageTypes.BUFFER_READY, BufferReadyPayload(trackId))
    }

    
    fun suggestTrack(trackInfo: TrackInfo) {
        if (!isInRoom) {
            log(LogLevel.ERROR, "Cannot suggest track", "Not in room")
            return
        }
        if (_role.value == RoomRole.HOST) {
            log(LogLevel.WARNING, "Host should not suggest tracks")
            return
        }
        sendMessage(MessageTypes.SUGGEST_TRACK, SuggestTrackPayload(trackInfo))
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.listen_together_suggestion_sent), Toast.LENGTH_SHORT).show()
        }
    }

    
    fun approveSuggestion(suggestionId: String) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot approve suggestion", "Not host")
            return
        }
        
        
        val suggestion = _pendingSuggestions.value.find { it.suggestionId == suggestionId }
        
        sendMessage(MessageTypes.APPROVE_SUGGESTION, ApproveSuggestionPayload(suggestionId))
        
        
        if (suggestion != null) {
            scope.launch { _events.emit(ListenTogetherEvent.LocalSuggestionApproved(suggestion)) }
        }
        
        
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
        
        
        suggestionNotifications.remove(suggestionId)?.let { notifId ->
            NotificationManagerCompat.from(context).cancel(notifId)
        }
    }

    
    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot reject suggestion", "Not host")
            return
        }
        sendMessage(MessageTypes.REJECT_SUGGESTION, RejectSuggestionPayload(suggestionId, reason))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
        
        
        suggestionNotifications.remove(suggestionId)?.let { notifId ->
            NotificationManagerCompat.from(context).cancel(notifId)
        }
    }

    
    fun requestSync() {
        if (_roomState.value == null) {
            log(LogLevel.ERROR, "Cannot request sync", "Not in room")
            return
        }
        log(LogLevel.INFO, "Requesting sync state from server")
        sendMessageNoPayload(MessageTypes.REQUEST_SYNC)
    }

    
    fun blockUser(username: String) {
        val updated = _blockedUsernames.value.toMutableSet()
        updated.add(username)
        _blockedUsernames.value = updated
        
        
        _pendingJoinRequests.value = _pendingJoinRequests.value
            .filter { it.username !in _blockedUsernames.value }
        _pendingSuggestions.value = _pendingSuggestions.value
            .filter { it.fromUsername !in _blockedUsernames.value }
        
        
        scope.launch {
            saveBlockedUsernames()
        }
        
        log(LogLevel.INFO, "User blocked", username)
    }

    
    fun unblockUser(username: String) {
        val updated = _blockedUsernames.value.toMutableSet()
        updated.remove(username)
        _blockedUsernames.value = updated
        
        
        scope.launch {
            saveBlockedUsernames()
        }
        
        log(LogLevel.INFO, "User unblocked", username)
    }

    
    fun isUserBlocked(username: String): Boolean = username in _blockedUsernames.value

    
    val isInRoom: Boolean
        get() = _roomState.value != null

    
    val isHost: Boolean
        get() = _role.value == RoomRole.HOST
    
    
    fun forceReconnect() {
        log(LogLevel.INFO, "Forcing reconnection to server")
        reconnectAttempts = 0  
        
        if (webSocket != null) {
            try {
                webSocket?.close(1000, "Forcing reconnection")
            } catch (e: Exception) {
                log(LogLevel.DEBUG, "Error closing WebSocket", e.message)
            }
            webSocket = null
        }
        
        _connectionState.value = ConnectionState.DISCONNECTED
        
        
        scope.launch {
            delay(500)
            connect()
        }
    }
    
    
    val hasPersistedSession: Boolean
        get() = sessionToken != null && storedRoomCode != null
    
    
    fun getPersistedRoomCode(): String? = storedRoomCode
    
    
    fun getSessionAge(): Long = if (sessionStartTime > 0) {
        System.currentTimeMillis() - sessionStartTime
    } else {
        0L
    }
}
