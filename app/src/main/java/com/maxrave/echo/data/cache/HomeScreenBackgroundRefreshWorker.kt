package iad1tya.echo.music.data.cache

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for background refresh of home screen data
 */
class HomeScreenBackgroundRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "BackgroundRefreshWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Background refresh worker started")
            
            // Get the background refresh manager from the application
            val application = applicationContext as? android.app.Application
            if (application == null) {
                Log.e(TAG, "Application context is null")
                return@withContext Result.failure()
            }
            
            // We need to get the background refresh manager instance
            // This would typically be injected or accessed through a singleton
            // For now, we'll create a new instance
            val cacheManager = HomeScreenCacheManager(application)
            val backgroundRefreshManager = HomeScreenBackgroundRefreshManager(application, cacheManager)
            
            // Perform the background refresh
            backgroundRefreshManager.performBackgroundRefresh()
            
            Log.d(TAG, "Background refresh worker completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Background refresh worker failed", e)
            Result.failure()
        }
    }
}

