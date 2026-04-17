package iad1tya.echo.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import com.echo.innertube.CloudflareDnsResolver
import com.echo.innertube.YouTube
import com.echo.innertube.models.YouTubeLocale
import com.echo.kugou.KuGou
import iad1tya.echo.music.utils.potoken.AppContextHolder
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.canvas.ArchiveTuneCanvas
import iad1tya.echo.music.constants.*
import com.metrolist.lastfm.LastFM
import iad1tya.echo.music.canvas.CanvasArtworkPlaybackCache
import iad1tya.echo.music.di.ApplicationScope
import iad1tya.echo.music.extensions.toEnum
import iad1tya.echo.music.extensions.toInetSocketAddress
import iad1tya.echo.music.utils.CrashHandler
import iad1tya.echo.music.utils.DiagnosticsLogTree
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import timber.log.Timber
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        CanvasArtworkPlaybackCache.init(filesDir)
        ArchiveTuneCanvas.initialize(BuildConfig.CANVAS_BEARER_TOKEN)
        AppContextHolder.initialize(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))
        Timber.plant(Timber.DebugTree())
        Timber.plant(DiagnosticsLogTree())

        // Initialize Firebase with error handling
        try {
            val firebaseApp = FirebaseApp.initializeApp(this)
            if (firebaseApp != null) {
                // Enable Firebase Crashlytics collection
                try {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to enable Crashlytics")
                }
                
                // Set user properties for Firebase Analytics
                try {
                    FirebaseAnalytics.getInstance(this).apply {
                        setUserProperty("app_version", BuildConfig.VERSION_NAME)
                        setUserProperty("architecture", BuildConfig.ARCHITECTURE)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to set Firebase Analytics properties")
                }
            } else {
                Timber.w("Firebase initialization returned null - continuing without Firebase")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize Firebase - app will continue without Firebase services")
        }

        // تهيئة إعدادات التطبيق عند الإقلاع
        applicationScope.launch {
            initializeSettings()
            observeSettingsChanges()
        }
    }

    private suspend fun initializeSettings() {
        val settings = dataStore.data.first()

        // One-time migration: turn SponsorBlock off for all existing users.
        // Users can enable it again manually afterward.
        if (settings[SponsorBlockResetV420DoneKey] != true) {
            dataStore.edit { prefs ->
                prefs[SponsorBlockEnabledKey] = false
                prefs[SponsorBlockResetV420DoneKey] = true
            }
        }

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")

        YouTube.locale = YouTubeLocale(
            gl = settings[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = settings[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )

        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        CloudflareDnsResolver.isEnabled = settings[CloudflareDnsEnabledKey] ?: true

        if (settings[ProxyEnabledKey] == true) {
            val username = settings[ProxyUsernameKey].orEmpty()
            val password = settings[ProxyPasswordKey].orEmpty()
            val type = settings[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP)

            if (username.isNotEmpty() || password.isNotEmpty()) {
                if (type == Proxy.Type.HTTP) {
                    YouTube.proxyAuth = Credentials.basic(username, password)
                } else {
                    Authenticator.setDefault(object : Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication =
                            PasswordAuthentication(username, password.toCharArray())
                    })
                }
            }
            try {
                settings[ProxyUrlKey]?.let {
                    YouTube.proxy = Proxy(type, it.toInetSocketAddress())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@App, "Failed to parse proxy url.", Toast.LENGTH_SHORT).show()
                }
                reportException(e)
            }
        }

        YouTube.useLoginForBrowse = settings[UseLoginForBrowse] ?: true

        // Initialize Last.fm
        LastFM.initialize(
            apiKey = BuildConfig.LASTFM_API_KEY,
            secret = BuildConfig.LASTFM_SECRET,
        )
        settings[LastFMSessionKey]?.let { sessionKey ->
            if (sessionKey.isNotEmpty()) {
                LastFM.sessionKey = sessionKey
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Music playback channel — must be created before MusicService posts its first
            // notification so the importance level is guaranteed (DefaultMediaNotificationProvider
            // creates the channel lazily on first post, and Samsung can silently downgrade it).
            if (nm.getNotificationChannel("music_channel_01") == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        "music_channel_01",
                        getString(R.string.music_player),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = getString(R.string.music_player)
                        setShowBadge(false)
                    }
                )
            }

            val channel = NotificationChannel(
                "updates",
                getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.update_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun observeSettingsChanges() {
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData?.takeIf { it != "null" }
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        Timber.e(e, "Could not parse cookie. Clearing existing cookie.")
                        forgetAccount(this@App)
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[CloudflareDnsEnabledKey] ?: true }
                .distinctUntilChanged()
                .collect { enabled ->
                    CloudflareDnsResolver.isEnabled = enabled
                }
        }

        // Sync Last.fm session key
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[LastFMSessionKey] ?: "" }
                .distinctUntilChanged()
                .collect { sessionKey ->
                    LastFM.sessionKey = sessionKey.ifEmpty { null }
                }
        }

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cacheSize = dataStore.get(MaxImageCacheSizeKey, 512)

        return ImageLoader.Builder(this).apply {
            crossfade(false)
            allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            if (cacheSize == 0) {
                diskCachePolicy(CachePolicy.DISABLED)
            } else {
                diskCache(
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil"))
                        .maxSizeBytes(cacheSize * 1024 * 1024L)
                        .build()
                )
            }
        }.build()
    }

    companion object {
        suspend fun forgetAccount(context: Context) {
            context.dataStore.edit { settings ->
                settings.remove(InnerTubeCookieKey)
                settings.remove(VisitorDataKey)
                settings.remove(DataSyncIdKey)
                settings.remove(AccountNameKey)
                settings.remove(AccountEmailKey)
                settings.remove(AccountChannelHandleKey)
            }
        }
    }
}
