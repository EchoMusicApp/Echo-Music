package iad1tya.echo.music.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance monitoring utility for tracking app performance metrics
 */
class PerformanceMonitor private constructor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    companion object {
        @Volatile
        private var INSTANCE: PerformanceMonitor? = null
        
        fun getInstance(context: Context): PerformanceMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isMonitoring) {
                try {
                    collectPerformanceMetrics()
                    delay(5000) // Collect metrics every 5 seconds
                } catch (e: Exception) {
                    Log.e("PerformanceMonitor", "Error collecting metrics: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Collect current performance metrics
     */
    private fun collectPerformanceMetrics() {
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
            
            val metric = PerformanceMetric(
                timestamp = System.currentTimeMillis(),
                totalMemoryMB = totalMemory / (1024 * 1024),
                usedMemoryMB = usedMemory / (1024 * 1024),
                freeMemoryMB = freeMemory / (1024 * 1024),
                maxMemoryMB = maxMemory / (1024 * 1024),
                memoryUsagePercent = memoryUsagePercent,
                isLowMemory = memoryInfo.lowMemory,
                availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
            )
            
            performanceMetrics["current"] = metric
            
            // Log performance warnings
            when {
                memoryUsagePercent > 90 -> {
                    Log.w("PerformanceMonitor", "CRITICAL: Memory usage at ${memoryUsagePercent.toInt()}%")
                }
                memoryUsagePercent > 80 -> {
                    Log.w("PerformanceMonitor", "WARNING: High memory usage at ${memoryUsagePercent.toInt()}%")
                }
                memoryUsagePercent > 70 -> {
                    Log.i("PerformanceMonitor", "INFO: Moderate memory usage at ${memoryUsagePercent.toInt()}%")
                }
            }
            
        } catch (e: Exception) {
            Log.e("PerformanceMonitor", "Error collecting performance metrics: ${e.message}")
        }
    }
    
    /**
     * Get current performance metrics
     */
    fun getCurrentMetrics(): PerformanceMetric? {
        return performanceMetrics["current"]
    }
    
    /**
     * Check if performance is degraded
     */
    fun isPerformanceDegraded(): Boolean {
        val current = getCurrentMetrics()
        return current?.let { 
            it.memoryUsagePercent > 80 || it.isLowMemory 
        } ?: false
    }
    
    /**
     * Get performance recommendations
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val current = getCurrentMetrics()
        
        current?.let { metric ->
            when {
                metric.memoryUsagePercent > 90 -> {
                    recommendations.add("Critical memory usage - consider closing other apps")
                    recommendations.add("Restart the app to free memory")
                }
                metric.memoryUsagePercent > 80 -> {
                    recommendations.add("High memory usage - optimize animations")
                    recommendations.add("Clear app cache")
                }
                metric.memoryUsagePercent > 70 -> {
                    recommendations.add("Moderate memory usage - monitor performance")
                }
            }
            
            if (metric.isLowMemory) {
                recommendations.add("Device is low on memory")
            }
        }
        
        return recommendations
    }
    
    /**
     * Log performance summary
     */
    fun logPerformanceSummary() {
        val current = getCurrentMetrics()
        current?.let { metric ->
            Log.i("PerformanceMonitor", """
                Performance Summary:
                - Memory Usage: ${metric.usedMemoryMB}MB / ${metric.maxMemoryMB}MB (${metric.memoryUsagePercent.toInt()}%)
                - Available Memory: ${metric.availableMemoryMB}MB
                - Low Memory: ${metric.isLowMemory}
                - Performance Status: ${if (isPerformanceDegraded()) "DEGRADED" else "NORMAL"}
            """.trimIndent())
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
        performanceMetrics.clear()
    }
}

/**
 * Data class for performance metrics
 */
data class PerformanceMetric(
    val timestamp: Long,
    val totalMemoryMB: Long,
    val usedMemoryMB: Long,
    val freeMemoryMB: Long,
    val maxMemoryMB: Long,
    val memoryUsagePercent: Float,
    val isLowMemory: Boolean,
    val availableMemoryMB: Long
)

