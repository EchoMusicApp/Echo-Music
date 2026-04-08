package iad1tya.echo.music.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import com.echo.innertube.YouTube
import iad1tya.echo.music.constants.AudioQuality
import iad1tya.echo.music.constants.AudioQualityKey
import iad1tya.echo.music.constants.DownloadAutoRetryKey
import iad1tya.echo.music.constants.DownloadChargingOnlyKey
import iad1tya.echo.music.constants.DownloadRetryLimitKey
import iad1tya.echo.music.constants.DownloadWifiOnlyKey
import iad1tya.echo.music.constants.PlayerStreamClient
import iad1tya.echo.music.constants.PlayerStreamClientKey
import iad1tya.echo.music.constants.PoTokenGvsKey
import iad1tya.echo.music.constants.PoTokenPlayerKey
import iad1tya.echo.music.constants.UseVisitorDataKey
import iad1tya.echo.music.constants.WebClientPoTokenEnabledKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.FormatEntity
import iad1tya.echo.music.db.entities.SongEntity
import iad1tya.echo.music.di.DownloadCache
import iad1tya.echo.music.di.PlayerCache
import iad1tya.echo.music.utils.StreamClientUtils
import iad1tya.echo.music.utils.YTPlayerUtils
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.enumPreference
import iad1tya.echo.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
) {
    private val appContext = context
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val playerStreamClient by enumPreference(context, PlayerStreamClientKey, PlayerStreamClient.ANDROID_VR)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()
    private val downloadRetryCount = mutableMapOf<String, Int>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .proxy(YouTube.proxy)
                            .addInterceptor { chain ->
                                val request = chain.request()
                                val clientParam = request.url.queryParameter("c")
                                val ua = StreamClientUtils.resolveUserAgent(clientParam)
                                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)
                                val builder = request.newBuilder().header("User-Agent", ua)
                                originReferer.origin?.let { builder.header("Origin", it) }
                                originReferer.referer?.let { builder.header("Referer", it) }
                                chain.proceed(builder.build())
                            }
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
        ) { dataSpec ->
            val mediaId = dataSpec.key
                ?: dataSpec.uri.host
                ?: dataSpec.uri.lastPathSegment
                ?: dataSpec.uri.toString().removePrefix("echo://")
            require(mediaId.isNotBlank()) { "No media id" }
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    preferredStreamClient = playerStreamClient,
                    webClientPoTokenEnabled = appContext.dataStore.get(WebClientPoTokenEnabledKey, false),
                    useVisitorData = appContext.dataStore.get(UseVisitorDataKey, false),
                    manualGvsPoToken = appContext.dataStore.get(PoTokenGvsKey),
                    manualPlayerPoToken = appContext.dataStore.get(PoTokenPlayerKey),
                )
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) {
                        existing.copy(dateDownload = now)
                    } else {
                        existing
                    }
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                        dateDownload = now,
                        isDownloaded = false
                    )
                }

                upsert(updatedSong)
            }

            val streamUrl = playbackData.streamUrl.let {
                "${it}&range=0-${format.contentLength ?: 10000000}"
            }

            songUrlCache[mediaId] =
                streamUrl to (System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L)
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    downloadRetryCount.remove(download.request.id)
                                    database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                                }
                                Download.STATE_FAILED -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)

                                    val prefs = appContext.dataStore.data.first()
                                    val autoRetry = prefs[DownloadAutoRetryKey] ?: true
                                    val retryLimit = (prefs[DownloadRetryLimitKey] ?: 2).coerceIn(1, 5)
                                    val currentAttempt = downloadRetryCount[download.request.id] ?: 0

                                    if (autoRetry && currentAttempt < retryLimit) {
                                        downloadRetryCount[download.request.id] = currentAttempt + 1
                                        DownloadService.sendAddDownload(
                                            appContext,
                                            ExoDownloadService::class.java,
                                            download.request,
                                            false
                                        )
                                    }
                                }
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }
                }
            )
        }

    init {
        scope.launch {
            appContext.dataStore.data.collect { prefs ->
                var requirementsMask = Requirements.DEVICE_STORAGE_NOT_LOW
                if (prefs[DownloadWifiOnlyKey] == true) {
                    requirementsMask = requirementsMask or Requirements.NETWORK_UNMETERED
                } else {
                    requirementsMask = requirementsMask or Requirements.NETWORK
                }
                if (prefs[DownloadChargingOnlyKey] == true) {
                    requirementsMask = requirementsMask or Requirements.DEVICE_CHARGING
                }
                downloadManager.requirements = Requirements(requirementsMask)
            }
        }

        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun release() {
        scope.cancel()
    }
}
