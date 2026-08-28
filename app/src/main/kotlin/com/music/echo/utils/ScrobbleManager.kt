package echo.music.iad1tya.utils

import echo.music.iad1tya.models.MediaMetadata
import com.music.echo.utils.lastfm.LastFM
import echo.music.iad1tya.utils.isLocalMediaId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ScrobbleManager(
    private val scope: CoroutineScope,
    var minSongDuration: Int = 30,
    var scrobbleDelayPercent: Float = 0.5f,
    var scrobbleDelaySeconds: Int = 50
) {
    var useNowPlaying = true
    var enableScrobbling = false
    var useSendLikes = false

    private var currentMetadata: MediaMetadata? = null
    private var duration: Long? = null
    private var isPlaying = false
    
    private var playTimeSeconds = 0
    private var scrobbled = false
    private var trackJob: Job? = null
    private var scrobblingJob: Job? = null

    fun destroy() {
        trackJob?.cancel()
        scrobblingJob?.cancel()
    }

    fun onSongStart(metadata: MediaMetadata?, duration: Long? = null) {
        if (metadata?.id == currentMetadata?.id) return
        
        stopTracking()
        
        currentMetadata = metadata
        this.duration = duration
        playTimeSeconds = 0
        scrobbled = false

        if (!enableScrobbling || metadata == null || metadata.id.isLocalMediaId()) return
        
        if (useNowPlaying && LastFM.isInitialized()) {
            scrobblingJob?.cancel()
            scrobblingJob = scope.launch {
                try {
                    val artists = metadata.artists.joinToString(", ") { it.name }
                    LastFM.updateNowPlaying(
                        artist = artists.ifEmpty { "Unknown Artist" },
                        track = metadata.title,
                        album = metadata.album?.title,
                        duration = duration?.let { (it / 1000).toInt() }
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Last.fm updateNowPlaying failed")
                }
            }
        }
    }

    fun onSongResume(metadata: MediaMetadata) {
        onPlayerStateChanged(true, metadata, duration)
    }

    fun onSongPause() {
        onPlayerStateChanged(false, currentMetadata, duration)
    }

    fun onPlayerStateChanged(isPlaying: Boolean, metadata: MediaMetadata?, duration: Long? = null) {
        this.isPlaying = isPlaying
        
        if (metadata != null && metadata.id != currentMetadata?.id) {
             onSongStart(metadata, duration)
        }
        
        if (isPlaying) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    private fun startTracking() {
        trackJob?.cancel()
        if (scrobbled || currentMetadata == null || !enableScrobbling) return
        
        trackJob = scope.launch {
            while (true) {
                delay(1000)
                playTimeSeconds++
                checkScrobbleThreshold()
            }
        }
    }
    
    private fun stopTracking() {
        trackJob?.cancel()
    }

    fun onSongStop() {
        stopTracking()
    }

    private fun checkScrobbleThreshold() {
        val meta = currentMetadata ?: return
        val dur = duration?.let { it / 1000 } ?: return

        if (scrobbled) return
        
        if (dur < minSongDuration && dur > 0) return

        val actualDuration = if (dur > 0) dur else 300L
        val percentThreshold = (actualDuration * scrobbleDelayPercent).toInt()
        val absoluteThreshold = scrobbleDelaySeconds
        val threshold = minOf(percentThreshold, absoluteThreshold)

        if (playTimeSeconds >= threshold) {
            scrobbled = true
            stopTracking()
            doScrobble(meta, actualDuration)
        }
    }
    
    private fun doScrobble(metadata: MediaMetadata, durationInSeconds: Long) {
        val artists = metadata.artists.joinToString(", ") { it.name }
        if (artists.isEmpty() || metadata.id.isLocalMediaId() || !LastFM.isInitialized()) return
        
        val timestamp = System.currentTimeMillis() / 1000 - playTimeSeconds
        
        scrobblingJob?.cancel()
        scrobblingJob = scope.launch {
            try {
                LastFM.scrobble(
                    artist = artists,
                    track = metadata.title,
                    timestamp = timestamp,
                    album = metadata.album?.title,
                    duration = durationInSeconds.toInt()
                )
                Timber.d("Successfully scrobbled ${metadata.title}")
            } catch (e: Exception) {
                Timber.e(e, "Last.fm scrobble failed")
            }
        }
    }
}
