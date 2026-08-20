package org.echomusic.crashlytics

import android.content.Context
import android.util.Log
import echo.music.iad1tya.domain.data.player.PlayerError

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Log.e("Crashlytics", "Crash reported: ${throwable.localizedMessage}")
}

fun configCrashlytics(applicationContext: Context, dsn: String) {
    Log.d("Crashlytics", "Configuring crashlytics")
}

fun pushPlayerError(error: PlayerError) {
    Log.e("Crashlytics", "Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
}