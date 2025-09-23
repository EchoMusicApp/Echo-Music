package iad1tya.echo.music.viewModel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import iad1tya.echo.kotlinytmusicscraper.models.response.DownloadProgress
import iad1tya.echo.music.R
import iad1tya.echo.music.common.Config.ALBUM_CLICK
import iad1tya.echo.music.common.Config.DOWNLOAD_CACHE
import iad1tya.echo.music.common.Config.PLAYLIST_CLICK
import iad1tya.echo.music.common.Config.RECOVER_TRACK_QUEUE
import iad1tya.echo.music.common.Config.SHARE
import iad1tya.echo.music.common.Config.SONG_CLICK
import iad1tya.echo.music.common.Config.VIDEO_CLICK
import iad1tya.echo.music.common.DownloadState
import iad1tya.echo.music.common.SELECTED_LANGUAGE
import iad1tya.echo.music.common.STATUS_DONE
import iad1tya.echo.music.data.dataStore.DataStoreManager
import iad1tya.echo.music.data.dataStore.DataStoreManager.Settings.FALSE
import iad1tya.echo.music.data.dataStore.DataStoreManager.Settings.TRUE
import iad1tya.echo.music.data.db.entities.AlbumEntity
import iad1tya.echo.music.data.db.entities.LocalPlaylistEntity
import iad1tya.echo.music.data.db.entities.LyricsEntity
import iad1tya.echo.music.data.db.entities.NewFormatEntity
import iad1tya.echo.music.data.db.entities.PairSongLocalPlaylist
import iad1tya.echo.music.data.db.entities.PlaylistEntity
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.data.db.entities.SongInfoEntity
import iad1tya.echo.music.data.db.entities.TranslatedLyricsEntity
import iad1tya.echo.music.data.manager.LocalPlaylistManager
import iad1tya.echo.music.data.model.browse.album.Track
import iad1tya.echo.music.data.model.metadata.Lyrics
import iad1tya.echo.music.data.model.update.UpdateData
import iad1tya.echo.music.extension.isSong
import iad1tya.echo.music.extension.isVideo
import iad1tya.echo.music.extension.toListName
import iad1tya.echo.music.extension.toLyrics
import iad1tya.echo.music.extension.toLyricsEntity
import iad1tya.echo.music.extension.toMediaItem
import iad1tya.echo.music.extension.toSongEntity
import iad1tya.echo.music.extension.toTrack
import iad1tya.echo.music.service.ControlState
import iad1tya.echo.music.service.NowPlayingTrackState
import iad1tya.echo.music.service.PlayerEvent
import iad1tya.echo.music.service.PlaylistType
import iad1tya.echo.music.service.QueueData
import iad1tya.echo.music.service.RepeatState
import iad1tya.echo.music.service.SimpleMediaState
import iad1tya.echo.music.service.SleepTimerState
import iad1tya.echo.music.service.test.notification.NotifyWork
import iad1tya.echo.music.utils.Resource
import iad1tya.echo.music.utils.VersionManager
import iad1tya.echo.music.viewModel.base.BaseViewModel
import aditya.echo.spotify.model.response.spotify.CanvasResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.reflect.KClass

@UnstableApi
class SharedViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val localPlaylistManager: LocalPlaylistManager by inject()
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private val _isUpToDate = MutableStateFlow(false)
    val isUpToDate: StateFlow<Boolean> = _isUpToDate

    private val downloadedCache: SimpleCache by inject(qualifier = named(DOWNLOAD_CACHE))
    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    private val context
        get() = getApplication<Application>()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null
    private var quality: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    private var _canvas: MutableStateFlow<CanvasResponse?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResponse?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<Intent?> = MutableStateFlow(null)
    val intent: StateFlow<Intent?> = _intent

    // Recently played data
    private val _recentlyPlayed: MutableStateFlow<List<iad1tya.echo.music.data.type.RecentlyType>> = MutableStateFlow(emptyList())
    val recentlyPlayed: StateFlow<List<iad1tya.echo.music.data.type.RecentlyType>> = _recentlyPlayed.asStateFlow()

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    val blurBg: StateFlow<Boolean> =
        dataStoreManager.blurPlayerBackground
            .map { it == TRUE }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(500L),
                initialValue = false,
            )

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    private var _likeStatus = MutableStateFlow<Boolean>(false)
    val likeStatus: StateFlow<Boolean> = _likeStatus

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    init {
        mainRepository.initYouTube(viewModelScope)
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            val timeLineJob =
                launch {
                    combine(
                        timeline.filterNotNull(),
                        nowPlayingState.filterNotNull(),
                    ) { timeline, nowPlayingState ->
                        Pair(timeline, nowPlayingState)
                    }.distinctUntilChanged { old, new ->
                        (old.first.total.toString() + old.second.songEntity?.videoId).hashCode() ==
                            (new.first.total.toString() + new.second.songEntity?.videoId).hashCode()
                    }.collectLatest {
                        log("Timeline job ${(it.first.total.toString() + it.second.songEntity?.videoId).hashCode()}")
                        val nowPlaying = it.second
                        val timeline = it.first
                        if (timeline.total > 0 && nowPlaying.songEntity != null) {
                            if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                Log.w(tag, "Duration is ${timeline.total}")
                                Log.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                            }
                            nowPlaying.songEntity.let { song ->
                                if (nowPlayingScreenData.value.lyricsData == null) {
                                    Log.w(tag, "Get lyrics from format")
                                    getLyricsFromFormat(song, (timeline.total / 1000).toInt())
                                }
                            }
                        }
                    }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        Log.w(tag, "GetVideo is $it")
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.distinctUntilChanged().collectLatest {
                        setLyricsProvider()
                    }
                }
            val shareSavedLyricsJob =
                launch {
                    dataStoreManager.helpBuildLyricsDatabase.distinctUntilChanged().collectLatest {
                        _shareSavedLyrics.value = it == TRUE
                    }
                }
            timeLineJob.join()
            checkGetVideoJob.join()
            lyricsProviderJob.join()
            shareSavedLyricsJob.join()
        }

        runBlocking {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            simpleMediaServiceHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    Log.w(tag, "NowPlayingState is $state")
                    canvasJob?.cancel()
                    _nowPlayingState.value = state
                    state.track?.let { track ->
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = state.track.title,
                                artistName =
                                    state.track
                                        .artists
                                        .toListName()
                                        .joinToString(", "),
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = null,
                                songInfoData = null,
                                playlistName = simpleMediaServiceHandler.queueData.value?.playlistName ?: "",
                            )
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = song.thumbnails,
                                isExplicit = song.isExplicit,
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    simpleMediaServiceHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }
                            SimpleMediaState.Ended -> {
                                _timeline.update {
                                    it.copy(
                                        current = -1L,
                                        total = -1L,
                                        bufferedPercent = 0,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    if (_timeline.value.total > 0L) {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = simpleMediaServiceHandler.getPlayerDuration(),
                                            )
                                        }
                                    }
                                } else {
                                    _timeline.update {
                                        it.copy(
                                            loading = true,
                                        )
                                    }
                                }
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = simpleMediaServiceHandler.getProgress(),
                                        loading = false,
                                        total = mediaState.duration,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Log.w(tag, "ControllerJob is running")
                    simpleMediaServiceHandler.controlState.collectLatest {
                        Log.w(tag, "ControlState is $it")
                        _controllerState.value = it
                    }
                }
            val sleepTimerJob =
                launch {
                    simpleMediaServiceHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    simpleMediaServiceHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
        }
        // Reset downloading songs & playlists to not downloaded
        checkAllDownloadingSongs()
        checkAllDownloadingPlaylists()
        checkAllDownloadingLocalPlaylists()
    }

    fun setIntent(intent: Intent?) {
        _intent.value = intent
    }

    private var recentlyPlayedJob: Job? = null
    
    fun getRecentlyPlayed() {
        // Cancel previous job to prevent memory leaks
        recentlyPlayedJob?.cancel()
        recentlyPlayedJob = viewModelScope.launch {
            try {
                mainRepository.getAllRecentData().collect { recentData ->
                    _recentlyPlayed.value = recentData
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error getting recently played: ${e.message}")
                _recentlyPlayed.value = emptyList()
            }
        }
    }

    fun blurFullscreenLyrics(): Boolean {
        return try {
            runBlocking { dataStoreManager.blurFullscreenLyrics.first() == TRUE }
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error getting blur setting: ${e.message}")
            false
        }
    }

    private fun getLikeStatus(videoId: String?) {
        viewModelScope.launch {
            if (videoId != null) {
                _likeStatus.value = false
                mainRepository.getLikeStatus(videoId).collectLatest { status ->
                    _likeStatus.value = status
                }
            }
        }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Log.w(tag, "Start getCanvas: $videoId $duration")
//        canvasJob?.cancel()
        viewModelScope.launch {
            if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                mainRepository.getCanvas(videoId, duration).cancellable().collect { response ->
                    _canvas.value = response
                    Log.w(tag, "Canvas is $response")
                    if (response != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) {
                        _nowPlayingScreenData.update {
                            it.copy(
                                canvasData =
                                    response.canvases.firstOrNull()?.canvas_url?.let { canvasUrl ->
                                        NowPlayingScreenData.CanvasData(
                                            isVideo = canvasUrl.contains(".mp4"),
                                            url = canvasUrl,
                                        )
                                    },
                            )
                        }
                        if (response
                                .canvases
                                .firstOrNull()
                                ?.canvas_url
                                ?.contains(".mp4") == true
                        ) {
                            mainRepository.updateCanvasUrl(videoId, response.canvases.first().canvas_url)
                        }
                        val canvasThumbs = response.canvases.firstOrNull()?.thumbsOfCanva
                        if (!canvasThumbs.isNullOrEmpty()) {
                            (
                                canvasThumbs.let {
                                    it
                                        .maxByOrNull {
                                            (it.height ?: 0) + (it.width ?: 0)
                                        }?.url
                                } ?: canvasThumbs.first().url
                            )?.let { thumb ->
                                mainRepository.updateCanvasThumbUrl(videoId, thumb)
                            }
                        }
                    } else {
                        nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                            _nowPlayingScreenData.update {
                                it.copy(
                                    canvasData =
                                        NowPlayingScreenData.CanvasData(
                                            isVideo = url.contains(".mp4"),
                                            url = url,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun getString(key: String): String? {
        return try {
            runBlocking { dataStoreManager.getString(key).first() }
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error getting string for key $key: ${e.message}")
            null
        }
    }

    fun putString(
        key: String,
        value: String,
    ) {
        viewModelScope.launch {
            try {
                dataStoreManager.putString(key, value)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error putting string for key $key: ${e.message}")
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        simpleMediaServiceHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        simpleMediaServiceHandler.sleepStop()
    }

    private var _downloadState: MutableStateFlow<Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<Download?> = _downloadState.asStateFlow()

    fun checkIsRestoring() {
        viewModelScope.launch {
            mainRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCache.keys.contains(song.videoId)) {
                        mainRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            mainRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            if (data.tracks.isNullOrEmpty() ||
                                (
                                    !downloadedCache.keys.containsAll(
                                        data.tracks,
                                    )
                                )
                            ) {
                                mainRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            if (data.tracks.isNullOrEmpty() ||
                                (
                                    !downloadedCache.keys.containsAll(
                                        data.tracks,
                                    )
                                )
                            ) {
                                mainRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            if (data.tracks.isNullOrEmpty() ||
                                (
                                    !downloadedCache.keys.containsAll(
                                        data.tracks,
                                    )
                                )
                            ) {
                                mainRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            mainRepository.insertLyrics(lyrics)
        }
    }

    private fun getSavedLyrics(track: Track) {
        viewModelScope.launch {
            mainRepository.getSavedLyrics(track.videoId).cancellable().collectLatest { lyrics ->
                if (lyrics != null) {
                    val lyricsData = lyrics.toLyrics()
                    Log.d(tag, "Saved Lyrics $lyricsData")
                    updateLyrics(
                        track.videoId,
                        track.durationSeconds ?: 0,
                        lyricsData,
                        false,
                        LyricsProvider.OFFLINE,
                    )
                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            try {
                mainRepository.getFullMetadata(videoId).collectLatest {
                    try {
                        if (it != null) {
                            val track = it.toTrack()
                            simpleMediaServiceHandler.setQueueData(
                                QueueData(
                                    listTracks = arrayListOf(track),
                                    firstPlayedTrack = track,
                                    playlistId = "RDAMVM$videoId",
                                    playlistName = context.getString(R.string.shared),
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            loadMediaItemFromTrack(track, SONG_CLICK)
                        } else {
                            Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error processing shared media item: ${e.message}")
                        Toast.makeText(context, "Error loading shared track", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading shared media item: ${e.message}")
                Toast.makeText(context, "Error loading shared track", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @UnstableApi
    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        try {
            quality = runBlocking { 
                try {
                    dataStoreManager.quality.first()
                } catch (e: Exception) {
                    Log.e("SharedViewModel", "Error getting quality: ${e.message}")
                    "AUDIO_QUALITY_MEDIUM" // Default fallback
                }
            }
            viewModelScope.launch {
                try {
                    // Clear media items safely
                    simpleMediaServiceHandler.clearMediaItems()
                    
                    // Insert song with error handling
                    try {
                        val insertResult = mainRepository.insertSong(track.toSongEntity()).first()
                        if (insertResult != null) {
                            // Get song entity and update like status
                            val songEntity = mainRepository.getSongById(track.videoId).first()
                            if (songEntity != null) {
                                Log.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                                _liked.value = songEntity.liked
                                
                                // Update inLibrary timestamp to current time for recently played
                                mainRepository.updateSongInLibrary(
                                    java.time.LocalDateTime.now(),
                                    track.videoId
                                )
                                
                                // Refresh recently played data after updating
                                getRecentlyPlayed()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error inserting song: ${e.message}")
                        // Continue execution even if song insertion fails
                    }
                    
                    // Update duration safely
                    track.durationSeconds?.let {
                        try {
                            mainRepository.updateDurationSeconds(
                                it,
                                track.videoId,
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error updating duration: ${e.message}")
                        }
                    }
                    
                    // Add media item safely
                    try {
                        withContext(Dispatchers.Main) {
                            simpleMediaServiceHandler.addMediaItem(track.toMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error adding media item: ${e.message}")
                        makeToast("Error loading track")
                        return@launch
                    }

                    // Handle different click types safely
                    when (type) {
                        SONG_CLICK -> {
                            try {
                                simpleMediaServiceHandler.getRelated(track.videoId)
                            } catch (e: Exception) {
                                Log.e(tag, "Error getting related songs: ${e.message}")
                            }
                        }

                        VIDEO_CLICK -> {
                            try {
                                simpleMediaServiceHandler.getRelated(track.videoId)
                            } catch (e: Exception) {
                                Log.e(tag, "Error getting related videos: ${e.message}")
                            }
                        }

                        SHARE -> {
                            try {
                                simpleMediaServiceHandler.getRelated(track.videoId)
                            } catch (e: Exception) {
                                Log.e(tag, "Error getting related for share: ${e.message}")
                            }
                        }

                        PLAYLIST_CLICK -> {
                            try {
                                if (index == null) {
                                    loadPlaylistOrAlbum(index = 0)
                                } else {
                                    loadPlaylistOrAlbum(index = index)
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "Error loading playlist: ${e.message}")
                                makeToast("Error loading playlist")
                            }
                        }

                        ALBUM_CLICK -> {
                            try {
                                if (index == null) {
                                    loadPlaylistOrAlbum(index = 0)
                                } else {
                                    loadPlaylistOrAlbum(index = index)
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "Error loading album: ${e.message}")
                                makeToast("Error loading album")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error in loadMediaItemFromTrack: ${e.message}")
                    makeToast("Error loading track")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in loadMediaItemFromTrack outer: ${e.message}")
            makeToast("Error loading track")
        }
    }

    @UnstableApi
    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            try {
                when (uiEvent) {
                    UIEvent.Backward -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Backward)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling backward event: ${e.message}")
                        }
                    }

                    UIEvent.Forward -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Forward)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling forward event: ${e.message}")
                        }
                    }
                    
                    UIEvent.PlayPause -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.PlayPause)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling play/pause event: ${e.message}")
                        }
                    }

                    UIEvent.Next -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Next)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling next event: ${e.message}")
                        }
                    }
                    
                    UIEvent.Previous -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Previous)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling previous event: ${e.message}")
                        }
                    }

                    UIEvent.Stop -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Stop)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling stop event: ${e.message}")
                        }
                    }
                    
                    is UIEvent.UpdateProgress -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(
                                PlayerEvent.UpdateProgress(uiEvent.newProgress)
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling progress update: ${e.message}")
                        }
                    }

                    UIEvent.Repeat -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Repeat)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling repeat event: ${e.message}")
                        }
                    }
                    
                    UIEvent.Shuffle -> {
                        try {
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Shuffle)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling shuffle event: ${e.message}")
                        }
                    }
                    
                    UIEvent.ToggleLike -> {
                        try {
                            Log.w(tag, "ToggleLike")
                            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                        } catch (e: Exception) {
                            Log.e(tag, "Error handling toggle like event: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in onUIEvent: ${e.message}")
            }
        }

    @UnstableApi
    override fun onCleared() {
        Log.w("Check onCleared", "onCleared")
        try {
            // Cancel all running jobs to prevent memory leaks
            recentlyPlayedJob?.cancel()
            
            // Clear any cached data to free memory
            _nowPlayingState.value = null
            _controllerState.value = ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false
            )
            _sleepTimerState.value = SleepTimerState(
                isDone = false,
                timeRemaining = 0
            )
            
            Log.d("SharedViewModel", "ViewModel cleared successfully")
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error in onCleared: ${e.message}", e)
        } finally {
            super.onCleared()
        }
    }

    fun getLocation() {
        viewModelScope.launch {
            try {
                regionCode = dataStoreManager.location.first()
                quality = dataStoreManager.quality.first()
                language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                Log.d("SharedViewModel", "Location settings loaded: region=$regionCode, quality=$quality, language=$language")
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error getting location settings: ${e.message}", e)
                // Set defaults with error handling
                try {
                    regionCode = "US"
                    quality = "AUDIO_QUALITY_MEDIUM"
                    language = "en"
                    Log.w("SharedViewModel", "Using default location settings")
                } catch (defaultError: Exception) {
                    Log.e("SharedViewModel", "Error setting default location: ${defaultError.message}", defaultError)
                }
            }
        }
    }

    private fun checkAllDownloadingLocalPlaylists() {
        viewModelScope.launch {
            localPlaylistManager.getAllDownloadingLocalPlaylists().collectLatest { playlists ->
                playlists.forEach { playlist ->
                    localPlaylistManager.updateDownloadState(playlist.id, 0).lastOrNull()
                }
            }
        }
    }

    private fun checkAllDownloadingPlaylists() {
        viewModelScope.launch {
            mainRepository.getAllDownloadingPlaylist().collectLatest { list ->
                list.forEach { data ->
                    when (data) {
                        is AlbumEntity -> {
                            mainRepository.updateAlbumDownloadState(data.browseId, 0)
                        }
                        is PlaylistEntity -> {
                            mainRepository.updatePlaylistDownloadState(data.id, 0)
                        }
                        else -> {
                            // Skip
                        }
                    }
                }
            }
        }
    }

    private fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
            mainRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    mainRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Log.w(tag, "Get format for $mediaId: $f")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                    }
                }
        }
    }

    private var songInfoJob: Job? = null

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    mainRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }

    private var _updateResponse = MutableStateFlow<UpdateData?>(null)
    val updateResponse: StateFlow<UpdateData?> = _updateResponse

    fun checkForUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _isUpToDate.value = false
            // Default to GitHub release updates since update channel was removed
            mainRepository.checkForGithubReleaseUpdate().collectLatest { response ->
                dataStoreManager.putString(
                    "CheckForUpdateAt",
                    System.currentTimeMillis().toString(),
                )
                if (response != null) {
                    val currentVersion = "v${VersionManager.getVersionName()}"
                    val latestVersion = response.tagName ?: ""
                    
                    if (currentVersion != latestVersion) {
                        // Update available
                        _updateResponse.value =
                            UpdateData(
                                tagName = latestVersion,
                                releaseTime = response.publishedAt ?: "",
                                body = response.body ?: "",
                            )
                        showedUpdateDialog = true
                        _isUpToDate.value = false
                    } else {
                        // App is up to date
                        _isUpToDate.value = true
                        _updateResponse.value = null
                    }
                } else {
                    // No response from GitHub
                    _isUpToDate.value = false
                    _updateResponse.value = null
                }
                _isCheckingUpdate.value = false
            }
        }
    }

    fun resetUpToDateState() {
        _isUpToDate.value = false
    }

    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        simpleMediaServiceHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        simpleMediaServiceHandler.loadPlaylistOrAlbum(index)
    }

    private fun updateLyrics(
        videoId: String,
        duration: Int, // 0 if translated lyrics
        lyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.LRCLIB,
    ) {
        if (lyrics == null) {
            _nowPlayingScreenData.update {
                it.copy(
                    lyricsData = null,
                )
            }
            return
        }

        if (isTranslatedLyrics) {
            val originalLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
            if (originalLyrics != null && originalLyrics.lines != null && lyrics.lines != null) {
                var outOfSyncCount = 0

                originalLyrics.lines.forEach { originalLine ->
                    val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                    val closestTranslatedLine =
                        lyrics.lines.minByOrNull {
                            abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                        }

                    if (closestTranslatedLine != null) {
                        val translatedTime = closestTranslatedLine.startTimeMs.toLongOrNull() ?: 0L
                        val timeDiff = abs(originalTime - translatedTime)

                        if (timeDiff > 1000L) { // Lệch quá 1 giây
                            outOfSyncCount++
                        }
                    }
                }

                if (outOfSyncCount > 5) {
                    Log.w(tag, "Translated lyrics out of sync: $outOfSyncCount lines with time diff > 1s")

                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        mainRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        Log.d(tag, "Removed out-of-sync translated lyrics for $videoId")
                    }
                    return
                }
            }
        }

        val shouldSendLyricsToSimpMusic = false // Removed SIMPMUSIC lyrics sending
        if (_nowPlayingState.value?.songEntity?.videoId == videoId) {
            val track = _nowPlayingState.value?.track
            when (isTranslatedLyrics) {
                true -> {
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = lyrics,
                                ),
                        )
                    }
                }
                false -> {
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                NowPlayingScreenData.LyricsData(
                                    lyrics = lyrics,
                                    lyricsProvider = lyricsProvider,
                                ),
                        )
                    }
                    // Save lyrics to database
                    viewModelScope.launch {
                        mainRepository.insertLyrics(
                            LyricsEntity(
                                videoId = videoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun getLyricsFromFormat(
        song: SongEntity,
        duration: Int,
    ) {
        viewModelScope.launch {
            val videoId = song.videoId
            Log.w(tag, "Get Lyrics From Format for $videoId")
            val artist =
                if (song.artistName?.firstOrNull() != null &&
                    song.artistName
                        .firstOrNull()
                        ?.contains("Various Artists") == false
                ) {
                    song.artistName.firstOrNull()
                } else {
                    simpleMediaServiceHandler.nowPlaying
                        .first()
                        ?.mediaMetadata
                        ?.artist
                        ?: ""
                }
            val lyricsProvider = dataStoreManager.lyricsProvider.first()
            val smartLyricsDefaults = dataStoreManager.smartLyricsDefaults.first()
            
            // Determine if this is YouTube video or YouTube Music content
            val isYouTubeVideo = videoId.startsWith("http") && videoId.contains("youtube.com")
            val isYouTubeMusic = videoId.startsWith("http") && videoId.contains("music.youtube.com")
            
            // Use smart provider selection if enabled, otherwise use manually selected provider
            val effectiveProvider = if (smartLyricsDefaults) {
                // Use smart defaults
                when {
                    isYouTubeVideo -> DataStoreManager.YOUTUBE // YouTube videos default to YouTube transcript
                    isYouTubeMusic && dataStoreManager.spdc.first().isNotEmpty() -> DataStoreManager.SPOTIFY // YouTube Music with Spotify login defaults to Spotify
                    isYouTubeMusic -> DataStoreManager.LRCLIB // YouTube Music without Spotify defaults to LRCLIB
                    dataStoreManager.spdc.first().isNotEmpty() -> DataStoreManager.SPOTIFY // Other content with Spotify login defaults to Spotify
                    else -> DataStoreManager.LRCLIB // Default to LRCLIB for other content
                }
            } else {
                // Use manually selected provider
                lyricsProvider
            }
            
            when (effectiveProvider) {

                DataStoreManager.LRCLIB -> {
                    getLrclibLyrics(
                        song,
                        (artist ?: "").toString(),
                        duration,
                    )
                }
                DataStoreManager.SPOTIFY -> {
                    val track = _nowPlayingState.value?.track
                    if (track != null) {
                        val query = "${song.title} ${artist ?: ""}"
                        getSpotifyLyrics(track, query, duration)
                    } else {
                        // Fallback to LRCLIB if no track available
                        getLrclibLyrics(
                            song,
                            (artist ?: "").toString(),
                            duration,
                        )
                    }
                }
                DataStoreManager.YOUTUBE -> {
                    Log.d(tag, "Getting YouTube transcript lyrics for $videoId")
                    mainRepository.getYouTubeCaption(videoId).cancellable().collect { response ->
                        when (response) {
                            is Resource.Success -> {
                                Log.d(tag, "YouTube transcript response received")
                                if (response.data != null) {
                                    val lyrics = response.data.first
                                    val translatedLyrics = response.data.second
                                    Log.d(tag, "YouTube transcript data: lyrics=${lyrics != null}, translatedLyrics=${translatedLyrics != null}")
                                    if (lyrics != null) {
                                        Log.d(tag, "YouTube transcript lyrics found: ${lyrics.lines?.size ?: 0} lines, syncType=${lyrics.syncType}")
                                        insertLyrics(lyrics.toLyricsEntity(videoId))
                                        updateLyrics(
                                            videoId,
                                            duration,
                                            lyrics,
                                            false,
                                            LyricsProvider.YOUTUBE,
                                        )
                                        if (translatedLyrics != null) {
                                            Log.d(tag, "YouTube transcript translated lyrics found: ${translatedLyrics.lines?.size ?: 0} lines")
                                            updateLyrics(
                                                videoId,
                                                duration,
                                                translatedLyrics,
                                                true,
                                                LyricsProvider.YOUTUBE,
                                            )
                                        } else {
                                            Log.d(tag, "No translated lyrics available")
                                        }
                                    } else {
                                        Log.w(tag, "YouTube transcript lyrics is null")
                                    }
                                } else {
                                    Log.w(tag, "YouTube transcript data is null, trying LRCLIB fallback")
                                    getLrclibLyrics(
                                        song,
                                        (artist ?: "").toString(),
                                        duration,
                                    )
                                }
                            }

                            is Resource.Error -> {
                                Log.w(tag, "YouTube transcript error: ${response.message}, trying LRCLIB fallback")
                                getLrclibLyrics(
                                    song,
                                    (artist ?: "").toString(),
                                    duration,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getLrclibLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            Log.d(tag, "Getting LRCLIB lyrics for ${song.videoId}")
            mainRepository.getLrclibLyrics(song.videoId).cancellable().collect { response ->
                when (response) {
                    is Resource.Success -> {
                        if (response.data != null) {
                            Log.d(tag, "LRCLIB lyrics found: ${response.data.lines?.size ?: 0} lines")
                            updateLyrics(
                                song.videoId,
                                duration,
                                response.data,
                                false,
                                LyricsProvider.LRCLIB,
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.w(tag, "LRCLIB lyrics error: ${response.message}")
                        // Try YouTube transcript as fallback
                        getYouTubeTranscriptLyrics(song, duration)
                    }
                }
            }
        }
    }

    private fun getYouTubeTranscriptLyrics(
        song: SongEntity,
        duration: Int,
    ) {
        viewModelScope.launch {
            Log.d(tag, "Getting YouTube transcript lyrics for ${song.videoId}")
            mainRepository.getYouTubeCaption(song.videoId).cancellable().collect { response ->
                when (response) {
                    is Resource.Success -> {
                        Log.d(tag, "YouTube transcript response received")
                        if (response.data != null) {
                            val (lyrics, translatedLyrics) = response.data
                            Log.d(tag, "YouTube transcript data: lyrics=${lyrics != null}, translatedLyrics=${translatedLyrics != null}")
                            if (lyrics != null) {
                                Log.d(tag, "YouTube transcript lyrics found: ${lyrics.lines?.size ?: 0} lines, syncType=${lyrics.syncType}")
                                updateLyrics(
                                    song.videoId,
                                    duration,
                                    lyrics,
                                    false,
                                    LyricsProvider.YOUTUBE,
                                )
                            } else {
                                Log.w(tag, "YouTube transcript lyrics is null")
                            }
                        } else {
                            Log.w(tag, "YouTube transcript response data is null")
                        }
                    }
                    is Resource.Error -> {
                        Log.w(tag, "YouTube transcript lyrics error: ${response.message}")
                        // No more fallbacks, lyrics not available
                    }
                }
            }
        }
    }

    private fun getSpotifyLyrics(
        track: Track,
        query: String,
        duration: Int? = null,
    ) {
        viewModelScope.launch {
            Log.d("Check SpotifyLyrics", "SpotifyLyrics $query")
            mainRepository.getSpotifyLyrics(query, duration).cancellable().collect { response ->
                Log.d("Check SpotifyLyrics", response.toString())
                when (response) {
                    is Resource.Success -> {
                        if (response.data != null) {
                            insertLyrics(
                                response.data.toLyricsEntity(
                                    track.videoId,
                                ),
                            )
                            updateLyrics(
                                track.videoId,
                                duration ?: 0,
                                response.data,
                                false,
                                LyricsProvider.SPOTIFY,
                            )
                        }
                    }

                    is Resource.Error -> {
                        getLrclibLyrics(
                            track.toSongEntity(),
                            track.artists.toListName().firstOrNull() ?: "",
                            duration ?: 0,
                        )
                    }
                }
            }
        }
    }

    fun setLyricsProvider() {
        viewModelScope.launch {
            nowPlayingState.value?.songEntity?.let {
                getLyricsFromFormat(it, timeline.value.total.toInt() / 1000)
            }
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            simpleMediaServiceHandler.loadMoreCatalog(listTrack)
            Toast
                .makeText(
                    context,
                    context.getString(R.string.added_to_queue),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    fun addToYouTubeLiked() {
        viewModelScope.launch {
            val videoId = simpleMediaServiceHandler.nowPlaying.first()?.mediaId
            if (videoId != null) {
                val like = likeStatus.value
                if (!like) {
                    mainRepository
                        .addToYouTubeLiked(
                            simpleMediaServiceHandler.nowPlaying.first()?.mediaId,
                        ).collect { response ->
                            if (response == 200) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.added_to_youtube_liked),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                getLikeStatus(videoId)
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.error),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                } else {
                    mainRepository
                        .removeFromYouTubeLiked(
                            simpleMediaServiceHandler.nowPlaying.first()?.mediaId,
                        ).collect {
                            if (it == 200) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.removed_from_youtube_liked),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                getLikeStatus(videoId)
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.error),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                }
            }
        }
    }

    fun getTranslucentBottomBar() = dataStoreManager.translucentBottomBar

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    fun shouldCheckForUpdate(): Boolean = runBlocking { dataStoreManager.autoCheckForUpdates.first() == TRUE }

    fun runWorker() {
        Log.w("Check Worker", "Worker")
        val request =
            PeriodicWorkRequestBuilder<NotifyWork>(
                12L,
                TimeUnit.HOURS,
            ).addTag("Worker Test")
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(application).enqueueUniquePeriodicWork(
            "Artist Worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    fun downloadFile(bitmap: Bitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            ).path}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                mainRepository
                    .downloadToFile(
                        track = track,
                        bitmap = bitmap,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun onDoneReview(isDismissOnly: Boolean = true) {
        viewModelScope.launch {
            if (!isDismissOnly) {
                dataStoreManager.doneOpenAppTime()
            } else {
                dataStoreManager.openApp()
            }
        }
    }

    fun onDoneRequestingShareLyrics(contributor: Pair<String, String>? = null) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(true)
            dataStoreManager.setContributorLyricsDatabase(
                contributor,
            )
        }
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

enum class LyricsProvider {
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    AI,
    OFFLINE,
}

data class TimeLine(
    val current: Long,
    val total: Long,
    val bufferedPercent: Int,
    val loading: Boolean = true,
)

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Lyrics? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}