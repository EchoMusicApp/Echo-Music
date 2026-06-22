

package iad1tya.echo.music.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    var player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    var triggerTime by mutableLongStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    fun start(minute: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minute == -1) {
            pauseWhenSongEnd = true
            triggerTime = -1L
        } else {
            pauseWhenSongEnd = false
            triggerTime = System.currentTimeMillis() + minute.minutes.inWholeMilliseconds
            sleepTimerJob =
                scope.launch {
                    val delayTime = triggerTime - System.currentTimeMillis()
                    if (delayTime > 0) {
                        delay(delayTime)
                    }
                    fadeOutAndPause()
                    triggerTime = -1L
                }
        }
    }

    private suspend fun fadeOutAndPause() {
        val initialVolume = player.volume
        val fadeDuration = 3000L
        val steps = 30
        val stepDelay = fadeDuration / steps
        for (i in steps downTo 1) {
            player.volume = initialVolume * (i.toFloat() / steps)
            delay(stepDelay)
        }
        player.pause()
        player.volume = initialVolume
    }

    
    fun notifySongTransition() {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            scope.launch { fadeOutAndPause() }
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            scope.launch { fadeOutAndPause() }
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            scope.launch { fadeOutAndPause() }
        }
    }
}
