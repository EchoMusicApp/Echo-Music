package iad1tya.echo.music.utils

import android.content.Context
import android.util.Log
import java.lang.Thread.UncaughtExceptionHandler
import iad1tya.echo.music.data.dataStore.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Custom UncaughtExceptionHandler that logs crashes to internal storage
 * This extends the existing crash handling system
 */
class CrashLoggingHandler(
    private val context: Context,
    private val defaultHandler: UncaughtExceptionHandler?
) : UncaughtExceptionHandler, KoinComponent {
    
    private val tag = "CrashLoggingHandler"
    private val dataStoreManager: DataStoreManager by inject()
    
    private fun isCrashReportEnabled(): Boolean {
        return try {
            runBlocking {
                dataStoreManager.crashReportEnabled.first()
            }
        } catch (e: Exception) {
            true // Default to enabled if there's an error
        }
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(tag, "Uncaught exception detected in thread: ${thread.name}")
            
            // Log crash to internal storage
            val additionalInfo = buildString {
                appendLine("Thread: ${thread.name}")
                appendLine("Thread ID: ${thread.threadId()}")
                appendLine("Thread State: ${thread.state}")
                appendLine("Thread Priority: ${thread.priority}")
                appendLine("Is Daemon: ${thread.isDaemon}")
                appendLine("Is Alive: ${thread.isAlive}")
            }
            
            // Log crash to internal storage
            CrashLogger.logCrash(context, throwable, additionalInfo)
            
            // Log crash analytics only if crash reporting is enabled
            if (isCrashReportEnabled()) {
                try {
                    AnalyticsHelper.logUncaughtException(throwable, thread.name, additionalInfo)
                    AnalyticsHelper.logCrash(
                        crashType = throwable.javaClass.simpleName,
                        crashMessage = throwable.message ?: "No message",
                        stackTrace = throwable.stackTraceToString(),
                        additionalInfo = additionalInfo
                    )
                } catch (analyticsException: Exception) {
                    Log.e(tag, "Failed to log crash analytics: ${analyticsException.message}", analyticsException)
                }
            } else {
                Log.d(tag, "Crash reporting disabled, skipping analytics")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error in crash logging handler: ${e.message}", e)
        } finally {
            // Always call the default handler to maintain existing behavior
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
