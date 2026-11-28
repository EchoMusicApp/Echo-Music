package echo.desktop.core

import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Desktop equivalent of the Android app's startup configuration (App.kt): a valid country/language
 * for the request context, plus a visitor id. Without a country code YouTube answers 400.
 */
object YouTubeSetup {
    private val mutex = Mutex()
    private var done = false

    suspend fun ensureInit() =
        mutex.withLock {
            if (done) return@withLock
            val locale = Locale.getDefault()
            val gl = locale.country.takeIf { it.length == 2 } ?: "US"
            val hl = locale.language.takeIf { it.isNotBlank() && it != "und" } ?: "en"
            YouTube.locale = YouTubeLocale(gl = gl, hl = hl)
            YouTube.visitorData = YouTube.visitorData().getOrNull()
            done = true
        }
}
