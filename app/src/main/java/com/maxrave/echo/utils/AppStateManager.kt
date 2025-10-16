package iad1tya.echo.music.utils

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global app state manager to track app lifecycle and navigation state
 * Helps prevent unnecessary data reloading when navigating between sections
 */
object AppStateManager {
    
    private const val TAG = "AppStateManager"
    
    // App lifecycle states
    private val _isAppFirstLaunch = MutableStateFlow(true)
    val isAppFirstLaunch: StateFlow<Boolean> = _isAppFirstLaunch.asStateFlow()
    
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()
    
    // Navigation states
    private val _currentScreen = MutableStateFlow<String?>(null)
    val currentScreen: StateFlow<String?> = _currentScreen.asStateFlow()
    
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()
    
    // Data loading states
    private val _homeDataLoaded = MutableStateFlow(false)
    val homeDataLoaded: StateFlow<Boolean> = _homeDataLoaded.asStateFlow()
    
    private val _recentlyPlayedLoaded = MutableStateFlow(false)
    val recentlyPlayedLoaded: StateFlow<Boolean> = _recentlyPlayedLoaded.asStateFlow()
    
    // Cache timestamps
    private var homeDataLoadTime: Long = 0L
    private var recentlyPlayedLoadTime: Long = 0L
    
    // Cache validity duration (5 minutes)
    private const val CACHE_VALIDITY_DURATION = 5 * 60 * 1000L
    
    /**
     * Mark app as launched (called when app starts)
     */
    fun onAppLaunched() {
        Log.d(TAG, "App launched")
        _isAppFirstLaunch.value = true
        _isAppInForeground.value = true
        resetDataLoadingStates()
    }
    
    /**
     * Mark app as resumed (called when app comes to foreground)
     */
    fun onAppResumed() {
        Log.d(TAG, "App resumed")
        _isAppInForeground.value = true
    }
    
    /**
     * Mark app as paused (called when app goes to background)
     */
    fun onAppPaused() {
        Log.d(TAG, "App paused")
        _isAppInForeground.value = false
    }
    
    /**
     * Mark app as no longer first launch
     */
    fun markAppNotFirstLaunch() {
        Log.d(TAG, "App no longer first launch")
        _isAppFirstLaunch.value = false
    }
    
    /**
     * Update current screen
     */
    fun setCurrentScreen(screen: String) {
        val previousScreen = _currentScreen.value
        _currentScreen.value = screen
        
        if (previousScreen != null && previousScreen != screen) {
            Log.d(TAG, "Navigating from $previousScreen to $screen")
            _isNavigating.value = true
            
            // Reset navigation state after a short delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                _isNavigating.value = false
            }
        }
    }
    
    /**
     * Check if home data should be loaded
     */
    fun shouldLoadHomeData(): Boolean {
        val hasLoaded = _homeDataLoaded.value
        val isFirstLaunch = _isAppFirstLaunch.value
        val isCacheValid = isHomeDataCacheValid()
        
        val shouldLoad = !hasLoaded || isFirstLaunch || !isCacheValid
        
        Log.d(TAG, "Should load home data: $shouldLoad (hasLoaded: $hasLoaded, isFirstLaunch: $isFirstLaunch, isCacheValid: $isCacheValid)")
        return shouldLoad
    }
    
    /**
     * Check if recently played data should be loaded
     */
    fun shouldLoadRecentlyPlayed(): Boolean {
        val hasLoaded = _recentlyPlayedLoaded.value
        val isFirstLaunch = _isAppFirstLaunch.value
        val isCacheValid = isRecentlyPlayedCacheValid()
        
        val shouldLoad = !hasLoaded || isFirstLaunch || !isCacheValid
        
        Log.d(TAG, "Should load recently played: $shouldLoad (hasLoaded: $hasLoaded, isFirstLaunch: $isFirstLaunch, isCacheValid: $isCacheValid)")
        return shouldLoad
    }
    
    /**
     * Mark home data as loaded
     */
    fun markHomeDataLoaded() {
        Log.d(TAG, "Home data loaded")
        _homeDataLoaded.value = true
        homeDataLoadTime = System.currentTimeMillis()
    }
    
    /**
     * Mark recently played data as loaded
     */
    fun markRecentlyPlayedLoaded() {
        Log.d(TAG, "Recently played data loaded")
        _recentlyPlayedLoaded.value = true
        recentlyPlayedLoadTime = System.currentTimeMillis()
    }
    
    /**
     * Check if home data cache is still valid
     */
    private fun isHomeDataCacheValid(): Boolean {
        if (homeDataLoadTime == 0L) return false
        val currentTime = System.currentTimeMillis()
        val isValid = (currentTime - homeDataLoadTime) < CACHE_VALIDITY_DURATION
        Log.d(TAG, "Home data cache valid: $isValid (age: ${currentTime - homeDataLoadTime}ms)")
        return isValid
    }
    
    /**
     * Check if recently played cache is still valid
     */
    private fun isRecentlyPlayedCacheValid(): Boolean {
        if (recentlyPlayedLoadTime == 0L) return false
        val currentTime = System.currentTimeMillis()
        val isValid = (currentTime - recentlyPlayedLoadTime) < CACHE_VALIDITY_DURATION
        Log.d(TAG, "Recently played cache valid: $isValid (age: ${currentTime - recentlyPlayedLoadTime}ms)")
        return isValid
    }
    
    /**
     * Reset data loading states (called when app is launched)
     */
    private fun resetDataLoadingStates() {
        _homeDataLoaded.value = false
        _recentlyPlayedLoaded.value = false
        homeDataLoadTime = 0L
        recentlyPlayedLoadTime = 0L
        Log.d(TAG, "Data loading states reset")
    }
    
    /**
     * Force refresh all data (called when user explicitly refreshes)
     */
    fun forceRefreshAllData() {
        Log.d(TAG, "Force refreshing all data")
        resetDataLoadingStates()
    }
    
    /**
     * Get current app state summary
     */
    fun getAppStateSummary(): String {
        return buildString {
            appendLine("=== App State Summary ===")
            appendLine("Is First Launch: ${_isAppFirstLaunch.value}")
            appendLine("Is In Foreground: ${_isAppInForeground.value}")
            appendLine("Current Screen: ${_currentScreen.value}")
            appendLine("Is Navigating: ${_isNavigating.value}")
            appendLine("Home Data Loaded: ${_homeDataLoaded.value}")
            appendLine("Recently Played Loaded: ${_recentlyPlayedLoaded.value}")
            appendLine("Home Data Cache Valid: ${isHomeDataCacheValid()}")
            appendLine("Recently Played Cache Valid: ${isRecentlyPlayedCacheValid()}")
            appendLine("========================")
        }
    }
}
