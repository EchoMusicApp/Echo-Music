package iad1tya.echo.music.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import iad1tya.echo.music.data.dataStore.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AnalyticsHelper : KoinComponent {
    
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private val dataStoreManager: DataStoreManager by inject()
    
    fun initialize(context: Context) {
        firebaseAnalytics = Firebase.analytics
    }
    
    private fun isAnalyticsEnabled(): Boolean {
        return try {
            runBlocking {
                dataStoreManager.analyticsEnabled.first()
            }
        } catch (e: Exception) {
            true // Default to enabled if there's an error
        }
    }
    
    fun logEvent(eventName: String, parameters: Map<String, Any>? = null) {
        // Check if analytics is enabled before logging
        if (!isAnalyticsEnabled()) {
            return
        }
        
        val bundle = Bundle().apply {
            parameters?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Int -> putLong(key, value.toLong())
                    is Double -> putDouble(key, value)
                    is Boolean -> putLong(key, if (value) 1L else 0L)
                }
            }
        }
        firebaseAnalytics?.logEvent(eventName, bundle)
    }
    
    // Music-related events
    fun logSongPlayed(songTitle: String, artistName: String, source: String = "unknown") {
        val params = mapOf(
            "song_title" to songTitle,
            "artist_name" to artistName,
            "source" to source
        )
        logEvent("song_played", params)
    }
    
    fun logSongPaused(songTitle: String, artistName: String) {
        val params = mapOf(
            "song_title" to songTitle,
            "artist_name" to artistName
        )
        logEvent("song_paused", params)
    }
    
    fun logSongSkipped(songTitle: String, artistName: String) {
        val params = mapOf(
            "song_title" to songTitle,
            "artist_name" to artistName
        )
        logEvent("song_skipped", params)
    }
    
    fun logPlaylistCreated(playlistName: String) {
        val params = mapOf("playlist_name" to playlistName)
        logEvent("playlist_created", params)
    }
    
    fun logPlaylistDeleted(playlistName: String) {
        val params = mapOf("playlist_name" to playlistName)
        logEvent("playlist_deleted", params)
    }
    
    fun logSongAddedToPlaylist(songTitle: String, playlistName: String) {
        val params = mapOf(
            "song_title" to songTitle,
            "playlist_name" to playlistName
        )
        logEvent("song_added_to_playlist", params)
    }
    
    fun logSongRemovedFromPlaylist(songTitle: String, playlistName: String) {
        val params = mapOf(
            "song_title" to songTitle,
            "playlist_name" to playlistName
        )
        logEvent("song_removed_from_playlist", params)
    }
    
    // Search events
    fun logSearchPerformed(query: String, resultCount: Int) {
        val params = mapOf(
            "search_query" to query,
            "result_count" to resultCount.toLong()
        )
        logEvent("search_performed", params)
    }
    
    // Settings events
    fun logSettingChanged(settingName: String, newValue: String) {
        val params = mapOf(
            "setting_name" to settingName,
            "new_value" to newValue
        )
        logEvent("setting_changed", params)
    }
    
    // App lifecycle events
    fun logAppOpened() {
        logEvent("app_opened")
    }
    
    fun logAppBackgrounded() {
        logEvent("app_backgrounded")
    }
    
    // Error events
    fun logError(errorType: String, errorMessage: String) {
        val params = mapOf(
            "error_type" to errorType,
            "error_message" to errorMessage
        )
        logEvent("error_occurred", params)
    }
    
    // Crash analytics events
    fun logCrash(crashType: String, crashMessage: String, stackTrace: String? = null, additionalInfo: String? = null) {
        val params = mutableMapOf(
            "crash_type" to crashType,
            "crash_message" to crashMessage,
            "timestamp" to System.currentTimeMillis()
        )
        
        stackTrace?.let { params["stack_trace_length"] = it.length.toLong() }
        additionalInfo?.let { params["additional_info"] = it }
        
        logEvent("app_crash", params)
    }
    
    fun logUncaughtException(exception: Throwable, threadName: String, additionalInfo: String? = null) {
        val params = mutableMapOf(
            "exception_type" to exception.javaClass.simpleName,
            "exception_message" to (exception.message ?: "No message"),
            "thread_name" to threadName,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Add stack trace info without exposing sensitive data
        val stackTrace = exception.stackTraceToString()
        params["stack_trace_length"] = stackTrace.length.toLong()
        params["stack_trace_lines"] = stackTrace.lines().size.toLong()
        
        additionalInfo?.let { params["additional_info"] = it }
        
        logEvent("uncaught_exception", params)
    }
    
    fun logCrashRecovery(recoveryMethod: String, success: Boolean) {
        val params = mapOf(
            "recovery_method" to recoveryMethod,
            "recovery_success" to success,
            "timestamp" to System.currentTimeMillis()
        )
        logEvent("crash_recovery", params)
    }
    
    fun logCrashReportGenerated(reportSize: Long, reportLocation: String) {
        val params = mapOf(
            "report_size_bytes" to reportSize,
            "report_location" to reportLocation,
            "timestamp" to System.currentTimeMillis()
        )
        logEvent("crash_report_generated", params)
    }
    
    fun logCrashReportExported(exportMethod: String, success: Boolean) {
        val params = mapOf(
            "export_method" to exportMethod,
            "export_success" to success,
            "timestamp" to System.currentTimeMillis()
        )
        logEvent("crash_report_exported", params)
    }
    
    // User engagement events
    fun logScreenViewed(screenName: String) {
        val params = mapOf("screen_name" to screenName)
        logEvent("screen_viewed", params)
    }
    
    fun logButtonClicked(buttonName: String, screenName: String) {
        val params = mapOf(
            "button_name" to buttonName,
            "screen_name" to screenName
        )
        logEvent("button_clicked", params)
    }
}