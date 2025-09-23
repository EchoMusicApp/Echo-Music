package iad1tya.echo.music.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * Utility class for memory optimization and monitoring
 */
class MemoryOptimizer private constructor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var memoryCheckJob: Job? = null
    private var isMonitoring = false
    
    companion object {
        @Volatile
        private var INSTANCE: MemoryOptimizer? = null
        
        fun getInstance(context: Context): MemoryOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryOptimizer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Start monitoring memory usage
     */
    fun startMemoryMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        memoryCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isMonitoring) {
                try {
                    checkMemoryUsage()
                    delay(10000) // Check every 10 seconds
                } catch (e: Exception) {
                    Log.e("MemoryOptimizer", "Error in memory monitoring: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Stop monitoring memory usage
     */
    fun stopMemoryMonitoring() {
        isMonitoring = false
        memoryCheckJob?.cancel()
        memoryCheckJob = null
    }
    
    /**
     * Check current memory usage and optimize if needed
     */
    private fun checkMemoryUsage() {
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
            val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
            val usedMemoryMB = totalMemoryMB - availableMemoryMB
            val memoryUsagePercent = (usedMemoryMB.toFloat() / totalMemoryMB.toFloat()) * 100
            
            Log.d("MemoryOptimizer", "Memory usage: ${usedMemoryMB}MB / ${totalMemoryMB}MB (${memoryUsagePercent.toInt()}%)")
            
            // Optimize memory if usage is high
            when {
                memoryUsagePercent > 85 -> {
                    Log.w("MemoryOptimizer", "High memory usage detected (${memoryUsagePercent.toInt()}%)")
                    optimizeMemory()
                }
                memoryUsagePercent > 70 -> {
                    Log.w("MemoryOptimizer", "Moderate memory usage (${memoryUsagePercent.toInt()}%)")
                    lightMemoryOptimization()
                }
                else -> {
                    Log.d("MemoryOptimizer", "Memory usage is normal (${memoryUsagePercent.toInt()}%)")
                }
            }
            
        } catch (e: Exception) {
            Log.e("MemoryOptimizer", "Error checking memory usage: ${e.message}")
        }
    }
    
    /**
     * Perform light memory optimization
     */
    private fun lightMemoryOptimization() {
        try {
            // Suggest garbage collection
            System.gc()
            Log.d("MemoryOptimizer", "Light memory optimization completed")
        } catch (e: Exception) {
            Log.e("MemoryOptimizer", "Error in light memory optimization: ${e.message}")
        }
    }
    
    /**
     * Perform aggressive memory optimization
     */
    private fun optimizeMemory() {
        try {
            // Force garbage collection multiple times
            repeat(3) {
                System.gc()
                Thread.sleep(100)
            }
            
            // Clear system caches if possible
            try {
                val runtime = Runtime.getRuntime()
                val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
                
                // Additional cleanup
                System.runFinalization()
                System.gc()
                
                val afterMemory = runtime.totalMemory() - runtime.freeMemory()
                val freedMemory = (beforeMemory - afterMemory) / (1024 * 1024)
                
                Log.d("MemoryOptimizer", "Memory optimization freed ${freedMemory}MB")
            } catch (e: Exception) {
                Log.e("MemoryOptimizer", "Error in advanced memory optimization: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e("MemoryOptimizer", "Error in memory optimization: ${e.message}")
        }
    }
    
    /**
     * Get current memory usage info
     */
    fun getMemoryInfo(): MemoryInfo {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
            val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
            val usedMemoryMB = totalMemoryMB - availableMemoryMB
            val memoryUsagePercent = (usedMemoryMB.toFloat() / totalMemoryMB.toFloat()) * 100
            
            MemoryInfo(
                totalMemoryMB = totalMemoryMB,
                usedMemoryMB = usedMemoryMB,
                availableMemoryMB = availableMemoryMB,
                memoryUsagePercent = memoryUsagePercent,
                isLowMemory = memoryInfo.lowMemory
            )
        } catch (e: Exception) {
            Log.e("MemoryOptimizer", "Error getting memory info: ${e.message}")
            MemoryInfo()
        }
    }
    
    /**
     * Check if device is low on memory
     */
    fun isLowMemory(): Boolean {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.lowMemory
        } catch (e: Exception) {
            Log.e("MemoryOptimizer", "Error checking low memory: ${e.message}")
            false
        }
    }
    
    /**
     * Force memory optimization
     */
    fun forceOptimization() {
        optimizeMemory()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMemoryMonitoring()
    }
}

/**
 * Data class for memory information
 */
data class MemoryInfo(
    val totalMemoryMB: Long = 0,
    val usedMemoryMB: Long = 0,
    val availableMemoryMB: Long = 0,
    val memoryUsagePercent: Float = 0f,
    val isLowMemory: Boolean = false
)

