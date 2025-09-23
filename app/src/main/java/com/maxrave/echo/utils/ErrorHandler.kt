package iad1tya.echo.music.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Centralized error handling utility for the app
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Coroutine exception handler for global error handling
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.message}", throwable)
        handleError(throwable, "Coroutine")
    }
    
    /**
     * Safe coroutine scope with error handling
     */
    val safeCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExceptionHandler)
    
    /**
     * Handle errors with logging and optional recovery
     */
    fun handleError(throwable: Throwable, context: String = "Unknown") {
        try {
            Log.e(TAG, "Error in $context: ${throwable.message}", throwable)
            
            // Log additional context
            when (throwable) {
                is OutOfMemoryError -> {
                    Log.e(TAG, "OutOfMemoryError detected - triggering garbage collection")
                    System.gc()
                }
                is IllegalStateException -> {
                    Log.e(TAG, "IllegalStateException - possible lifecycle issue")
                }
                is NullPointerException -> {
                    Log.e(TAG, "NullPointerException - possible null reference")
                }
                is SecurityException -> {
                    Log.e(TAG, "SecurityException - permission or security issue")
                }
                else -> {
                    Log.e(TAG, "Unknown error type: ${throwable.javaClass.simpleName}")
                }
            }
            
            // You can add crash reporting here (Firebase Crashlytics, etc.)
            // FirebaseCrashlytics.getInstance().recordException(throwable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in error handler: ${e.message}", e)
        }
    }
    
    /**
     * Execute a block safely with error handling
     */
    inline fun <T> safeExecute(
        context: String = "Unknown",
        noinline onError: ((Throwable) -> Unit)? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleError(e, context)
            onError?.invoke(e)
            null
        }
    }
    
    /**
     * Execute a suspend block safely with error handling
     */
    suspend inline fun <T> safeExecuteSuspend(
        context: String = "Unknown",
        noinline onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleError(e, context)
            onError?.invoke(e)
            null
        }
    }
    
    /**
     * Launch a coroutine safely with error handling
     */
    fun safeLaunch(
        context: String = "Unknown",
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        safeCoroutineScope.launch {
            try {
                block()
            } catch (e: Exception) {
                handleError(e, context)
                onError?.invoke(e)
            }
        }
    }
    
    /**
     * Check if an error is recoverable
     */
    fun isRecoverableError(throwable: Throwable): Boolean {
        return when (throwable) {
            is OutOfMemoryError -> false
            is SecurityException -> false
            is IllegalStateException -> true
            is NullPointerException -> true
            is IllegalArgumentException -> true
            else -> true
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getUserFriendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is OutOfMemoryError -> "The app is running low on memory. Please close other apps and try again."
            is SecurityException -> "Permission denied. Please check app permissions."
            is IllegalStateException -> "Something went wrong. Please try again."
            is NullPointerException -> "An unexpected error occurred. Please restart the app."
            is IllegalArgumentException -> "Invalid input. Please check your data and try again."
            else -> "An unexpected error occurred. Please try again."
        }
    }
}

/**
 * Extension function for safe execution
 */
inline fun <T> T?.safeExecute(
    context: String = "Unknown",
    noinline onError: ((Throwable) -> Unit)? = null,
    block: T.() -> Unit
) {
    if (this != null) {
        ErrorHandler.safeExecute(context, onError) {
            block()
        }
    }
}
