package iad1tya.echo.music.utils

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Firebase integration testing utility
 * Provides methods to test and verify Firebase services are working correctly
 */
object FirebaseTestUtils {
    
    private const val TAG = "FirebaseTestUtils"
    
    /**
     * Test all Firebase services
     */
    fun testFirebaseIntegration(context: Context) {
        Log.d(TAG, "Starting Firebase integration tests...")
        
        testAnalytics(context)
        testCrashlytics(context)
        
        Log.d(TAG, "Firebase integration tests completed")
    }
    
    /**
     * Test Firebase Analytics
     */
    private fun testAnalytics(context: Context) {
        try {
            val analytics = FirebaseManager.getAnalytics()
            if (analytics != null) {
                // Test basic analytics event
                analytics.logEvent("firebase_test_analytics", null)
                Log.d(TAG, "Analytics test: PASSED")
            } else {
                Log.e(TAG, "Analytics test: FAILED - Analytics instance is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analytics test: FAILED - ${e.message}", e)
        }
    }
    
    /**
     * Test Firebase Crashlytics
     */
    private fun testCrashlytics(context: Context) {
        try {
            val crashlytics = FirebaseManager.getCrashlytics()
            if (crashlytics != null) {
                // Test custom key
                crashlytics.setCustomKey("test_key", "test_value")
                
                // Test logging
                crashlytics.log("Firebase Crashlytics test log")
                
                // Test non-fatal exception
                try {
                    throw RuntimeException("Test exception for Crashlytics")
                } catch (e: Exception) {
                    crashlytics.recordException(e)
                }
                
                Log.d(TAG, "Crashlytics test: PASSED")
            } else {
                Log.e(TAG, "Crashlytics test: FAILED - Crashlytics instance is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crashlytics test: FAILED - ${e.message}", e)
        }
    }
    
    
    /**
     * Test Firebase initialization status
     */
    fun testFirebaseInitialization(): Boolean {
        val isInitialized = FirebaseManager.isInitialized()
        Log.d(TAG, "Firebase initialization status: $isInitialized")
        return isInitialized
    }
    
    /**
     * Test Firebase service instances
     */
    fun testFirebaseInstances(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
        results["Analytics"] = FirebaseManager.getAnalytics() != null
        results["Crashlytics"] = FirebaseManager.getCrashlytics() != null
        
        results.forEach { (service, isAvailable) ->
            Log.d(TAG, "Firebase $service: ${if (isAvailable) "AVAILABLE" else "NOT AVAILABLE"}")
        }
        
        return results
    }
    
    /**
     * Test Firebase configuration
     */
    fun testFirebaseConfiguration(context: Context): Map<String, String> {
        val config = mutableMapOf<String, String>()
        
        try {
            // Test app version
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            config["app_version"] = packageInfo.versionName ?: "unknown"
            
            // Test package name
            config["package_name"] = context.packageName
            
            // Test Firebase initialization
            config["firebase_initialized"] = FirebaseManager.isInitialized().toString()
            
            Log.d(TAG, "Firebase configuration test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase configuration test failed: ${e.message}", e)
            config["error"] = e.message ?: "Unknown error"
        }
        
        return config
    }
    
    /**
     * Generate Firebase test report
     */
    fun generateFirebaseTestReport(context: Context): String {
        val report = StringBuilder()
        
        report.appendLine("=== Firebase Integration Test Report ===")
        report.appendLine("Timestamp: ${System.currentTimeMillis()}")
        report.appendLine()
        
        // Test initialization
        report.appendLine("1. Firebase Initialization:")
        report.appendLine("   Status: ${if (FirebaseManager.isInitialized()) "PASSED" else "FAILED"}")
        report.appendLine()
        
        // Test instances
        report.appendLine("2. Firebase Service Instances:")
        val instances = testFirebaseInstances()
        instances.forEach { (service, isAvailable) ->
            report.appendLine("   $service: ${if (isAvailable) "AVAILABLE" else "NOT AVAILABLE"}")
        }
        report.appendLine()
        
        // Test configuration
        report.appendLine("3. Firebase Configuration:")
        val config = testFirebaseConfiguration(context)
        config.forEach { (key, value) ->
            report.appendLine("   $key: $value")
        }
        report.appendLine()
        
        report.appendLine("=== End of Report ===")
        
        val reportString = report.toString()
        Log.d(TAG, "Firebase test report generated:\n$reportString")
        
        return reportString
    }
}
