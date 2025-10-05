package iad1tya.echo.music.utils

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

object CrashlyticsHelper {
    
    private var crashlytics: FirebaseCrashlytics? = null
    
    fun initialize(context: Context) {
        try {
            crashlytics = FirebaseManager.getCrashlytics()
        } catch (e: Exception) {
            // Crashlytics initialization failed, continue without it
        }
    }
    
    /**
     * Record a non-fatal exception
     */
    fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            // Ignore crashlytics errors to prevent infinite loops
        }
    }
    
    /**
     * Log a custom message
     */
    fun log(message: String) {
        try {
            crashlytics?.log(message)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Set a custom key-value pair for crash reports
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Set a custom key-value pair for crash reports (Long)
     */
    fun setCustomKey(key: String, value: Long) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Set a custom key-value pair for crash reports (Boolean)
     */
    fun setCustomKey(key: String, value: Boolean) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Set a custom key-value pair for crash reports (Float)
     */
    fun setCustomKey(key: String, value: Float) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Set user identifier for crash reports
     */
    fun setUserId(userId: String) {
        try {
            crashlytics?.setUserId(userId)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Log music-related events for debugging crashes
     */
    fun logMusicEvent(event: String, songTitle: String? = null, artistName: String? = null) {
        try {
            log("Music Event: $event")
            songTitle?.let { setCustomKey("current_song", it) }
            artistName?.let { setCustomKey("current_artist", it) }
            setCustomKey("last_music_event", event)
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Log app state for debugging crashes
     */
    fun logAppState(state: String, additionalInfo: Map<String, String> = emptyMap()) {
        try {
            log("App State: $state")
            setCustomKey("app_state", state)
            additionalInfo.forEach { (key, value) ->
                setCustomKey("state_$key", value)
            }
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Log performance metrics for debugging crashes
     */
    fun logPerformanceMetrics(metrics: Map<String, Any>) {
        try {
            log("Performance Metrics")
            metrics.forEach { (key, value) ->
                when (value) {
                    is String -> setCustomKey("perf_$key", value)
                    is Long -> setCustomKey("perf_$key", value)
                    is Int -> setCustomKey("perf_$key", value.toLong())
                    is Float -> setCustomKey("perf_$key", value)
                    is Boolean -> setCustomKey("perf_$key", value)
                }
            }
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Log memory usage for debugging crashes
     */
    fun logMemoryUsage() {
        try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val freeMemory = runtime.freeMemory()
            
            setCustomKey("memory_used_mb", usedMemory / 1024 / 1024)
            setCustomKey("memory_max_mb", maxMemory / 1024 / 1024)
            setCustomKey("memory_free_mb", freeMemory / 1024 / 1024)
            setCustomKey("memory_usage_percent", (usedMemory.toFloat() / maxMemory * 100))
            
            log("Memory: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB")
        } catch (e: Exception) {
            // Ignore crashlytics errors
        }
    }
    
    /**
     * Test crashlytics by throwing a test exception (only for testing)
     */
    fun testCrashlytics() {
        try {
            log("Testing Crashlytics integration")
            throw RuntimeException("Test crash for Crashlytics")
        } catch (e: Exception) {
            recordException(e)
        }
    }
}
