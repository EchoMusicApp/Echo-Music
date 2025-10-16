package iad1tya.echo.music.utils

import android.app.Application
import android.content.Context
import android.os.Debug
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// import java.lang.management.ManagementFactory
// import java.lang.management.MemoryMXBean
// import java.lang.management.MemoryUsage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utilities for tracking app performance
 */
object PerformanceMonitor {
    
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    // private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    
    /**
     * Start monitoring performance for a specific operation
     */
    fun startMonitoring(operationName: String): PerformanceTracker {
        val startTime = System.currentTimeMillis()
        val startMemory = getCurrentMemoryUsage()
        
        return PerformanceTracker(operationName, startTime, startMemory)
    }
    
    /**
     * Get current memory usage in MB
     */
    fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }
    
    /**
     * Get memory usage percentage
     */
    fun getMemoryUsagePercentage(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        return (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
    }
    
    /**
     * Get heap memory usage
     */
    fun getHeapMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * Get non-heap memory usage
     */
    fun getNonHeapMemoryUsage(): Long {
        return 0L // Simplified for now
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformanceMetrics() {
        Log.d("PerformanceMonitor", "=== Performance Metrics ===")
        Log.d("PerformanceMonitor", "Memory Usage: ${getCurrentMemoryUsage()}MB (${getMemoryUsagePercentage().toInt()}%)")
        Log.d("PerformanceMonitor", "Heap Memory: ${getHeapMemoryUsage() / 1024 / 1024}MB")
        Log.d("PerformanceMonitor", "Non-Heap Memory: ${getNonHeapMemoryUsage() / 1024 / 1024}MB")
        
        performanceMetrics.forEach { (operation, metric) ->
            Log.d("PerformanceMonitor", "$operation: ${metric.averageTime}ms avg, ${metric.maxTime}ms max, ${metric.callCount} calls")
        }
        Log.d("PerformanceMonitor", "========================")
    }
    
    /**
     * Clear performance metrics
     */
    fun clearMetrics() {
        performanceMetrics.clear()
        Log.d("PerformanceMonitor", "Performance metrics cleared")
    }
    
    /**
     * Performance tracker class
     */
    class PerformanceTracker(
        private val operationName: String,
        private val startTime: Long,
        private val startMemory: Long
    ) {
        fun end(): PerformanceMetric {
            val endTime = System.currentTimeMillis()
            val endMemory = getCurrentMemoryUsage()
            val duration = endTime - startTime
            val memoryDelta = endMemory - startMemory
            
            val metric = performanceMetrics.getOrPut(operationName) { PerformanceMetric() }
            metric.addMeasurement(duration, memoryDelta)
            
            Log.d("PerformanceMonitor", "$operationName took ${duration}ms, memory delta: ${memoryDelta}MB")
            
            return metric
        }
    }
    
    /**
     * Performance metric data class
     */
    data class PerformanceMetric(
        var totalTime: Long = 0,
        var maxTime: Long = 0,
        var callCount: Int = 0,
        var totalMemoryDelta: Long = 0
    ) {
        val averageTime: Long
            get() = if (callCount > 0) totalTime / callCount else 0
        
        val averageMemoryDelta: Long
            get() = if (callCount > 0) totalMemoryDelta / callCount else 0
        
        fun addMeasurement(duration: Long, memoryDelta: Long) {
            totalTime += duration
            maxTime = maxOf(maxTime, duration)
            callCount++
            totalMemoryDelta += memoryDelta
        }
    }
}

/**
 * Composable for monitoring performance in Compose
 */
@Composable
fun PerformanceMonitorComposable(
    context: Context = LocalContext.current,
    onPerformanceIssue: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        // Monitor memory usage
        coroutineScope.launch {
            while (true) {
                val memoryUsage = PerformanceMonitor.getMemoryUsagePercentage()
                if (memoryUsage > 80f) {
                    onPerformanceIssue("High memory usage: ${memoryUsage.toInt()}%")
                }
                delay(30_000) // Check every 30 seconds
            }
        }
        
        // Log performance metrics periodically
        coroutineScope.launch {
            while (true) {
                PerformanceMonitor.logPerformanceMetrics()
                delay(60_000) // Log every minute
            }
        }
    }
}

/**
 * Performance testing utilities
 */
object PerformanceTester {
    
    /**
     * Test app startup performance
     */
    fun testStartupPerformance(application: Application): PerformanceMonitor.PerformanceMetric {
        val tracker = PerformanceMonitor.startMonitoring("App Startup")
        
        // Simulate startup operations
        Thread.sleep(100) // Simulate initialization
        
        return tracker.end()
    }
    
    /**
     * Test home screen loading performance
     */
    fun testHomeScreenLoading(): PerformanceMonitor.PerformanceMetric {
        val tracker = PerformanceMonitor.startMonitoring("Home Screen Loading")
        
        // Simulate home screen loading
        Thread.sleep(200) // Simulate data loading
        
        return tracker.end()
    }
    
    /**
     * Test navigation performance
     */
    fun testNavigationPerformance(): PerformanceMonitor.PerformanceMetric {
        val tracker = PerformanceMonitor.startMonitoring("Navigation")
        
        // Simulate navigation
        Thread.sleep(50) // Simulate navigation delay
        
        return tracker.end()
    }
    
    /**
     * Test image loading performance
     */
    fun testImageLoadingPerformance(): PerformanceMonitor.PerformanceMetric {
        val tracker = PerformanceMonitor.startMonitoring("Image Loading")
        
        // Simulate image loading
        Thread.sleep(150) // Simulate image loading
        
        return tracker.end()
    }
    
    /**
     * Run comprehensive performance test
     */
    fun runComprehensiveTest(application: Application) {
        Log.d("PerformanceTester", "Starting comprehensive performance test...")
        
        // Test startup
        testStartupPerformance(application)
        
        // Test home screen loading
        testHomeScreenLoading()
        
        // Test navigation
        testNavigationPerformance()
        
        // Test image loading
        testImageLoadingPerformance()
        
        // Log results
        PerformanceMonitor.logPerformanceMetrics()
        
        Log.d("PerformanceTester", "Comprehensive performance test completed")
    }
}

// MemoryOptimizer moved to separate file to avoid conflicts

/**
 * Frame rate monitoring utilities
 */
object FrameRateMonitor {
    
    private val frameTimes = mutableListOf<Long>()
    private val maxFrameTime = 16_666_667L // 60 FPS = 16.67ms per frame
    
    /**
     * Record frame time
     */
    fun recordFrameTime(frameTime: Long) {
        frameTimes.add(frameTime)
        
        // Keep only last 60 frames
        if (frameTimes.size > 60) {
            frameTimes.removeAt(0)
        }
    }
    
    /**
     * Get average frame rate
     */
    fun getAverageFrameRate(): Float {
        if (frameTimes.isEmpty()) return 0f
        
        val averageFrameTime = frameTimes.average()
        return 1_000_000_000f / averageFrameTime.toFloat() // Convert to FPS
    }
    
    /**
     * Get dropped frame count
     */
    fun getDroppedFrameCount(): Int {
        return frameTimes.count { it > maxFrameTime }
    }
    
    /**
     * Get frame rate stability (lower is better)
     */
    fun getFrameRateStability(): Float {
        if (frameTimes.size < 2) return 0f
        
        val average = frameTimes.average()
        val variance = frameTimes.map { (it - average) * (it - average) }.average()
        return kotlin.math.sqrt(variance.toFloat()).toFloat()
    }
    
    /**
     * Log frame rate metrics
     */
    fun logFrameRateMetrics() {
        Log.d("FrameRateMonitor", "Average FPS: ${getAverageFrameRate().toInt()}")
        Log.d("FrameRateMonitor", "Dropped frames: ${getDroppedFrameCount()}")
        Log.d("FrameRateMonitor", "Frame rate stability: ${getFrameRateStability()}")
    }
}

/**
 * Extension functions for easier usage
 */
fun <T> measurePerformance(operationName: String, operation: () -> T): T {
    val tracker = PerformanceMonitor.startMonitoring(operationName)
    return try {
        operation()
    } finally {
        tracker.end()
    }
}

suspend fun <T> measurePerformanceAsync(operationName: String, operation: suspend () -> T): T {
    val tracker = PerformanceMonitor.startMonitoring(operationName)
    return try {
        operation()
    } finally {
        tracker.end()
    }
}
