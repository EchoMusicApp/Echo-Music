/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.extensions

import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

fun ExoPlayer.setOffloadEnabled(enabled: Boolean) {
    val candidates =
        listOf(
            "experimentalSetOffloadSchedulingEnabled",
            "setOffloadSchedulingEnabled",
            "setOffloadEnabled",
        )

    for (name in candidates) {
        try {
            val method = this::class.java.getMethod(name, Boolean::class.javaPrimitiveType)
            method.invoke(this, enabled)
            return
        } catch (_: NoSuchMethodException) {
        } catch (t: Throwable) {
            Timber.tag("ExoPlayerExtensions").v(t, "$name reflection failed")
            return
        }
    }

    Timber.tag("ExoPlayerExtensions").v("No offload toggle method found")
}
