package iad1tya.echo.music.data.cache

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Background refresh manager for home screen data
 * Refreshes cached data in the background to keep it fresh
 */
class HomeScreenBackgroundRefreshManager(
    private val application: Application,
    private val cacheManager: HomeScreenCacheManager
) {
    
    companion object {
        private const val TAG = "BackgroundRefresh"
        private const val WORK_NAME = "home_screen_background_refresh"
        private const val REFRESH_INTERVAL_HOURS = 6L // Refresh every 6 hours
    }
    
    private val workManager = WorkManager.getInstance(application)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
     * Start background refresh
     */
    fun startBackgroundRefresh() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val backgroundRefreshRequest = PeriodicWorkRequestBuilder<HomeScreenBackgroundRefreshWorker>(
                REFRESH_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                backgroundRefreshRequest
            )
            
            Log.d(TAG, "Background refresh started - will refresh every $REFRESH_INTERVAL_HOURS hours")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background refresh", e)
        }
    }
    
    /**
     * Stop background refresh
     */
    fun stopBackgroundRefresh() {
        try {
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Background refresh stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop background refresh", e)
        }
    }
    
    /**
     * Perform immediate background refresh
     */
    fun performBackgroundRefresh() {
        backgroundScope.launch {
            try {
                Log.d(TAG, "Performing background refresh")
                
                // Refresh home data
                if (fetchHomeDataCallback != null) {
                    val homeData = fetchHomeDataCallback!!()
                    cacheManager.markAsCached("home_data.txt")
                    Log.d(TAG, "Background refreshed home data: ${homeData.size} items")
                }
                
                // Refresh chart data
                if (fetchChartDataCallback != null) {
                    val chartData = fetchChartDataCallback!!()
                    cacheManager.markAsCached("chart_data.txt")
                    Log.d(TAG, "Background refreshed chart data")
                }
                
                // Refresh new release data
                if (fetchNewReleaseCallback != null) {
                    val newReleaseData = fetchNewReleaseCallback!!()
                    cacheManager.markAsCached("new_release.txt")
                    Log.d(TAG, "Background refreshed new release data: ${newReleaseData.size} items")
                }
                
                // Refresh mood data
                if (fetchMoodDataCallback != null) {
                    val moodData = fetchMoodDataCallback!!()
                    cacheManager.markAsCached("mood_data.txt")
                    Log.d(TAG, "Background refreshed mood data")
                }
                
                // Refresh recently played data
                if (fetchRecentlyPlayedCallback != null) {
                    val recentlyPlayedData = fetchRecentlyPlayedCallback!!()
                    cacheManager.markAsCached("recently_played.txt")
                    Log.d(TAG, "Background refreshed recently played data: ${recentlyPlayedData.size} items")
                }
                
                Log.d(TAG, "Background refresh completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during background refresh", e)
            }
        }
    }
    
    /**
     * Check if background refresh is enabled
     */
    fun isBackgroundRefreshEnabled(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            workInfos.any { it.state.isFinished.not() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check background refresh status", e)
            false
        }
    }
}
