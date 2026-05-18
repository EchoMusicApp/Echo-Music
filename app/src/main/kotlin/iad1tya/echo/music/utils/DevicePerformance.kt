




package iad1tya.echo.music.utils

import android.app.ActivityManager
import android.content.Context

fun Context.isLowRamDevice(): Boolean {
    val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice == true
}