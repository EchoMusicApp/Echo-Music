package iad1tya.echo.music.utils

import iad1tya.echo.music.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope

class ScrobbleManager(
    private val scope: CoroutineScope,
    var minSongDuration: Int = 30,
    var scrobbleDelayPercent: Float = 0.5f,
    var scrobbleDelaySeconds: Int = 50
) {
    var useNowPlaying = true

    fun destroy() {}

    fun onSongStart(metadata: MediaMetadata?, duration: Long? = null) {}

    fun onSongResume(metadata: MediaMetadata) {}

    fun onSongPause() {}

    fun onSongStop() {}

    fun onPlayerStateChanged(isPlaying: Boolean, metadata: MediaMetadata?, duration: Long? = null) {}
}
