package echo.music.iad1tya.utils

import android.content.Context

object AppContextHolder {
    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}
