package iad1tya.echo.music.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages animation performance based on device capabilities and memory usage
 */
class AnimationPerformanceManager private constructor(private val context: Context) {
    
    private val _isLowMemoryMode = MutableStateFlow(false)
    val isLowMemoryMode: StateFlow<Boolean> = _isLowMemoryMode.asStateFlow()
    
    private val _isLowEndDevice = MutableStateFlow(false)
    val isLowEndDevice: StateFlow<Boolean> = _isLowEndDevice.asStateFlow()
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    companion object {
        @Volatile
        private var INSTANCE: AnimationPerformanceManager? = null
        
        fun getInstance(context: Context): AnimationPerformanceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnimationPerformanceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        checkDeviceCapabilities()
        startMemoryMonitoring()
    }
    
    private fun checkDeviceCapabilities() {
        try {
            // Check if device is low-end based on memory
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
            val isLowEnd = totalMemoryMB < 2048 // Less than 2GB RAM
            
            _isLowEndDevice.value = isLowEnd
            
            Log.d("AnimationManager", "Device memory: ${totalMemoryMB}MB, Low-end: $isLowEnd")
        } catch (e: Exception) {
            Log.e("AnimationManager", "Error checking device capabilities: ${e.message}")
            _isLowEndDevice.value = true // Assume low-end on error
        }
    }
    
    private fun startMemoryMonitoring() {
        // Monitor memory usage periodically
        android.os.Handler(android.os.Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                try {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    
                    val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
                    val isLowMemory = availableMemoryMB < 200 // Less than 200MB available
                    
                    _isLowMemoryMode.value = isLowMemory
                    
                    if (isLowMemory) {
                        Log.w("AnimationManager", "Low memory detected: ${availableMemoryMB}MB available")
                    }
                } catch (e: Exception) {
                    Log.e("AnimationManager", "Error monitoring memory: ${e.message}")
                }
                
                // Check again in 5 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 5000)
            }
        })
    }
    
    /**
     * Get optimized animation duration based on device capabilities
     */
    fun getOptimizedDuration(baseDuration: Int): Int {
        return when {
            _isLowMemoryMode.value -> baseDuration / 2 // Reduce duration in low memory
            _isLowEndDevice.value -> (baseDuration * 0.75).toInt() // Slightly reduce for low-end devices
            else -> baseDuration
        }
    }
    
    /**
     * Get optimized animation spec for smooth performance
     */
    fun getOptimizedAnimationSpec(baseDuration: Int): AnimationSpec<Float> {
        val optimizedDuration = getOptimizedDuration(baseDuration)
        return tween(
            durationMillis = optimizedDuration,
            easing = if (_isLowMemoryMode.value) LinearEasing else FastOutSlowInEasing
        )
    }
    
    /**
     * Check if complex animations should be disabled
     */
    fun shouldDisableComplexAnimations(): Boolean {
        return _isLowMemoryMode.value || _isLowEndDevice.value
    }
    
    /**
     * Get recommended animation complexity level
     */
    fun getAnimationComplexityLevel(): AnimationComplexity {
        return when {
            _isLowMemoryMode.value -> AnimationComplexity.MINIMAL
            _isLowEndDevice.value -> AnimationComplexity.SIMPLE
            else -> AnimationComplexity.FULL
        }
    }
    
    /**
     * Force garbage collection when memory is low
     */
    fun optimizeMemoryIfNeeded() {
        if (_isLowMemoryMode.value) {
            System.gc()
            Log.d("AnimationManager", "Memory optimization triggered")
        }
    }
}

enum class AnimationComplexity {
    MINIMAL,    // Only essential animations
    SIMPLE,     // Basic animations without complex effects
    FULL        // All animations enabled
}

