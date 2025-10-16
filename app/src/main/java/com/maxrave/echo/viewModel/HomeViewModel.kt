package iad1tya.echo.music.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import iad1tya.echo.music.R
import iad1tya.echo.music.common.DownloadState
import iad1tya.echo.music.common.SELECTED_LANGUAGE
import iad1tya.echo.music.common.SUPPORTED_LANGUAGE
import iad1tya.echo.music.data.dataStore.DataStoreManager.Settings.TRUE
import iad1tya.echo.music.data.db.entities.LocalPlaylistEntity
import iad1tya.echo.music.data.db.entities.PairSongLocalPlaylist
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.data.model.browse.album.Track
import iad1tya.echo.music.data.model.explore.mood.Mood
import iad1tya.echo.music.data.model.home.HomeDataCombine
import iad1tya.echo.music.data.model.home.HomeItem
import iad1tya.echo.music.data.model.home.chart.Chart
import iad1tya.echo.music.extension.toSongEntity
import iad1tya.echo.music.utils.Resource
import iad1tya.echo.music.viewModel.base.BaseViewModel
import iad1tya.echo.music.data.cache.HomeScreenCacheManager
import iad1tya.echo.music.data.cache.HomeScreenLazyLoader
import iad1tya.echo.music.data.cache.HomeScreenBackgroundRefreshManager
import iad1tya.echo.music.utils.AppStateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

@UnstableApi
class HomeViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList
    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null
    
    // Cache and lazy loading
    private val cacheManager = HomeScreenCacheManager(application)
    private val lazyLoader = HomeScreenLazyLoader(cacheManager, viewModelScope)
    private val backgroundRefreshManager = HomeScreenBackgroundRefreshManager(application, cacheManager)
    
    // Lazy loading state
    val lazyLoadingState = lazyLoader.loadingState

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<ArrayList<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<ArrayList<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val youTubeCookie =
        dataStoreManager
            .cookie
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val chartKey =
        dataStoreManager
            .chartKey
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ZZ")

    init {
        if (runBlocking { dataStoreManager.cookie.first() }.isEmpty() &&
            runBlocking {
                dataStoreManager.shouldShowLogInRequiredAlert.first() == TRUE
            }
        ) {
            _showLogInAlert.update { true }
        }
        
        // Initialize lazy loading with data fetch callbacks
        setupLazyLoading()
        
        homeJob = Job()
        viewModelScope.launch {
            regionCodeChart.value = dataStoreManager.chartKey.first()
            exploreChart(regionCodeChart.value ?: "ZZ")
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            
            // Start lazy loading instead of immediate loading
            lazyLoader.startLazyLoading()
            
            // Listen to lazy loader data and update local state
            observeLazyLoaderData()
            
            //  refresh when region change
            val job1 =
                launch {
                    dataStoreManager.location.distinctUntilChanged().collect {
                        regionCode = it
                        refreshData()
                    }
                }
            //  refresh when language change
            val job2 =
                launch {
                    dataStoreManager.language.distinctUntilChanged().collect {
                        language = it
                        refreshData()
                    }
                }
            val job3 =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collect {
                        refreshData()
                        _accountInfo.emit(
                            Pair(
                                dataStoreManager.getString("AccountName").first(),
                                dataStoreManager.getString("AccountThumbUrl").first(),
                            ),
                        )
                    }
                }
            val job4 =
                launch {
                    params.collectLatest {
                        refreshData()
                    }
                }
            val job5 =
                launch {
                    youTubeCookie.collectLatest {
                        if (it.isNotEmpty()) {
                            refreshData()
                        }
                    }
                }
            val job6 =
                launch {
                    dataStoreManager.chartKey.distinctUntilChanged().collect {
                        exploreChart(it)
                    }
                }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }
    
    /**
     * Setup lazy loading with data fetch callbacks
     */
    private fun setupLazyLoading() {
        val fetchHomeData = suspend {
            Log.d("HomeViewModel", "Fetching home data")
            val result = mainRepository.getHomeData(params.value).first()
            when (result) {
                is Resource.Success -> result.data ?: emptyList()
                else -> emptyList()
            }
        }
        
        val fetchChartData = suspend {
            Log.d("HomeViewModel", "Fetching chart data")
            val result = mainRepository.getChartData(dataStoreManager.chartKey.first()).first()
            when (result) {
                is Resource.Success -> result.data
                else -> null
            }
        }
        
        val fetchNewRelease = suspend {
            Log.d("HomeViewModel", "Fetching new release data")
            val result = mainRepository.getNewRelease().first()
            when (result) {
                is Resource.Success -> result.data ?: emptyList()
                else -> emptyList()
            }
        }
        
        val fetchMoodData = suspend {
            Log.d("HomeViewModel", "Fetching mood data")
            val result = mainRepository.getMoodAndMomentsData().first()
            when (result) {
                is Resource.Success -> result.data
                else -> null
            }
        }
        
        val fetchRecentlyPlayed = suspend {
            Log.d("HomeViewModel", "Fetching recently played data")
            // This would need to be implemented in SharedViewModel
            emptyList<iad1tya.echo.music.data.db.entities.SongEntity>()
        }
        
        // Setup lazy loader
        lazyLoader.setDataFetchCallbacks(
            fetchHomeData = fetchHomeData,
            fetchChartData = fetchChartData,
            fetchNewRelease = fetchNewRelease,
            fetchMoodData = fetchMoodData,
            fetchRecentlyPlayed = fetchRecentlyPlayed
        )
        
        // Setup background refresh manager
        backgroundRefreshManager.setDataFetchCallbacks(
            fetchHomeData = fetchHomeData,
            fetchChartData = fetchChartData,
            fetchNewRelease = fetchNewRelease,
            fetchMoodData = fetchMoodData,
            fetchRecentlyPlayed = fetchRecentlyPlayed
        )
        
        // Start background refresh
        backgroundRefreshManager.startBackgroundRefresh()
    }
    
    /**
     * Observe lazy loader data and update local state
     */
    private fun observeLazyLoaderData() {
        viewModelScope.launch {
            lazyLoader.homeData.collect { data ->
                _homeItemList.value = data
                loading.value = false
            }
        }
        
        viewModelScope.launch {
            lazyLoader.chartData.collect { data ->
                _chart.value = data
                loadingChart.value = false
            }
        }
        
        viewModelScope.launch {
            lazyLoader.newReleaseData.collect { data ->
                _newRelease.value = ArrayList(data)
            }
        }
        
        viewModelScope.launch {
            lazyLoader.moodData.collect { data ->
                _exploreMoodItem.value = data
            }
        }
    }
    
    /**
     * Refresh data using lazy loader
     */
    fun refreshData() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Refreshing data")
            lazyLoader.refreshData(HomeScreenLazyLoader.DataType.HOME_DATA)
            lazyLoader.refreshData(HomeScreenLazyLoader.DataType.CHART_DATA)
            lazyLoader.refreshData(HomeScreenLazyLoader.DataType.NEW_RELEASE)
            lazyLoader.refreshData(HomeScreenLazyLoader.DataType.MOOD_DATA)
        }
    }
    
    /**
     * Clear cache and refresh data
     */
    fun clearCacheAndRefresh() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Clearing cache and refreshing data")
            cacheManager.clearAllCache()
            refreshData()
        }
    }
    
    /**
     * Check if home data should be loaded (only load once per session)
     */
    fun shouldLoadHomeData(): Boolean {
        return AppStateManager.shouldLoadHomeData()
    }
    
    /**
     * Initialize home data only once when app starts
     */
    fun initializeHomeDataOnce(params: String? = null) {
        if (AppStateManager.shouldLoadHomeData()) {
            Log.d("HomeViewModel", "Initializing home data for the first time")
            getHomeItemList(params, forceRefresh = false)
        } else {
            Log.d("HomeViewModel", "Home data already loaded, skipping initialization")
            loading.value = false
        }
    }

    fun getHomeItemList(params: String? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // If force refresh is requested, clear the cache
            if (forceRefresh) {
                Log.d("HomeViewModel", "Force refresh requested, clearing cache")
                cacheManager.clearAllCache()
            }
            
            // Check if data is already loaded and cache is valid (unless force refresh is requested)
            if (!forceRefresh && _homeItemList.value.isNotEmpty() && cacheManager.isCacheValid("home_data.txt")) {
                Log.d("HomeViewModel", "Using cached home data, skipping reload")
                loading.value = false
                return@launch
            }
            
            loading.value = true
            language =
                runBlocking {
                    dataStoreManager.getString(SELECTED_LANGUAGE).first()
                        ?: SUPPORTED_LANGUAGE.codes.first()
                }
            regionCode = runBlocking { dataStoreManager.location.first() }
            homeJob?.cancel()
            homeJob =
                viewModelScope.launch {
                combine(
                    mainRepository.getHomeData(params),
                    mainRepository.getMoodAndMomentsData(),
                    mainRepository.getChartData(dataStoreManager.chartKey.first()),
                    mainRepository.getNewRelease(),
                ) { home, exploreMood, exploreChart, newRelease ->
                    HomeDataCombine(home, exploreMood, exploreChart, newRelease)
                }.collect { result ->
                    val home = result.home
                    Log.d("home size", "${home.data?.size}")
                    val exploreMoodItem = result.mood
                    val chart = result.chart
                    val newRelease = result.newRelease
                    when (home) {
                        is Resource.Success -> {
                            _homeItemList.value = home.data ?: listOf()
                            // Mark cache as valid when data is successfully loaded
                            cacheManager.markAsCached("home_data.txt")
                            // Mark as loaded in AppStateManager
                            AppStateManager.markHomeDataLoaded()
                            Log.d("HomeViewModel", "Home data loaded and cached: ${home.data?.size} items")
                        }

                        else -> {
                            _homeItemList.value = listOf()
                        }
                    }
                    when (chart) {
                        is Resource.Success -> {
                            _chart.value = chart.data
                            cacheManager.markAsCached("chart_data.txt")
                            Log.d("HomeViewModel", "Chart data loaded and cached")
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    when (newRelease) {
                        is Resource.Success -> {
                            _newRelease.value = newRelease.data ?: arrayListOf()
                            cacheManager.markAsCached("new_release.txt")
                            Log.d("HomeViewModel", "New release data loaded and cached: ${newRelease.data?.size} items")
                        }

                        else -> {
                            _newRelease.value = arrayListOf()
                        }
                    }
                    when (exploreMoodItem) {
                        is Resource.Success -> {
                            _exploreMoodItem.value = exploreMoodItem.data
                            cacheManager.markAsCached("mood_data.txt")
                            Log.d("HomeViewModel", "Mood data loaded and cached")
                        }

                        else -> {
                            _exploreMoodItem.value = null
                        }
                    }
                    regionCodeChart.value = dataStoreManager.chartKey.first()
                    Log.d("HomeViewModel", "getHomeItemList: $result")
                    dataStoreManager.cookie.first().let {
                        if (it != "") {
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                    }
                    when {
                        home is Resource.Error -> home.message
                        exploreMoodItem is Resource.Error -> exploreMoodItem.message
                        chart is Resource.Error -> chart.message
                        else -> null
                    }?.let {
                        showSnackBarErrorState.emit(it)
                        Log.w("Error", "getHomeItemList: ${home.message}")
                        Log.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                        Log.w("Error", "getHomeItemList: ${chart.message}")
                    }
                    loading.value = false
                }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            mainRepository
                .getChartData(
                    region,
                ).collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = values.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    fun updateLikeStatus(
        videoId: String,
        b: Boolean,
    ) {
        viewModelScope.launch {
            if (b) {
                mainRepository.updateLikeStatus(videoId, 1)
            } else {
                mainRepository.updateLikeStatus(videoId, 0)
            }
        }
    }

    fun getSongEntity(track: Track) {
        viewModelScope.launch {
            mainRepository.insertSong(track.toSongEntity()).first().let {
                println("Insert song $it")
            }
            mainRepository.getSongById(track.videoId).collect { values ->
                Log.w("HomeViewModel", "getSongEntity: $values")
                _songEntity.value = values
            }
        }
    }

    private var _localPlaylist: MutableStateFlow<List<LocalPlaylistEntity>> =
        MutableStateFlow(
            listOf(),
        )
    val localPlaylist: StateFlow<List<LocalPlaylistEntity>> = _localPlaylist

    fun getAllLocalPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _localPlaylist.emit(values)
            }
        }
    }

    fun updateDownloadState(
        videoId: String,
        state: Int,
    ) {
        viewModelScope.launch {
            mainRepository.getSongById(videoId).collect { songEntity ->
                _songEntity.value = songEntity
            }
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    private var _downloadState: MutableStateFlow<Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<Download?> = _downloadState.asStateFlow()

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
    }

    fun updateLocalPlaylistTracks(
        list: List<String>,
        id: Long,
    ) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast
                    .makeText(
                        getApplication(),
                        application.getString(R.string.added_to_playlist),
                        Toast.LENGTH_SHORT,
                    ).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(
                        DownloadState.STATE_DOWNLOADED,
                        id,
                    )
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(
                        DownloadState.STATE_NOT_DOWNLOADED,
                        id,
                    )
                }
            }
        }
    }

    fun addToYouTubePlaylist(
        localPlaylistId: Long,
        youtubePlaylistId: String,
        videoId: String,
    ) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                localPlaylistId,
                LocalPlaylistEntity.YouTubeSyncState.Syncing,
            )
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.Synced,
                    )
                    Toast
                        .makeText(
                            getApplication(),
                            application.getString(R.string.added_to_youtube_playlist),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                    )
                    Toast
                        .makeText(
                            getApplication(),
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }
}