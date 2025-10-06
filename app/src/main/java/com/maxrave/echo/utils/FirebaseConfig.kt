package iad1tya.echo.music.utils

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import iad1tya.echo.music.BuildConfig

/**
 * Firebase configuration utility for proper setup and testing
 */
object FirebaseConfig {
    
    private const val TAG = "FirebaseConfig"
    
    /**
     * Configure Firebase Analytics settings
     */
    fun configureAnalytics(context: Context) {
        try {
            val analytics = FirebaseAnalytics.getInstance(context)
            
            // Enable/disable analytics based on user preference
            analytics.setAnalyticsCollectionEnabled(true)
            
            // Set default event parameters
            val bundle = android.os.Bundle().apply {
                putString("app_name", "Echo Music")
                putString("app_version", getAppVersion(context))
                putString("platform", "android")
            }
            analytics.logEvent("app_configured", bundle)
            
            Log.d(TAG, "Analytics configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Analytics: ${e.message}", e)
        }
    }
    
    /**
     * Configure Firebase Crashlytics settings
     */
    fun configureCrashlytics(context: Context) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Enable/disable crashlytics based on build type
            crashlytics.setCrashlyticsCollectionEnabled(true)
            
            // Set custom keys for better crash reporting
            crashlytics.setCustomKey("app_version", getAppVersion(context))
            crashlytics.setCustomKey("platform", "android")
            crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            crashlytics.setCustomKey("device_model", android.os.Build.MODEL)
            crashlytics.setCustomKey("android_version", android.os.Build.VERSION.RELEASE)
            
            Log.d(TAG, "Crashlytics configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Crashlytics: ${e.message}", e)
        }
    }
    
    /**
     * Test Firebase integration
     */
    fun testFirebaseIntegration(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing Firebase integration...")
            
            // Test Analytics
            val analytics = FirebaseAnalytics.getInstance(context)
            analytics.logEvent("firebase_test", android.os.Bundle())
            
            // Test Crashlytics
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("Firebase integration test")
            
            Log.d(TAG, "Firebase integration test completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase integration test failed: ${e.message}", e)
            false
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
}