package iad1tya.echo.music.engine

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import iad1tya.echo.music.data.EchoBrainRepository
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.extensions.toMediaItem
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.models.QueueItemSource
import com.music.innertube.models.WatchEndpoint
import iad1tya.echo.music.db.DatabaseDao
import iad1tya.echo.music.engine.brain.FlowNeuroEngine
import iad1tya.echo.music.engine.brain.InteractionType
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EchoBrainEngine @Inject constructor(
    private val repository: EchoBrainRepository,
    private val databaseDao: DatabaseDao,
    val neuroEngine: FlowNeuroEngine
) {
    private var trackingJob: Job? = null
    private var currentTrackId: String? = null
    private var currentTrackMeta: MediaMetadata? = null
    private var totalDurationPlayed: Long = 0L
    private var lastPlayResumeTime: Long = 0L
    private var hasTriggeredAiQueue = false
    
    val isEnabled = MutableStateFlow(true) // This will be bound to datastore
    
    private var playerConnection: PlayerConnection? = null
    private var engineScope: CoroutineScope? = null

    fun initialize(connection: PlayerConnection, scope: CoroutineScope) {
        if (playerConnection != null) return
        playerConnection = connection
        engineScope = scope
        
        scope.launch {
            connection.playbackState.collectLatest { state ->
                handlePlaybackState(state)
            }
        }
        scope.launch {
            connection.currentMediaItemIndex.collectLatest { index ->
                handleMediaItemTransition(connection.player.currentMediaItem)
            }
        }
        scope.launch {
            isEnabled.collectLatest { enabled ->
                if (!enabled) {
                    trackingJob?.cancel()
                    hasTriggeredAiQueue = false
                    
                    // Remove Echo Brain items from the queue
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val player = connection.player
                        val toRemove = mutableListOf<Int>()
                        for (i in 0 until player.mediaItemCount) {
                            val item = player.getMediaItemAt(i)
                            if (item.metadata?.source == QueueItemSource.ECHO_BRAIN) {
                                toRemove.add(i)
                            }
                        }
                        // Remove from end to start to maintain indices
                        toRemove.reversed().forEach { index ->
                            player.removeMediaItem(index)
                        }
                    }
                }
            }
        }
    }

    private fun handlePlaybackState(state: Int) {
        val conn = playerConnection ?: return
        if (!isEnabled.value) return
        
        when (state) {
            Player.STATE_READY -> {
                if (conn.player.playWhenReady) {
                    if (trackingJob?.isActive != true) {
                        trackingJob = engineScope?.launch { delay(Long.MAX_VALUE) }
                        lastPlayResumeTime = System.currentTimeMillis()
                        startTracking()
                    }
                } else {
                    pauseTracking()
                }
            }
            else -> pauseTracking()
        }
    }

    private fun pauseTracking() {
        if (trackingJob?.isActive == true) {
            val now = System.currentTimeMillis()
            if (lastPlayResumeTime > 0) {
                totalDurationPlayed += (now - lastPlayResumeTime)
                lastPlayResumeTime = 0
            }
            trackingJob?.cancel()
        }
    }

    private fun handleMediaItemTransition(mediaItem: MediaItem?) {
        val conn = playerConnection ?: return
        val scope = engineScope ?: return
        
        pauseTracking()
        
        // Log previous track if we were tracking it
        currentTrackId?.let { trackId ->
            val durationPlayed = totalDurationPlayed
            val skipped = durationPlayed < 15000L
            val engaged = durationPlayed >= 15000L
            
            val trackMeta = currentTrackMeta
            
            scope.launch {
                repository.logPlayEvent(trackId, System.currentTimeMillis() - durationPlayed, durationPlayed, skipped, engaged)
                if (skipped) {
                    repository.logActivity("Negative Signal", "Skipped $trackId before 15s")
                }
                
                trackMeta?.let {
                    if (engaged) {
                        neuroEngine.onMediaMetadataInteraction(it, InteractionType.WATCHED, 1.0f)
                    } else {
                        neuroEngine.onMediaMetadataInteraction(it, InteractionType.SKIPPED, 0.1f)
                    }
                }
            }
        }

        // Reset for new track
        totalDurationPlayed = 0L
        lastPlayResumeTime = 0L
        hasTriggeredAiQueue = false
        val newTrackId = mediaItem?.metadata?.id
        currentTrackId = newTrackId
        currentTrackMeta = mediaItem?.metadata
        
        if (newTrackId != null && conn.player.playWhenReady) {
            trackingJob = engineScope?.launch { delay(Long.MAX_VALUE) }
            lastPlayResumeTime = System.currentTimeMillis()
            startTracking()
        }
    }

    private fun startTracking() {
        val scope = engineScope ?: return
        
        currentTrackId?.let { trackId ->
            if (!hasTriggeredAiQueue) {
                hasTriggeredAiQueue = true
                fetchAndInjectSuggestions(trackId)
            }
        }
    }

    private fun fetchAndInjectSuggestions(trackId: String) {
        if (!isEnabled.value) return
        val scope = engineScope ?: return
        val conn = playerConnection ?: return
        scope.launch {
            repository.logActivity("Analyzing", "Analyzing track $trackId for batch suggestions")
            
            // 1. Anchor: Current Track
            val anchorDeferred = async { com.music.innertube.YouTube.next(WatchEndpoint(videoId = trackId)).getOrNull()?.items?.mapNotNull { it.toMediaItem().metadata } ?: emptyList<MediaMetadata>() }
            
            // 2. Momentum: Previous Track
            val previousTrackId = if (conn.player.currentMediaItemIndex > 0) {
                conn.player.getMediaItemAt(conn.player.currentMediaItemIndex - 1).metadata?.id
            } else null
            
            val momentumDeferred = async {
                if (previousTrackId != null) {
                    com.music.innertube.YouTube.next(WatchEndpoint(videoId = previousTrackId)).getOrNull()?.items?.mapNotNull { it.toMediaItem().metadata } ?: emptyList<MediaMetadata>()
                } else emptyList<MediaMetadata>()
            }
            
            // 3. Vault: Top Local Songs (Exploitation)
            val vaultDeferred = async {
                try {
                    databaseDao.topSongs(15).first().map { it.toMediaMetadata() }
                } catch (e: Exception) {
                    emptyList<MediaMetadata>()
                }
            }
            
            // Await all sources
            val anchorCandidates = anchorDeferred.await()
            val momentumCandidates = momentumDeferred.await()
            val vaultCandidates = vaultDeferred.await()
            
            val allCandidates = (anchorCandidates + momentumCandidates + vaultCandidates).distinctBy { it.id }
            
            if (allCandidates.isNotEmpty()) {
            val recentSongs = buildSet {
                for (i in 0 until conn.player.mediaItemCount) {
                    conn.player.getMediaItemAt(i).metadata?.id?.let { add(it) }
                }
            }
            
            val ranked = neuroEngine.rank(allCandidates, recentSongs)
            // Take top 3 for the "Runway"
            val topCandidates = ranked.take(3)
            
            val itemsToInject = topCandidates.map { it ->
                    val newMeta = MediaMetadata(
                        id = it.id,
                        title = it.title,
                        artists = it.artists,
                        duration = it.duration,
                        album = it.album,
                        source = QueueItemSource.ECHO_BRAIN,
                        suggestedBy = "Echo Brain",
                        thumbnailUrl = it.thumbnailUrl
                    )
                    newMeta.toMediaItem()
                }
                
                if (itemsToInject.isNotEmpty()) {
                    // Inject the runway batch with a delay to prevent Media3 PlaybackStatsListener crash
                    kotlinx.coroutines.delay(1500)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        conn.player.addMediaItems(conn.player.currentMediaItemIndex + 1, itemsToInject)
                    }
                    repository.logActivity("Queued", "Added ${itemsToInject.size} tracks to runway queue")
                }
            }
        }
    }

    suspend fun getBrainSnapshot() = neuroEngine.getBrainSnapshot()

    suspend fun generateBrainMix(): List<MediaMetadata> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Vault: Top Local Songs (Exploitation)
                val vaultCandidates = databaseDao.topSongs(20).first().map { it.toMediaMetadata() }
                
                // 2. Anchor: Recommendations for Top 3 Songs
                val anchorDeferreds = vaultCandidates.take(3).map { track ->
                    async {
                        com.music.innertube.YouTube.next(WatchEndpoint(videoId = track.id)).getOrNull()?.items?.mapNotNull { it.toMediaItem().metadata } ?: emptyList<MediaMetadata>()
                    }
                }
                
                val anchorCandidates = anchorDeferreds.awaitAll().flatten()
                val allCandidates = (anchorCandidates + vaultCandidates).distinctBy { it.id }.shuffled()
                
                if (allCandidates.isNotEmpty()) {
                    val ranked = neuroEngine.rank(allCandidates, emptySet())
                    ranked.take(30).map {
                        MediaMetadata(
                            id = it.id,
                            title = it.title,
                            artists = it.artists,
                            duration = it.duration,
                            album = it.album,
                            source = QueueItemSource.ECHO_BRAIN,
                            suggestedBy = "Echo Brain",
                            thumbnailUrl = it.thumbnailUrl
                        )
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

}