package echo.music.iad1tya.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AiRecommendationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AiRecommendationHelper.generateRecommendations(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
