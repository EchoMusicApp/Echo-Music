@file:Suppress("DEPRECATION")

package iad1tya.echo.music.playback

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.SQLException
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.provider.Settings
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
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
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken

import com.google.android.gms.cast.framework.CastContext
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
import com.echo.innertube.YouTube
import iad1tya.echo.music.dlna.DLNAManager
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import iad1tya.echo.music.MainActivity
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.AudioNormalizationKey
import iad1tya.echo.music.constants.AudioEngineMode
import iad1tya.echo.music.constants.AudioEngineModeKey
import iad1tya.echo.music.constants.AudioOffload
import iad1tya.echo.music.constants.AudioQualityKey
import iad1tya.echo.music.constants.AutoDownloadOnLikeKey
import iad1tya.echo.music.constants.AutoLoadMoreKey
import iad1tya.echo.music.constants.AutoSkipNextOnErrorKey
import iad1tya.echo.music.constants.CrossfadeDurationKey
import iad1tya.echo.music.constants.CrossfadeEnabledKey
import iad1tya.echo.music.constants.CrossfadeGaplessKey
import iad1tya.echo.music.constants.DiscordActivityNameKey
import iad1tya.echo.music.constants.DiscordActivityTypeKey
import iad1tya.echo.music.constants.DiscordAdvancedModeKey
import iad1tya.echo.music.constants.DiscordButton1TextKey
import iad1tya.echo.music.constants.DiscordButton1VisibleKey
import iad1tya.echo.music.constants.DiscordButton2TextKey
import iad1tya.echo.music.constants.DiscordButton2VisibleKey
import iad1tya.echo.music.constants.DiscordStatusKey
import iad1tya.echo.music.constants.DiscordTokenKey
import iad1tya.echo.music.constants.DiscordUseDetailsKey
import iad1tya.echo.music.constants.EnableDiscordRPCKey
import iad1tya.echo.music.constants.EnableLastFMScrobblingKey
import iad1tya.echo.music.constants.LastFMUseNowPlaying
import iad1tya.echo.music.constants.ScrobbleDelayPercentKey
import iad1tya.echo.music.constants.ScrobbleDelaySecondsKey
import iad1tya.echo.music.constants.ScrobbleMinSongDurationKey
import com.metrolist.lastfm.LastFM
import iad1tya.echo.music.utils.DiscordRPC
import iad1tya.echo.music.utils.ScrobbleManager
import android.os.Handler
import android.os.Looper
import iad1tya.echo.music.constants.DisableLoadMoreWhenRepeatAllKey
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.HistoryDuration
import iad1tya.echo.music.constants.KeepScreenOn
import iad1tya.echo.music.constants.HideVideoSongsKey
import iad1tya.echo.music.constants.HideYoutubeShortsKey
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleLike
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleRepeatMode
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleShuffle
import iad1tya.echo.music.constants.MediaSessionConstants.CommandToggleStartRadio
import iad1tya.echo.music.constants.MusicHapticsEnabledKey
import iad1tya.echo.music.constants.PauseListenHistoryKey
import iad1tya.echo.music.constants.PauseOnMute
import iad1tya.echo.music.constants.PersistentQueueKey
import iad1tya.echo.music.constants.PlayerVolumeKey
import iad1tya.echo.music.constants.PreventDuplicateTracksInQueueKey
import iad1tya.echo.music.constants.ProEqEnabledKey
import iad1tya.echo.music.constants.ProEqGainDbKey
import iad1tya.echo.music.constants.EqualizerBandLevelsMbKey
import iad1tya.echo.music.constants.EqualizerBassBoostEnabledKey
import iad1tya.echo.music.constants.EqualizerBassBoostStrengthKey
import iad1tya.echo.music.constants.EqualizerEnabledKey
import iad1tya.echo.music.constants.EqualizerOutputGainEnabledKey
import iad1tya.echo.music.constants.EqualizerOutputGainMbKey
import iad1tya.echo.music.constants.EqualizerSelectedProfileIdKey
import iad1tya.echo.music.constants.EqualizerVirtualizerEnabledKey
import iad1tya.echo.music.constants.EqualizerVirtualizerStrengthKey
import iad1tya.echo.music.constants.RememberShuffleAndRepeatKey
import iad1tya.echo.music.constants.RepeatModeKey
import iad1tya.echo.music.constants.ResumeOnBluetoothConnectKey
import iad1tya.echo.music.constants.ShuffleModeKey
import iad1tya.echo.music.constants.ShowLyricsKey
import iad1tya.echo.music.constants.SpatialAudioEnabledKey
import iad1tya.echo.music.constants.SpatialAudioStrengthKey
import iad1tya.echo.music.constants.AudioArEnabledKey
import iad1tya.echo.music.constants.AudioArAutoCalibrateKey
import iad1tya.echo.music.constants.AudioArSensitivityKey
import iad1tya.echo.music.constants.SimilarContent
import iad1tya.echo.music.constants.SkipSilenceKey
import iad1tya.echo.music.constants.SponsorBlockEnabledKey
import iad1tya.echo.music.api.SponsorBlockService
import android.widget.Toast
import iad1tya.echo.music.constants.ForceStopOnTaskClearKey
import iad1tya.echo.music.constants.StopMusicOnTaskClearKey
import iad1tya.echo.music.constants.TTSAnnouncementEnabledKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.Event
import iad1tya.echo.music.db.entities.FormatEntity
import iad1tya.echo.music.db.entities.LyricsEntity
import iad1tya.echo.music.db.entities.RelatedSongMap
import iad1tya.echo.music.di.DownloadCache
import iad1tya.echo.music.di.PlayerCache
import iad1tya.echo.music.extensions.SilentHandler
import iad1tya.echo.music.extensions.collect
import iad1tya.echo.music.extensions.collectLatest
import iad1tya.echo.music.extensions.currentMetadata
import iad1tya.echo.music.extensions.findNextMediaItemById
import iad1tya.echo.music.extensions.mediaItems
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.extensions.setOffloadEnabled
import iad1tya.echo.music.extensions.toMediaItem
import iad1tya.echo.music.extensions.toPersistQueue
import iad1tya.echo.music.extensions.toQueue
import iad1tya.echo.music.lyrics.LyricsHelper
import iad1tya.echo.music.lyrics.LyricsPreloadManager
import iad1tya.echo.music.models.PersistPlayerState
import iad1tya.echo.music.models.PersistQueue
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.queues.EmptyQueue
import iad1tya.echo.music.playback.queues.Queue
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.playback.queues.filterExplicit
import iad1tya.echo.music.playback.queues.filterVideoSongs
import iad1tya.echo.music.utils.CoilBitmapLoader
import iad1tya.echo.music.utils.NetworkConnectivityObserver
import iad1tya.echo.music.utils.SyncUtils
import iad1tya.echo.music.utils.TTSManager
import iad1tya.echo.music.utils.MusicHapticsManager
import iad1tya.echo.music.utils.YTPlayerUtils
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.SpatialAudioManager
import iad1tya.echo.music.utils.enumPreference
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.reportException
import iad1tya.echo.music.widget.MusicWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
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
    lateinit var ttsManager: TTSManager

    @Inject
    lateinit var hapticsManager: MusicHapticsManager

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false

    private var scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)
    private var lyricsPreloadManager: LyricsPreloadManager? = null

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        iad1tya.echo.music.constants.AudioQuality.AUTO
    )
    private val audioEngineMode by enumPreference(
        this,
        AudioEngineModeKey,
        AudioEngineMode.STANDARD
    )

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

    val playerVolume = MutableStateFlow(1f)
    val eqCapabilities = MutableStateFlow<EqCapabilities?>(null)

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    
    // Google Cast support
    lateinit var castConnectionHandler: CastConnectionHandler
    
    // DLNA/UPnP support
    @Inject
    lateinit var dlnaManager: DLNAManager

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var spatialAudioManager: SpatialAudioManager? = null

    private var lastPlaybackSpeed = 1.0f

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private var consecutivePlaybackErr = 0
    private var retryJob: Job? = null

    private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    // Enhanced error tracking for strict retry management
    private var currentMediaIdRetryCount = mutableMapOf<String, Int>()
    private val MAX_RETRY_PER_SONG = 3
    private val RETRY_DELAY_MS = 1000L

    // Track failed songs to prevent infinite retry loops
    private val recentlyFailedSongs = mutableSetOf<String>()
    private var failedSongsClearJob: Job? = null

    // Pause on mute state
    private var wasPlayingBeforeVolumeMute = false
    private var isPausedByVolumeMute = false
    private var volumeObserver: ContentObserver? = null

    // Discord RPC
    private var discordRpc: DiscordRPC? = null
    private var discordUpdateJob: Job? = null

    // Last.fm scrobbling
    private var scrobbleManager: ScrobbleManager? = null

    // Crossfade state
    private var crossfadeEnabled = false

    // Haptics polling
    private var hapticsPollingJob: Job? = null
    private var musicHapticsEnabled = false
    private var crossfadeDuration = 3000L // ms
    private var crossfadeGapless = false
    private var crossfadeTriggerJob: Job? = null
    private var crossfadeOutJob: Job? = null
    private var crossfadeInJob: Job? = null
    private var isCrossfadingIn = false
    private var fadingPlayer: ExoPlayer? = null
    private var lastCrossfadeTrackDuration = C.TIME_UNSET
    private var pendingManualSkipCrossfadeIn = false
    private var proAudioRetryJob: Job? = null
    val isCrossfading = MutableStateFlow(false)

    // Bluetooth resume callback
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val hasBluetooth = addedDevices?.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } == true
            if (hasBluetooth && dataStore.get(ResumeOnBluetoothConnectKey, false)) {
                // Resume regardless of buffering state — audio routing change can briefly
                // push the player into STATE_BUFFERING, so don't gate on STATE_READY.
                if (!player.isPlaying) {
                    player.play()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
                ACTION_NEXT -> if (player.hasNextMediaItem()) {
                    if (isWithinCrossfadeWindow()) {
                        pendingManualSkipCrossfadeIn = true
                    }
                    player.seekToNext()
                }
                ACTION_PREVIOUS -> if (player.hasPreviousMediaItem()) player.seekToPrevious()
            }
        }

        try {
            return super.onStartCommand(intent, flags, startId)
        } catch (e: Exception) {
            // Check if it's the specific foreground service exception (available in API 31+)
            if (Build.VERSION.SDK_INT >= 31 && e.javaClass.name.contains("ForegroundServiceStartNotAllowedException")) {
                Log.e("MusicService", "ForegroundServiceStartNotAllowedException caught", e)
                // Stop the service to prevent ANR/Crash loop if possible, though system might have already killed it
                stopSelf()
                return START_NOT_STICKY
            }
            throw e
        }
    }

    override fun onCreate() {
        // Fix for NPE: Initialize connectivityManager early (Issue 6, 7)
        connectivityManager = getSystemService() ?: run {
             Log.e("MusicService", "ConnectivityManager not available")
             stopSelf()
             return
        }
        connectivityObserver = NetworkConnectivityObserver(this)

        scope.launch {
            playerVolume.value = dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f)
        }

        super.onCreate()
        lyricsPreloadManager = LyricsPreloadManager(
            context = this,
            database = database,
            networkConnectivity = connectivityObserver,
            lyricsHelper = lyricsHelper,
        )
        try {
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.small_icon)
                },
        )
        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                    setOffloadEnabled(dataStore.get(AudioOffload, false))
                }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        // Register Bluetooth audio device callback for auto-resume
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        // Register ContentObserver for pause-on-mute — more reliable than
        // Player.Listener.onDeviceVolumeChanged which only fires for ExoPlayer API calls.
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (!dataStore.get(PauseOnMute, false)) return
                val streamVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.isStreamMute(AudioManager.STREAM_MUSIC) || streamVol == 0
                } else {
                    streamVol == 0
                }
                if (isMuted) {
                    if (player.isPlaying) {
                        wasPlayingBeforeVolumeMute = true
                        isPausedByVolumeMute = true
                        player.pause()
                    }
                } else {
                    if (wasPlayingBeforeVolumeMute && isPausedByVolumeMute && !player.isPlaying) {
                        wasPlayingBeforeVolumeMute = false
                        isPausedByVolumeMute = false
                        player.play()
                    }
                }
            }
        }
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )

        // Initialize crossfade settings
        crossfadeEnabled = dataStore.get(CrossfadeEnabledKey, false)
        crossfadeDuration = ((dataStore.get(CrossfadeDurationKey, 3f)) * 1000).toLong()
        crossfadeGapless = dataStore.get(CrossfadeGaplessKey, false)

        // Watch crossfade preference changes
        scope.launch {
            dataStore.data.collect { prefs ->
                crossfadeEnabled = prefs[CrossfadeEnabledKey] ?: false
                crossfadeDuration = ((prefs[CrossfadeDurationKey] ?: 3f) * 1000).toLong()
                crossfadeGapless = prefs[CrossfadeGaplessKey] ?: false
            }
        }
        
        // Initialize Google Cast handler
        castConnectionHandler = CastConnectionHandler(this, scope, this)
        
        // Initialize Google Cast - only if permissions are granted
        // On Android 12 and below, location permission is required for Cast device discovery
        try {
            val hasRequiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+: Only NEARBY_WIFI_DEVICES permission is needed
                ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 12 and below: Location permission is required
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
            
            if (hasRequiredPermissions) {
                castConnectionHandler.initialize()
                Log.d("MusicService", "CastConnectionHandler initialized")
            } else {
                Log.d("MusicService", "Skipping Cast initialization - required permissions not granted")
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to initialize Cast: ${e.message}", e)
        }
        
        // Initialize Spatial Audio Manager (Audio AR)
        try {
            spatialAudioManager = SpatialAudioManager(this, dataStore)
            spatialAudioManager?.loadCalibrationPoint()
            Log.d("MusicService", "SpatialAudioManager initialized")
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to initialize SpatialAudioManager: ${e.message}", e)
        }
        
        // Initialize DLNA
        try {
            if (::dlnaManager.isInitialized) {
                dlnaManager.start()
                Log.d("MusicService", "DLNA service initialized")
                
                // Monitor DLNA device selection changes
                scope.launch {
                    dlnaManager.selectedDevice.collect { device ->
                        if (device != null) {
                            Log.d("MusicService", "DLNA device selected: ${device.name}")
                            // If currently playing, stream to DLNA device
                            if (player.playbackState == Player.STATE_READY && player.currentMediaItem != null) {
                                val metadata = currentMediaMetadata.value
                                val mediaUrl = player.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
                                
                                if (mediaUrl.isNotEmpty()) {
                                    val success = dlnaManager.playMedia(
                                        mediaUrl = mediaUrl,
                                        title = metadata?.title ?: "",
                                        artist = metadata?.artists?.firstOrNull()?.name ?: ""
                                    )
                                    
                                    if (success) {
                                        // Pause local player
                                        player.pause()
                                    }
                                }
                            }
                        } else {
                            Log.d("MusicService", "DLNA device deselected, resuming local playback")
                            // Resume local playback if it was paused for DLNA
                            if (player.playbackState == Player.STATE_READY && !player.playWhenReady) {
                                player.play()
                            }
                        }
                    }
                }
            } else {
                Log.w("MusicService", "DLNA manager not initialized by Hilt")
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to initialize DLNA: ${e.message}", e)
            reportException(e)
        }

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

        // Remember shuffle mode across restarts
        if (dataStore.get(RememberShuffleAndRepeatKey, true)) {
            player.shuffleModeEnabled = dataStore.get(ShuffleModeKey, false)
        }

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        // connectivityManager initialized at start of onCreate
        // connectivityObserver initialized at start of onCreate

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    // Simple auto-play logic like OuterTune
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        playerVolume.collectLatest(scope) {
            mediaSession.player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
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
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                // Check again if lyrics were added manually during the fetch duration
                if (database.lyrics(mediaMetadata.id).first() == null) {
                    database.query {
                        upsert(
                            LyricsEntity(
                                id = mediaMetadata.id,
                                lyrics = lyrics,
                            ),
                        )
                    }
                }
            }
        }

        // SponsorBlock Integration
        val currentSkipSegments = MutableStateFlow<List<SponsorBlockService.Segment>>(emptyList())
        
        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[SponsorBlockEnabledKey] ?: false }.distinctUntilChanged()
        ) { media, enabled ->
             media to enabled
        }.collectLatest(scope) { (media, enabled) ->
            if (enabled && media != null) {
                currentSkipSegments.value = SponsorBlockService.getSkipSegments(media.id)
            } else {
                currentSkipSegments.value = emptyList()
            }
        }

        // SponsorBlock Skipper
        scope.launch {
            while (isActive) {
                delay(1000L)
                if (player.isPlaying && currentSkipSegments.value.isNotEmpty()) {
                    val position = player.currentPosition / 1000f
                    val segment = currentSkipSegments.value.firstOrNull { position >= it.start && position < it.end }
                    
                    if (segment != null) {
                        player.seekTo((segment.end * 1000).toLong())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MusicService, "Skipped ${segment.category}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        dataStore.data
            .map { it[MusicHapticsEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                musicHapticsEnabled = enabled
                updateMusicHapticsState()
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) -> setupLoudnessEnhancer()}

        dataStore.data
            .map {
                listOf(
                    it[EqualizerEnabledKey] ?: false,
                    it[ProEqEnabledKey] ?: false,
                    it[ProEqGainDbKey] ?: 0f,
                    it[EqualizerBandLevelsMbKey] ?: "",
                    it[EqualizerOutputGainEnabledKey] ?: false,
                    it[EqualizerOutputGainMbKey] ?: 0,
                    it[EqualizerBassBoostEnabledKey] ?: false,
                    it[EqualizerBassBoostStrengthKey] ?: 0,
                    it[EqualizerVirtualizerEnabledKey] ?: false,
                    it[EqualizerVirtualizerStrengthKey] ?: 0,
                    it[SpatialAudioEnabledKey] ?: false,
                    it[SpatialAudioStrengthKey] ?: 500,
                    it[AudioArEnabledKey] ?: false,
                    it[AudioEngineModeKey] ?: AudioEngineMode.STANDARD.name,
                )
            }
            .distinctUntilChanged()
            .collectLatest(scope) {
                setupProAudioEffects()
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                // Convert back to proper queue type
                val restoredQueue = queue.toQueue()
                playQueue(
                    queue = restoredQueue,
                    playWhenReady = false,
                )
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map { it.toMediaItem() }
            }

            // Restore player state
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistPlayerState
                    }
                }
            }.onSuccess { playerState ->
                // Restore player settings after queue is loaded
                scope.launch {
                    delay(1000) // Wait for queue to be loaded
                    player.repeatMode = playerState.repeatMode
                    player.shuffleModeEnabled = playerState.shuffleModeEnabled
                    player.volume = playerState.volume

                    // Restore position if it's still valid
                    if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                        player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                    }
                }
            }
        }

        // Save queue periodically to prevent queue loss from crash or force kill

        // Discord RPC initialization
        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            updateDiscordRPC(it, true)
                        }
                    }
                }
            }

        // Last.fm scrobble initialization
        dataStore.data
            .map { it[EnableLastFMScrobblingKey] ?: false }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled && scrobbleManager == null) {
                    val delayPercent = dataStore.get(ScrobbleDelayPercentKey, LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
                    val minSongDuration = dataStore.get(ScrobbleMinSongDurationKey, LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
                    val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)
                    scrobbleManager = ScrobbleManager(
                        scope,
                        minSongDuration = minSongDuration,
                        scrobbleDelayPercent = delayPercent,
                        scrobbleDelaySeconds = delaySeconds
                    )
                    scrobbleManager?.useNowPlaying = dataStore.get(LastFMUseNowPlaying, false)
                } else if (!enabled && scrobbleManager != null) {
                    scrobbleManager?.destroy()
                    scrobbleManager = null
                }
            }

        dataStore.data
            .map { it[LastFMUseNowPlaying] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                scrobbleManager?.useNowPlaying = it
            }

        dataStore.data
            .map { prefs ->
                Triple(
                    prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                    prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                    prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                )
            }
            .distinctUntilChanged()
            .collect(scope) { (delayPercent, minSongDuration, delaySeconds) ->
                scrobbleManager?.let {
                    it.scrobbleDelayPercent = delayPercent
                    it.minSongDuration = minSongDuration
                    it.scrobbleDelaySeconds = delaySeconds
                }
            }

        // Watch Discord customization preferences
        dataStore.data
            .map {
                listOf(
                    it[DiscordUseDetailsKey],
                    it[DiscordAdvancedModeKey],
                    it[DiscordStatusKey],
                    it[DiscordButton1TextKey],
                    it[DiscordButton1VisibleKey],
                    it[DiscordButton2TextKey],
                    it[DiscordButton2VisibleKey],
                    it[DiscordActivityTypeKey],
                    it[DiscordActivityNameKey]
                )
            }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) {
                if (player.playbackState == Player.STATE_READY) {
                    currentSong.value?.let { song ->
                        updateDiscordRPC(song, true)
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

        // Save queue more frequently when playing to ensure state is preserved
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
        } catch (e: Exception) {
            Log.e("MusicService", "Critical error during service initialization", e)
            reportException(e)
            // Try to cleanup partially initialized components
            try {
                if (::player.isInitialized) {
                    player.release()
                }
                if (::mediaSession.isInitialized) {
                    mediaSession.release()
                }
            } catch (cleanupError: Exception) {
                Log.e("MusicService", "Error during cleanup", cleanupError)
            }
            stopSelf()
            throw e // Re-throw to let system know service failed to start
        }
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
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }

                player.volume = playerVolume.value

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = false

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
                    player.volume = (playerVolume.value * 0.2f)
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {

                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }

                player.volume = playerVolume.value

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true

                player.volume = playerVolume.value

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

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }
    


    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        /**
         * Auto skip to the next media item on error.
         *
         * To prevent a "runaway diesel engine" scenario, force the user to take action after
         * too many errors come up too quickly. Pause to show player "stopped" state
         */
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
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
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
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
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
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
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
                }.let { status ->
                    // Filter video songs from initial queue
                    val hideVideos = dataStore.get(HideVideoSongsKey, false)
                    if (hideVideos) {
                        status.copy(items = status.items.filterVideoSongs(true))
                    } else status
                }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            if (queue.preloadItem != null) {
                player.addMediaItems(
                    0,
                    initialStatus.items.subList(0, initialStatus.mediaItemIndex)
                )
                player.addMediaItems(
                    initialStatus.items.subList(
                        initialStatus.mediaItemIndex + 1,
                        initialStatus.items.size
                    )
                )
            } else {
                player.setMediaItems(
                    initialStatus.items,
                    if (initialStatus.mediaItemIndex >
                        0
                    ) {
                        initialStatus.mediaItemIndex
                    } else {
                        0
                    },
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return

        // Save current song
        val currentSong = player.currentMediaItem

        // Remove other songs from queue
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            // Add radio songs after current song
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
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
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                YouTube
                    .next(WatchEndpoint(playlistId = playlistId))
                    .onSuccess {
                        YouTube
                            .next(WatchEndpoint(playlistId = it.endpoint.playlistId))
                            .onSuccess {
                                automixItems.value =
                                    it.items.map { song ->
                                        song.toMediaItem()
                                    }
                            }
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
        // Remove duplicate tracks from queue if enabled
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

        // If queue is empty or player is idle, play immediately instead
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            player.play()
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        // Insert items immediately after the current item in the window/index space
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            // Rebuild shuffle order so that newly inserted items are played next
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                // Newly inserted indices are a contiguous range [insertIndex, insertIndex + items.size)
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                // Collect existing shuffle traversal order excluding current index
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() // preserve original forward order

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                // Build new shuffle order: current -> newly inserted (in insertion order) -> rest
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                // Fill any missing indices (safety) to ensure a full permutation
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
        // Remove duplicate tracks from queue if enabled
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
        player.prepare()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                val song = it.song.toggleLike()
                update(song)
                syncUtils.likeSong(song)

                // Check if auto-download on like is enabled and the song is now liked
                if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                    // Trigger download for the liked song
                    val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                        .Builder(song.id, "echo://${song.id}".toUri())
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
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Log.w(TAG, "setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }

        // Create or recreate enhancer if needed
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Log.d(TAG, "LoudnessEnhancer created for sessionId=$audioSessionId")
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

                    val loudnessDb = format?.loudnessDb
                    val outputGainMb = if (dataStore.get(EqualizerOutputGainEnabledKey, false)) {
                        dataStore.get(EqualizerOutputGainMbKey, 0)
                    } else {
                        0
                    }

                    withContext(Dispatchers.Main) {
                        if (loudnessDb != null) {
                            val targetGain = (-loudnessDb * 100).toInt() + outputGainMb
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                                Log.d(TAG, "LoudnessEnhancer gain applied: $clampedGain mB")
                            } catch (e: Exception) {
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                            Log.w(TAG, "setupLoudnessEnhancer: loudnessDb is null, enhancer disabled")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                        Log.d(TAG, "setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
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
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun setupProAudioEffects() {
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) {
            if (proAudioRetryJob?.isActive != true) {
                proAudioRetryJob = scope.launch {
                    repeat(20) {
                        delay(150)
                        val retrySession = player.audioSessionId
                        if (retrySession != C.AUDIO_SESSION_ID_UNSET && retrySession > 0) {
                            setupProAudioEffects()
                            return@launch
                        }
                    }
                }
            }
            return
        }

        proAudioRetryJob?.cancel()
        proAudioRetryJob = null

        val proEqEnabled = dataStore.get(EqualizerEnabledKey, false) || dataStore.get(ProEqEnabledKey, false)
        val preampDb = dataStore.get(ProEqGainDbKey, 0f).coerceIn(-8f, 8f)
        val bandLevels = decodeBandLevelsMb(dataStore.get(EqualizerBandLevelsMbKey, ""))
        val bassBoostEnabled = dataStore.get(EqualizerBassBoostEnabledKey, false)
        val bassBoostStrength = dataStore.get(EqualizerBassBoostStrengthKey, 0).coerceIn(0, 1000)
        val virtualizerEnabled = dataStore.get(EqualizerVirtualizerEnabledKey, false)
        val virtualizerStrength = dataStore.get(EqualizerVirtualizerStrengthKey, 0).coerceIn(0, 1000)
        
        // Standard spatial audio
        val spatialEnabled = dataStore.get(SpatialAudioEnabledKey, false)
        val spatialStrength = dataStore.get(SpatialAudioStrengthKey, 500).coerceIn(0, 1000)
        
        // Audio AR (Augmented Reality) - advanced spatial audio with device rotation tracking
        val audioArEnabled = dataStore.get(AudioArEnabledKey, false)

        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, sessionId)
            }
            if (bassBoost == null) {
                bassBoost = BassBoost(0, sessionId)
            }

            val eq = equalizer
            if (eq != null) {
                val bands = eq.numberOfBands.toInt().coerceAtLeast(0)
                val range = runCatching { eq.bandLevelRange }.getOrNull()
                val minMb = range?.getOrNull(0)?.toInt() ?: -1500
                val maxMb = range?.getOrNull(1)?.toInt() ?: 1500
                val levels = resampleLevelsByIndex(bandLevels, bands)
                val gainMb = (preampDb * 100).toInt().coerceIn(-1000, 1000)

                eqCapabilities.value =
                    EqCapabilities(
                        bandCount = bands,
                        minBandLevelMb = minMb,
                        maxBandLevelMb = maxMb,
                        centerFreqHz = (0 until bands).map { band ->
                            (runCatching { eq.getCenterFreq(band.toShort()) }.getOrNull() ?: 0) / 1000
                        },
                        systemPresets = (0 until eq.numberOfPresets.toInt()).map { idx ->
                            runCatching { eq.getPresetName(idx.toShort()).toString() }.getOrNull() ?: "Preset ${idx + 1}"
                        },
                    )

                if (proEqEnabled) {
                    eq.enabled = true
                    for (i in 0 until bands) {
                        val bandLevelMb = levels.getOrNull(i)?.coerceIn(minMb, maxMb) ?: (gainMb)
                        eq.setBandLevel(i.toShort(), (bandLevelMb + gainMb).coerceIn(minMb, maxMb).toShort())
                    }
                } else {
                    eq.enabled = false
                }
            }

            bassBoost?.let { bb ->
                runCatching { bb.enabled = bassBoostEnabled }
                runCatching { bb.setStrength(bassBoostStrength.toShort()) }
            }

            virtualizer?.let { v ->
                runCatching { v.enabled = virtualizerEnabled }
                runCatching { v.setStrength(virtualizerStrength.toShort()) }
            }
        } catch (e: Exception) {
            reportException(e)
            eqCapabilities.value = null
        }

        // Apply spatial audio effects (standard + Audio AR)
        if (spatialEnabled || audioArEnabled) {
            try {
                if (virtualizer == null) {
                    virtualizer = Virtualizer(0, sessionId)
                }
                virtualizer?.strengthSupported?.let { supported ->
                    if (supported) {
                        var effectiveStrength = spatialStrength
                        
                        // Enhance strength with Audio AR if enabled
                        if (audioArEnabled && spatialAudioManager != null) {
                            // Modulate strength based on soundstage rotation and depth
                            val rotationInfluence = Math.abs(spatialAudioManager?.currentSoundstageRotation ?: 0f) / 180f
                            val depthInfluence = spatialAudioManager?.currentSoundstageDepth ?: 1f
                            val audioArBoost = (1f + rotationInfluence * 0.3f) * depthInfluence
                            effectiveStrength = (spatialStrength * audioArBoost).toInt().coerceIn(0, 1000)
                        }
                        
                        virtualizer?.setStrength(effectiveStrength.toShort())
                    }
                }
                virtualizer?.enabled = true
                
                if (audioArEnabled) {
                    Log.d("MusicService", "Pro Audio: Spatial Audio AR enabled - Rotation: ${spatialAudioManager?.currentSoundstageRotation}°, Depth: ${spatialAudioManager?.currentSoundstageDepth}x")
                }
            } catch (e: Exception) {
                reportException(e)
            }
        } else {
            virtualizer?.enabled = false
        }
    }

    fun applyEqFlatPreset() {
        scope.launch {
            val caps = eqCapabilities.value
            val bandCount = caps?.bandCount ?: runCatching { equalizer?.numberOfBands?.toInt() }.getOrNull() ?: 0
            val encoded = encodeBandLevelsMb(List(bandCount.coerceAtLeast(0)) { 0 })
            dataStore.edit { prefs ->
                prefs[EqualizerEnabledKey] = true
                prefs[EqualizerBandLevelsMbKey] = encoded
                prefs[EqualizerSelectedProfileIdKey] = "flat"
                prefs[ProEqEnabledKey] = true
            }
        }
    }

    private fun ensureAudioEffects(sessionId: Int) {
        if (sessionId <= 0) return

        if (equalizer == null) {
            equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        }
        if (bassBoost == null) {
            bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        }
        if (virtualizer == null) {
            virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        }
        if (loudnessEnhancer == null) {
            loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        }

        equalizer?.let { eq ->
            val bandCount = eq.numberOfBands.toInt().coerceAtLeast(0)
            val range = runCatching { eq.bandLevelRange }.getOrNull()
            val minMb = range?.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range?.getOrNull(1)?.toInt() ?: 1500
            val center = (0 until bandCount).map { band ->
                (runCatching { eq.getCenterFreq(band.toShort()) }.getOrNull() ?: 0) / 1000
            }
            val presets = (0 until eq.numberOfPresets.toInt()).map { idx ->
                runCatching { eq.getPresetName(idx.toShort()).toString() }.getOrNull() ?: "Preset ${idx + 1}"
            }
            eqCapabilities.value = EqCapabilities(bandCount, minMb, maxMb, center, presets)
        }
    }

    fun applySystemEqPreset(presetIndex: Int) {
        scope.launch {
            ensureAudioEffects(player.audioSessionId)
            val eq = equalizer ?: return@launch
            val maxPreset = runCatching { eq.numberOfPresets.toInt() }.getOrNull() ?: 0
            if (presetIndex !in 0 until maxPreset) return@launch

            runCatching { eq.usePreset(presetIndex.toShort()) }.getOrNull() ?: return@launch

            val bandCount = runCatching { eq.numberOfBands.toInt() }.getOrNull() ?: 0
            val levels = (0 until bandCount).map { band ->
                runCatching { eq.getBandLevel(band.toShort()).toInt() }.getOrNull() ?: 0
            }

            val encoded = encodeBandLevelsMb(levels)
            if (encoded.isBlank()) return@launch

            dataStore.edit { prefs ->
                prefs[EqualizerEnabledKey] = true
                prefs[EqualizerBandLevelsMbKey] = encoded
                prefs[EqualizerSelectedProfileIdKey] = "system:$presetIndex"
                prefs[ProEqEnabledKey] = true
            }
        }
    }

    /**
     * Recenter the Audio AR soundstage to the current device orientation.
     */
    fun recenterAudioAr() {
        spatialAudioManager?.recenter()
        Log.d("MusicService", "Audio AR recentered")
    }

    private fun releaseProAudioEffects() {
        proAudioRetryJob?.cancel()
        proAudioRetryJob = null

        try {
            equalizer?.release()
        } catch (e: Exception) {
            reportException(e)
        } finally {
            equalizer = null
        }

        try {
            bassBoost?.release()
        } catch (e: Exception) {
            reportException(e)
        } finally {
            bassBoost = null
        }

        try {
            virtualizer?.release()
        } catch (e: Exception) {
            reportException(e)
        } finally {
            virtualizer = null
        }

        eqCapabilities.value = null
    }

    // hapticsPollingJob retained for cancellation in onDestroy; polling is no longer
    // needed — the Visualizer in MusicHapticsManager fires its own callbacks.
    private fun startHapticsPolling() { /* no-op: Visualizer drives callbacks directly */ }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        setupProAudioEffects()
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
        releaseProAudioEffects()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        lastPlaybackSpeed = -1.0f // force update song

        setupLoudnessEnhancer()
        setupProAudioEffects()

        // Schedule crossfade for next transition
        scheduleCrossfade()

        // If user skipped near track end, fade in the next track so transition isn't abrupt.
        if (pendingManualSkipCrossfadeIn && crossfadeEnabled) {
            pendingManualSkipCrossfadeIn = false
            startManualSkipCrossfadeIn()
        }

        // Last.fm scrobble on track change
        scrobbleManager?.onSongStop()
        if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
            scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
        }

        // TTS Song Announcement
        if (dataStore.get(TTSAnnouncementEnabledKey, false) && mediaItem != null) {
            val metadata = player.currentMetadata
            val title = metadata?.title ?: ""
            val artist = metadata?.artists?.firstOrNull()?.name ?: ""
            if (title.isNotEmpty()) {
                val announcement = if (artist.isNotEmpty()) "Now playing $title by $artist" else "Now playing $title"
                ttsManager.speak(announcement)
            }
        }
        
        // Stream to DLNA device if selected
        scope.launch {
            try {
                val selectedDevice = dlnaManager.selectedDevice.value
                if (selectedDevice != null && mediaItem != null) {
                    val metadata = currentMediaMetadata.value
                    val mediaUrl = mediaItem.localConfiguration?.uri?.toString() ?: ""
                    
                    if (mediaUrl.isNotEmpty()) {
                        Log.d("MusicService", "Streaming to DLNA device: ${selectedDevice.name}")
                        val success = dlnaManager.playMedia(
                            mediaUrl = mediaUrl,
                            title = metadata?.title ?: "",
                            artist = metadata?.artists?.firstOrNull()?.name ?: ""
                        )
                        
                        if (success) {
                            // Pause local player when streaming to DLNA
                            player.pause()
                            Log.d("MusicService", "Successfully started DLNA playback")
                        } else {
                            Log.e("MusicService", "Failed to start DLNA playback")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error streaming to DLNA: ${e.message}", e)
            }
        }
        
        // Update widget
        updateWidget()

        val queue = player.mediaItems.mapNotNull { it.metadata }
        if (queue.isNotEmpty()) {
            lyricsPreloadManager?.onSongChanged(player.currentMediaItemIndex, queue)
        }

        // Auto load more songs
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems =
                    currentQueue.nextPage()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }

        // Save state when media item changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        // Save state when playback state changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }

        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()
        }
        
        // Reset consecutive error counter when playback is successful
        if (playbackState == Player.STATE_READY) {
            consecutivePlaybackErr = 0
            // Reset retry count for current song on successful playback
            player.currentMediaItem?.mediaId?.let { mediaId ->
                resetRetryCount(mediaId)
            }

            setupProAudioEffects()
        }
        
        // Update widget
        updateWidget()

        updateMusicHapticsState()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            setupLoudnessEnhancer()
            setupProAudioEffects()
        }

        // Music Haptics should react immediately to play/pause as well.
        updateMusicHapticsState()

        // Update widget
        updateWidget()
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
        setupLoudnessEnhancer()
        setupProAudioEffects()
        updateMusicHapticsState()
    }

    private fun updateMusicHapticsState() {
        if (!musicHapticsEnabled) {
            hapticsManager.stop()
            return
        }

        if (player.playWhenReady && player.playbackState == Player.STATE_READY && player.audioSessionId > 0) {
            hapticsManager.start(player.audioSessionId)
        } else {
            hapticsManager.stop()
        }
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
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

        // Last.fm scrobble state tracking
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            scrobbleManager?.onPlayerStateChanged(player.isPlaying, player.currentMetadata, duration = player.duration)
        }

        // Discord RPC updates
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (!player.isPlaying && !events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION
                )
            ) {
                scope.launch {
                    discordRpc?.close()
                }
            }
        }
        if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_IS_PLAYING_CHANGED
            ) && player.isPlaying
        ) {
            val mediaId = player.currentMetadata?.id
            if (mediaId != null) {
                scope.launch {
                    database.song(mediaId).first()?.let { song ->
                        updateDiscordRPC(song)
                    }
                }
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)

        if (!crossfadeEnabled) return
        if (reason != Player.DISCONTINUITY_REASON_SEEK) return
        if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) return

        val windowStart = (lastCrossfadeTrackDuration - crossfadeDuration).coerceAtLeast(0L)
        if (lastCrossfadeTrackDuration > 0L && lastCrossfadeTrackDuration != C.TIME_UNSET && oldPosition.positionMs >= windowStart) {
            pendingManualSkipCrossfadeIn = true
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // If queue is empty, don't shuffle
            if (player.mediaItemCount == 0) return

            // Always put current playing item at first
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }

        // Save state when shuffle mode changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }

        // Persist shuffle mode
        if (dataStore.get(RememberShuffleAndRepeatKey, true)) {
            scope.launch {
                dataStore.edit { settings ->
                    settings[ShuffleModeKey] = shuffleModeEnabled
                }
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Save state when repeat mode changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        super.onPlaybackParametersChanged(playbackParameters)
        if (playbackParameters.speed != lastPlaybackSpeed) {
            lastPlaybackSpeed = playbackParameters.speed
        }
    }

    // onDeviceVolumeChanged is intentionally NOT used for pause-on-mute because it only
    // fires for volume changes made through ExoPlayer's own APIs, not hardware volume keys.
    // The ContentObserver registered in onCreate is the reliable replacement.

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.e("MusicService", "Playback error: ${error.message}", error)

        try {
            val mediaId = player.currentMediaItem?.mediaId

            // Check if this song has failed too many times
            if (mediaId != null && hasExceededRetryLimit(mediaId)) {
                markSongAsFailed(mediaId)
                handleFinalFailure()
                return
            }

            // Aggressive cache clearing for all playback errors
            if (mediaId != null) {
                performAggressiveCacheClear(mediaId)
            }

            // Handle specific error types with targeted strategies
            when {
                isAudioRendererError(error) -> {
                    handleAudioRendererError(mediaId)
                    return
                }
                isRangeNotSatisfiableError(error) -> {
                    handleRangeNotSatisfiableError(mediaId)
                    return
                }
                isPageReloadError(error) -> {
                    handlePageReloadError(mediaId)
                    return
                }
                isExpiredUrlError(error) -> {
                    handleExpiredUrlError(mediaId)
                    return
                }
                !isNetworkConnected.value || isNetworkRelatedError(error) -> {
                    waitOnNetworkError()
                    return
                }
            }

            // IO_UNSPECIFIED and IO_BAD_HTTP_STATUS fallback
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
            ) {
                handleGenericIOError(mediaId)
                return
            }

            // Final fallback
            if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
                skipOnError()
            } else {
                stopOnError()
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Exception in error handler", e)
            reportException(e)
            try {
                player.pause()
            } catch (fallbackError: Exception) {
                Log.e("MusicService", "Failed to pause player in fallback", fallbackError)
            }
        }
    }

    // ── Error detection helpers ──────────────────────────────────────────────

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
            "page must be reloaded",
            "reload"
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
            error.cause is java.net.ConnectException ||
            error.cause is java.net.UnknownHostException ||
            (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
    }

    private fun isAudioRendererError(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
            (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
            (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
    }

    // ── Per-song retry tracking ─────────────────────────────────────────────

    private fun performAggressiveCacheClear(mediaId: String) {
        Log.d("MusicService", "Performing aggressive cache clear for $mediaId")
        songUrlCache.remove(mediaId)
        try {
            playerCache.removeResource(mediaId)
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to clear player cache for $mediaId", e)
        }
        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to refresh for $mediaId", e)
        }
    }

    private fun hasExceededRetryLimit(mediaId: String): Boolean {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        return currentRetries >= MAX_RETRY_PER_SONG
    }

    private fun incrementRetryCount(mediaId: String) {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        currentMediaIdRetryCount[mediaId] = currentRetries + 1
        Log.d("MusicService", "Retry count for $mediaId: ${currentRetries + 1}/$MAX_RETRY_PER_SONG")
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
            delay(5 * 60 * 1000L) // 5 minutes
            recentlyFailedSongs.clear()
            Log.d("MusicService", "Cleared recently failed songs list")
        }
    }

    // ── Error type handlers ─────────────────────────────────────────────────

    private fun handleAudioRendererError(mediaId: String?) {
        if (mediaId == null) { handleFinalFailure(); return }
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            try {
                player.pause()
                delay(RETRY_DELAY_MS * 3) // 3s for audio renderer to settle

                val currentIndex = player.currentMediaItemIndex
                if (currentIndex != C.INDEX_UNSET) {
                    val currentPosition = player.currentPosition
                    player.seekTo(currentIndex, currentPosition)
                    player.prepare()
                    delay(500)
                    player.play()
                } else {
                    handleFinalFailure()
                }
            } catch (e: Exception) {
                Log.e("MusicService", "AudioRenderer error recovery failed", e)
                handleFinalFailure()
            }
        }
    }

    private fun handleRangeNotSatisfiableError(mediaId: String?) {
        if (mediaId == null) { handleFinalFailure(); return }
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            performAggressiveCacheClear(mediaId)
            delay(RETRY_DELAY_MS)

            // Force re-prepare from position 0 to avoid range issues
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, 0)
            player.prepare()
            Log.d("MusicService", "Retrying playback for $mediaId after 416 error (from position 0)")
        }
    }

    private fun handlePageReloadError(mediaId: String?) {
        if (mediaId == null) { handleFinalFailure(); return }
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            performAggressiveCacheClear(mediaId)
            delay(RETRY_DELAY_MS * 2) // Extra delay for page reload errors

            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
            Log.d("MusicService", "Retrying playback for $mediaId after page reload error")
        }
    }

    private fun handleExpiredUrlError(mediaId: String?) {
        if (mediaId == null) { handleFinalFailure(); return }
        incrementRetryCount(mediaId)

        songUrlCache.remove(mediaId)
        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to clear decryption caches", e)
        }

        retryJob?.cancel()
        retryJob = scope.launch {
            delay(RETRY_DELAY_MS)

            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
            player.play()
            Log.d("MusicService", "Retrying playback for $mediaId after 403 error")
        }
    }

    private fun handleGenericIOError(mediaId: String?) {
        if (mediaId == null) { handleFinalFailure(); return }
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            if (mediaId != null) performAggressiveCacheClear(mediaId)
            delay(RETRY_DELAY_MS)

            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
            Log.d("MusicService", "Retrying playback for $mediaId after generic IO error")
        }
    }

    private fun handleFinalFailure() {
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            Log.d("MusicService", "All recovery attempts exhausted, auto-skipping to next track")
            skipOnError()
        } else {
            Log.d("MusicService", "All recovery attempts exhausted, stopping playback")
            stopOnError()
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
                    .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(
                                OkHttpClient
                                    .Builder()
                                    .proxy(YouTube.proxy)
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .readTimeout(8, TimeUnit.SECONDS)
                                    .callTimeout(10, TimeUnit.SECONDS)
                                    .proxyAuthenticator { _, response ->
                                        YouTube.proxyAuth?.let { auth ->
                                            response.request.newBuilder()
                                                .header("Proxy-Authorization", auth)
                                                .build()
                                        } ?: response.request
                                    }
                                    .build(),
                            ),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            if (dataSpec.uri.scheme == "file" || dataSpec.uri.scheme == "content") {
                return@Factory dataSpec
            }
            val mediaId = dataSpec.key
                ?: dataSpec.uri.host
                ?: dataSpec.uri.lastPathSegment
                ?: dataSpec.uri.toString().removePrefix("echo://")

            if (mediaId.isBlank()) {
                Log.e("MusicService", "DataSpec has no media id key")
                throw PlaybackException(
                    "No media ID available for playback",
                    null,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }

            // Check if the content is already downloaded/cached
            if (downloadCache.isCached(
                    mediaId,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                ) || playerCache.isCached(
                    mediaId,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                )
            ) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }

            // Check if we have a valid cached URL (not expired)
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            // Need to fetch a new URL - either first time or URL expired
            // Check if this is an uploaded song that needs special handling
            val isUploadedSong = runBlocking(Dispatchers.IO) {
                database.song(mediaId).first()?.song?.isUploaded == true
            }
            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    playlistId = if (isUploadedSong) "MLPT" else null,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
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
                        throwable.message ?: throwable.javaClass.simpleName,
                        throwable,
                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }
            }

            val nonNullPlayback = requireNotNull(playbackData) {
                "No playback data available for media ID: $mediaId"
            }
            run {
                val format = nonNullPlayback.format

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
                            loudnessDb = nonNullPlayback.audioConfig?.loudnessDb,
                            playbackUrl = nonNullPlayback.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, nonNullPlayback) }

                val streamUrl = nonNullPlayback.streamUrl

                songUrlCache[mediaId] =
                    streamUrl to System.currentTimeMillis() + (nonNullPlayback.streamExpiresInSeconds * 1000L)
                return@Factory dataSpec.withUri(streamUrl.toUri())
            }
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            DefaultExtractorsFactory(),
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): DefaultAudioSink {
                val hiFiMode = audioEngineMode == AudioEngineMode.HIFI_EXPERIMENTAL
                if (hiFiMode) {
                    NativeAudioEngine.initializeIfAvailable()
                }
                return DefaultAudioSink
                    .Builder(this@MusicService)
                    .setEnableFloatOutput(enableFloatOutput || hiFiMode)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams || hiFiMode)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(
                            emptyArray(),
                            SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                            SonicAudioProcessor(),
                        ),
                    ).build()
            }
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        // Track immediately if played for at least 1 second (1000ms)
        if (playbackStats.totalPlayTimeMs >= 1000 &&
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

            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl = YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
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
            return
        }

        // Save current queue with proper type information
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

        // Save player state
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
        }.onFailure {
            reportException(it)
        }
        runCatching {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistAutomix)
                }
            }
        }.onFailure {
            reportException(it)
        }
        runCatching {
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistPlayerState)
                }
            }
        }.onFailure {
            reportException(it)
        }
    }

    override fun startForegroundService(service: Intent?): ComponentName? {
        return try {
            super.startForegroundService(service)
        } catch (e: Exception) {
            // Check if it's the specific foreground service exception (available in API 31+)
            if (Build.VERSION.SDK_INT >= 31 && e.javaClass.name.contains("ForegroundServiceStartNotAllowedException")) {
                Log.e("MusicService", "ForegroundServiceStartNotAllowedException caught in startForegroundService", e)
                null
            } else if (e is IllegalStateException) {
                 Log.e("MusicService", "IllegalStateException caught in startForegroundService", e)
                 null
            } else {
                throw e
            }
        }
    }

    // ===== Crossfade =====
    private fun scheduleCrossfade() {
        crossfadeTriggerJob?.cancel()

        // Handoff: main player just transitioned to the next song after the crossfade window
        if (isCrossfadingIn) {
            isCrossfadingIn = false
            crossfadeOutJob?.cancel()
            val fp = fadingPlayer
            if (fp != null && crossfadeEnabled) {
                // Sync main player to where fadingPlayer currently is, then release fadingPlayer
                val syncPos = try { fp.currentPosition } catch (_: Exception) { 0L }
                try {
                    player.volume = playerVolume.value
                    if (syncPos > 500L) player.seekTo(syncPos)
                } catch (_: Exception) {}
                scope.launch {
                    delay(200) // let seek settle
                    try { fp.stop(); fp.release() } catch (_: Exception) {}
                }
                fadingPlayer = null
                isCrossfading.value = false
            } else {
                // fadingPlayer unavailable — fade the main player in from silence
                val targetVolume = playerVolume.value
                crossfadeInJob?.cancel()
                crossfadeInJob = scope.launch {
                    val steps = 50
                    val stepDuration = (crossfadeDuration / steps).coerceAtLeast(10L)
                    try { player.volume = 0f } catch (_: Exception) {}
                    for (i in 1..steps) {
                        if (!isActive) return@launch
                        delay(stepDuration)
                        val progress = i.toFloat() / steps
                        try { player.volume = targetVolume * progress } catch (_: Exception) {}
                    }
                    try { player.volume = targetVolume } catch (_: Exception) {}
                    isCrossfading.value = false
                }
            }
        }

        if (!crossfadeEnabled || !player.hasNextMediaItem()) return
        if (crossfadeGapless && isNextItemGapless()) return

        val duration = player.duration
        lastCrossfadeTrackDuration = duration
        if (duration <= 0 || duration == C.TIME_UNSET) return
        // Skip crossfade for songs shorter than 2× the crossfade window
        if (duration < crossfadeDuration * 2) return

        val triggerAt = duration - crossfadeDuration

        crossfadeTriggerJob = scope.launch {
            while (isActive) {
                delay(300)
                if (player.isPlaying && player.currentPosition >= triggerAt) break
            }
            if (isActive) startCrossfadeOut()
        }
    }

    private fun isWithinCrossfadeWindow(): Boolean {
        if (!crossfadeEnabled || !player.hasNextMediaItem()) return false
        val duration = player.duration
        if (duration <= 0L || duration == C.TIME_UNSET) return false
        if (duration < crossfadeDuration * 2) return false

        val triggerAt = (duration - crossfadeDuration).coerceAtLeast(0L)
        return player.currentPosition >= triggerAt
    }

    private fun startManualSkipCrossfadeIn() {
        crossfadeInJob?.cancel()

        val targetVolume = playerVolume.value
        val steps = 40
        val stepDuration = (crossfadeDuration / steps).coerceAtLeast(10L)
        isCrossfading.value = true

        crossfadeInJob = scope.launch {
            try { player.volume = 0f } catch (_: Exception) {}

            for (i in 1..steps) {
                if (!isActive) return@launch
                delay(stepDuration)
                val progress = i.toFloat() / steps
                try { player.volume = targetVolume * progress } catch (_: Exception) {}
            }

            try { player.volume = targetVolume } catch (_: Exception) {}
            isCrossfading.value = false
        }
    }

    private fun startCrossfadeOut() {
        crossfadeOutJob?.cancel()
        crossfadeInJob?.cancel()
        // Release any previous fadingPlayer
        fadingPlayer?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
        fadingPlayer = null

        val targetVolume = playerVolume.value
        val steps = 50
        val stepDuration = (crossfadeDuration / steps).coerceAtLeast(10L)
        isCrossfadingIn = true
        isCrossfading.value = true

        // ── Second player for the next song ──────────────────────────────────
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex != C.INDEX_UNSET) {
            try {
                val nextItem = player.getMediaItemAt(nextIndex)
                val fp = ExoPlayer.Builder(this)
                    .setMediaSourceFactory(createMediaSourceFactory())
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        false
                    )
                    .build()
                fp.volume = 0f
                fp.setMediaItem(nextItem)
                fp.prepare()
                fp.playWhenReady = true
                fadingPlayer = fp

                // Fade in the next song on fadingPlayer
                crossfadeInJob = scope.launch {
                    // Wait briefly for the second player to buffer
                    var waited = 0L
                    while (isActive && waited < 3000L) {
                        delay(100); waited += 100
                        if (fp.playbackState == Player.STATE_READY || fp.isPlaying) break
                    }
                    for (i in 1..steps) {
                        if (!isActive) return@launch
                        delay(stepDuration)
                        val progress = i.toFloat() / steps
                        try { fp.volume = targetVolume * progress } catch (_: Exception) {}
                    }
                    try { fp.volume = targetVolume } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                // Could not create second player — fall back to fade-in-after-transition
            }
        }

        // ── Fade out the current song on main player ─────────────────────────
        crossfadeOutJob = scope.launch {
            for (i in 1..steps) {
                if (!isActive) return@launch
                delay(stepDuration)
                if (!player.isPlaying) {
                    // User paused — abort, restore everything
                    isCrossfadingIn = false
                    isCrossfading.value = false
                    try { player.volume = targetVolume } catch (_: Exception) {}
                    fadingPlayer?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
                    fadingPlayer = null
                    crossfadeInJob?.cancel()
                    return@launch
                }
                val progress = i.toFloat() / steps
                try { player.volume = targetVolume * (1f - progress) } catch (_: Exception) {}
            }
            try { player.volume = 0f } catch (_: Exception) {}
        }
    }

    private fun isNextItemGapless(): Boolean {
        if (!player.hasNextMediaItem()) return false
        val currentAlbum = player.currentMediaItem?.mediaMetadata?.albumTitle?.toString()
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return false
        val nextAlbum = player.getMediaItemAt(nextIndex).mediaMetadata.albumTitle?.toString()
        return !currentAlbum.isNullOrEmpty() && currentAlbum == nextAlbum
    }

    private fun updateDiscordRPC(song: iad1tya.echo.music.db.entities.Song, showFeedback: Boolean = false) {
        val useDetails = dataStore.get(DiscordUseDetailsKey, false)
        val advancedMode = dataStore.get(DiscordAdvancedModeKey, false)

        val status = if (advancedMode) dataStore.get(DiscordStatusKey, "online") else "online"
        val b1Text = if (advancedMode) dataStore.get(DiscordButton1TextKey, "") else ""
        val b1Visible = if (advancedMode) dataStore.get(DiscordButton1VisibleKey, true) else true
        val b2Text = if (advancedMode) dataStore.get(DiscordButton2TextKey, "") else ""
        val b2Visible = if (advancedMode) dataStore.get(DiscordButton2VisibleKey, true) else true
        val activityType = if (advancedMode) dataStore.get(DiscordActivityTypeKey, "listening") else "listening"
        val activityName = if (advancedMode) dataStore.get(DiscordActivityNameKey, "") else ""

        discordUpdateJob?.cancel()
        discordUpdateJob = scope.launch {
            discordRpc?.updateSong(
                song,
                player.currentPosition,
                player.playbackParameters.speed,
                useDetails,
                status,
                b1Text,
                b1Visible,
                b2Text,
                b2Visible,
                activityType,
                activityName
            )?.onFailure {
                if (showFeedback) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            this@MusicService,
                            "Discord RPC update failed: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun cleanupCrossfade() {
        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null
        crossfadeOutJob?.cancel()
        crossfadeOutJob = null
        crossfadeInJob?.cancel()
        crossfadeInJob = null
        isCrossfadingIn = false
        pendingManualSkipCrossfadeIn = false
        isCrossfading.value = false
        // Restore player volume in case we were mid-fade
        try { player.volume = playerVolume.value } catch (_: Exception) {}
        fadingPlayer?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        fadingPlayer = null
    }

    override fun onDestroy() {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        // TTS cleanup
        ttsManager.shutdown()

        // Haptics cleanup
        hapticsPollingJob?.cancel()
        hapticsManager.stop()

        // Last.fm cleanup
        scrobbleManager?.destroy()
        scrobbleManager = null

        // Discord cleanup
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        discordUpdateJob?.cancel()
        
        connectivityObserver.unregister()
        lyricsPreloadManager?.destroy()
        lyricsPreloadManager = null
        abandonAudioFocus()
        releaseLoudnessEnhancer()
        releaseProAudioEffects()
        spatialAudioManager?.release()
        spatialAudioManager = null
        cleanupCrossfade()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        volumeObserver?.let { contentResolver.unregisterContentObserver(it) }
        volumeObserver = null
        player.removeListener(this)
        player.removeListener(sleepTimer)
        
        if (::castConnectionHandler.isInitialized) {
            castConnectionHandler.release()
        }
        mediaSession.release()
        player.release()
        
        super.onDestroy()
    }

    suspend fun getStreamUrl(mediaId: String): String? {
        // Return cached URL if available and not expired
        songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
            return it.first
        }
        
        // Resolve URL if not cached
        return try {
            val isUploadedSong = database.song(mediaId).first()?.song?.isUploaded == true
            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = mediaId,
                playlistId = if (isUploadedSong) "MLPT" else null,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager
            ).getOrNull()
            
            val streamUrl = playbackData?.streamUrl
            if (streamUrl != null) {
                // Cache it with expiry
                val expiry = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                songUrlCache[mediaId] = streamUrl to expiry
                streamUrl
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to resolve URL for Cast: ${e.message}")
            null
        }
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (dataStore.get(ForceStopOnTaskClearKey, false)) {
            player.pause()
            stopSelf()
            android.os.Process.killProcess(android.os.Process.myPid())
            return
        }

        // Check if user wants to stop music when task is cleared
        if (dataStore.get(StopMusicOnTaskClearKey, true)) {
            player.pause()
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession
    
    private fun updateWidget() {
        val metadata = player.currentMetadata
        MusicWidgetProvider.updateWidget(
            context = this,
            songTitle = metadata?.title,
            artistName = metadata?.artists?.joinToString(", ") { it.name },
            albumArtUrl = metadata?.thumbnailUrl,
            isPlaying = player.isPlaying
        )
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val SEARCH = "search"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "iad1tya.echo.music.playback.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "iad1tya.echo.music.playback.ACTION_NEXT"
        const val ACTION_PREVIOUS = "iad1tya.echo.music.playback.ACTION_PREVIOUS"
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        // Constants for audio normalization
        private const val MAX_GAIN_MB = 800 // Maximum gain in millibels (8 dB)
        private const val MIN_GAIN_MB = -800 // Minimum gain in millibels (-8 dB)

        private const val TAG = "MusicService"
    }
}
