package iad1tya.echo.music.playback

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.Player
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.models.MediaMetadata as AppMediaMetadata
import iad1tya.echo.music.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Manages Google Cast connections and media playback on Cast devices.
 *
 * ## Connection model
 *
 * The app uses AndroidX [MediaRouteButton] to show the system Cast dialog
 * for route discovery, and [MediaRouter.selectRoute] from
 * [CastDevicePickerSheet] for custom device selection. Once a route is
 * selected, [SessionManager] automatically starts a Cast session and fires
 * the [SessionManagerListener] callbacks below.
 *
 * [CastContext] handles device discovery via MediaRouter; the app registers
 * the [SessionManagerListener] and reacts to session events.
 */
class CastConnectionHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val musicService: MusicService
) {
    // ── Core Cast components ──────────────────────────────────────────────
    private var castContext: CastContext? = null
    private var sessionManager: SessionManager? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    private var castSession: CastSession? = null

    // ── Public state flows ────────────────────────────────────────────────
    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()

    private val _castPosition = MutableStateFlow(0L)
    val castPosition: StateFlow<Long> = _castPosition.asStateFlow()

    private val _castDuration = MutableStateFlow(0L)
    val castDuration: StateFlow<Long> = _castDuration.asStateFlow()

    private val _castIsPlaying = MutableStateFlow(false)
    val castIsPlaying: StateFlow<Boolean> = _castIsPlaying.asStateFlow()

    private val _castIsBuffering = MutableStateFlow(false)
    val castIsBuffering: StateFlow<Boolean> = _castIsBuffering.asStateFlow()

    private val _castVolume = MutableStateFlow(1.0f)
    val castVolume: StateFlow<Float> = _castVolume.asStateFlow()

    private val _autoReconnecting = MutableStateFlow(false)
    val autoReconnecting: StateFlow<Boolean> = _autoReconnecting.asStateFlow()

    // ── Internal state ───────────────────────────────────────────────────
    private var positionUpdateJob: Job? = null
    private var currentMediaId: String? = null
    private var lastCastItemId: Int = -1
    private var isReloadingQueue: Boolean = false

    /** Flag to prevent reverse sync when Cast triggers local player update. */
    @Volatile
    var isSyncingFromCast: Boolean = false
        private set

    private var pendingSyncOperation: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    // ── Retry state ──────────────────────────────────────────────────────
    private val maxQueueLoadRetries = 2

    // ── Queue editing state ──────────────────────────────────────────────
    private val _queueItems = MutableStateFlow<List<MediaQueueItem>>(emptyList())

    // ═════════════════════════════════════════════════════════════════════
    // CALLBACKS
    // ═════════════════════════════════════════════════════════════════════

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            remoteMediaClient?.let { client ->
                val mediaStatus = client.mediaStatus
                val playerState = mediaStatus?.playerState
                _castIsPlaying.value = playerState == MediaStatus.PLAYER_STATE_PLAYING ||
                        playerState == MediaStatus.PLAYER_STATE_BUFFERING ||
                        playerState == MediaStatus.PLAYER_STATE_LOADING
                _castIsBuffering.value = playerState == MediaStatus.PLAYER_STATE_BUFFERING ||
                        playerState == MediaStatus.PLAYER_STATE_LOADING
                _castDuration.value = client.streamDuration

                // Use castSession.volume for the real device volume.
                // mediaStatus.streamVolume is unreliable — it can return 1.0
                // while the actual device volume is different (e.g. 0.4).
                castSession?.let { s ->
                    _castVolume.value = s.volume.toFloat()
                }

                val currentItemId = mediaStatus?.currentItemId ?: -1
                if (currentItemId != -1 && currentItemId != lastCastItemId && lastCastItemId != -1 && !isReloadingQueue && mediaStatus != null) {
                    Timber.d("Cast item changed: $lastCastItemId -> $currentItemId")
                    handleCastItemChanged(mediaStatus)
                }
                lastCastItemId = currentItemId

                pendingSyncOperation?.complete(Unit)

                Timber.d("Cast status: playing=${_castIsPlaying.value}, buffering=${_castIsBuffering.value}, itemId=$currentItemId, deviceVolume=${_castVolume.value}")
            }
        }

        override fun onMediaError(error: MediaError) {
            Timber.e("Cast media error: reason=${error.reason}, error=$error")
            handleMediaError(error)
        }

        override fun onQueueStatusUpdated() {
            Timber.d("Cast queue status updated")
            val status = remoteMediaClient?.mediaStatus
            if (status != null) {
                _queueItems.value = status.queueItems?.toList() ?: emptyList()
            }
            pendingSyncOperation?.complete(Unit)
        }
    }

    private val sessionManagerListener: SessionManagerListener<CastSession> =
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) {
                Timber.d("Cast session starting")
                _isConnecting.value = true
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                Timber.d("Cast session started: $sessionId")
                _isCasting.value = true
                _isConnecting.value = false
                _autoReconnecting.value = false
                _castDeviceName.value = session.castDevice?.friendlyName
                castSession = session
                remoteMediaClient = session.remoteMediaClient
                remoteMediaClient?.registerCallback(remoteMediaClientCallback)

                // Read the Cast device volume — do NOT push local volume to it.
                // The speaker keeps its own volume; we just mirror the value.
                _castVolume.value = session.volume.toFloat()

                startPositionUpdates()
                loadCurrentMedia()

                // Re-sync volume after a short delay — the device may not report
                // its real volume until after the media load triggers onStatusUpdated.
                scope.launch {
                    delay(2000L)
                    castSession?.let { s ->
                        val realVolume = s.volume.toFloat()
                        if (realVolume != _castVolume.value) {
                            Timber.d("Cast volume re-sync: ${_castVolume.value} -> $realVolume")
                            _castVolume.value = realVolume
                        }
                    }
                }
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                Timber.e("Cast session start failed: $error")
                _isCasting.value = false
                _isConnecting.value = false
                _autoReconnecting.value = false
            }

            override fun onSessionEnding(session: CastSession) {
                Timber.d("Cast session ending")
                val castPosition = remoteMediaClient?.approximateStreamPosition ?: _castPosition.value
                if (castPosition > 0) {
                    musicService.player.seekTo(castPosition)
                }
            }

            override fun onSessionEnded(session: CastSession, error: Int) {
                Timber.d("Cast session ended: error=$error")
                _isCasting.value = false
                _isConnecting.value = false
                _castDeviceName.value = null
                castSession = null

                remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
                remoteMediaClient = null

                stopPositionUpdates()

                musicService.player.pause()
            }

            override fun onSessionResuming(session: CastSession, sessionId: String) {
                _isConnecting.value = true
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                _isCasting.value = true
                _isConnecting.value = false
                _autoReconnecting.value = false
                _castDeviceName.value = session.castDevice?.friendlyName
                castSession = session

                remoteMediaClient = session.remoteMediaClient
                remoteMediaClient?.registerCallback(remoteMediaClientCallback)
                _castVolume.value = session.volume.toFloat()

                startPositionUpdates()

                scope.launch {
                    delay(2000L)
                    castSession?.let { s ->
                        val realVolume = s.volume.toFloat()
                        if (realVolume != _castVolume.value) {
                            Timber.d("Cast volume re-sync (resumed): ${_castVolume.value} -> $realVolume")
                            _castVolume.value = realVolume
                        }
                    }
                }
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                _isConnecting.value = false
            }

            override fun onSessionSuspended(session: CastSession, reason: Int) {}
        }

    // ═════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Initialize Cast context and session manager.
     * Safe to call multiple times; returns true if Cast is available.
     */
    fun initialize(): Boolean {
        return try {
            castContext = CastContext.getSharedInstance(context)
            sessionManager = castContext?.sessionManager

            sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)

            // Check if already connected
            sessionManager?.currentCastSession?.let { session ->
                _isCasting.value = true
                _castDeviceName.value = session.castDevice?.friendlyName
                castSession = session
                remoteMediaClient = session.remoteMediaClient
                remoteMediaClient?.registerCallback(remoteMediaClientCallback)
                _castVolume.value = session.volume.toFloat()
                startPositionUpdates()
            }

            true
        } catch (e: RuntimeException) {
            Timber.e(e, "Failed to initialize Cast - Google Play Services may not be available")
            false
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Cast")
            false
        }
    }

    /** Check if Cast framework is available on this device without throwing. */
    fun isCastAvailable(): Boolean {
        return try {
            CastContext.getSharedInstance(context)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        _autoReconnecting.value = false
        sessionManager?.endCurrentSession(true)
    }

    fun loadCurrentMedia() {
        val metadata = musicService.currentMediaMetadata.value ?: return
        loadMediaWithQueue(metadata)
    }

    fun loadMedia(metadata: AppMediaMetadata) {
        loadMediaWithQueue(metadata)
    }

    // ── Playback controls ────────────────────────────────────────────────

    fun play() {
        remoteMediaClient?.play()
    }

    fun pause() {
        remoteMediaClient?.pause()
    }

    fun seekTo(position: Long) {
        val seekOptions = MediaSeekOptions.Builder()
            .setPosition(position)
            .build()
        remoteMediaClient?.seek(seekOptions)
    }

    fun setVolume(volume: Float) {
        try {
            val clampedVolume = volume.coerceIn(0f, 1f)
            castSession?.volume = clampedVolume.toDouble()
            // Read back the actual volume Cast applied (it may clamp differently)
            castSession?.let { s ->
                _castVolume.value = s.volume.toFloat()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set Cast volume")
        }
    }

    /**
     * Navigate to a media item if it's in the Cast queue.
     * Returns true if successful, false if the item isn't in the queue.
     */
    fun navigateToMediaIfInQueue(mediaId: String): Boolean {
        val client = remoteMediaClient ?: return false
        val mediaStatus = client.mediaStatus ?: return false
        val queueItems = mediaStatus.queueItems
        if (queueItems.isEmpty()) return false

        val targetIndex = queueItems.indexOfFirst {
            it.media?.customData?.optString("mediaId") == mediaId
        }
        if (targetIndex < 0) {
            Timber.d("Media $mediaId not found in Cast queue")
            return false
        }

        val currentItemId = mediaStatus.currentItemId
        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }

        if (targetIndex == currentIndex) {
            currentMediaId = mediaId
            musicService.player.pause()
            return true
        }

        val targetItem = queueItems[targetIndex]
        Timber.d("Navigating Cast to item at index $targetIndex (mediaId=$mediaId)")

        executeWithSyncFlag {
            val player = musicService.player
            for (i in 0 until player.mediaItemCount) {
                if (player.getMediaItemAt(i).mediaId == mediaId) {
                    player.seekTo(i, 0)
                    break
                }
            }
            player.pause()
            client.queueJumpToItem(targetItem.itemId, org.json.JSONObject())
            currentMediaId = mediaId
        }

        return true
    }

    fun skipToNext() {
        val client = remoteMediaClient
        val mediaStatus = client?.mediaStatus
        if (mediaStatus != null && mediaStatus.queueItemCount > 0) {
            val currentItemId = mediaStatus.currentItemId
            val queueItems = mediaStatus.queueItems
            val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
            if (currentIndex >= 0 && currentIndex < queueItems.size - 1) {
                client.queueNext(org.json.JSONObject())
                musicService.player.pause()
                return
            }
        }
        val player = musicService.player
        if (player.hasNextMediaItem()) {
            player.pause()
            player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val client = remoteMediaClient
        val mediaStatus = client?.mediaStatus
        if (mediaStatus != null && mediaStatus.queueItemCount > 0) {
            val currentItemId = mediaStatus.currentItemId
            val queueItems = mediaStatus.queueItems
            val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
            if (currentIndex > 0) {
                client.queuePrev(org.json.JSONObject())
                musicService.player.pause()
                return
            }
        }
        val player = musicService.player
        if (player.hasPreviousMediaItem()) {
            player.pause()
            player.seekToPreviousMediaItem()
        }
    }

    // ── Queue editing ────────────────────────────────────────────────────

    /** Remove an item from the Cast queue by its itemId. */
    fun removeItemFromQueue(itemId: Int) {
        val client = remoteMediaClient ?: return
        scope.launch {
            try {
                client.queueRemoveItem(itemId, org.json.JSONObject())
                Timber.d("Removed item $itemId from Cast queue")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove item $itemId from Cast queue")
            }
        }
    }

    /** Move an item in the Cast queue to a new position. */
    fun moveItemInQueue(itemId: Int, newIndex: Int) {
        val client = remoteMediaClient ?: return
        scope.launch {
            try {
                client.queueMoveItemToNewIndex(itemId, newIndex, org.json.JSONObject())
                Timber.d("Moved item $itemId to index $newIndex in Cast queue")
            } catch (e: Exception) {
                Timber.e(e, "Failed to move item $itemId in Cast queue")
            }
        }
    }

    /** Clear all items from the Cast queue. */
    fun clearQueue() {
        val client = remoteMediaClient ?: return
        scope.launch {
            try {
                val mediaStatus = client.mediaStatus
                if (mediaStatus != null && mediaStatus.queueItemCount > 0) {
                    val itemIds = mediaStatus.queueItems.map { it.itemId }.toIntArray()
                    client.queueRemoveItems(itemIds, org.json.JSONObject())
                }
                Timber.d("Cleared Cast queue")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear Cast queue")
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // INTERNAL
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Execute a block with isSyncingFromCast = true.
     * Uses onStatusUpdated/onQueueStatusUpdated callbacks to determine
     * when to clear the flag, with a timeout fallback.
     */
    private fun executeWithSyncFlag(block: () -> Unit) {
        isSyncingFromCast = true
        pendingSyncOperation = kotlinx.coroutines.CompletableDeferred()

        try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "Error during sync operation")
            pendingSyncOperation?.completeExceptionally(e)
        }

        scope.launch {
            try {
                kotlinx.coroutines.withTimeout(5000L) {
                    pendingSyncOperation?.await()
                }
            } catch (_: Exception) {
                Timber.w("Sync operation timed out, resetting flag anyway")
            } finally {
                isSyncingFromCast = false
                pendingSyncOperation = null
            }
        }
    }

    /** Handle when Cast changes to a different item (user pressed next/prev on Cast widget). */
    private fun handleCastItemChanged(mediaStatus: MediaStatus) {
        val queueItems = mediaStatus.queueItems
        val currentItemId = mediaStatus.currentItemId

        // Queue exhausted: Cast has no current item (itemId=0) or queue is empty.
        // Nothing to sync when the Cast queue is empty — playback naturally ends.
        if (currentItemId == 0 || queueItems.isEmpty()) {
            Timber.d("Cast queue exhausted or empty (currentItemId=$currentItemId, queueSize=${queueItems.size})")
            return
        }

        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
        if (currentIndex < 0) return

        val currentQueueItem = queueItems[currentIndex]
        val customData = currentQueueItem.media?.customData
        val castMediaId = customData?.optString("mediaId")
        Timber.d("Cast switched to item: index=$currentIndex, mediaId=$castMediaId, queueSize=${queueItems.size}")

        if (castMediaId != null && castMediaId != currentMediaId) {
            currentMediaId = castMediaId
            executeWithSyncFlag {
                val player = musicService.player
                val playerItemCount = player.mediaItemCount
                for (i in 0 until playerItemCount) {
                    val mediaItem = player.getMediaItemAt(i)
                    if (mediaItem.mediaId == castMediaId) {
                        player.pause()
                        player.seekTo(i, 0)
                        player.pause()
                        val itemsAhead = queueItems.size - 1 - currentIndex
                        val itemsBehind = currentIndex
                        if (itemsAhead < 2 || itemsBehind < 2) {
                            scope.launch {
                                extendQueueIfNeeded(i, playerItemCount, itemsAhead < 2, itemsBehind < 2)
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    /** Extend the Cast queue by adding more items at the edges if needed. */
    private suspend fun extendQueueIfNeeded(
        localPlayerIndex: Int,
        playerItemCount: Int,
        extendForward: Boolean,
        extendBackward: Boolean
    ) {
        if (isReloadingQueue) return
        val client = remoteMediaClient ?: return

        // Read the fresh queue directly from the Cast device, not stale callback data
        val freshQueue = client.mediaStatus?.queueItems ?: return
        if (freshQueue.isEmpty()) return

        // Find the current item in the fresh queue by mediaId
        val freshCastIndex = freshQueue.indexOfFirst {
            it.media?.customData?.optString("mediaId") == currentMediaId
        }
        if (freshCastIndex < 0) return

        isReloadingQueue = true
        try {
            // Track all mediaIds already in the Cast queue (mutable — updated as we add)
            val existingMediaIds = mutableSetOf<String>()
            freshQueue.mapNotNullTo(existingMediaIds) {
                it.media?.customData?.optString("mediaId")
            }

            // Extend forward: append items after the current position
            if (extendForward) {
                var addedCount = 0
                var nextLocalIndex = localPlayerIndex + 1
                while (addedCount < 2 && nextLocalIndex < playerItemCount) {
                    val nextItem = musicService.player.getMediaItemAt(nextLocalIndex)
                    if (nextItem.mediaId !in existingMediaIds) {
                        nextItem.metadata?.let { metadata ->
                            buildMediaInfo(metadata)?.let { mediaInfo ->
                                val queueItem = MediaQueueItem.Builder(mediaInfo).build()
                                withContext(Dispatchers.Main) {
                                    client.queueAppendItem(queueItem, null)
                                }
                                existingMediaIds.add(nextItem.mediaId)
                                addedCount++
                            }
                        }
                    }
                    nextLocalIndex++
                }
            }

            // Extend backward: insert items before the first Cast queue item
            if (extendBackward && localPlayerIndex > 0) {
                val firstCastItemId = freshQueue.firstOrNull()?.itemId
                if (firstCastItemId != null) {
                    var addedCount = 0
                    var prevLocalIndex = localPlayerIndex - 1
                    while (addedCount < 2 && prevLocalIndex >= 0) {
                        val prevItem = musicService.player.getMediaItemAt(prevLocalIndex)
                        if (prevItem.mediaId !in existingMediaIds) {
                            prevItem.metadata?.let { metadata ->
                                buildMediaInfo(metadata)?.let { mediaInfo ->
                                    val queueItem = MediaQueueItem.Builder(mediaInfo).build()
                                    withContext(Dispatchers.Main) {
                                        client.queueInsertItems(
                                            arrayOf(queueItem),
                                            firstCastItemId,
                                            org.json.JSONObject()
                                        )
                                    }
                                    existingMediaIds.add(prevItem.mediaId)
                                    addedCount++
                                }
                            }
                        }
                        prevLocalIndex--
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extend Cast queue")
        } finally {
            delay(500)
            isReloadingQueue = false
        }
    }

    /**
     * Build MediaInfo for a single track.
     */
    private suspend fun buildMediaInfo(metadata: AppMediaMetadata): MediaInfo? {
        val streamUrl = musicService.getStreamUrl(metadata.id) ?: return null
        val castMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, metadata.title)
            putString(MediaMetadata.KEY_ARTIST, metadata.artists.joinToString(", ") { it.name })
            metadata.album?.title?.let { putString(MediaMetadata.KEY_ALBUM_TITLE, it) }
            metadata.thumbnailUrl?.let { thumbUrl ->
                val highQualityUrl = thumbUrl.resize(1080, 1080)
                addImage(WebImage(Uri.parse(highQualityUrl)))
            }
        }
        return MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mp4")
            .setMetadata(castMetadata)
            .setCustomData(org.json.JSONObject().put("mediaId", metadata.id))
            .build()
    }

    /**
     * Load media with queue context. Implements retry with backoff on failure.
     */
    private fun loadMediaWithQueue(metadata: AppMediaMetadata) {
        if (!_isCasting.value) return
        isReloadingQueue = true
        scope.launch {
            var retries = 0
            var success = false
            while (!success && retries <= maxQueueLoadRetries) {
                try {
                    if (retries > 0) {
                        Timber.d("Retrying Cast queue load (attempt ${retries + 1})")
                        delay((1000L * retries))
                    }
                    currentMediaId = metadata.id
                    _castIsBuffering.value = true
                    lastCastItemId = -1

                    val player = musicService.player
                    val currentIndex = player.currentMediaItemIndex
                    val mediaItemCount = player.mediaItemCount
                    val shuffleEnabled = player.shuffleModeEnabled
                    val timeline = player.currentTimeline
                    val queueItems = mutableListOf<MediaQueueItem>()
                    val prevItems = mutableListOf<androidx.media3.common.MediaItem>()

                    if (!timeline.isEmpty) {
                        var prevIdx = currentIndex
                        for (i in 0 until 2) {
                            prevIdx = timeline.getPreviousWindowIndex(prevIdx, Player.REPEAT_MODE_OFF, shuffleEnabled)
                            if (prevIdx == androidx.media3.common.C.INDEX_UNSET) break
                            prevItems.add(0, player.getMediaItemAt(prevIdx))
                        }
                    }
                    for (prevItem in prevItems) {
                        prevItem.metadata?.let { prevMetadata ->
                            buildMediaInfo(prevMetadata)?.let { mediaInfo ->
                                queueItems.add(MediaQueueItem.Builder(mediaInfo).build())
                            }
                        }
                    }
                    val startIndex = queueItems.size
                    val currentMediaInfo = buildMediaInfo(metadata)
                    if (currentMediaInfo == null) {
                        Timber.e("Failed to get stream URL for Cast")
                        _castIsBuffering.value = false
                        isReloadingQueue = false
                        return@launch
                    }
                    queueItems.add(MediaQueueItem.Builder(currentMediaInfo).build())

                    if (!timeline.isEmpty) {
                        var nextIdx = currentIndex
                        for (i in 0 until 2) {
                            nextIdx = timeline.getNextWindowIndex(nextIdx, Player.REPEAT_MODE_OFF, shuffleEnabled)
                            if (nextIdx == androidx.media3.common.C.INDEX_UNSET) break
                            val nextItem = player.getMediaItemAt(nextIdx)
                            nextItem.metadata?.let { nextMetadata ->
                                buildMediaInfo(nextMetadata)?.let { mediaInfo ->
                                    queueItems.add(MediaQueueItem.Builder(mediaInfo).build())
                                }
                            }
                        }
                    }

                    val startPosition = if (player.currentMediaItem?.mediaId == metadata.id) {
                        player.currentPosition
                    } else {
                        0L
                    }

                    Timber.d("Loading Cast queue: ${queueItems.size} items, startIndex=$startIndex, shuffle=$shuffleEnabled")

                    withContext(Dispatchers.Main) {
                        val client = remoteMediaClient ?: return@withContext
                        client.queueLoad(
                            queueItems.toTypedArray(),
                            startIndex,
                            MediaStatus.REPEAT_MODE_REPEAT_OFF,
                            startPosition,
                            org.json.JSONObject()
                        )
                        musicService.player.pause()
                    }

                    Timber.d("Loaded media on Cast: ${metadata.title}")
                    success = true
                } catch (e: Exception) {
                    retries++
                    Timber.e(e, "Failed to load media on Cast (attempt $retries/$maxQueueLoadRetries)")
                    if (retries > maxQueueLoadRetries) {
                        _castIsBuffering.value = false
                        isReloadingQueue = false
                        handleCastLoadFailure()
                    }
                } finally {
                    if (success) {
                        delay(1500)
                    }
                    isReloadingQueue = false
                }
            }
        }
    }

    /**
     * Handle Cast load failure - fallback to local playback.
     */
    private fun handleCastLoadFailure() {
        Timber.w("Cast load failed after retries, falling back to local playback")
        scope.launch {
            try {
                musicService.player.playWhenReady = true
            } catch (_: Exception) { }
        }
        showToast("Cast playback failed, switching to local playback")
    }

    /**
     * Handle media errors with retry for recoverable errors and fallback for permanent ones.
     */
    private var mediaErrorRetryCount = 0
    private val maxMediaErrorRetries = 2

    private fun handleMediaError(error: MediaError) {
        Timber.e("Cast media error: reason=${error.reason}")
        if (mediaErrorRetryCount < maxMediaErrorRetries) {
            mediaErrorRetryCount++
            Timber.d("Recoverable Cast error, retrying ($mediaErrorRetryCount/$maxMediaErrorRetries)")
            scope.launch {
                delay(1000L * mediaErrorRetryCount)
                loadCurrentMedia()
            }
        } else {
            Timber.w("Unrecoverable Cast media error, falling back to local playback")
            mediaErrorRetryCount = 0
            handleCastLoadFailure()
        }
    }

    // ── Position updates ─────────────────────────────────────────────────

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive && _isCasting.value) {
                remoteMediaClient?.let { client ->
                    _castPosition.value = client.approximateStreamPosition
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // ── Toast helper ─────────────────────────────────────────────────────

    private fun showToast(message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { }
    }

    // ── Release ──────────────────────────────────────────────────────────

    fun release() {
        stopPositionUpdates()
        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    companion object {
        /**
         * Safe check if Cast is available on this device without throwing exceptions.
         */
        fun isCastAvailable(context: Context): Boolean {
            return try {
                CastContext.getSharedInstance(context)
                true
            } catch (e: RuntimeException) {
                Timber.d("Cast not available: ${e.message}")
                false
            } catch (e: Exception) {
                Timber.d("Cast not available: ${e.message}")
                false
            }
        }
    }
}
