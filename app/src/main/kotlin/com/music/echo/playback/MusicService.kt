

@file:Suppress("DEPRECATION")

package iad1tya.echo.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import iad1tya.echo.music.MainActivity
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.AudioNormalizationKey
import iad1tya.echo.music.constants.AudioOffload
import iad1tya.echo.music.constants.AudioQualityKey
import iad1tya.echo.music.constants.AutoDownloadOnLikeKey
import iad1tya.echo.music.constants.AutoLoadMoreKey
import iad1tya.echo.music.constants.AutoSkipNextOnErrorKey
import iad1tya.echo.music.constants.AutomixCrossfadeKey
import iad1tya.echo.music.constants.CrossfadeDurationKey
import iad1tya.echo.music.constants.CrossfadeEnabledKey
import iad1tya.echo.music.constants.CrossfadeGaplessKey
import iad1tya.echo.music.constants.DisableLoadMoreWhenRepeatAllKey
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import iad1tya.echo.music.constants.DiscordActivityNameKey
import iad1tya.echo.music.constants.DiscordActivityTypeKey
import iad1tya.echo.music.constants.DiscordTokenKey
import iad1tya.echo.music.constants.EnableDiscordRPCKey
import iad1tya.echo.music.constants.EnableLastFMScrobblingKey
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.HideVideoSongsKey
import iad1tya.echo.music.constants.HistoryDuration
import iad1tya.echo.music.constants.LastFMSessionKey
import iad1tya.echo.music.constants.LastFMUseNowPlaying
import iad1tya.echo.music.constants.LastFMUseSendLikes
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleLike
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleRepeatMode
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleShuffle
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleStartRadio
import iad1tya.echo.music.constants.PauseListenHistoryKey
import iad1tya.echo.music.constants.PauseOnMute
import iad1tya.echo.music.constants.PersistentQueueKey
import iad1tya.echo.music.constants.PersistentShuffleAcrossQueuesKey

import iad1tya.echo.music.constants.RememberShuffleAndRepeatKey
import iad1tya.echo.music.constants.RepeatModeKey
import iad1tya.echo.music.constants.ResumeOnBluetoothConnectKey
import iad1tya.echo.music.constants.ScrobbleDelayPercentKey
import iad1tya.echo.music.constants.ScrobbleDelaySecondsKey
import iad1tya.echo.music.constants.ScrobbleMinSongDurationKey
import iad1tya.echo.music.constants.ShowLyricsKey
import iad1tya.echo.music.constants.ShuffleModeKey
import iad1tya.echo.music.constants.ShufflePlaylistFirstKey
import iad1tya.echo.music.constants.PreloadLyricsEnabledKey
import iad1tya.echo.music.constants.PreloadNextSongEnabledKey
import iad1tya.echo.music.constants.PreloadNextSongLimitKey
import iad1tya.echo.music.constants.PreventDuplicateTracksInQueueKey
import iad1tya.echo.music.constants.SimilarContent
import iad1tya.echo.music.constants.SkipSilenceInstantKey
import iad1tya.echo.music.constants.SkipSilenceKey
import iad1tya.echo.music.constants.IpVersionKey
import com.music.innertube.models.IpVersion
import okhttp3.Dns
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.Event
import iad1tya.echo.music.db.entities.FormatEntity
import iad1tya.echo.music.db.entities.LyricsEntity
import iad1tya.echo.music.db.entities.RelatedSongMap
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.di.DownloadCache
import iad1tya.echo.music.di.PlayerCache
import iad1tya.echo.music.eq.EqualizerService
import iad1tya.echo.music.eq.audio.CustomEqualizerAudioProcessor
import iad1tya.echo.music.eq.data.EQProfileRepository
import iad1tya.echo.music.extensions.SilentHandler
import iad1tya.echo.music.extensions.collect
import iad1tya.echo.music.extensions.collectLatest
import iad1tya.echo.music.extensions.currentMetadata
import iad1tya.echo.music.extensions.findNextMediaItemById
import iad1tya.echo.music.extensions.mediaItems
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.extensions.setOffloadEnabled
import iad1tya.echo.music.extensions.toEnum
import iad1tya.echo.music.extensions.toMediaItem
import iad1tya.echo.music.extensions.toPersistQueue
import iad1tya.echo.music.extensions.toQueue
import iad1tya.echo.music.echomusic.updater.downloadmanager.EchoNotificationProvider
import iad1tya.echo.music.lyrics.LyricsHelper
import iad1tya.echo.music.models.PersistPlayerState
import iad1tya.echo.music.models.PersistQueue
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.db.entities.BeatInfoEntity
import iad1tya.echo.music.playback.audio.BeatAnalyzer
import iad1tya.echo.music.playback.audio.SilenceDetectorAudioProcessor
import iad1tya.echo.music.playback.queues.EmptyQueue
import iad1tya.echo.music.playback.queues.Queue
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.playback.queues.filterExplicit
import iad1tya.echo.music.playback.queues.filterVideoSongs
import iad1tya.echo.music.utils.CoilBitmapLoader
import iad1tya.echo.music.ui.screens.settings.DiscordPresenceManager
import iad1tya.echo.music.utils.NetworkConnectivityObserver
import iad1tya.echo.music.utils.ScrobbleManager
import iad1tya.echo.music.utils.SyncUtils
import iad1tya.echo.music.utils.YTPlayerUtils
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.reportException
import iad1tya.echo.music.widget.EchoMusicWidgetManager
import iad1tya.echo.music.widget.MusicWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import iad1tya.echo.music.utils.isLocalMediaId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

private const val INSTANT_SILENCE_SKIP_STEP_MS = 15_000L
private const val INSTANT_SILENCE_SKIP_SETTLE_MS = 350L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var equalizerService: EqualizerService

    @Inject
    lateinit var eqProfileRepository: EQProfileRepository

    @Inject
    lateinit var widgetManager: EchoMusicWidgetManager

    @Inject
    lateinit var listenTogetherManager: iad1tya.echo.music.listentogether.ListenTogetherManager
    

    private lateinit var audioManager: AudioManager
    // Wi-Fi Lock: Prevents modern Wi-Fi 6/7 routers from putting the Wi-Fi chip into
    // low-power sleep mode while music is actively streaming in the background.
    // Without this, the router's power-saving protocol (Target Wake Time) causes
    // packet delays, leading to audio buffering or playback stopping after the screen turns off.
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false
    private var reentrantFocusGain = false
    private var wasPlayingBeforeVolumeMute = false
    private var isPausedByVolumeMute = false
    var preferredDeviceId: Int? = null 
        private set

    private var crossfadeEnabled = false
    private var crossfadeDuration = 5000f
    private var crossfadeGapless = true
    private var crossfadeTriggerJob: Job? = null

    private var automixEnabled = false
    private var activeAutomixPlan: AutomixPlan? = null
    private var automixBaseParams: PlaybackParameters = PlaybackParameters.DEFAULT
    private val analysisDataSourceFactory by lazy { createDataSourceFactory() }
    private val beatAnalysisJobs = java.util.Collections.synchronizedMap(mutableMapOf<String, BeatAnalysisHandle>())
    private val immediateBeatAnalysisMutex = kotlinx.coroutines.sync.Mutex()
    private val lookaheadBeatAnalysisMutex = kotlinx.coroutines.sync.Mutex()

    private enum class BeatAnalysisPriority {
        IMMEDIATE,
        LOOKAHEAD,
    }

    private data class BeatAnalysisHandle(
        val priority: BeatAnalysisPriority,
        val job: Job,
    )

    private fun beatAnalysisTimeoutMs(priority: BeatAnalysisPriority): Long =
        when (priority) {
            BeatAnalysisPriority.IMMEDIATE -> 45_000L
            BeatAnalysisPriority.LOOKAHEAD -> 25_000L
        }

    private data class AutomixPair(
        val currentId: String,
        val nextId: String,
    )

    /** Beat-aligned transition computed from cached BeatInfoEntity of both tracks. */
    private data class AutomixPlan(
        val currentId: String,
        val nextId: String,
        val triggerTimeMs: Long,
        val incomingStartMs: Long,
        val tempoRatio: Float,
        /** DJ blend length: 16 beats of the outgoing track, clamped to sane bounds. */
        val overlapMs: Long,
    )

    private data class AutomixPlanResult(
        val plan: AutomixPlan?,
        val pairAnalyzed: Boolean,
    )

    /** Live state of the automix engine, surfaced in the player debug overlay. */
    data class AutomixDebugInfo(
        val status: String,
        val outBpm: Float? = null,
        val outConfidence: Float? = null,
        val outMixOutMs: Long? = null,
        val inBpm: Float? = null,
        val inConfidence: Float? = null,
        val inMixInMs: Long? = null,
        val triggerTimeMs: Long? = null,
        val incomingStartMs: Long? = null,
        val tempoRatio: Float? = null,
    )

    val automixDebugInfo = MutableStateFlow<AutomixDebugInfo?>(null)

    private val secondaryPlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Timber.tag(TAG).e(error, "Secondary player error")
            secondaryPlayer?.stop()
            secondaryPlayer?.clearMediaItems()
            secondaryPlayer = null
        }
    }

    private var scope = CoroutineScope(Dispatchers.Main) + Job()

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private lateinit var audioQuality: iad1tya.echo.music.constants.AudioQuality
    private lateinit var ipVersion: IpVersion

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<iad1tya.echo.music.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    lateinit var playerVolume: MutableStateFlow<Float>
    val isMuted = MutableStateFlow(false)

    fun toggleMute() {
        val newMutedState = !isMuted.value
        isMuted.value = newMutedState
        
        player.volume = if (newMutedState) 0f else playerVolume.value
    }

    fun setMuted(muted: Boolean) {
        isMuted.value = muted
        
        
        player.volume = if (muted) 0f else playerVolume.value
    }

    fun setPreferredAudioDevice(deviceId: Int?) { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val deviceInfo = devices.find { it.id == deviceId }
            player.setPreferredAudioDevice(deviceInfo)
            preferredDeviceId = deviceId
        }
    }


    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
        private set
    private var secondaryPlayer: ExoPlayer? = null
    private var fadingPlayer: ExoPlayer? = null
    val isCrossfading = MutableStateFlow(false)
    val isAutomixing = MutableStateFlow(false)
    private var crossfadeJob: Job? = null

    private lateinit var mediaSession: MediaLibrarySession

    
    private val playerInitialized = MutableStateFlow(false)
    val isPlayerReady: kotlinx.coroutines.flow.StateFlow<Boolean> = playerInitialized.asStateFlow()

    
    private val _playerFlow = MutableStateFlow<ExoPlayer?>(null)
    val playerFlow = _playerFlow.asStateFlow()

    private val playerSilenceProcessors = HashMap<Player, SilenceDetectorAudioProcessor>()


    private val instantSilenceSkipEnabled = MutableStateFlow(false)

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var lastPresenceToken: String? = null


    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: kotlinx.coroutines.Job? = null

    private var scrobbleManager: ScrobbleManager? = null

    private var listenBrainzEnabled = false
    private var listenBrainzToken = ""
    private var listenBrainzCurrentStartTs: Long = 0L
    private var listenBrainzCurrentMediaId: String? = null

    // Cached playback preferences kept in sync with DataStore.
    private var cachedRepeatMode: Int = REPEAT_MODE_OFF
    private var cachedShuffleEnabled: Boolean = false
    private var cachedPreloadEnabled: Boolean = true
    private var cachedPreloadLimit: Int = 1
    private var cachedPreloadLyrics: Boolean = true

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    
    private var originalQueueSize: Int = 0

    private var consecutivePlaybackErr = 0
    private var retryJob: Job? = null
    private var retryCount = 0
    private var silenceSkipJob: Job? = null

    
    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    
    private val bypassCacheForQualityChange = mutableSetOf<String>()

    
    private var currentMediaIdRetryCount = mutableMapOf<String, Int>()
    private val MAX_RETRY_PER_SONG = 3
    private val RETRY_DELAY_MS = 1000L

    
    private val recentlyFailedSongs = mutableSetOf<String>()
    private var failedSongsClearJob: Job? = null

    
    var castConnectionHandler: CastConnectionHandler? = null
        private set

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (!player.isPlaying) {
                        scope.launch(Dispatchers.IO) {
                            DiscordPresenceManager.stop()
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (player.isPlaying) {
                        scope.launch {
                            currentSong.value?.let { song ->
                                ensurePresenceManager()
                            }
                        }
                    }
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesAdded(addedDevices)
            val hasBluetooth = addedDevices?.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } == true

            if (hasBluetooth) {
                if (dataStore.get(ResumeOnBluetoothConnectKey, false)) {
                    if (player.playbackState == Player.STATE_READY && !player.isPlaying) {
                        player.play()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        
        // Workaround for ForegroundServiceStartNotAllowedException
        setListener(object : Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                Timber.tag(TAG).e("ForegroundServiceStartNotAllowedException caught by MediaSessionService listener")
                reportException(Exception("ForegroundServiceStartNotAllowedException caught by MediaSessionService listener"))
            }
        })
        
        playerInitialized.value = false

        scrobbleManager = ScrobbleManager(scope)

        scope.launch {
            dataStore.data.map { it[EnableLastFMScrobblingKey] ?: false }.distinctUntilChanged().collect {
                scrobbleManager?.enableScrobbling = it
            }
        }
        scope.launch {
            dataStore.data.map { it[LastFMUseNowPlaying] ?: false }.distinctUntilChanged().collect {
                scrobbleManager?.useNowPlaying = it
            }
        }
        scope.launch {
            dataStore.data.map { it[LastFMUseSendLikes] ?: false }.distinctUntilChanged().collect {
                scrobbleManager?.useSendLikes = it
            }
        }
        scope.launch {
            dataStore.data.map { it[LastFMSessionKey] }.distinctUntilChanged().collect { sessionKey ->
                com.music.echo.utils.lastfm.LastFM.sessionKey = sessionKey
            }
        }        
        

        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.music_player),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            val pending = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.music_player))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher_nobg)  
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
            safeStartForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to create foreground notification")
            reportException(e)
        }

        setMediaNotificationProvider(
            EchoNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.ic_launcher_nobg)
                },
        )
        player = createExoPlayer()
        player.addListener(this@MusicService)
        sleepTimer = SleepTimer(scope, player)
        player.addListener(sleepTimer)
        playerInitialized.value = true
        Timber.tag(TAG).d("Player successfully initialized")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        abandonAudioFocus()
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        
        if (dataStore.get(RememberShuffleAndRepeatKey, true)) {
            player.shuffleModeEnabled = dataStore.get(ShuffleModeKey, false)
        }

        
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        audioQuality = dataStore.get(AudioQualityKey).toEnum(iad1tya.echo.music.constants.AudioQuality.OPUS)
        ipVersion = dataStore.get(IpVersionKey).toEnum(IpVersion.IPV4)
        playerVolume = MutableStateFlow(1f)

        
        initializeCast()

        
        scope.launch {
            eqProfileRepository.activeProfile.collect { profile ->
                if (profile != null) {
                    val result = equalizerService.applyProfile(profile)
                    if (result.isSuccess && player.playbackState == Player.STATE_READY && player.isPlaying) {
                        
                        
                        
                        player.seekTo(player.currentPosition)
                    }
                } else {
                    equalizerService.disable()
                    if (player.playbackState == Player.STATE_READY && player.isPlaying) {
                        player.seekTo(player.currentPosition)
                    }
                }
            }
        }

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    triggerRetry()
                }
                
                if (isConnected && player.isPlaying) {
                    val mediaId = player.currentMetadata?.id
                    if (mediaId != null) {
                        database.song(mediaId).first()?.let { song ->
                            ensurePresenceManager()
                        }
                    }
                }
            }
        }

        
        scope.launch {
            dataStore.data
                .map { it[iad1tya.echo.music.constants.ListenBrainzEnabledKey] ?: false }
                .distinctUntilChanged()
                .collect { listenBrainzEnabled = it }
        }

        scope.launch {
            dataStore.data
                .map { it[iad1tya.echo.music.constants.ListenBrainzTokenKey] ?: "" }
                .distinctUntilChanged()
                .collect { listenBrainzToken = it }
        }

        var isFirstQualityEmit = true
        scope.launch {
            dataStore.data
                .map { it[AudioQualityKey]?.let { value ->
                    iad1tya.echo.music.constants.AudioQuality.entries.find { it.name == value }
                } ?: iad1tya.echo.music.constants.AudioQuality.OPUS }
                .distinctUntilChanged()
                .collect { newQuality ->
                    val oldQuality = audioQuality
                    audioQuality = newQuality

                    
                    if (isFirstQualityEmit) {
                        isFirstQualityEmit = false
                        Timber.tag("MusicService").i("QUALITY INIT: $newQuality")
                        return@collect
                    }

                    Timber.tag("MusicService").i("QUALITY CHANGED: $oldQuality -> $newQuality")

                    Timber.tag("MusicService").i("QUALITY CHANGED: $oldQuality -> $newQuality. Will take effect starting from the next song.")

                    // Clear cache for upcoming songs so they fetch the new quality, keeping the currently playing track's URL cache entry intact.
                    val currentMediaId = player.currentMediaItem?.mediaId
                    val currentCachedEntry = currentMediaId?.let { mediaId ->
                        songUrlCache.filter { it.key.startsWith("${mediaId}_") }
                    }
                    songUrlCache.clear()
                    if (currentCachedEntry != null) {
                        songUrlCache.putAll(currentCachedEntry)
                    }

                    // Re-trigger prefetch to fetch the next songs in the new quality
                    preloadUpcomingItems()
                }
        }

        
        scope.launch {
            dataStore.data
                .map { it[IpVersionKey]?.toEnum(IpVersion.IPV4) ?: IpVersion.IPV4 }
                .distinctUntilChanged()
                .collect { newIpVersion ->
                    val oldIpVersion = ipVersion
                    ipVersion = newIpVersion

                    if (isFirstQualityEmit) return@collect

                    Timber.tag("MusicService").i("IP VERSION CHANGED: $oldIpVersion -> $newIpVersion")

                    
                    val mediaId = player.currentMediaItem?.mediaId ?: return@collect
                    val currentPosition = player.currentPosition
                    val currentIndex = player.currentMediaItemIndex
                    val wasPlaying = player.isPlaying

                    
                    songUrlCache.remove("${mediaId}_${audioQuality.name}")

                    
                    player.stop()
                    player.seekTo(currentIndex, currentPosition)
                    player.prepare()
                    if (wasPlaying) {
                        player.play()
                    }
                }
        }

        combine(playerVolume, isMuted) { volume, muted ->
            if (muted) 0f else volume
        }.collectLatest(scope) {
            player.volume = it
        }



        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            updateWidgetUI(player.isPlaying)
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyricsWithProvider.lyrics,
                            provider = lyricsWithProvider.provider,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { (it[SkipSilenceKey] ?: false) to (it[SkipSilenceInstantKey] ?: false) }
            .distinctUntilChanged()
            .collectLatest(scope) { (skipSilence, instantSkip) ->
                player.skipSilenceEnabled = skipSilence
                secondaryPlayer?.skipSilenceEnabled = skipSilence

                val enableInstant = skipSilence && instantSkip
                instantSilenceSkipEnabled.value = enableInstant

                playerSilenceProcessors.values.forEach { processor ->
                    processor.instantModeEnabled = enableInstant
                    if (!enableInstant) {
                        processor.resetTracking()
                    }
                }

                if (!enableInstant) {
                    silenceSkipJob?.cancel()
                }
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) -> setupLoudnessEnhancer()}

        combine(
            dataStore.data.map { it[AudioOffload] ?: false },
            dataStore.data.map { it[CrossfadeEnabledKey] ?: false }
        ) { offloadPref, crossfadeEnabled ->
             
             if (crossfadeEnabled) false else offloadPref
        }.distinctUntilChanged()
        .collectLatest(scope) { useOffload ->
             player.setOffloadEnabled(useOffload)
             secondaryPlayer?.setOffloadEnabled(useOffload)
        }



        combine(
            dataStore.data.map { prefs ->
                Triple(
                    prefs[CrossfadeEnabledKey] ?: false,
                    prefs[CrossfadeDurationKey] ?: 5f,
                    prefs[CrossfadeGaplessKey] ?: true
                )
            },
            listenTogetherManager.roomState
        ) { (enabled, duration, gapless), roomState ->
            
            Triple(enabled && roomState == null, duration, gapless)
        }
            .distinctUntilChanged()
            .collect(scope) { (enabled, duration, gapless) ->
                crossfadeEnabled = enabled
                crossfadeDuration = duration * 1000f
                crossfadeGapless = gapless
                if (enabled) {
                    prepareAutomixForCurrentPair()
                    scheduleCrossfade()
                } else {
                    crossfadeTriggerJob?.cancel()
                    crossfadeTriggerJob = null
                    automixDebugInfo.value = null
                }
            }

        dataStore.data
            .map { it[AutomixCrossfadeKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) {
                automixEnabled = it
                if (it) {
                    prepareAutomixForCurrentPair()
                    scheduleCrossfade()
                } else {
                    crossfadeTriggerJob?.cancel()
                    crossfadeTriggerJob = null
                    activeAutomixPlan = null
                    automixDebugInfo.value = null
                    scheduleCrossfade()
                }
            }

        // Keep cached preferences in sync so Player.Listener callbacks can read
        // them without blocking the main thread.
        dataStore.data
            .map { it[RepeatModeKey] ?: REPEAT_MODE_OFF }
            .distinctUntilChanged()
            .collect(scope) { cachedRepeatMode = it }

        dataStore.data
            .map { it[ShuffleModeKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) { cachedShuffleEnabled = it }

        dataStore.data
            .map { it[PreloadNextSongEnabledKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { cachedPreloadEnabled = it }

        dataStore.data
            .map { it[PreloadNextSongLimitKey] ?: 1 }
            .distinctUntilChanged()
            .collect(scope) { cachedPreloadLimit = it }

        dataStore.data
            .map { it[PreloadLyricsEnabledKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { cachedPreloadLyrics = it }


        if (dataStore.get(PersistentQueueKey, true)) {
            val queueFile = filesDir.resolve(PERSISTENT_QUEUE_FILE)
            if (queueFile.exists()) {
                runCatching {
                    queueFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    runCatching {
                        
                        val restoredQueue = queue.toQueue()
                        
                        scope.launch {
                            playerInitialized.first { it }
                            if (isActive) {
                                playQueue(
                                    queue = restoredQueue,
                                    playWhenReady = false,
                                )
                            }
                        }
                    }.onFailure { error ->
                        Timber.tag(TAG).w(error, "Failed to restore persisted queue, clearing data")
                        clearPersistedQueueFiles()
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read persisted queue, clearing data")
                    clearPersistedQueueFiles()
                }
            }

            val automixFile = filesDir.resolve(PERSISTENT_AUTOMIX_FILE)
            if (automixFile.exists()) {
                runCatching {
                    automixFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    runCatching {
                        automixItems.value = queue.items.map { it.toMediaItem() }
                    }.onFailure { error ->
                        Timber.tag(TAG).w(error, "Failed to restore automix queue, clearing data")
                        clearPersistedQueueFiles()
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read automix queue, clearing data")
                    clearPersistedQueueFiles()
                }
            }

            
            val playerStateFile = filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE)
            if (playerStateFile.exists()) {
                runCatching {
                    playerStateFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistPlayerState
                        }
                    }
                }.onSuccess { playerState ->
                    
                    scope.launch {
                        delay(1000) 
                        
                        
                        

                        
                        if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                            player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                        }
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read player state, clearing data")
                    clearPersistedQueueFiles()
                }
            }
        }

        
        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
    }

    private fun createExoPlayer(): ExoPlayer {
        val eqProcessor = CustomEqualizerAudioProcessor()
        equalizerService.addAudioProcessor(eqProcessor)

        val silenceProcessor = SilenceDetectorAudioProcessor { handleLongSilenceDetected() }

        
        runBlocking {
            val skipSilence = dataStore.get(SkipSilenceKey, false)
            val instantSkip = dataStore.get(SkipSilenceInstantKey, false)
            silenceProcessor.instantModeEnabled = skipSilence && instantSkip
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRenderersFactory(eqProcessor, silenceProcessor))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(50_000, 50_000, 750, 2_000)
                    .build()
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false,
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .setDeviceVolumeControlEnabled(true)
            .build()

        playerSilenceProcessors[player] = silenceProcessor

        player.apply {
                runBlocking {
                    val offload = dataStore.get(AudioOffload, false)
                    val crossfade = dataStore.get(CrossfadeEnabledKey, false)
                    setOffloadEnabled(if (crossfade) false else offload)
                    skipSilenceEnabled = dataStore.get(SkipSilenceKey, false)
                }
                addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))

                
            }
        _playerFlow.value = player
        return player
    }

    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {

            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss && !player.isPlaying && !reentrantFocusGain) {
                    reentrantFocusGain = true
                    scope.launch {
                        delay(300)
                        if (hasAudioFocus && wasPlayingBeforeAudioFocusLoss && !player.isPlaying) {
                            
                            if (castConnectionHandler?.isCasting?.value != true) {
                                player.play()
                            }
                            wasPlayingBeforeAudioFocusLoss = false
                        }
                        reentrantFocusGain = false
                    }
                }

                player.volume = if (isMuted.value) 0f else playerVolume.value
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.volume = if (isMuted.value) 0f else (playerVolume.value * 0.2f)
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                player.volume = if (isMuted.value) 0f else playerVolume.value
                lastAudioFocusState = focusChange
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    /**
     * Acquires a high-performance Wi-Fi lock when playback starts.
     *
     * WIFI_MODE_FULL_HIGH_PERF tells the system to keep the Wi-Fi chip fully
     * active with minimal latency — disabling power-saving sleep cycles.
     * This is called every time [player.isPlaying] becomes true.
     */
    private fun acquireWifiLock() {
        if (wifiLock == null) {
            val wifiManager = applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
            wifiLock = wifiManager?.createWifiLock(
                android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "echo_music:wifi_lock"
            )
        }
        if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
            Timber.tag(TAG).d("Wi-Fi lock acquired")
        }
    }

    /**
     * Releases the Wi-Fi lock when playback is paused, stopped, or the service is destroyed.
     *
     * Releasing the lock allows the device to return to normal Wi-Fi power-saving
     * behaviour, preserving battery when music is not playing.
     */
    private fun releaseWifiLock() {
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
            Timber.tag(TAG).d("Wi-Fi lock released")
        }
    }

    private fun clearPersistedQueueFiles() {
        runCatching { filesDir.resolve(PERSISTENT_QUEUE_FILE).delete() }
        runCatching { filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete() }
        runCatching { filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete() }
    }

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }

    private fun waitOnNetworkError() {
        if (waitingForNetworkConnection.value) return

        
        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.tag(TAG).w("Max retry count ($MAX_RETRY_COUNT) reached, stopping playback")
            stopOnError()
            retryCount = 0
            return
        }

        waitingForNetworkConnection.value = true

        
        retryJob?.cancel()
        retryJob = scope.launch {
            
            val delayMs = minOf(3000L * (1 shl retryCount), 30000L)
            Timber.tag(TAG).d("Waiting ${delayMs}ms before retry attempt ${retryCount + 1}/$MAX_RETRY_COUNT")
            delay(delayMs)

            if (isNetworkConnected.value && waitingForNetworkConnection.value) {
                retryCount++
                triggerRetry()
            }
        }
    }

    private fun triggerRetry() {
        waitingForNetworkConnection.value = false
        retryJob?.cancel()

        if (player.currentMediaItem != null) {
            
            
            if (retryCount > 3) {
                Timber.tag(TAG).d("Retry count > 3, attempting to refresh stream URL")
                val currentPosition = player.currentPosition
                player.seekTo(player.currentMediaItemIndex, currentPosition)
            }
            player.prepare()
            
            
        }
    }

    private fun skipOnError() {
        
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            
            if (castConnectionHandler?.isCasting?.value != true) {
                player.play()
            }
            return
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked ==
                                true
                            ) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.ic_heart else R.drawable.ic_heart_outline)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else {
                var updatedSong = song.song
                if (song.song.duration == -1) {
                    updatedSong = updatedSong.copy(duration = duration)
                }
                
                if (song.song.isVideo != mediaMetadata.isVideoSong) {
                    updatedSong = updatedSong.copy(isVideo = mediaMetadata.isVideoSong)
                }
                if (updatedSong != song.song) {
                    update(updatedSong)
                }
            }
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()

        
        if (!playerInitialized.value) {
            Timber.tag(TAG).w("playQueue called before player initialization, queuing request")
            scope.launch {
                playerInitialized.first { it }
                playQueue(queue, playWhenReady)
            }
            return
        }

        currentQueue = queue
        queueTitle = null
        val persistShuffleAcrossQueues = dataStore.get(PersistentShuffleAcrossQueuesKey, false)
        val previousShuffleEnabled = player.shuffleModeEnabled
        if (!persistShuffleAcrossQueues) {
            player.shuffleModeEnabled = false
        }
        
        originalQueueSize = 0
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue.getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            
            originalQueueSize = initialStatus.items.size
            if (queue.preloadItem != null) {
                val safeIndex = initialStatus.mediaItemIndex.coerceIn(0, (initialStatus.items.size - 1).coerceAtLeast(0))
                player.addMediaItems(
                    0,
                    initialStatus.items.subList(0, safeIndex)
                )
                player.addMediaItems(
                    initialStatus.items.subList(
                        (safeIndex + 1).coerceAtMost(initialStatus.items.size),
                        initialStatus.items.size
                    )
                )
            } else {
                val safeIndex = initialStatus.mediaItemIndex.coerceIn(0, (initialStatus.items.size - 1).coerceAtLeast(0))
                player.setMediaItems(
                    initialStatus.items,
                    safeIndex,
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }

            
            if (player.shuffleModeEnabled) {
                val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
            }
        }
    }

    fun startRadioSeamlessly() {
        
        if (!playerInitialized.value) {
            Timber.tag(TAG).w("startRadioSeamlessly called before player initialization")
            return
        }

        val currentMediaMetadata = player.currentMetadata ?: return

        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMediaMetadata.id

        scope.launch(SilentHandler) {
            
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(
                    videoId = currentMediaId
                )
            )

            try {
                val initialStatus = withContext(Dispatchers.IO) {
                    radioQueue.getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }

                if (initialStatus.title != null) {
                    queueTitle = initialStatus.title
                }

                
                val radioItems = initialStatus.items.filter { item ->
                    item.mediaId != currentMediaId
                }

                if (radioItems.isNotEmpty()) {
                    val itemCount = player.mediaItemCount

                    if (itemCount > currentIndex + 1) {
                        player.removeMediaItems(currentIndex + 1, itemCount)
                    }

                    player.addMediaItems(currentIndex + 1, radioItems)
                    if (player.shuffleModeEnabled) {
                        val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                        applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                    }
                }

                currentQueue = radioQueue
            } catch (e: Exception) {
                
                try {
                    val nextResult = withContext(Dispatchers.IO) {
                        YouTube.next(WatchEndpoint(videoId = currentMediaId)).getOrNull()
                    }
                    nextResult?.relatedEndpoint?.let { relatedEndpoint ->
                        val relatedPage = withContext(Dispatchers.IO) {
                            YouTube.related(relatedEndpoint).getOrNull()
                        }
                        relatedPage?.songs?.let { songs ->
                            val radioItems = songs
                                .filter { it.id != currentMediaId }
                                .map { it.toMediaItem() }
                                .filterExplicit(dataStore.get(HideExplicitKey, false))
                                .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))

                            if (radioItems.isNotEmpty()) {
                                val itemCount = player.mediaItemCount
                                if (itemCount > currentIndex + 1) {
                                    player.removeMediaItems(currentIndex + 1, itemCount)
                                }
                                player.addMediaItems(currentIndex + 1, radioItems)
                                if (player.shuffleModeEnabled) {
                                    val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                                    applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    
                }
            }
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore.get(SimilarContent, true) &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                try {
                    
                    YouTube.next(WatchEndpoint(playlistId = playlistId))
                        .onSuccess { firstResult ->
                            YouTube.next(WatchEndpoint(playlistId = firstResult.endpoint.playlistId))
                                .onSuccess { secondResult ->
                                    automixItems.value = secondResult.items.map { song ->
                                        song.toMediaItem()
                                    }
                                }
                                .onFailure {
                                    
                                    if (firstResult.items.isNotEmpty()) {
                                        automixItems.value = firstResult.items.map { song ->
                                            song.toMediaItem()
                                        }
                                    }
                                }
                        }
                        .onFailure {
                            
                            val currentSong = player.currentMetadata
                            if (currentSong != null) {
                                
                                YouTube.next(WatchEndpoint(
                                    videoId = currentSong.id
                                )).onSuccess { radioResult ->
                                    val filteredItems = radioResult.items
                                        .filter { it.id != currentSong.id }
                                        .map { it.toMediaItem() }
                                    if (filteredItems.isNotEmpty()) {
                                        automixItems.value = filteredItems
                                    }
                                }.onFailure {
                                    
                                    YouTube.next(WatchEndpoint(videoId = currentSong.id)).getOrNull()?.relatedEndpoint?.let { relatedEndpoint ->
                                        YouTube.related(relatedEndpoint).onSuccess { relatedPage ->
                                            val relatedItems = relatedPage.songs
                                                .filter { it.id != currentSong.id }
                                                .map { it.toMediaItem() }
                                            if (relatedItems.isNotEmpty()) {
                                                automixItems.value = relatedItems

                                            }
                                        }
                                    }
                                }
                            }
                        }
                } catch (_: Exception) {
                    
                }
            }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            
            if (castConnectionHandler?.isCasting?.value != true) {
                player.play()
            }
            return
        }

        
        if (dataStore.get(PreventDuplicateTracksInQueueKey, false)) {
            val itemIds = items.map { it.mediaId }.toSet()
            val indicesToRemove = mutableListOf<Int>()
            val currentIndex = player.currentMediaItemIndex

            for (i in 0 until player.mediaItemCount) {
                if (i != currentIndex && player.getMediaItemAt(i).mediaId in itemIds) {
                    indicesToRemove.add(i)
                }
            }

            
            indicesToRemove.sortedDescending().forEach { index ->
                player.removeMediaItem(index)
            }
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() 

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        
        if (dataStore.get(PreventDuplicateTracksInQueueKey, false)) {
            val itemIds = items.map { it.mediaId }.toSet()
            val indicesToRemove = mutableListOf<Int>()
            val currentIndex = player.currentMediaItemIndex

            for (i in 0 until player.mediaItemCount) {
                if (i != currentIndex && player.getMediaItemAt(i).mediaId in itemIds) {
                    indicesToRemove.add(i)
                }
            }

            
            indicesToRemove.sortedDescending().forEach { index ->
                player.removeMediaItem(index)
            }
        }

        player.addMediaItems(items)
        if (player.shuffleModeEnabled) {
            val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }
        player.prepare()
    }

    fun toggleLibrary() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let {
                val isInLibrary = it.song.inLibrary != null
                val token = if (isInLibrary) it.song.libraryRemoveToken else it.song.libraryAddToken

                
                token?.let { feedbackToken ->
                    YouTube.feedback(listOf(feedbackToken))
                }

                
                database.query {
                    update(it.song.toggleLibrary())
                }
                currentMediaMetadata.value = player.currentMetadata
            }
        }
    }

    fun toggleLike() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let {
                val song = it.song.toggleLike()
                database.query {
                    update(song)
                    syncUtils.likeSong(song)

                    
                    if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                        
                        val downloadRequest =
                            androidx.media3.exoplayer.offline.DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.title.toByteArray())
                                .build()
                        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                            this@MusicService,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false
                        )
                    }
                }
                currentMediaMetadata.value = player.currentMetadata
            }
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Timber.tag(TAG).w("setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }

        
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Timber.tag(TAG).d("LoudnessEnhancer created for sessionId=$audioSessionId")
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }

        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }

                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }

                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }

                    Timber.tag(TAG).d("Audio normalization enabled: $normalizeAudio")
                    Timber.tag(TAG).d("Format loudnessDb: ${format?.loudnessDb}, perceptualLoudnessDb: ${format?.perceptualLoudnessDb}")

                    
                    val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb

                    withContext(Dispatchers.Main) {
                        if (loudness != null) {
                            val loudnessDb = loudness.toFloat()
                            val targetGain = (-loudnessDb * 100).toInt()
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)

                            Timber.tag(TAG).d("Calculated raw normalization gain: $targetGain mB (from loudness: $loudnessDb)")

                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                                Timber.tag(TAG).i("LoudnessEnhancer gain applied: $clampedGain mB")
                            } catch (e: Exception) {
                                Timber.tag(TAG).e(e, "Failed to apply loudness enhancement")
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                            Timber.tag(TAG).w("Normalization enabled but no loudness data available - no normalization applied")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                        Timber.tag(TAG).d("setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Timber.tag(TAG).d("LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Timber.tag(TAG).e(e, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    private var previousMediaItemIndex = C.INDEX_UNSET

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // Stale plan belongs to the previous track; planner re-arms when the new one is READY.
        if (!isCrossfading.value) automixDebugInfo.value = null
        prepareAutomixForCurrentPair()

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            if (cachedRepeatMode == REPEAT_MODE_ONE &&
                previousMediaItemIndex != C.INDEX_UNSET &&
                previousMediaItemIndex != player.currentMediaItemIndex) {

                player.seekTo(previousMediaItemIndex, 0)
            }
        }
        previousMediaItemIndex = player.currentMediaItemIndex

        lastPlaybackSpeed = -1.0f 

        preloadUpcomingItems()
        setupLoudnessEnhancer()

        discordUpdateJob?.cancel()

        scrobbleManager?.onSongStop()
        checkAndSubmitListenBrainzFinished()

        if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
            scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
            player.currentMediaItem?.mediaId?.let { mediaId ->
                if (listenBrainzCurrentMediaId != mediaId) {
                    listenBrainzCurrentMediaId = mediaId
                    listenBrainzCurrentStartTs = System.currentTimeMillis()
                }
                checkAndSubmitListenBrainzPlayingNow(mediaId)
            }
        }

        
        
        if (castConnectionHandler?.isCasting?.value == true &&
            castConnectionHandler?.isSyncingFromCast != true &&
            mediaItem != null) {
            val metadata = mediaItem.metadata
            if (metadata != null) {
                
                
                val navigated = castConnectionHandler?.navigateToMediaIfInQueue(metadata.id) ?: false
                if (!navigated) {
                    
                    castConnectionHandler?.loadMedia(metadata)
                }
            }
        }

        
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems = withContext(Dispatchers.IO) {
                    currentQueue.nextPage()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }
                if (player.playbackState != STATE_IDLE && mediaItems.isNotEmpty()) {
                    player.addMediaItems(mediaItems)
                    if (player.shuffleModeEnabled) {
                        val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                        applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                    }
                }
            }
        }

        
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        
        if (playbackState == Player.STATE_ENDED) {
            if (cachedRepeatMode == REPEAT_MODE_ALL && player.mediaItemCount > 0) {
                player.seekTo(0, 0)
                player.prepare()
                player.play()
            }
        }

        
        if (dataStore.get(PersistentQueueKey, true) && !isSilenceSkipping) {
            saveQueueToDisk()
        }

        if (playbackState == Player.STATE_READY) {
            consecutivePlaybackErr = 0
            retryCount = 0
            waitingForNetworkConnection.value = false
            retryJob?.cancel()

            
            player.currentMediaItem?.mediaId?.let { mediaId ->
                resetRetryCount(mediaId)
                Timber.tag(TAG).d("Playback successful for $mediaId, reset retry count")
            }
            scheduleCrossfade()
            prepareAutomixForCurrentPair()
        }

        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()
            checkAndSubmitListenBrainzFinished()
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        
        if (playWhenReady && castConnectionHandler?.isCasting?.value == true) {
            player.pause()
            return
        }

        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            if (playWhenReady) {
                isPausedByVolumeMute = false
            }

            if (!playWhenReady && !isPausedByVolumeMute) {
                wasPlayingBeforeVolumeMute = false
            }
        }

        if (playWhenReady) {
            setupLoudnessEnhancer()
        }
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                EVENT_TIMELINE_CHANGED,
                EVENT_POSITION_DISCONTINUITY
            )
        ) {
            prepareAutomixForCurrentPair()
            scheduleCrossfade()
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                val focusGranted = requestAudioFocus()
                if (focusGranted) {
                    openAudioEffectSession()
                }
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }

        
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            updateWidgetUI(player.isPlaying)
            if (player.isPlaying) {
                startWidgetUpdates()
                acquireWifiLock()
            } else {
                stopWidgetUpdates()
                releaseWifiLock()
            }
            if (!player.isPlaying && !events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                scope.launch {
                    DiscordPresenceManager.stop()
                }
            }
        }

        
        if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) {
            val mediaId = player.currentMetadata?.id
            if (mediaId != null) {
                scope.launch {
                    
                    database.song(mediaId).first()?.let { song ->
                        ensurePresenceManager()
                    }
                }
            }
        }

        
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            scrobbleManager?.onPlayerStateChanged(player.isPlaying, player.currentMetadata, duration = player.duration)
            
            if (player.isPlaying) {
                player.currentMediaItem?.mediaId?.let { mediaId ->
                    if (listenBrainzCurrentMediaId != mediaId) {
                        checkAndSubmitListenBrainzFinished()
                        listenBrainzCurrentMediaId = mediaId
                        listenBrainzCurrentStartTs = System.currentTimeMillis()
                        scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
                    }
                    checkAndSubmitListenBrainzPlayingNow(mediaId)
                }
            }
        }

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            
            if (player.mediaItemCount == 0) return

            val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
            val currentIndex = player.currentMediaItemIndex
            val totalCount = player.mediaItemCount

            applyShuffleOrder(currentIndex, totalCount, shufflePlaylistFirst)
        }

        
        if (dataStore.get(RememberShuffleAndRepeatKey, true)) {
            scope.launch {
                dataStore.edit { settings ->
                    settings[ShuffleModeKey] = shuffleModeEnabled
                }
            }
        }

        
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    
    private fun applyShuffleOrder(
        currentIndex: Int,
        totalCount: Int,
        shufflePlaylistFirst: Boolean
    ) {
        if (totalCount == 0) return

        if (shufflePlaylistFirst && originalQueueSize > 0 && originalQueueSize < totalCount) {
            
            val originalIndices = (0 until originalQueueSize).filter { it != currentIndex }.toMutableList()
            val addedIndices = (originalQueueSize until totalCount).filter { it != currentIndex }.toMutableList()

            originalIndices.shuffle()
            addedIndices.shuffle()

            val shuffledIndices = IntArray(totalCount)
            var pos = 0
            shuffledIndices[pos++] = currentIndex

            if (currentIndex < originalQueueSize) {
                originalIndices.forEach { shuffledIndices[pos++] = it }
                addedIndices.forEach { shuffledIndices[pos++] = it }
            } else {
                (0 until originalQueueSize).shuffled().forEach { shuffledIndices[pos++] = it }
                addedIndices.forEach { shuffledIndices[pos++] = it }
            }
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        } else {
            val shuffledIndices = IntArray(totalCount) { it }
            shuffledIndices.shuffle()
            
            val currentItemIndexInShuffled = shuffledIndices.indexOf(currentIndex)
            if (currentItemIndexInShuffled != -1) { 
                val temp = shuffledIndices[0]
                shuffledIndices[0] = shuffledIndices[currentItemIndexInShuffled]
                shuffledIndices[currentItemIndexInShuffled] = temp
            }
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        super.onPlaybackParametersChanged(playbackParameters)
        if (playbackParameters.speed != lastPlaybackSpeed) {
            lastPlaybackSpeed = playbackParameters.speed
            discordUpdateJob?.cancel()

            
            discordUpdateJob = scope.launch {
                delay(1000)
                if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
                    currentSong.value?.let { song ->
                        ensurePresenceManager()
                    }
                }
            }
        }
    }

    
    private fun getHttpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
        }
        return null
    }

    
    private fun isExpiredUrlError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 403
    }

    
    private fun isRangeNotSatisfiableError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 416
    }

    
    private fun isPageReloadError(error: PlaybackException): Boolean {
        val errorMessage = error.message?.lowercase() ?: ""
        val causeMessage = error.cause?.message?.lowercase() ?: ""
        val innerCauseMessage = error.cause?.cause?.message?.lowercase() ?: ""

        val reloadKeywords = listOf(
            "page needs to be reloaded",
            "pagina deve essere ricaricata",
            "la pagina deve essere ricaricata",
            "page must be reloaded",
            "reload",
            "ricaricata"
        )

        return reloadKeywords.any { keyword ->
            errorMessage.contains(keyword) ||
            causeMessage.contains(keyword) ||
            innerCauseMessage.contains(keyword)
        }
    }

    private fun isNetworkRelatedError(error: PlaybackException): Boolean {
        
        if (isExpiredUrlError(error) || isRangeNotSatisfiableError(error) || isPageReloadError(error)) {
            return false
        }
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
                error.cause is java.net.ConnectException ||
                error.cause is java.net.UnknownHostException ||
                (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
    }

    
    private fun isAudioRendererError(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK
    }

    private fun isCacheOrStreamCorruptionError(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        
        if (!playerInitialized.value) {
            Timber.tag(TAG).e(error, "Player error occurred but player not initialized")
            return
        }

        val mediaId = player.currentMediaItem?.mediaId
        Timber.tag(TAG).w(error, "Player error occurred for $mediaId: errorCode=${error.errorCode}, message=${error.message}")
        val isFallbackError = error.message?.contains("fallback", ignoreCase = true) == true
        if (!isFallbackError) {
            reportException(error)
        }

        
        if (mediaId != null && hasExceededRetryLimit(mediaId)) {
            Timber.tag(TAG).w("Song $mediaId has exceeded retry limit, skipping")
            markSongAsFailed(mediaId)
            handleFinalFailure()
            return
        }

        
        if (mediaId != null) {
            performAggressiveCacheClear(mediaId)
        }

        
        when {
            isAudioRendererError(error) -> {
                Timber.tag(TAG).d("AudioTrack error detected (${error.errorCode}), performing safe recovery")
                handleAudioRendererError(mediaId)
                return
            }
            isRangeNotSatisfiableError(error) -> {
                Timber.tag(TAG).d("Range Not Satisfiable (416) detected, performing strict recovery")
                handleRangeNotSatisfiableError(mediaId)
                return
            }
            isCacheOrStreamCorruptionError(error) -> {
                Timber.tag(TAG).d("Cache or stream corruption detected, clearing cache and refreshing URL")
                handleExpiredUrlError(mediaId)
                return
            }
            isPageReloadError(error) -> {
                Timber.tag(TAG).d("Page reload error detected, performing strict recovery")
                handlePageReloadError(mediaId)
                return
            }
            isExpiredUrlError(error) -> {
                Timber.tag(TAG).d("Expired URL (403) detected, refreshing stream URL")
                handleExpiredUrlError(mediaId)
                return
            }

            !isNetworkConnected.value -> {
                Timber.tag(TAG).d("Network disconnected, waiting for connection")
                waitOnNetworkError()
                return
            }
            isNetworkRelatedError(error) -> {
                Timber.tag(TAG).d("Network-related error detected, handling as generic IO error")
                handleGenericIOError(mediaId)
                return
            }
        }

        
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
            Timber.tag(TAG).d("IO error detected (${error.errorCode}), attempting recovery")
            handleGenericIOError(mediaId)
            return
        }

        
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            Timber.tag(TAG).d("Auto-skipping to next track due to unrecoverable error")
            skipOnError()
        } else {
            Timber.tag(TAG).d("Stopping playback due to unrecoverable error")
            stopOnError()
        }
    }

    
    private fun performAggressiveCacheClear(mediaId: String) {
        Timber.tag(TAG).d("Performing aggressive cache clear for $mediaId")

        
        songUrlCache.remove("${mediaId}_${audioQuality.name}")

        
        try {
            playerCache.removeResource(mediaId)
            Timber.tag(TAG).d("Cleared player cache for $mediaId")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear player cache for $mediaId")
        }

        
        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
            Timber.tag(TAG).d("Cleared decryption caches for $mediaId")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear decryption caches for $mediaId")
        }
    }

    
    private fun hasExceededRetryLimit(mediaId: String): Boolean {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        return currentRetries >= MAX_RETRY_PER_SONG
    }

    
    private fun incrementRetryCount(mediaId: String) {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        currentMediaIdRetryCount[mediaId] = currentRetries + 1
        Timber.tag(TAG).d("Retry count for $mediaId: ${currentRetries + 1}/$MAX_RETRY_PER_SONG")
    }

    
    private fun resetRetryCount(mediaId: String) {
        currentMediaIdRetryCount.remove(mediaId)
        recentlyFailedSongs.remove(mediaId)
    }

    
    private fun markSongAsFailed(mediaId: String) {
        recentlyFailedSongs.add(mediaId)
        currentMediaIdRetryCount.remove(mediaId)

        
        failedSongsClearJob?.cancel()
        failedSongsClearJob = scope.launch {
            delay(5 * 60 * 1000L) 
            recentlyFailedSongs.clear()
            Timber.tag(TAG).d("Cleared recently failed songs list")
        }
    }

    
    private fun handleAudioRendererError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            try {
                
                val wasPlaying = player.playWhenReady
                player.pause()
                Timber.tag(TAG).d("Paused playback due to AudioTrack error")

                
                
                delay(RETRY_DELAY_MS * 3) 

                
                if (!playerInitialized.value) {
                    Timber.tag(TAG).w("Player no longer initialized, aborting AudioTrack recovery")
                    return@launch
                }

                val currentIndex = player.currentMediaItemIndex
                if (currentIndex != C.INDEX_UNSET) {
                    
                    val currentPosition = player.currentPosition
                    player.seekTo(currentIndex, currentPosition)
                    player.prepare()

                    Timber.tag(TAG).d("Retrying playback for $mediaId after AudioTrack error")

                    
                    if (wasPlaying) {
                        delay(500) 
                        if (hasAudioFocus && playerInitialized.value) {
                            if (castConnectionHandler?.isCasting?.value != true) {
                                player.play()
                            }
                        }
                    }
                } else {
                    Timber.tag(TAG).w("Invalid media item index during AudioTrack recovery")
                    handleFinalFailure()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error during AudioTrack error recovery")
                handleFinalFailure()
            }
        }
    }

    
    private fun handleRangeNotSatisfiableError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            
            performAggressiveCacheClear(mediaId)


            
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, 0)
            player.prepare()

            Timber.tag(TAG).d("Retrying playback for $mediaId after 416 error (from position 0)")
        }
    }

    
    private fun handlePageReloadError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            Timber.tag(TAG).d("Handling page reload error for $mediaId")

            
            performAggressiveCacheClear(mediaId)

            
            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()

            Timber.tag(TAG).d("Retrying playback for $mediaId after page reload error")
        }
    }

    
    private fun handleExpiredUrlError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        
        songUrlCache.remove("${mediaId}_${audioQuality.name}")
        Timber.tag(TAG).d("Cleared cached URL for $mediaId")

        
        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear decryption caches")
        }

        retryJob?.cancel()
        retryJob = scope.launch {

            
            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()

            Timber.tag(TAG).d("Retrying playback for $mediaId after 403 error")
        }
    }

    
    private fun handleGenericIOError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            performAggressiveCacheClear(mediaId)


            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()

            Timber.tag(TAG).d("Retrying playback for $mediaId after generic IO error")
        }
    }

    
    private fun handleFinalFailure() {
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            Timber.tag(TAG).d("All recovery attempts exhausted, auto-skipping to next track")
            skipOnError()
        } else {
            Timber.tag(TAG).d("All recovery attempts exhausted, stopping playback")
            stopOnError()
        }
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        super.onDeviceVolumeChanged(volume, muted)
        val pauseOnMute = dataStore.get(PauseOnMute, false)

        if ((volume == 0 || muted) && pauseOnMute) {
            if (player.isPlaying) {
                wasPlayingBeforeVolumeMute = true
                isPausedByVolumeMute = true
                player.pause()
            }
        } else if (volume > 0 && !muted && pauseOnMute) {
            if (wasPlayingBeforeVolumeMute && !player.isPlaying && castConnectionHandler?.isCasting?.value != true) {
                wasPlayingBeforeVolumeMute = false
                isPausedByVolumeMute = false
                player.play()
            }
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource
                    .Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        OkHttpDataSource.Factory(
                            OkHttpClient
                                    .Builder()
                                    .dns(object : Dns {
                                        override fun lookup(hostname: String): List<InetAddress> {
                                            val addresses = Dns.SYSTEM.lookup(hostname)
                                            return when (this@MusicService.ipVersion) {
                                                IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                                                IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                                                IpVersion.AUTO -> addresses
                                            }
                                        }
                                    })
                                    .proxy(YouTube.proxy)
                                    .proxyAuthenticator { _, response ->
                                        YouTube.proxyAuth?.let { auth ->
                                            response.request.newBuilder()
                                                .header("Proxy-Authorization", auth)
                                                .build()
                                        } ?: response.request
                                    }
                                    .build()
                            )
                    )
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    
    private var isSilenceSkipping = false

    private fun handleLongSilenceDetected() {
        if (!instantSilenceSkipEnabled.value) return
        if (silenceSkipJob?.isActive == true) return

        silenceSkipJob = scope.launch {
            
            delay(200)
            performInstantSilenceSkip()
        }
    }

    private suspend fun performInstantSilenceSkip() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        if (duration <= INSTANT_SILENCE_SKIP_STEP_MS) return

        isSilenceSkipping = true
        try {
            var hops = 0
            val silenceProcessor = playerSilenceProcessors[player] ?: return
            while (coroutineContext.isActive && instantSilenceSkipEnabled.value && silenceProcessor.isCurrentlySilent()) {
                val current = player.currentPosition
                val target = (current + INSTANT_SILENCE_SKIP_STEP_MS).coerceAtMost(duration - 500)

                if (target <= current) break

                
                silenceProcessor.resetTracking()
                player.seekTo(target)
                hops++

                if (hops >= 80 || target >= duration - 500) break

                delay(INSTANT_SILENCE_SKIP_SETTLE_MS)
            }
            if (hops > 0) {
                Timber.tag(TAG).d("Silence skip: jumped $hops times")
            }
        } finally {
            isSilenceSkipping = false
        }
    }

    private fun updateListenBrainz(title: String, artistNames: String, releaseName: String, durationMs: Long, isFinished: Boolean, startMs: Long = 0, endMs: Long = 0, positionMs: Long = 0) {
        val cleanToken = listenBrainzToken.trim()
        if (!listenBrainzEnabled || cleanToken.isBlank()) return
        scope.launch {
            if (isFinished) {
                com.music.echo.ui.screens.settings.ListenBrainzManager.submitFinished(
                    context = this@MusicService,
                    token = cleanToken,
                    title = title,
                    artistNames = artistNames,
                    releaseName = releaseName,
                    durationMs = durationMs,
                    startMs = startMs,
                    endMs = endMs
                )
            } else {
                com.music.echo.ui.screens.settings.ListenBrainzManager.submitPlayingNow(
                    context = this@MusicService,
                    token = cleanToken,
                    title = title,
                    artistNames = artistNames,
                    releaseName = releaseName,
                    durationMs = durationMs,
                    positionMs = positionMs
                )
            }
        }
    }

    private fun currentPresenceSong(): Song? {
        val mediaId = player.currentMediaItem?.mediaId ?: return null
        return runBlocking(Dispatchers.IO) { database.song(mediaId).firstOrNull() }
    }

    private fun ensurePresenceManager() {
        if (DiscordPresenceManager.lastRpcStartTime != null && lastPresenceToken != null) {
            if (dataStore.get(EnableDiscordRPCKey, true) && dataStore.get(DiscordTokenKey, "").isNotBlank()) {
                DiscordPresenceManager.restart()
            }
            return
        }

        scope.launch {
            if (!dataStore.get(EnableDiscordRPCKey, true)) {
                if (DiscordPresenceManager.lastRpcStartTime != null) {
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                    lastPresenceToken = null
                }
                return@launch
            }

            val key = dataStore.get(DiscordTokenKey, "")
            if (key.isBlank()) {
                if (DiscordPresenceManager.lastRpcStartTime != null) {
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                    lastPresenceToken = null
                }
                return@launch
            }

            if (DiscordPresenceManager.lastRpcStartTime != null && lastPresenceToken == key) {
                return@launch
            }

            try {
                DiscordPresenceManager.stop()
                DiscordPresenceManager.start(
                    context = this@MusicService,
                    token = key,
                    songProvider = { currentPresenceSong() },
                    positionProvider = { player.currentPosition },
                    isPausedProvider = { !player.isPlaying }
                )
                lastPresenceToken = key
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Failed to start presence manager")
            }
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(
            DefaultDataSource.Factory(this, createCacheDataSource())
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            if (mediaId.isLocalMediaId()) {
                val localUri = android.net.Uri.parse(mediaId)
                try {
                    contentResolver.openFileDescriptor(localUri, "r")?.close()
                } catch (e: java.io.FileNotFoundException) {
                    throw androidx.media3.common.PlaybackException("Local file deleted", e, androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
                }
                return@Factory dataSpec
            }


            
            var shouldBypassCache = bypassCacheForQualityChange.contains(mediaId)
            
            val dbFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).firstOrNull() }
            
            val cachedLength = androidx.media3.datasource.cache.ContentMetadata.getContentLength(downloadCache.getContentMetadata(mediaId))
                .takeIf { it != androidx.media3.common.C.LENGTH_UNSET.toLong() } ?: dbFormat?.contentLength ?: -1L
            val isFullyDownloaded = cachedLength > 0 && downloadCache.isCached(mediaId, 0, cachedLength)

            val activeQualityInCache = songUrlCache.keys.find { it.startsWith("${mediaId}_") }?.substringAfter("_")?.let {
                runCatching { iad1tya.echo.music.constants.AudioQuality.valueOf(it) }.getOrNull()
            }
            val lockedQuality = activeQualityInCache ?: audioQuality

            if (!shouldBypassCache && !isFullyDownloaded && dbFormat != null) {
                val isLosslessCache = dbFormat.codecs == "flac"
                val isSaavnCache = dbFormat.codecs == "mp4a.40.2" || dbFormat.mimeType.contains("mp4", ignoreCase = true)
                
                val cacheMatchesTarget = when (lockedQuality) {
                    iad1tya.echo.music.constants.AudioQuality.LOSSLESS -> isLosslessCache
                    iad1tya.echo.music.constants.AudioQuality.SAAVN -> isSaavnCache
                    iad1tya.echo.music.constants.AudioQuality.OPUS -> !isLosslessCache && !isSaavnCache
                }
                
                if (!cacheMatchesTarget) {
                    shouldBypassCache = true
                    Timber.tag(TAG).i("Quality changed to $lockedQuality for $mediaId. Clearing playerCache to prevent container mismatch.")
                    playerCache.removeResource(mediaId)
                }
            }

            if (!shouldBypassCache) {
                if (isFullyDownloaded) {
                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                    return@Factory dataSpec
                }

                if (downloadCache.isCached(
                        mediaId,
                        dataSpec.position,
                        if (dataSpec.length >= 0) dataSpec.length else 1
                    )
                ) {
                    songUrlCache["${mediaId}_${lockedQuality.name}"]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                        scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                        return@Factory dataSpec.withUri(it.first.toUri())
                    }
                    // Fall through to fetch real URL since it's only partially downloaded
                }

                if (playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)) {
                    songUrlCache["${mediaId}_${lockedQuality.name}"]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                        scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                        return@Factory dataSpec.withUri(it.first.toUri())
                    }
                    Timber.tag(TAG).w("Ghost cache entry for $mediaId, re-fetching")
                    playerCache.removeResource(mediaId)
                }

                songUrlCache["${mediaId}_${lockedQuality.name}"]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                        scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                        return@Factory dataSpec.withUri(it.first.toUri())
                }
            } else {
                Timber.tag("MusicService").i("BYPASSING CACHE for $mediaId due to quality change")
            }

            Timber.tag("MusicService").i("FETCHING STREAM: $mediaId | quality=$lockedQuality")
            val playbackData = runBlocking(Dispatchers.IO) {
                val dbSong = database.song(mediaId).firstOrNull()
                val knownArtist = dbSong?.artists?.joinToString { it.name }?.replace(" - Topic", "")
                val knownTitle = dbSong?.song?.title
                val knownDuration = dbSong?.song?.duration?.let { if (it > 0) it * 1000L else null }

                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = lockedQuality,
                    connectivityManager = connectivityManager,
                    context = this@MusicService,
                    knownArtist = knownArtist,
                    knownTitle = knownTitle,
                    knownDurationMs = knownDuration
                )
            }.getOrElse { throwable ->
                when (throwable) {
                    is PlaybackException -> throw throwable

                    is java.net.ConnectException, is java.net.UnknownHostException -> {
                        throw PlaybackException(
                            getString(R.string.error_no_internet),
                            throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        )
                    }

                    is java.net.SocketTimeoutException -> {
                        throw PlaybackException(
                            getString(R.string.error_timeout),
                            throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        )
                    }

                    else -> throw PlaybackException(
                        getString(R.string.error_unknown),
                        throwable,
                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }
            }

            val nonNullPlayback = requireNotNull(playbackData) {
                getString(R.string.error_unknown)
            }
            run {
                val format = nonNullPlayback.format
                
                val isFinalLossless = format.mimeType.contains("flac", ignoreCase = true)
                val isFinalSaavn = format.mimeType.contains("mp4", ignoreCase = true) || format.mimeType.contains("m4a", ignoreCase = true)
                
                var targetCacheKey = mediaId
                
                if (dbFormat != null && !shouldBypassCache) {
                    val cacheIsLossless = dbFormat.codecs == "flac"
                    val cacheIsSaavn = dbFormat.codecs == "mp4a.40.2" || dbFormat.mimeType.contains("mp4", ignoreCase = true)
                    
                    if (isFinalLossless != cacheIsLossless || isFinalSaavn != cacheIsSaavn) {
                        Timber.tag(TAG).w("Format fallback detected AFTER fetch. Clearing playerCache to prevent mismatch crash.")
                        playerCache.removeResource(mediaId)
                        
                        if (activeQualityInCache != null) {
                            Timber.tag(TAG).e("Format changed mid-stream for $mediaId. Throwing to force player restart.")
                            runBlocking(Dispatchers.IO) { database.query { deleteFormat(mediaId) } }
                            throw PlaybackException(
                                "Container format changed mid-stream due to fallback",
                                null,
                                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                            )
                        }
                    }
                } else if (dbFormat != null && shouldBypassCache) {
                    val cacheIsLossless = dbFormat.codecs == "flac"
                    val cacheIsSaavn = dbFormat.codecs == "mp4a.40.2" || dbFormat.mimeType.contains("mp4", ignoreCase = true)
                    
                    if (isFinalLossless != cacheIsLossless || isFinalSaavn != cacheIsSaavn) {
                        Timber.tag(TAG).i("Bypassed cache and fetched different format. Using custom cache key to prevent intercept.")
                        targetCacheKey = "${mediaId}_diff"
                    } else {
                        Timber.tag(TAG).i("Bypassed cache but fallback resulted in cached format. Using original cache key.")
                        targetCacheKey = mediaId
                    }
                }

                val loudnessDb = nonNullPlayback.audioConfig?.loudnessDb
                val perceptualLoudnessDb = nonNullPlayback.audioConfig?.perceptualLoudnessDb

                Timber.tag(TAG).d("Storing format for $mediaId with loudnessDb: $loudnessDb, perceptualLoudnessDb: $perceptualLoudnessDb")
                if (loudnessDb == null && perceptualLoudnessDb == null) {
                    Timber.tag(TAG).w("No loudness data available from YouTube for video: $mediaId")
                }

                if (!isFullyDownloaded || targetCacheKey == mediaId) {
                    database.query {
                        upsert(
                            FormatEntity(
                                id = mediaId,
                                itag = format.itag,
                                mimeType = format.mimeType.split(";")[0],
                                codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                                bitrate = format.bitrate,
                                sampleRate = format.audioSampleRate,
                                contentLength = format.contentLength ?: 0L,
                                loudnessDb = loudnessDb,
                                perceptualLoudnessDb = perceptualLoudnessDb,
                                playbackUrl = nonNullPlayback.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                            )
                        )
                    }
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, nonNullPlayback) }

                
                if (bypassCacheForQualityChange.remove(mediaId)) {
                    Timber.tag("MusicService").d("Cleared bypass cache flag for $mediaId after fresh fetch")
                }

                val streamUrl = nonNullPlayback.streamUrl

                songUrlCache["${mediaId}_${lockedQuality.name}"] =
                    streamUrl to System.currentTimeMillis() + (nonNullPlayback.streamExpiresInSeconds * 1000L)
                
                return@Factory dataSpec.buildUpon().setKey(targetCacheKey).setUri(streamUrl.toUri()).build()
            }
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            androidx.media3.extractor.DefaultExtractorsFactory()
        )

    private fun createRenderersFactory(
        eqProcessor: CustomEqualizerAudioProcessor,
        silenceProcessor: SilenceDetectorAudioProcessor
    ) =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        
                        arrayOf(
                            eqProcessor,
                            silenceProcessor,
                        ),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        val historyDurationMs = dataStore[HistoryDuration]?.times(1000f) ?: 30000f

        if (playbackStats.totalPlayTimeMs >= historyDurationMs &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }
        }

        if (playbackStats.totalPlayTimeMs >= historyDurationMs) {
            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                    ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                        .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                playbackUrl?.let {
                    YouTube.registerPlayback(null, playbackUrl)
                        .onFailure {
                            reportException(it)
                        }
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (player.mediaItemCount == 0) {
            Timber.tag(TAG).d("Skipping queue save - no media items")
            return
        }

        try {
            
            val persistQueue = currentQueue.toPersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex,
                position = player.currentPosition
            )

            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixItems.value.mapNotNull { it.metadata },
                    mediaItemIndex = 0,
                    position = 0,
                )

            
            val persistPlayerState = PersistPlayerState(
                playWhenReady = player.playWhenReady,
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentPosition = player.currentPosition,
                currentMediaItemIndex = player.currentMediaItemIndex,
                playbackState = player.playbackState
            )

            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
                Timber.tag(TAG).d("Queue saved successfully")
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to save queue")
                reportException(it)
            }

            runCatching {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
                Timber.tag(TAG).d("Automix saved successfully")
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to save automix")
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistPlayerState)
                    }
                }
                Timber.tag(TAG).d("Player state saved successfully")
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to save player state")
                reportException(it)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during queue save operation")
            reportException(e)
        }
    }

    /**
     * Safe helper method to invoke startForeground without throwing ForegroundServiceStartNotAllowedException
     * when running under Android 14+ (SDK 34) background execution limits (#816).
     */
    private fun safeStartForeground(notificationId: Int, notification: android.app.Notification) {
        try {
            startForeground(notificationId, notification)
        } catch (e: Exception) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                e is android.app.ForegroundServiceStartNotAllowedException
            ) {
                Timber.tag(TAG).w(e, "ForegroundServiceStartNotAllowedException: Suppressed background FGS start on Android 14+")
            } else {
                throw e
            }
        }
    }

    override fun onDestroy() {
        isRunning = false

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            
        }
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        castConnectionHandler?.release()
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        DiscordPresenceManager.stop()
        connectivityObserver.unregister()
        releaseWifiLock()
        abandonAudioFocus()
        releaseLoudnessEnhancer()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        playerSilenceProcessors.remove(player)
        
        
        
        player.release()
        discordUpdateJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).d("MusicService: onTaskRemoved triggered from recent apps switcher")
        try {
            if (!player.isPlaying) {
                mediaSession.isActive = false
                mediaSession.release()
                player.stop()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error cleaning up MediaSession in onTaskRemoved")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MusicWidgetReceiver.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                updateWidgetUI(player.isPlaying)
            }
            MusicWidgetReceiver.ACTION_LIKE -> {
                toggleLike()
            }
            MusicWidgetReceiver.ACTION_NEXT -> {
                player.seekToNext()
                updateWidgetUI(player.isPlaying)
            }
            MusicWidgetReceiver.ACTION_PREVIOUS -> {
                player.seekToPrevious()
                updateWidgetUI(player.isPlaying)
            }
            MusicWidgetReceiver.ACTION_UPDATE_WIDGET -> {
                updateWidgetUI(player.isPlaying)
            }
            "iad1tya.echo.music.ACTION_CLEAR_SONG_CACHE" -> {
                val songId = intent.getStringExtra("songId")
                if (songId != null) {
                    songUrlCache.keys.filter { it.startsWith("${songId}_") }.forEach {
                        songUrlCache.remove(it)
                    }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    
    private fun updateWidgetUI(isPlaying: Boolean) {
        scope.launch {
            try {
                val songData = currentSong.value
                val song = songData?.song
                val songTitle = song?.title ?: getString(R.string.no_song_playing)
                val artistName = songData?.artists?.joinToString(", ") { it.name } ?: getString(R.string.tap_to_open)
                val isLiked = songData?.song?.liked == true

                widgetManager.updateWidgets(
                    title = songTitle,
                    artist = artistName,
                    artworkUri = song?.thumbnailUrl,
                    isPlaying = isPlaying,
                    isLiked = isLiked,
                    duration = if (player.duration != C.TIME_UNSET) player.duration else 0,
                    currentPosition = player.currentPosition
                )
            } catch (e: Exception) {
                
            }
        }
    }

    private var widgetUpdateJob: Job? = null

    private fun startWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    updateWidgetUI(true)
                }
                delay(200)
            }
        }
    }

    private fun stopWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = null
    }

    private fun shareSong() {
        val songData = currentSong.value
        val songId = songData?.song?.id ?: return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://share.echomusic.fun/watch?v=$songId")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    
    suspend fun getStreamUrl(mediaId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    videoId = mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                ).getOrNull()
                playbackData?.streamUrl
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to get stream URL for Cast")
                null
            }
        }
    }

    
    private fun initializeCast() {
        if (dataStore.get(iad1tya.echo.music.constants.EnableGoogleCastKey, true)) {
            try {
                castConnectionHandler = CastConnectionHandler(this, scope, this)
                castConnectionHandler?.initialize()
                timber.log.Timber.d("Google Cast initialized")
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to initialize Google Cast")
            }
        }
    }


    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            prepareAutomixForCurrentPair()
            scheduleCrossfade()
        }
    }

    private fun currentAutomixPair(): AutomixPair? {
        val currentId = player.currentMediaItem?.mediaId ?: return null
        val repeatOne = cachedRepeatMode == REPEAT_MODE_ONE
        val nextId = if (repeatOne) {
            currentId
        } else {
            val nextIndex = player.nextMediaItemIndex
            if (nextIndex == C.INDEX_UNSET) return null
            player.getMediaItemAt(nextIndex).mediaId
        }
        return AutomixPair(currentId, nextId)
    }

    private fun prepareAutomixForCurrentPair() {
        if (!automixEnabled || !crossfadeEnabled || isCrossfading.value) return
        val pair = currentAutomixPair() ?: return
        maybeAnalyzeBeat(pair.currentId, BeatAnalysisPriority.IMMEDIATE)
        if (pair.nextId != pair.currentId) {
            maybeAnalyzeBeat(pair.nextId, BeatAnalysisPriority.IMMEDIATE)
        }
    }

    private fun isAutomixPlanCurrent(plan: AutomixPlan): Boolean {
        val pair = currentAutomixPair() ?: return false
        return pair.currentId == plan.currentId && pair.nextId == plan.nextId
    }

    private fun scheduleCrossfade() {
        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null
        if (!crossfadeEnabled || player.duration == C.TIME_UNSET || player.duration <= crossfadeDuration) return
        if (crossfadeGapless && isNextItemGapless()) return
        if (!player.hasNextMediaItem() && player.repeatMode != REPEAT_MODE_ONE) return

        val baseTriggerTime = player.duration - crossfadeDuration.toLong()
        if (baseTriggerTime - player.currentPosition <= 0) return

        val targetMediaId = player.currentMediaItem?.mediaId
        val trackDuration = player.duration

        crossfadeTriggerJob = scope.launch {
            if (!automixEnabled) automixDebugInfo.value = null
            // Plan first: it enqueues analysis for current+next, which must not wait
            // behind slower far-queue lookahead fetches.
            val planResult = if (automixEnabled) {
                computeAutomixPlan(baseTriggerTime, trackDuration)
            } else {
                AutomixPlanResult(plan = null, pairAnalyzed = false)
            }
            val plan = planResult.plan
            if (automixEnabled && planResult.pairAnalyzed) analyzeUpcomingTracks()
            val triggerTime = plan?.triggerTimeMs ?: baseTriggerTime
            if (triggerTime - player.currentPosition <= 0) return@launch

            // Poll playback position instead of a wall-clock delay: position freezes on
            // pause, so the trigger can't misfire while paused and get lost.
            while (isActive) {
                if (player.currentMediaItem?.mediaId != targetMediaId) return@launch
                val remaining = triggerTime - player.currentPosition
                if (remaining <= 0) break
                delay(minOf(remaining, 250L))
            }
            if (isActive && player.isPlaying && player.currentMediaItem?.mediaId == targetMediaId && !sleepTimer.pauseWhenSongEnd) {
                if (plan != null && !isAutomixPlanCurrent(plan)) {
                    scheduleCrossfade()
                    return@launch
                }
                startCrossfade(plan)
            }
        }
    }

    /**
     * Builds a beat-aligned transition from cached beat analysis of the outgoing and
     * incoming tracks. Returns null (plain crossfade fallback) when either track lacks
     * usable analysis; kicks off lazy analysis in that case so a later transition can align.
     */
    private suspend fun computeAutomixPlan(baseTriggerTime: Long, trackDuration: Long): AutomixPlanResult {
        val pair = currentAutomixPair() ?: return AutomixPlanResult(plan = null, pairAnalyzed = false)
        val currentId = pair.currentId
        val nextId = pair.nextId

        val (outBeat, inBeat) = withContext(Dispatchers.IO) {
            database.beatInfo(currentId) to database.beatInfo(nextId)
        }
        if (outBeat == null) maybeAnalyzeBeat(currentId, BeatAnalysisPriority.IMMEDIATE)
        if (inBeat == null && nextId != currentId) maybeAnalyzeBeat(nextId, BeatAnalysisPriority.IMMEDIATE)
        val partialDebug = AutomixDebugInfo(
            status = "",
            outBpm = outBeat?.bpm, outConfidence = outBeat?.confidence, outMixOutMs = outBeat?.mixOutPointMs,
            inBpm = inBeat?.bpm, inConfidence = inBeat?.confidence, inMixInMs = inBeat?.mixInPointMs,
        )
        if (outBeat == null || inBeat == null) {
            Timber.tag(TAG).d(
                "Automix fallback: beat info missing (current=%s next=%s)",
                outBeat != null, inBeat != null
            )
            automixDebugInfo.value = partialDebug.copy(
                status = "fallback: analysis pending (" +
                    (if (outBeat == null) "current" else "") +
                    (if (outBeat == null && inBeat == null) "+" else "") +
                    (if (inBeat == null) "next" else "") + ")"
            )
            return AutomixPlanResult(plan = null, pairAnalyzed = false)
        }
        if (outBeat.confidence < 0.3f || inBeat.confidence < 0.3f || outBeat.bpm <= 0f || inBeat.bpm <= 0f) {
            Timber.tag(TAG).d(
                "Automix fallback: low confidence (out=%.2f/%.0fbpm in=%.2f/%.0fbpm)",
                outBeat.confidence, outBeat.bpm, inBeat.confidence, inBeat.bpm
            )
            automixDebugInfo.value = partialDebug.copy(status = "fallback: low confidence")
            return AutomixPlanResult(plan = null, pairAnalyzed = true)
        }

        val periodMs = (60_000f / outBeat.bpm).toDouble()

        // DJ blend: 16 beats of the outgoing track (4 bars), 6-16s bounds.
        val overlapMs = (16 * periodMs).toLong().coerceIn(6_000L, 16_000L)

        // Dynamic mix-out: start the transition where the song's body ends (outro begins)
        // rather than a fixed distance from the end. Sentinel <= 0 means "no outro found".
        val latestTrigger = trackDuration - overlapMs
        val mixOut = outBeat.mixOutPointMs?.takeIf { it > 0 }
        val effectiveTrigger = mixOut?.coerceAtMost(latestTrigger) ?: latestTrigger

        // Snap the fade start onto an 8-beat phrase boundary of the outgoing track's grid.
        // Anchor past the current position so re-planning late (pause/seek near the end)
        // still lands on the next musical boundary instead of giving up.
        val phraseMs = periodMs * 8
        val anchor = maxOf(effectiveTrigger, player.currentPosition + 1000)
        val k = ((anchor - outBeat.firstBeatOffsetMs) / phraseMs).toLong()
        var triggerTime = (outBeat.firstBeatOffsetMs + k * phraseMs).toLong()
        if (triggerTime < anchor) triggerTime = (outBeat.firstBeatOffsetMs + (k + 1) * phraseMs).toLong()
        if (triggerTime >= trackDuration - 3000) {
            Timber.tag(TAG).d("Automix fallback: trigger %d out of range (pos=%d dur=%d)", triggerTime, player.currentPosition, trackDuration)
            automixDebugInfo.value = partialDebug.copy(status = "fallback: trigger out of range")
            return AutomixPlanResult(plan = null, pairAnalyzed = true)
        }

        // Fold octave errors, then cap pitch-preserving stretch at ±8%.
        var tempoRatio = outBeat.bpm / inBeat.bpm
        while (tempoRatio > 1.5f) tempoRatio /= 2f
        while (tempoRatio < 0.667f) tempoRatio *= 2f
        if (tempoRatio !in 0.92f..1.08f) tempoRatio = 1f

        // Dynamic mix-in: skip the incoming track's intro, snapped onto its own 8-beat grid.
        val inPeriodMs = (60_000f / inBeat.bpm).toDouble()
        val rawStart = inBeat.mixInPointMs?.takeIf { it > 0 } ?: inBeat.firstBeatOffsetMs
        val inPhraseMs = inPeriodMs * 8
        val inK = kotlin.math.ceil((rawStart - inBeat.firstBeatOffsetMs) / inPhraseMs).toLong().coerceAtLeast(0)
        val incomingStart = (inBeat.firstBeatOffsetMs + inK * inPhraseMs).toLong()

        val plan = AutomixPlan(
            currentId = currentId,
            nextId = nextId,
            triggerTimeMs = triggerTime,
            incomingStartMs = incomingStart,
            tempoRatio = tempoRatio,
            overlapMs = overlapMs,
        )
        Timber.tag(TAG).d(
            "Automix plan: trigger=%dms incomingStart=%dms ratio=%.3f overlap=%dms",
            plan.triggerTimeMs, plan.incomingStartMs, plan.tempoRatio, plan.overlapMs
        )
        automixDebugInfo.value = partialDebug.copy(
            status = "plan ready",
            triggerTimeMs = plan.triggerTimeMs,
            incomingStartMs = plan.incomingStartMs,
            tempoRatio = plan.tempoRatio,
        )
        return AutomixPlanResult(plan = plan, pairAnalyzed = true)
    }

    /**
     * Queue lookahead: analyze the next few upcoming tracks while the current one plays,
     * so beat data is ready by the time their transition is planned.
     */
    private fun analyzeUpcomingTracks() {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return
        // Skip the immediate next item: the transition planner already enqueues it
        // (with priority over this far-queue lookahead).
        var index = timeline.getNextWindowIndex(
            player.currentMediaItemIndex, REPEAT_MODE_OFF, player.shuffleModeEnabled
        )
        if (index == C.INDEX_UNSET) return
        repeat(2) {
            index = timeline.getNextWindowIndex(index, REPEAT_MODE_OFF, player.shuffleModeEnabled)
            if (index == C.INDEX_UNSET) return
            maybeAnalyzeBeat(player.getMediaItemAt(index).mediaId, BeatAnalysisPriority.LOOKAHEAD)
        }
    }

    /**
     * Lazy per-track beat analysis; fetches audio through the playback data-source chain
     * (cache-first, network otherwise) and stores the result permanently. Serialized so
     * lookahead doesn't stack up parallel downloads.
     */
    private fun maybeAnalyzeBeat(
        mediaId: String,
        priority: BeatAnalysisPriority = BeatAnalysisPriority.IMMEDIATE,
    ) {
        synchronized(beatAnalysisJobs) {
            val existing = beatAnalysisJobs[mediaId]
            if (existing != null) {
                if (priority == BeatAnalysisPriority.IMMEDIATE &&
                    existing.priority == BeatAnalysisPriority.LOOKAHEAD
                ) {
                    Timber.tag(TAG).d("Beat analysis priority upgrade for %s", mediaId)
                    existing.job.cancel()
                    beatAnalysisJobs.remove(mediaId)
                } else {
                    return
                }
            }

            val job = scope.launch(Dispatchers.IO) {
                val mutex = when (priority) {
                    BeatAnalysisPriority.IMMEDIATE -> immediateBeatAnalysisMutex
                    BeatAnalysisPriority.LOOKAHEAD -> lookaheadBeatAnalysisMutex
                }
                mutex.withLock {
                    try {
                        runBeatAnalysis(mediaId, priority)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        Timber.tag(TAG).d("Beat analysis cancelled for %s (%s)", mediaId, priority)
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Beat analysis failed for $mediaId")
                    } finally {
                        synchronized(beatAnalysisJobs) {
                            val current = beatAnalysisJobs[mediaId]
                            if (current?.job == coroutineContext[Job]) {
                                beatAnalysisJobs.remove(mediaId)
                            }
                        }
                    }
                }
            }
            beatAnalysisJobs[mediaId] = BeatAnalysisHandle(priority, job)
        }
    }

    private suspend fun runBeatAnalysis(mediaId: String, priority: BeatAnalysisPriority) {
        val existing = database.beatInfo(mediaId)
        // Skip when analyzed with mix points (null mixOut = pre-mix-point row, rescan once).
        if (existing != null && !(existing.bpm > 0f && existing.mixOutPointMs == null)) return
        Timber.tag(TAG).d("Beat analysis starting for %s (%s)", mediaId, priority)

        val result: BeatAnalyzer.Result?
        val dataComplete: Boolean
        val startedAt = android.os.SystemClock.elapsedRealtime()
        val analysisContext = coroutineContext
        fun timedOutOrCancelled(): Boolean =
            !analysisContext.isActive ||
                android.os.SystemClock.elapsedRealtime() - startedAt > beatAnalysisTimeoutMs(priority)
        if (mediaId.isLocalMediaId()) {
            result = BeatAnalyzer.analyzeUri(
                this@MusicService,
                android.net.Uri.parse(mediaId),
                shouldCancel = ::timedOutOrCancelled,
            )
            dataComplete = true
        } else {
            val fetched = BeatAnalyzer.analyzeStream(
                analysisDataSourceFactory,
                mediaId,
                cacheDir,
                shouldCancel = { !analysisContext.isActive || timedOutOrCancelled() },
            )
                ?: run {
                    Timber.tag(TAG).d("Beat analysis skipped for %s: fetch failed", mediaId)
                    return // retry on a later transition
                }
            result = fetched.result
            dataComplete = fetched.complete
        }
        Timber.tag(TAG).d(
            "Beat analysis done for %s: %s", mediaId,
            result?.let { "bpm=%.1f conf=%.2f mixIn=%s mixOut=%s".format(it.bpm, it.confidence, it.mixInPointMs, it.mixOutPointMs) } ?: "failed (complete=$dataComplete)"
        )
        if (result == null && !dataComplete) return // partial data; retry when fully cached

        val entity = result?.let {
            BeatInfoEntity(
                mediaId, it.bpm, it.firstBeatOffsetMs, it.confidence,
                mixInPointMs = it.mixInPointMs ?: -1L, // -1 sentinel: scanned, none found
                mixOutPointMs = it.mixOutPointMs ?: -1L,
            )
        } ?: BeatInfoEntity(mediaId, 0f, 0L, 0f, mixInPointMs = -1L, mixOutPointMs = -1L)
        withContext(Dispatchers.IO) { database.upsert(entity) }

        // Fresh data may unlock a beat-aligned plan for the ongoing transition:
        // re-arm the scheduler if this track is the current or next item.
        withContext(Dispatchers.Main) {
            val currentId = player.currentMediaItem?.mediaId
            val nextIndex = player.nextMediaItemIndex
            val nextId = if (nextIndex != C.INDEX_UNSET) player.getMediaItemAt(nextIndex).mediaId else null
            if (mediaId == currentId || mediaId == nextId) scheduleCrossfade()
        }
    }

    private fun isNextItemGapless(): Boolean {
        val current = player.currentMediaItem?.mediaMetadata ?: return false
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return false
        val next = player.getMediaItemAt(nextIndex).mediaMetadata
        return current.albumTitle != null && current.albumTitle == next.albumTitle
    }

    private fun startCrossfade(plan: AutomixPlan? = null) {
        if (isCrossfading.value) return



        val savedRepeatMode = cachedRepeatMode
        val savedShuffleEnabled = cachedShuffleEnabled


        val targetIndex = if (savedRepeatMode == REPEAT_MODE_ONE) {
            player.currentMediaItemIndex
        } else {
            player.nextMediaItemIndex
        }
        if (targetIndex == C.INDEX_UNSET) return

        activeAutomixPlan = plan

        secondaryPlayer = createExoPlayer()
        val secPlayer = secondaryPlayer!!
        secPlayer.addListener(secondaryPlayerListener)

        val itemCount = player.mediaItemCount
        val items = mutableListOf<MediaItem>()

        for (i in 0 until itemCount) {
            items.add(player.getMediaItemAt(i))
        }

        secPlayer.setMediaItems(items)

        // Beat-aligned: start the incoming track on its first downbeat and
        // tempo-match it to the outgoing track for the overlap window. User tempo/pitch
        // from PlayerMenu is preserved by scaling on top of the current parameters.
        secPlayer.seekTo(targetIndex, plan?.incomingStartMs ?: 0)
        if (plan != null) {
            automixBaseParams = try { player.playbackParameters } catch (e: Exception) { PlaybackParameters.DEFAULT }
            if (plan.tempoRatio != 1f) {
                secPlayer.playbackParameters = PlaybackParameters(
                    automixBaseParams.speed * plan.tempoRatio,
                    automixBaseParams.pitch,
                )
            } else if (automixBaseParams != PlaybackParameters.DEFAULT) {
                secPlayer.playbackParameters = automixBaseParams
            }
        }
        secPlayer.volume = 0f


        secPlayer.repeatMode = savedRepeatMode
        secPlayer.shuffleModeEnabled = savedShuffleEnabled

        secPlayer.prepare()
        secPlayer.playWhenReady = true

        performCrossfadeSwap()

        
        if (savedShuffleEnabled) {
            val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }
    }

    private fun performCrossfadeSwap() {
        isCrossfading.value = true
        isAutomixing.value = activeAutomixPlan != null
        if (activeAutomixPlan != null) {
            automixDebugInfo.value = automixDebugInfo.value?.copy(status = "automixing now")
        }
        val nextPlayer = secondaryPlayer ?: return
        val currentPlayer = player

        fadingPlayer = currentPlayer
        player = nextPlayer
        _playerFlow.value = player
        secondaryPlayer = null

        fadingPlayer?.removeListener(this)
        fadingPlayer?.removeListener(sleepTimer)

        
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isCrossfading.value && fadingPlayer != null) {
                    try {
                        if (isPlaying) {
                            fadingPlayer?.play()
                        } else {
                            fadingPlayer?.pause()
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error syncing fadingPlayer play state")
                    }
                } else {
                    player.removeListener(this)
                }
            }
        })

        nextPlayer.removeListener(secondaryPlayerListener)
        nextPlayer.addListener(this)
        nextPlayer.addListener(sleepTimer)

        sleepTimer.player = player

        try {
            (mediaSession as MediaSession).player = player
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to swap player in MediaSession")
        }

        crossfadeJob = scope.launch {
            val djPlan = activeAutomixPlan
            val duration = djPlan?.overlapMs ?: crossfadeDuration.toLong()
            val steps = (duration / 100L).toInt().coerceIn(20, 150)
            val stepTime = duration / steps
            val startVolume = try { fadingPlayer?.volume ?: 1f } catch (e: Exception) { 1f }

            fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
                val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                return t * t * (3f - 2f * t)
            }

            try {
                for (i in 0..steps) {
                    if (!isActive) break

                    while (!player.isPlaying && isActive) {
                        delay(100)
                    }

                    val progress = i / steps.toFloat()
                    val fadeIn: Float
                    val fadeOut: Float
                    if (djPlan != null) {
                        // DJ blend: incoming rises to full by ~55%, outgoing holds until
                        // ~45% then drops — both near full mid-blend, beat-locked.
                        fadeIn = smoothstep(0f, 0.55f, progress)
                        fadeOut = 1f - smoothstep(0.45f, 1f, progress)
                    } else {
                        fadeIn = 1.0f - (1.0f - progress) * (1.0f - progress)
                        fadeOut = (1.0f - progress) * (1.0f - progress)
                    }

                    try {
                        player.volume = startVolume * fadeIn
                        fadingPlayer?.volume = startVolume * fadeOut
                    } catch (e: Exception) { break }

                    delay(stepTime)
                }
            } finally {
                try {
                    fadingPlayer?.volume = 0f
                    player.volume = startVolume
                } catch (e: Exception) {
                    Timber.tag(TAG).d(e, "Crossfade volume reset skipped, player likely released")
                }
                cleanupCrossfade()
                rampTempoToNormal()
            }
        }
    }

    /** After a tempo-matched overlap, ease the (now primary) player back to the base speed. */
    private fun rampTempoToNormal() {
        val plan = activeAutomixPlan ?: return
        activeAutomixPlan = null
        if (plan.tempoRatio == 1f) return
        val base = automixBaseParams
        scope.launch {
            val steps = 10
            val startSpeed = base.speed * plan.tempoRatio
            try {
                for (i in 1..steps) {
                    if (!isActive) break
                    val speed = startSpeed + (base.speed - startSpeed) * i / steps
                    try {
                        player.playbackParameters = PlaybackParameters(speed, base.pitch)
                    } catch (e: Exception) { break }
                    delay(200)
                }
            } finally {
                try {
                    player.playbackParameters = base
                } catch (e: Exception) {
                    Timber.tag(TAG).d(e, "Tempo ramp-back skipped, player likely released")
                }
            }
        }
    }

    private fun cleanupCrossfade() {
        fadingPlayer?.stop()
        fadingPlayer?.clearMediaItems()
        fadingPlayer?.release()
        fadingPlayer = null
        isCrossfading.value = false
        isAutomixing.value = false
        sleepTimer.notifySongTransition()
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val YOUTUBE_PLAYLIST = "youtube_playlist"
        const val SEARCH = "search"
        const val SHUFFLE_ACTION = "__shuffle__"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val MAX_RETRY_COUNT = 10
        
        private const val MAX_GAIN_MB = 300 
        private const val MIN_GAIN_MB = -1500 

        private const val TAG = "MusicService"

        @Volatile
        var isRunning = false
            private set
    }

    private var preloadJob: kotlinx.coroutines.Job? = null

    private fun preloadUpcomingItems() {
        val preloadEnabled = cachedPreloadEnabled
        if (!preloadEnabled) return

        val preloadLimit = cachedPreloadLimit
        val preloadLyrics = cachedPreloadLyrics

        val currentIndex = player.currentMediaItemIndex
        if (currentIndex == androidx.media3.common.C.INDEX_UNSET) return

        val limit = kotlin.math.min(preloadLimit, player.mediaItemCount - currentIndex - 1)
        if (limit <= 0) return

        val upcomingMediaIds = mutableListOf<String>()
        for (i in 1..limit) {
            upcomingMediaIds.add(player.getMediaItemAt(currentIndex + i).mediaId)
        }

        preloadJob?.cancel()
        preloadJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            for (mediaId in upcomingMediaIds) {

                val isFullyDownloaded = downloadCache.getCachedSpans(mediaId).isNotEmpty()
                if (!mediaId.isLocalMediaId() && !songUrlCache.containsKey("${mediaId}_${audioQuality.name}") && !isFullyDownloaded) {
                    Timber.tag(TAG).d("Preloading stream for $mediaId")
                    kotlin.runCatching {
                        val dbSong = database.song(mediaId).firstOrNull()
                        val knownArtist = dbSong?.artists?.joinToString(separator = ", ") { artist -> artist.name }?.replace(" - Topic", "")
                        
                        val playbackData = iad1tya.echo.music.utils.YTPlayerUtils.playerResponseForPlayback(
                            videoId = mediaId,
                            audioQuality = audioQuality,
                            connectivityManager = connectivityManager,
                            context = this@MusicService,
                            knownArtist = knownArtist,
                            knownTitle = dbSong?.song?.title,
                            knownDurationMs = dbSong?.song?.duration?.let { if (it > 0) it * 1000L else null }
                        )

                        playbackData.getOrNull()?.streamUrl?.let { streamUrl ->
                            songUrlCache["${mediaId}_${audioQuality.name}"] = Pair(streamUrl, System.currentTimeMillis() + 1000 * 60 * 60)
                            Timber.tag(TAG).d("Preloaded stream for $mediaId")
                        }
                    }
                }

                if (preloadLyrics) {
                    val dbLyrics = database.lyrics(mediaId).firstOrNull()
                    if (dbLyrics == null) {
                        Timber.tag(TAG).d("Preloading lyrics for $mediaId")
                        val dbSong = database.song(mediaId).firstOrNull()
                        if (dbSong != null) {
                            kotlin.runCatching {
                                val metadata = iad1tya.echo.music.models.MediaMetadata(
                                    id = dbSong.song.id,
                                    title = dbSong.song.title,
                                    artists = dbSong.artists.map { artist -> iad1tya.echo.music.models.MediaMetadata.Artist(artist.id, artist.name) },
                                    duration = dbSong.song.duration,
                                    thumbnailUrl = dbSong.thumbnailUrl
                                )
                                val lyricsResult = lyricsHelper.getLyrics(metadata)
                                database.query {
                                    upsert(iad1tya.echo.music.db.entities.LyricsEntity(id = mediaId, lyrics = lyricsResult.lyrics))
                                }
                                Timber.tag(TAG).d("Preloaded lyrics for $mediaId")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndSubmitListenBrainzFinished() {
        listenBrainzCurrentMediaId?.let { mediaId ->
            val startTs = listenBrainzCurrentStartTs
            if (startTs > 0) {
                scope.launch {
                    val mediaMetadata = player.mediaItems.find { it.mediaId == mediaId }?.metadata
                    val dbSong = if (mediaMetadata == null) database.song(mediaId).firstOrNull() else null
                    
                    val title = mediaMetadata?.title ?: dbSong?.song?.title ?: return@launch
                    val artistNames = mediaMetadata?.artists?.joinToString(" & ") { it.name } 
                        ?: dbSong?.artists?.joinToString(" & ") { it.name } ?: ""
                    val releaseName = mediaMetadata?.album?.title ?: dbSong?.album?.title ?: ""
                    val durationMs = mediaMetadata?.duration?.takeIf { it != -1 }?.times(1000L) 
                        ?: dbSong?.song?.duration?.takeIf { it != -1 }?.times(1000L) ?: 0L

                    updateListenBrainz(title, artistNames, releaseName, durationMs, isFinished = true, startMs = startTs, endMs = System.currentTimeMillis())
                }
            }
        }
        listenBrainzCurrentStartTs = 0L
        listenBrainzCurrentMediaId = null
    }

    private fun checkAndSubmitListenBrainzPlayingNow(mediaId: String) {
        scope.launch {
            val mediaMetadata = player.mediaItems.find { it.mediaId == mediaId }?.metadata
            val dbSong = if (mediaMetadata == null) database.song(mediaId).firstOrNull() else null
            
            val title = mediaMetadata?.title ?: dbSong?.song?.title ?: return@launch
            val artistNames = mediaMetadata?.artists?.joinToString(" & ") { it.name } 
                ?: dbSong?.artists?.joinToString(" & ") { it.name } ?: ""
            val releaseName = mediaMetadata?.album?.title ?: dbSong?.album?.title ?: ""
            val durationMs = mediaMetadata?.duration?.takeIf { it != -1 }?.times(1000L) 
                ?: dbSong?.song?.duration?.takeIf { it != -1 }?.times(1000L) ?: 0L

            updateListenBrainz(title, artistNames, releaseName, durationMs, isFinished = false)
        }
    }
}
