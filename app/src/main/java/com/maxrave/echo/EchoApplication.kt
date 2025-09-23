package iad1tya.echo.music

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.UnstableApi
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
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
import iad1tya.echo.music.configCrashlytics
import iad1tya.echo.music.utils.AnalyticsHelper
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
                            OkHttpClient()
                        },
                    ),
                )
            }.logger(DebugLogger())
            .allowHardware(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCache(newDiskCache())
            .crossfade(true)
            .build()

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        
        // Set up global exception handler to prevent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("EchoApp", "Uncaught exception in thread ${thread.name}: ${exception.message}", exception)
            
            // Log the exception details
            exception.printStackTrace()
            
            // Try to gracefully handle the exception
            try {
                // You can add additional crash reporting here if needed
                Log.e("EchoApp", "Crash handled gracefully")
            } catch (e: Exception) {
                Log.e("EchoApp", "Error in crash handler: ${e.message}")
            }
            
            // Let the default handler handle it (which will trigger CaocConfig)
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
        }
        
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        configCrashlytics(this)
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

        // initialize Firebase Analytics
        val firebaseAnalytics = Firebase.analytics
        AnalyticsHelper.initialize(this)
        Log.d("EchoApp", "Firebase Analytics initialized")

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
        super.onTerminate()

        Log.w("Terminate", "Checking")
    }
}