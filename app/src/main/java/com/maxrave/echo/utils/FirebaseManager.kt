package iad1tya.echo.music.utils

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Comprehensive Firebase initialization and configuration utility
 * Handles Analytics and Crashlytics
 */
object FirebaseManager {
    
    private const val TAG = "FirebaseManager"
    private var isInitialized = false
    
    // Firebase service instances
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    
    /**
     * Initialize all Firebase services
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Firebase already initialized")
            return
        }
        
        try {
            Log.d(TAG, "Starting Firebase initialization...")
            
            // Initialize Analytics
            initializeAnalytics(context)
            
            // Initialize Crashlytics
            initializeCrashlytics(context)
            
            isInitialized = true
            Log.d(TAG, "Firebase initialization completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
            // Don't throw exception to prevent app crash
        }
    }
    
    private fun initializeAnalytics(context: Context) {
        try {
            analytics = Firebase.analytics
            analytics?.setAnalyticsCollectionEnabled(true)
            
            // Set user properties for better analytics
            analytics?.setUserProperty("app_version", getAppVersion(context))
            analytics?.setUserProperty("platform", "android")
            analytics?.setUserProperty("build_type", getBuildType())
            
            Log.d(TAG, "Analytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Analytics initialization failed: ${e.message}", e)
        }
    }
    
    private fun initializeCrashlytics(context: Context) {
        try {
            crashlytics = Firebase.crashlytics
            crashlytics?.setCrashlyticsCollectionEnabled(true)
            
            // Set custom keys for better crash reporting
            crashlytics?.setCustomKey("app_version", getAppVersion(context))
            crashlytics?.setCustomKey("platform", "android")
            crashlytics?.setCustomKey("build_type", getBuildType())
            crashlytics?.setCustomKey("device_model", getDeviceModel())
            
            Log.d(TAG, "Crashlytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Crashlytics initialization failed: ${e.message}", e)
        }
    }
    
    
    /**
     * Get Analytics instance
     */
    fun getAnalytics(): FirebaseAnalytics? = analytics
    
    /**
     * Get Crashlytics instance
     */
    fun getCrashlytics(): FirebaseCrashlytics? = crashlytics
    
    /**
     * Check if Firebase is properly initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Set user ID for both Analytics and Crashlytics
     */
    fun setUserId(userId: String) {
        try {
            analytics?.setUserId(userId)
            crashlytics?.setUserId(userId)
            Log.d(TAG, "User ID set: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID: ${e.message}", e)
        }
    }
    
    /**
     * Set user properties for Analytics
     */
    fun setUserProperty(name: String, value: String) {
        try {
            analytics?.setUserProperty(name, value)
            Log.d(TAG, "User property set: $name = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property: ${e.message}", e)
        }
    }
    
    /**
     * Set custom key for Crashlytics
     */
    fun setCrashlyticsCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Crashlytics custom key: ${e.message}", e)
        }
    }
    
    /**
     * Set custom key for Crashlytics (Long)
     */
    fun setCrashlyticsCustomKey(key: String, value: Long) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Crashlytics custom key: ${e.message}", e)
        }
    }
    
    /**
     * Set custom key for Crashlytics (Boolean)
     */
    fun setCrashlyticsCustomKey(key: String, value: Boolean) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Crashlytics custom key: ${e.message}", e)
        }
    }
    
    /**
     * Record exception in Crashlytics
     */
    fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception: ${e.message}", e)
        }
    }
    
    /**
     * Log message in Crashlytics
     */
    fun logCrashlytics(message: String) {
        try {
            crashlytics?.log(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log to Crashlytics: ${e.message}", e)
        }
    }
    
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getBuildType(): String {
        return try {
            // This would need to be passed from build config
            "release" // or "debug"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceModel(): String {
        return try {
            android.os.Build.MODEL ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
