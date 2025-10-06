package iad1tya.echo.music.data.cache

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Lazy loading manager for home screen content
 * Loads content progressively to improve perceived performance
 */
class HomeScreenLazyLoader(
    private val cacheManager: HomeScreenCacheManager,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "HomeScreenLazyLoader"
        
        // Loading phases
        private const val PHASE_CACHE = 0
        private const val PHASE_ESSENTIAL = 1
        private const val PHASE_SECONDARY = 2
        private const val PHASE_BACKGROUND = 3
    }
    
    @Serializable
    data class LoadingPhase(
        val phase: Int,
        val name: String,
        val priority: Int
    )
    
    @Serializable
    data class LazyLoadingState(
        val currentPhase: Int = PHASE_CACHE,
        val isLoading: Boolean = true,
        val loadedPhases: Set<Int> = emptySet(),
        val error: String? = null
    )
    
    private val _loadingState = MutableStateFlow(LazyLoadingState())
    val loadingState: StateFlow<LazyLoadingState> = _loadingState.asStateFlow()
    
    private val _homeData = MutableStateFlow<List<iad1tya.echo.music.data.model.home.HomeItem>>(emptyList())
    val homeData: StateFlow<List<iad1tya.echo.music.data.model.home.HomeItem>> = _homeData.asStateFlow()
    
    private val _chartData = MutableStateFlow<iad1tya.echo.music.data.model.home.chart.Chart?>(null)
    val chartData: StateFlow<iad1tya.echo.music.data.model.home.chart.Chart?> = _chartData.asStateFlow()
    
    private val _newReleaseData = MutableStateFlow<List<iad1tya.echo.music.data.model.home.HomeItem>>(emptyList())
    val newReleaseData: StateFlow<List<iad1tya.echo.music.data.model.home.HomeItem>> = _newReleaseData.asStateFlow()
    
    private val _moodData = MutableStateFlow<iad1tya.echo.music.data.model.explore.mood.Mood?>(null)
    val moodData: StateFlow<iad1tya.echo.music.data.model.explore.mood.Mood?> = _moodData.asStateFlow()
    
    private val _recentlyPlayedData = MutableStateFlow<List<iad1tya.echo.music.data.db.entities.SongEntity>>(emptyList())
    val recentlyPlayedData: StateFlow<List<iad1tya.echo.music.data.db.entities.SongEntity>> = _recentlyPlayedData.asStateFlow()
    
    // Callbacks for data fetching
    private var fetchHomeDataCallback: (suspend () -> List<iad1tya.echo.music.data.model.home.HomeItem>)? = null
    private var fetchChartDataCallback: (suspend () -> iad1tya.echo.music.data.model.home.chart.Chart?)? = null
    private var fetchNewReleaseCallback: (suspend () -> List<iad1tya.echo.music.data.model.home.HomeItem>)? = null
    private var fetchMoodDataCallback: (suspend () -> iad1tya.echo.music.data.model.explore.mood.Mood?)? = null
    private var fetchRecentlyPlayedCallback: (suspend () -> List<iad1tya.echo.music.data.db.entities.SongEntity>)? = null
    
    /**
     * Set callbacks for data fetching
     */
    fun setDataFetchCallbacks(
        fetchHomeData: suspend () -> List<iad1tya.echo.music.data.model.home.HomeItem>,
        fetchChartData: suspend () -> iad1tya.echo.music.data.model.home.chart.Chart?,
        fetchNewRelease: suspend () -> List<iad1tya.echo.music.data.model.home.HomeItem>,
        fetchMoodData: suspend () -> iad1tya.echo.music.data.model.explore.mood.Mood?,
        fetchRecentlyPlayed: suspend () -> List<iad1tya.echo.music.data.db.entities.SongEntity>
    ) {
        fetchHomeDataCallback = fetchHomeData
        fetchChartDataCallback = fetchChartData
        fetchNewReleaseCallback = fetchNewRelease
        fetchMoodDataCallback = fetchMoodData
        fetchRecentlyPlayedCallback = fetchRecentlyPlayed
    }
    
    /**
     * Start lazy loading process
     */
    fun startLazyLoading() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting lazy loading process")
                _loadingState.value = LazyLoadingState(isLoading = true)
                
                // Phase 1: Load from cache immediately
                loadFromCache()
                
                // Phase 2: Load essential data (home data)
                loadEssentialData()
                
                // Phase 3: Load secondary data (chart, new release)
                loadSecondaryData()
                
                // Phase 4: Load background data (mood, recently played)
                loadBackgroundData()
                
                _loadingState.value = LazyLoadingState(
                    currentPhase = PHASE_BACKGROUND,
                    isLoading = false,
                    loadedPhases = setOf(PHASE_CACHE, PHASE_ESSENTIAL, PHASE_SECONDARY, PHASE_BACKGROUND)
                )
                
                Log.d(TAG, "Lazy loading completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during lazy loading", e)
                _loadingState.value = LazyLoadingState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Phase 1: Load data from cache
     */
    private suspend fun loadFromCache() {
        try {
            Log.d(TAG, "Phase 1: Loading from cache")
            
            // Check if we have valid cache for each data type
            val hasHomeCache = cacheManager.isCacheValid("home_data.txt")
            val hasChartCache = cacheManager.isCacheValid("chart_data.txt")
            val hasNewReleaseCache = cacheManager.isCacheValid("new_release.txt")
            val hasMoodCache = cacheManager.isCacheValid("mood_data.txt")
            val hasRecentlyPlayedCache = cacheManager.isCacheValid("recently_played.txt")
            
            Log.d(TAG, "Cache status - Home: $hasHomeCache, Chart: $hasChartCache, NewRelease: $hasNewReleaseCache, Mood: $hasMoodCache, RecentlyPlayed: $hasRecentlyPlayedCache")
            
            _loadingState.value = LazyLoadingState(
                currentPhase = PHASE_ESSENTIAL,
                isLoading = true,
                loadedPhases = setOf(PHASE_CACHE)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from cache", e)
        }
    }
    
    /**
     * Phase 2: Load essential data (home data)
     */
    private suspend fun loadEssentialData() {
        try {
            Log.d(TAG, "Phase 2: Loading essential data")
            
            // Check if we need to refresh home data
            val shouldRefresh = cacheManager.shouldRefreshCache("home_data.txt")
            if (shouldRefresh && fetchHomeDataCallback != null) {
                val freshHomeData = fetchHomeDataCallback!!()
                _homeData.value = freshHomeData
                cacheManager.markAsCached("home_data.txt")
                Log.d(TAG, "Refreshed home data: ${freshHomeData.size} items")
            }
            
            _loadingState.value = LazyLoadingState(
                currentPhase = PHASE_SECONDARY,
                isLoading = true,
                loadedPhases = setOf(PHASE_CACHE, PHASE_ESSENTIAL)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading essential data", e)
        }
    }
    
    /**
     * Phase 3: Load secondary data (chart, new release)
     */
    private suspend fun loadSecondaryData() {
        try {
            Log.d(TAG, "Phase 3: Loading secondary data")
            
            // Load chart data
            if (fetchChartDataCallback != null) {
                val freshChartData = fetchChartDataCallback!!()
                _chartData.value = freshChartData
                cacheManager.markAsCached("chart_data.txt")
                Log.d(TAG, "Loaded chart data")
            }
            
            // Load new release data
            if (fetchNewReleaseCallback != null) {
                val freshNewReleaseData = fetchNewReleaseCallback!!()
                _newReleaseData.value = freshNewReleaseData
                cacheManager.markAsCached("new_release.txt")
                Log.d(TAG, "Loaded ${freshNewReleaseData.size} new release items")
            }
            
            _loadingState.value = LazyLoadingState(
                currentPhase = PHASE_BACKGROUND,
                isLoading = true,
                loadedPhases = setOf(PHASE_CACHE, PHASE_ESSENTIAL, PHASE_SECONDARY)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading secondary data", e)
        }
    }
    
    /**
     * Phase 4: Load background data (mood, recently played)
     */
    private suspend fun loadBackgroundData() {
        try {
            Log.d(TAG, "Phase 4: Loading background data")
            
            // Load mood data
            if (fetchMoodDataCallback != null) {
                val freshMoodData = fetchMoodDataCallback!!()
                _moodData.value = freshMoodData
                cacheManager.markAsCached("mood_data.txt")
                Log.d(TAG, "Loaded mood data")
            }
            
            // Load recently played data
            if (fetchRecentlyPlayedCallback != null) {
                val freshRecentlyPlayedData = fetchRecentlyPlayedCallback!!()
                _recentlyPlayedData.value = freshRecentlyPlayedData
                cacheManager.markAsCached("recently_played.txt")
                Log.d(TAG, "Loaded ${freshRecentlyPlayedData.size} recently played items")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading background data", e)
        }
    }
    
    /**
     * Refresh specific data type
     */
    fun refreshData(type: DataType) {
        coroutineScope.launch {
            try {
                when (type) {
                    DataType.HOME_DATA -> {
                        if (fetchHomeDataCallback != null) {
                            val freshData = fetchHomeDataCallback!!()
                            _homeData.value = freshData
                            cacheManager.markAsCached("home_data.txt")
                        }
                    }
                    DataType.CHART_DATA -> {
                        if (fetchChartDataCallback != null) {
                            val freshData = fetchChartDataCallback!!()
                            _chartData.value = freshData
                            cacheManager.markAsCached("chart_data.txt")
                        }
                    }
                    DataType.NEW_RELEASE -> {
                        if (fetchNewReleaseCallback != null) {
                            val freshData = fetchNewReleaseCallback!!()
                            _newReleaseData.value = freshData
                            cacheManager.markAsCached("new_release.txt")
                        }
                    }
                    DataType.MOOD_DATA -> {
                        if (fetchMoodDataCallback != null) {
                            val freshData = fetchMoodDataCallback!!()
                            _moodData.value = freshData
                            cacheManager.markAsCached("mood_data.txt")
                        }
                    }
                    DataType.RECENTLY_PLAYED -> {
                        if (fetchRecentlyPlayedCallback != null) {
                            val freshData = fetchRecentlyPlayedCallback!!()
                            _recentlyPlayedData.value = freshData
                            cacheManager.markAsCached("recently_played.txt")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing ${type.name}", e)
            }
        }
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearCache() {
        cacheManager.clearAllCache()
        _homeData.value = emptyList()
        _chartData.value = null
        _newReleaseData.value = emptyList()
        _moodData.value = null
        _recentlyPlayedData.value = emptyList()
    }
    
    enum class DataType {
        HOME_DATA,
        CHART_DATA,
        NEW_RELEASE,
        MOOD_DATA,
        RECENTLY_PLAYED
    }
}
