package iad1tya.echo.music

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.UnstableApi
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import cat.ereza.customactivityoncrash.config.CaocConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.util.DebugLogger
import iad1tya.echo.music.di.databaseModule
import iad1tya.echo.music.di.mediaServiceModule
import iad1tya.echo.music.di.viewModelModule
import iad1tya.echo.music.utils.AnalyticsHelper
import iad1tya.echo.music.utils.PerformanceMonitor
import iad1tya.echo.music.utils.MemoryOptimizer
import iad1tya.echo.music.utils.CrashLoggingHandler
import iad1tya.echo.music.utils.CrashlyticsHelper
import iad1tya.echo.music.utils.FirebaseConfig
import iad1tya.echo.music.utils.FirebaseManager
import iad1tya.echo.music.utils.FirebaseTestUtils
import iad1tya.echo.music.ui.MainActivity
import iad1tya.echo.music.ui.theme.newDiskCache
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class EchoApplication :
    Application(),
    KoinComponent,
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                        },
                    ),
                )
            }.logger(DebugLogger())
            .allowHardware(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCache(newDiskCache())
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        
        // Set up crash logging system
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashLoggingHandler = CrashLoggingHandler(this, defaultHandler)
        Thread.setDefaultUncaughtExceptionHandler(crashLoggingHandler)
        
        // Set up global exception handler to prevent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("EchoApp", "Uncaught exception in thread ${thread.name}: ${exception.message}", exception)
            
            // Log the exception details
            exception.printStackTrace()
            
            // Try to gracefully handle the exception
            try {
                // Clear memory caches to prevent memory issues
                System.gc()
                
                // Log memory info for debugging
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val maxMemory = runtime.maxMemory()
                Log.e("EchoApp", "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB")
                
                Log.e("EchoApp", "Crash handled gracefully")
                
                // Log crash recovery analytics
                try {
                    AnalyticsHelper.logCrashRecovery(
                        recoveryMethod = "memory_cleanup_and_gc",
                        success = true
                    )
                } catch (analyticsException: Exception) {
                    Log.e("EchoApp", "Failed to log crash recovery analytics: ${analyticsException.message}", analyticsException)
                }
            } catch (e: Exception) {
                Log.e("EchoApp", "Error in crash handler: ${e.message}")
                
                // Log failed crash recovery analytics
                try {
                    AnalyticsHelper.logCrashRecovery(
                        recoveryMethod = "memory_cleanup_and_gc",
                        success = false
                    )
                } catch (analyticsException: Exception) {
                    Log.e("EchoApp", "Failed to log crash recovery failure analytics: ${analyticsException.message}", analyticsException)
                }
            }
            
            // Let the crash logging handler handle it
            crashLoggingHandler.uncaughtException(thread, exception)
        }
        
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@EchoApplication)
            modules(
                databaseModule,
                mediaServiceModule,
                viewModelModule,
            )
        }
        // provide custom configuration
        val workConfig =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build()

        // initialize WorkManager
        WorkManager.initialize(this, workConfig)

        // Initialize Firebase services
        try {
            FirebaseManager.initialize(this)
            Log.d("EchoApp", "Firebase services initialized")
            
            // Test Firebase integration in debug builds
            if (BuildConfig.DEBUG) {
                FirebaseTestUtils.testFirebaseIntegration(this)
                val report = FirebaseTestUtils.generateFirebaseTestReport(this)
                Log.d("EchoApp", "Firebase test report:\n$report")
            }
        } catch (e: Exception) {
            Log.e("EchoApp", "Failed to initialize Firebase services: ${e.message}")
        }
        
        // Initialize Analytics Helper
        try {
            AnalyticsHelper.initialize(this)
            Log.d("EchoApp", "Analytics Helper initialized")
        } catch (e: Exception) {
            Log.e("EchoApp", "Failed to initialize Analytics Helper: ${e.message}")
        }
        
        // Initialize Crashlytics Helper
        try {
            CrashlyticsHelper.initialize(this)
            Log.d("EchoApp", "Crashlytics Helper initialized")
        } catch (e: Exception) {
            Log.e("EchoApp", "Failed to initialize Crashlytics Helper: ${e.message}")
        }
        
        // Initialize performance monitoring
        val performanceMonitor = PerformanceMonitor.getInstance(this)
        performanceMonitor.startMonitoring()
        Log.d("EchoApp", "Performance monitoring started")
        
        // Initialize memory optimizer
        val memoryOptimizer = MemoryOptimizer.getInstance(this)
        memoryOptimizer.startMemoryMonitoring()
        Log.d("EchoApp", "Memory optimization started")

        CaocConfig.Builder
            .create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) // default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(true) // default: true
            .showErrorDetails(true) // default: true
            .showRestartButton(true) // default: true
            .errorDrawable(R.mipmap.ic_launcher_round)
            .logErrorOnRestart(false) // default: true
            .trackActivities(true) // default: false
            .minTimeBetweenCrashesMs(2000) // default: 3000 //default: bug image
            .restartActivity(MainActivity::class.java) // default: null (your app's launch activity)
            .apply()
    }

    override fun onTerminate() {
        try {
            // Cleanup performance monitoring
            PerformanceMonitor.getInstance(this).cleanup()
            MemoryOptimizer.getInstance(this).cleanup()
            
            Log.d("EchoApp", "Application terminated - cleanup completed")
        } catch (e: Exception) {
            Log.e("EchoApp", "Error during termination cleanup: ${e.message}")
        } finally {
            super.onTerminate()
        }
    }
}