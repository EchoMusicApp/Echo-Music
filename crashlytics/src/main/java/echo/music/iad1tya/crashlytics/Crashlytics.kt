package echo.music.iad1tya.crashlytics

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import echo.music.iad1tya.domain.data.player.PlayerError

fun reportCrash(throwable: Throwable) {
    Log.e("Crashlytics", "Crash reported: ${throwable.localizedMessage}")
    FirebaseCrashlytics.getInstance().recordException(throwable)
}

fun configCrashlytics(applicationContext: Context, dsn: String) {
    Log.d("Crashlytics", "Configuring crashlytics (Firebase auto-initializes, DSN ignored)")
}

fun pushPlayerError(error: PlayerError) {
    Log.e("Crashlytics", "Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
    val exception = Exception("Player Error: ${error.message} [Code: ${error.errorCode} - ${error.errorCodeName}]")
    FirebaseCrashlytics.getInstance().recordException(exception)
}