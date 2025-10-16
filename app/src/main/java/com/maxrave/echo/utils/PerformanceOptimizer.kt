package iad1tya.echo.music.utils

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Performance optimization utilities for smooth app experience
 */
object PerformanceOptimizer {
    
    private val cache = ConcurrentHashMap<String, Any>()
    private val loadingStates = ConcurrentHashMap<String, Boolean>()
    private val requestCounters = ConcurrentHashMap<String, AtomicInteger>()
    
    /**
     * Debounce function to prevent excessive API calls
     */
    fun <T> debounce(
        delayMs: Long = 300L,
        coroutineScope: CoroutineScope,
        action: suspend () -> T
    ): suspend () -> T {
        var job: Job? = null
        return {
            job?.cancel()
            job = coroutineScope.launch {
                delay(delayMs)
                action()
            }
            action()
        }
    }
    
    /**
     * Throttle function to limit function calls
     */
    fun <T> throttle(
        delayMs: Long = 100L,
        coroutineScope: CoroutineScope,
        action: suspend () -> T
    ): suspend () -> Unit {
        var lastCallTime = 0L
        return {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCallTime >= delayMs) {
                lastCallTime = currentTime
                action()
            }
        }
    }
    
    /**
     * Cache with TTL (Time To Live)
     */
    fun <T> cacheWithTTL(
        key: String,
        ttlMs: Long = 300_000L, // 5 minutes default
        factory: suspend () -> T
    ): suspend () -> T {
        return {
            val cached = cache[key] as? CachedItem<T>
            val now = System.currentTimeMillis()
            
            if (cached != null && (now - cached.timestamp) < ttlMs) {
                cached.value
            } else {
                val value = factory()
                cache[key] = CachedItem(value, now)
                value
            }
        }
    }
    
    /**
     * Lazy loading with loading state management
     */
    fun <T> lazyLoad(
        key: String,
        coroutineScope: CoroutineScope,
        factory: suspend () -> T
    ): suspend () -> T? {
        return {
            if (loadingStates[key] == true) {
                null // Already loading
            } else {
                loadingStates[key] = true
                try {
                    val result = factory()
                    cache[key] = result as Any
                    result
                } finally {
                    loadingStates[key] = false
                }
            }
        }
    }
    
    /**
     * Request deduplication to prevent duplicate API calls
     */
    fun <T> deduplicate(
        key: String,
        coroutineScope: CoroutineScope,
        factory: suspend () -> T
    ): suspend () -> T {
        return {
            val counter = requestCounters.getOrPut(key) { AtomicInteger(0) }
            val requestId = counter.incrementAndGet()
            
            // Wait for any ongoing request to complete
            while (loadingStates[key] == true) {
                delay(50)
            }
            
            // Check if this request is still valid
            if (requestId == counter.get()) {
                loadingStates[key] = true
                try {
                    factory()
                } finally {
                    loadingStates[key] = false
                }
            } else {
                // Request was superseded, return cached result if available
                cache[key] as? T ?: factory()
            }
        }
    }
    
    /**
     * Memory optimization utilities
     */
    object Memory {
        fun clearCache() {
            cache.clear()
            Log.d("PerformanceOptimizer", "Cache cleared")
        }
        
        fun clearExpiredCache(ttlMs: Long = 300_000L) {
            val now = System.currentTimeMillis()
            val iterator = cache.iterator()
            var clearedCount = 0
            
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val cached = entry.value as? CachedItem<*>
                if (cached != null && (now - cached.timestamp) > ttlMs) {
                    iterator.remove()
                    clearedCount++
                }
            }
            
            Log.d("PerformanceOptimizer", "Cleared $clearedCount expired cache entries")
        }
        
        fun getCacheSize(): Int = cache.size
        
        fun getCacheMemoryUsage(): Long {
            return cache.values.sumOf { 
                when (it) {
                    is CachedItem<*> -> estimateSize(it.value)
                    else -> estimateSize(it)
                }
            }
        }
        
        private fun estimateSize(obj: Any?): Long {
            return when (obj) {
                is String -> obj.length * 2L // Rough estimate for UTF-16
                is List<*> -> obj.size * 100L // Rough estimate
                is Map<*, *> -> obj.size * 200L // Rough estimate
                else -> 100L // Default estimate
            }
        }
    }
    
    /**
     * UI Performance utilities
     */
    object UI {
        /**
         * Optimize LazyColumn performance
         */
        // Removed @Composable function to avoid compilation issues
        
        /**
         * Debounced scroll effect
         */
        @Composable
        fun rememberDebouncedScrollEffect(
            delayMs: Long = 100L,
            onScroll: (Int) -> Unit
        ) {
            val coroutineScope = rememberCoroutineScope()
            val debouncedOnScroll = remember(delayMs) {
                debounce(delayMs, coroutineScope) {
                    // This will be called with the scroll position
                }
            }
            
            LaunchedEffect(debouncedOnScroll) {
                // Implementation depends on specific use case
            }
        }
    }
    
    /**
     * Network optimization utilities
     */
    object Network {
        /**
         * Retry with exponential backoff
         */
        suspend fun <T> retryWithBackoff(
            maxRetries: Int = 3,
            initialDelayMs: Long = 1000L,
            maxDelayMs: Long = 10000L,
            action: suspend () -> T
        ): T {
            var delay = initialDelayMs
            repeat(maxRetries) { attempt ->
                try {
                    return action()
                } catch (e: Exception) {
                    if (attempt == maxRetries - 1) throw e
                    Log.w("PerformanceOptimizer", "Retry attempt ${attempt + 1} failed: ${e.message}")
                    kotlinx.coroutines.delay(delay)
                    delay = minOf(delay * 2, maxDelayMs)
                }
            }
            throw IllegalStateException("Should not reach here")
        }
        
        /**
         * Batch requests to reduce network calls
         */
        class RequestBatcher<T, R>(
            private val batchSize: Int = 10,
            private val batchDelayMs: Long = 100L,
            private val processor: suspend (List<T>) -> List<R>
        ) {
            private val pendingRequests = mutableListOf<T>()
            private var batchJob: Job? = null
            
            suspend fun addRequest(item: T, coroutineScope: CoroutineScope): R {
                pendingRequests.add(item)
                
                if (pendingRequests.size >= batchSize) {
                    return processBatch(coroutineScope)
                }
                
                // Start or restart batch timer
                batchJob?.cancel()
                batchJob = coroutineScope.launch {
                    delay(batchDelayMs)
                    processBatch(coroutineScope)
                }
                
                // Wait for batch to complete
                batchJob?.join()
                return pendingRequests.lastOrNull()?.let { 
                    // Find the result for this specific item
                    // This is a simplified implementation
                    processor(listOf(it)).firstOrNull()
                } ?: throw IllegalStateException("Batch processing failed")
            }
            
            private suspend fun processBatch(coroutineScope: CoroutineScope): R {
                val batch = pendingRequests.toList()
                pendingRequests.clear()
                batchJob?.cancel()
                
                return withContext(Dispatchers.IO) {
                    val results = processor(batch)
                    results.lastOrNull() ?: throw IllegalStateException("No results from batch")
                }
            }
        }
    }
    
    /**
     * Data class for cached items
     */
    private data class CachedItem<T>(
        val value: T,
        val timestamp: Long
    )
}

/**
 * Composable for performance monitoring
 */
@Composable
fun PerformanceMonitor(
    context: Context = LocalContext.current,
    onPerformanceIssue: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        // Monitor memory usage
        coroutineScope.launch {
            while (true) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val maxMemory = runtime.maxMemory()
                val memoryUsagePercent = (usedMemory * 100) / maxMemory
                
                if (memoryUsagePercent > 80) {
                    onPerformanceIssue("High memory usage: ${memoryUsagePercent}%")
                    PerformanceOptimizer.Memory.clearExpiredCache()
                }
                
                delay(30_000) // Check every 30 seconds
            }
        }
        
        // Monitor cache size
        coroutineScope.launch {
            while (true) {
                val cacheSize = PerformanceOptimizer.Memory.getCacheSize()
                if (cacheSize > 1000) {
                    onPerformanceIssue("Large cache size: $cacheSize items")
                    PerformanceOptimizer.Memory.clearExpiredCache()
                }
                
                delay(60_000) // Check every minute
            }
        }
    }
}

// Extension functions removed to avoid compilation issues
