package org.echomusic.crashlytics

import android.content.Context
import echo.music.iad1tya.domain.data.player.PlayerError
import echo.music.iad1tya.logger.Logger

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Logger.e("Crashlytics", "NON-SENTRY crash: ${throwable.localizedMessage}")
}

fun configCrashlytics(applicationContext: Context, dsn: String) {
    Logger.d("Crashlytics", "NON-SENTRY start app")
}

fun pushPlayerError(error: PlayerError) {
    Logger.e("Crashlytics", "NON-SENTRY Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
}