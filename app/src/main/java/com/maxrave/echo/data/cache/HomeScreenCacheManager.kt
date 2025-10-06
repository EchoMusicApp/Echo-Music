package iad1tya.echo.music.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Simple cache manager for home screen data to improve loading performance
 * Uses file-based caching without serialization
 */
class HomeScreenCacheManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HomeScreenCache"
        private const val CACHE_DIR_NAME = "home_screen_cache"
        private const val CACHE_EXPIRY_HOURS = 24L // Cache expires after 24 hours
        private const val CACHE_REFRESH_THRESHOLD_HOURS = 6L // Refresh cache if older than 6 hours
        
        // Cache file names
        private const val HOME_DATA_CACHE = "home_data.txt"
        private const val CHART_DATA_CACHE = "chart_data.txt"
        private const val NEW_RELEASE_CACHE = "new_release.txt"
        private const val MOOD_DATA_CACHE = "mood_data.txt"
        private const val RECENTLY_PLAYED_CACHE = "recently_played.txt"
        private const val CACHE_METADATA = "cache_metadata.txt"
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
    
    init {
        // Ensure cache directory exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    data class CacheMetadata(
        val timestamp: Long,
        val version: Int = 1
    )
    
    /**
     * Check if cache is valid (not expired)
     */
    private fun isCacheValid(metadata: CacheMetadata): Boolean {
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - metadata.timestamp
        val expiryTime = TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS)
        return cacheAge < expiryTime
    }
    
    /**
     * Check if cache needs refresh (older than threshold)
     */
    private fun shouldRefreshCache(metadata: CacheMetadata): Boolean {
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - metadata.timestamp
        val refreshThreshold = TimeUnit.HOURS.toMillis(CACHE_REFRESH_THRESHOLD_HOURS)
        return cacheAge > refreshThreshold
    }
    
    /**
     * Save cache metadata
     */
    private suspend fun saveCacheMetadata(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val metadata = CacheMetadata(timestamp = System.currentTimeMillis())
                val file = File(cacheDir, fileName)
                file.writeText("${metadata.timestamp},${metadata.version}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save cache metadata for $fileName", e)
            }
        }
    }
    
    /**
     * Load cache metadata
     */
    private suspend fun loadCacheMetadata(fileName: String): CacheMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, fileName)
                if (!file.exists()) return@withContext null
                
                val content = file.readText()
                val parts = content.split(",")
                if (parts.size >= 2) {
                    CacheMetadata(
                        timestamp = parts[0].toLong(),
                        version = parts[1].toInt()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cache metadata for $fileName", e)
                null
            }
        }
    }
    
    /**
     * Check if cache file exists and is valid
     */
    suspend fun isCacheValid(fileName: String): Boolean {
        val metadata = loadCacheMetadata(fileName)
        return metadata != null && isCacheValid(metadata)
    }
    
    /**
     * Check if cache needs refresh
     */
    suspend fun shouldRefreshCache(fileName: String): Boolean {
        val metadata = loadCacheMetadata(fileName)
        return metadata == null || shouldRefreshCache(metadata)
    }
    
    /**
     * Save data to cache (simplified - just mark as cached)
     */
    suspend fun markAsCached(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                saveCacheMetadata(fileName)
                Log.d(TAG, "Marked $fileName as cached")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark $fileName as cached", e)
            }
        }
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
                Log.d(TAG, "All cache cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
            }
        }
    }
    
    /**
     * Get cache size in bytes
     */
    suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.sumOf { file ->
                    if (file.isFile) file.length() else 0L
                } ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get cache size", e)
                0L
            }
        }
    }
}
