package echo.music.iad1tya.listentogether

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.music.innertube.YouTube
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.ListenTogetherSmartResyncKey
import echo.music.iad1tya.constants.ListenTogetherSyncVolumeKey
import echo.music.iad1tya.extensions.currentMetadata
import echo.music.iad1tya.extensions.metadata
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.models.MediaMetadata.Album
import echo.music.iad1tya.models.MediaMetadata.Artist
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.PlayerConnection
import echo.music.iad1tya.utils.dataStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class ListenTogetherManager
@Inject
constructor(
  private val client: ListenTogetherClient,
  @ApplicationContext private val context: Context
) {
  companion object {
    private const val TAG = "ListenTogetherManager"
    private const val HEARTBEAT_MS = 2_000L
  }

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private var playerConnection: PlayerConnection? = null
  private var eventCollectorJob: Job? = null
  private var queueObserverJob: Job? = null
  private var volumeObserverJob: Job? = null
  private var playerListenerRegistered = false

  private val syncHostVolumeEnabled = MutableStateFlow(true)
  private val smartResyncEnabled = MutableStateFlow(true)
  private var lastSyncedVolume: Float? = null
  private var previousMuteState: Boolean? = null
  private var muteForcedByPreference = false

  private var lastRole: RoomRole = RoomRole.NONE

  @Volatile private var isSyncing = false

  private var lastSyncedIsPlaying: Boolean? = null
  private var lastSyncedTrackId: String? = null

  private var pendingPlayback: BufferedPlayback? = null
  private var waitingPlayer: Player? = null
  private val readinessListener =
    object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) finishBufferingIfReady()
      }
    }
  private var restoreSpeed: (() -> Unit)? = null
  private var speedResetJob: Job? = null

  val connectionState = client.connectionState
  val roomState = client.roomState
  val role = client.role
  val userId = client.userId
  val pendingJoinRequests = client.pendingJoinRequests
  val bufferingUsers = client.bufferingUsers
  val logs = client.logs
  val events = client.events
  val blockedUsernames = client.blockedUsernames
  val pendingSuggestions = client.pendingSuggestions
  val rtt = client.rtt

  val isInRoom: Boolean
    get() = client.isInRoom

  val isHost: Boolean
    get() = client.isHost

  val hasPersistedSession: Boolean
    get() = client.hasPersistedSession

  val allowParticipantControl: Boolean
    get() = roomState.value?.allowParticipantControl == true

  val canControlMusic: Boolean
    get() = !isInRoom || isHost || allowParticipantControl

  val isGuestPlaybackRestricted: Boolean
    get() = isInRoom && !canControlMusic

  val guestPlaybackRestricted =
    combine(client.roomState, client.role) { state, role ->
        role == RoomRole.GUEST && state?.allowParticipantControl != true
      }
      .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

  private val _chatMessages = MutableStateFlow<List<ChatMessagePayload>>(emptyList())
  val chatMessages = _chatMessages

  private val playerListener =
    object : Player.Listener {
      override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        try {
          if (isSyncing || !canControlMusic || !isInRoom) return

          val connection = playerConnection ?: return
          val player = connection.player

          Timber.tag(TAG).d("Play state changed: $playWhenReady (reason: $reason)")

          val currentTrackId = player.currentMediaItem?.mediaId
          if (currentTrackId != null && currentTrackId != lastSyncedTrackId) {
            Timber.tag(TAG)
              .d("[SYNC] Sending track change before play state: track = $currentTrackId")
            player.currentMetadata?.let { metadata ->
              sendTrackChangeInternal(metadata)
              lastSyncedTrackId = currentTrackId
              lastSyncedIsPlaying = false
            }
          }
          sendPlayState(if (isHost) player.isPlaying else playWhenReady, player)
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error in onPlayWhenReadyChanged")
        }
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isHost || !isInRoom || isSyncing) return
        val player = playerConnection?.player ?: return
        if (player.currentMediaItem?.mediaId == lastSyncedTrackId) sendPlayState(isPlaying, player)
      }

      private fun sendPlayState(playing: Boolean, player: Player) {
        val trackId = player.currentMediaItem?.mediaId ?: return
        if (playing == lastSyncedIsPlaying || (!playing && lastSyncedIsPlaying == null)) return
        val action = if (playing) PlaybackActions.PLAY else PlaybackActions.PAUSE
        client.sendPlaybackAction(action, trackId = trackId, position = player.currentPosition)
        lastSyncedIsPlaying = playing
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        try {
          if (isSyncing || !canControlMusic || !isInRoom) return
          if (mediaItem == null) return

          val connection = playerConnection ?: return
          val player = connection.player

          val trackId = mediaItem.mediaId
          if (trackId == lastSyncedTrackId) return

          lastSyncedTrackId = trackId

          lastSyncedIsPlaying = false

          player.currentMetadata?.let { metadata ->
            Timber.tag(TAG).d("Host sending track change: ${metadata.title}")
            sendTrackChange(metadata)
            sendPlayState(if (isHost) player.isPlaying else player.playWhenReady, player)
          }
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error in onMediaItemTransition")
        }
      }

      override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
      ) {
        try {
          if (isSyncing || !canControlMusic || !isInRoom) return

          if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            Timber.tag(TAG).d("Host sending SEEK to ${newPosition.positionMs}")
            sendIfAllowed {
              client.sendPlaybackAction(
                PlaybackActions.SEEK,
                trackId = newPosition.mediaItem?.mediaId,
                position = newPosition.positionMs
              )
            }
          }
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error in onPositionDiscontinuity")
        }
      }
    }

  fun setPlayerConnection(connection: PlayerConnection?) {
    Timber.tag(TAG).d("setPlayerConnection: ${connection != null}, isInRoom: $isInRoom")

    try {

      val oldConnection = playerConnection
      if (connection === oldConnection) return
      clearTiming()
      stopQueueSyncObservation()
      stopVolumeSyncObservation()
      if (playerListenerRegistered && oldConnection != null) {
        try {
          oldConnection.player.removeListener(playerListener)
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error removing old player listener")
        }
        playerListenerRegistered = false
      }
      oldConnection?.shouldBlockPlaybackChanges = null
      oldConnection?.onSkipPrevious = null
      oldConnection?.onSkipNext = null
      oldConnection?.onRestartSong = null

      playerConnection = connection
      refreshSyncCapabilities()
      if (connection != null && isInRoom && !isHost) client.requestSync()
    } catch (e: Exception) {
      Timber.tag(TAG).e(e, "Error in setPlayerConnection")
    }
  }

  private fun sendIfAllowed(action: () -> Unit) {
    if (!canControlMusic) return
    action()
  }

  private inline fun applyingRemote(action: () -> Unit) {
    val previous = isSyncing
    isSyncing = true
    try {
      action()
    } finally {
      isSyncing = previous
    }
  }

  private fun refreshSyncCapabilities() {
    val connection = playerConnection ?: return

    connection.shouldBlockPlaybackChanges = { isGuestPlaybackRestricted }

    connection.onSkipPrevious = {
      try {
        if (canControlMusic && !isSyncing) {
          Timber.tag(TAG).d("Skip Previous triggered")
          sendIfAllowed { client.sendPlaybackAction(PlaybackActions.SKIP_PREV) }
        }
      } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Error in onSkipPrevious")
      }
    }
    connection.onSkipNext = {
      try {
        if (canControlMusic && !isSyncing) {
          Timber.tag(TAG).d("Skip Next triggered")
          sendIfAllowed { client.sendPlaybackAction(PlaybackActions.SKIP_NEXT) }
        }
      } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Error in onSkipNext")
      }
    }
    connection.onRestartSong = {
      try {
        if (canControlMusic && !isSyncing) {
          Timber.tag(TAG).d("Restart Song triggered (sending 1ms as 0ms workaround)")
          sendIfAllowed { client.sendPlaybackAction(PlaybackActions.SEEK, position = 1L) }
        }
      } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Error in onRestartSong")
      }
    }

    if (isInRoom && canControlMusic) {
      if (!playerListenerRegistered) {
        try {
          connection.player.addListener(playerListener)
          playerListenerRegistered = true
          Timber.tag(TAG).d("Added player listener for room sync")
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Failed to add player listener")
          playerListenerRegistered = false
        }
      }
      if (isHost) {
        startQueueSyncObservation()
        startHeartbeat()
        startVolumeSyncObservation()
      } else if (allowParticipantControl) {
        startQueueSyncObservation()
        stopHeartbeat()
        stopVolumeSyncObservation()
      }
    } else {
      if (playerListenerRegistered && !isHost) {
        try {
          connection.player.removeListener(playerListener)
          playerListenerRegistered = false
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Failed to remove player listener")
        }
      }
      if (!isHost) {
        stopQueueSyncObservation()
      }
      if (!canControlMusic) {
        stopHeartbeat()
        stopVolumeSyncObservation()
      }
    }
    updateGuestMuteState()
  }

  private fun observePreferences() {
    scope.launch {
      context.dataStore.data
        .map {
          (try {
            it[ListenTogetherSyncVolumeKey]
          } catch (e: Exception) {
            null
          }) ?: true
        }
        .distinctUntilChanged()
        .collect { enabled -> syncHostVolumeEnabled.value = enabled }
    }
    scope.launch {
      context.dataStore.data
        .map {
          (try {
            it[ListenTogetherSmartResyncKey]
          } catch (e: Exception) {
            null
          }) ?: true
        }
        .distinctUntilChanged()
        .collect { enabled -> smartResyncEnabled.value = enabled }
    }
  }

  fun initialize() {
    Timber.tag(TAG).d("Initializing ListenTogetherManager")
    if (eventCollectorJob?.isActive == true) return
    eventCollectorJob =
      scope.launch {
        client.events.collect { event ->
          try {
            Timber.tag(TAG).d("Received event: $event")
            handleEvent(event)
          } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error handling event: $event")
          }
        }
      }

    scope.launch {
      role.collect { newRole ->
        try {
          if (newRole != lastRole && lastRole != RoomRole.NONE) clearTiming()
          lastRole = newRole
          refreshSyncCapabilities()
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error in role change handler")
        }
      }
    }

    scope.launch {
      roomState
        .map { it?.allowParticipantControl ?: false }
        .distinctUntilChanged()
        .collect { refreshSyncCapabilities() }
    }
  }

  private fun handleEvent(event: ListenTogetherEvent) {
    when (event) {
      is ListenTogetherEvent.Connected -> {
        Timber.tag(TAG).d("Connected to server with userId: ${event.userId}")
      }
      is ListenTogetherEvent.RoomCreated -> {
        Timber.tag(TAG).d("Room created: ${event.roomCode}")
        try {

          val connection = playerConnection
          val player = connection?.player
          if (player != null && !playerListenerRegistered) {
            try {
              player.addListener(playerListener)
              playerListenerRegistered = true
              Timber.tag(TAG).d("Added player listener as host")
            } catch (e: Exception) {
              Timber.tag(TAG).e(e, "Failed to add player listener on room create")
            }
          }

          lastSyncedIsPlaying = player?.isPlaying
          lastSyncedTrackId = player?.currentMediaItem?.mediaId

          player?.currentMetadata?.let { metadata ->
            Timber.tag(TAG).d("Room created with existing track: ${metadata.title}")

            sendTrackChangeInternal(metadata)

            val isPlaying = player.isPlaying
            if (isPlaying) {
              lastSyncedIsPlaying = true
              val position = player.currentPosition
              Timber.tag(TAG).d("Host already playing on room create, sending PLAY at $position")
              sendIfAllowed {
                client.sendPlaybackAction(
                  PlaybackActions.PLAY,
                  trackId = metadata.id,
                  position = position
                )
              }
            }
          }
          startQueueSyncObservation()
          startHeartbeat()
          startVolumeSyncObservation()
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error handling RoomCreated event")
        }
      }
      is ListenTogetherEvent.JoinApproved -> {
        Timber.tag(TAG).d("Join approved for room: ${event.roomCode}")

        saveMuteStateOnJoin()

        applyPlaybackState(
          currentTrack = event.state.currentTrack,
          anchor = event.state.playbackAnchor(event.receivedNs),
          queue = event.state.queue,
        )
        applyHostVolumeIfNeeded(event.state.volume)
        updateGuestMuteState()
      }
      is ListenTogetherEvent.PlaybackSync -> {
        Timber.tag(TAG).d("PlaybackSync received: ${event.action.action}")
        if (isInRoom) handlePlaybackSync(event.action, event.receivedNs)
      }
      is ListenTogetherEvent.UserJoined -> {
        Timber.tag(TAG).d("[SYNC] User joined: ${event.username}")

        if (isHost) {
          try {
            val connection = playerConnection
            val player = connection?.player
            player?.currentMetadata?.let { metadata ->
              if (metadata.id != lastSyncedTrackId) sendTrackChangeInternal(metadata)

              if (player.isPlaying) {
                val pos = player.currentPosition
                Timber.tag(TAG).d("[SYNC] Host playing, sending PLAY at $pos for new joiner")
                client.sendPlaybackAction(
                  PlaybackActions.PLAY,
                  trackId = player.currentMediaItem?.mediaId,
                  position = pos
                )
              }
            }
          } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error handling UserJoined event")
          }
        }
      }
      is ListenTogetherEvent.BufferWait -> {
        Timber.tag(TAG).d("BufferWait: waiting for ${event.waitingFor.size} users")
      }
      is ListenTogetherEvent.BufferComplete -> {
        Timber.tag(TAG).d("BufferComplete for track: ${event.trackId}")
        pendingPlayback?.roomReady(event.trackId)
        finishBufferingIfReady()
      }
      is ListenTogetherEvent.SyncStateReceived -> {
        Timber.tag(TAG)
          .d(
            "SyncStateReceived: playing=${event.state.isPlaying}, pos=${event.state.position}, track=${event.state.currentTrack?.id}"
          )
        if (isInRoom && !isHost) {
          handleSyncState(event.state, event.receivedNs)
        }
      }
      is ListenTogetherEvent.Kicked -> {
        Timber.tag(TAG).d("Kicked from room: ${event.reason}")
        cleanup()
      }
      is ListenTogetherEvent.Disconnected -> {
        clearTiming()
        Timber.tag(TAG).d("Disconnected from server")
      }
      is ListenTogetherEvent.Reconnecting -> {
        clearTiming()
        Timber.tag(TAG).d("Reconnecting: attempt ${event.attempt}/${event.maxAttempts}")
      }
      is ListenTogetherEvent.Reconnected -> {
        Timber.tag(TAG).d("Reconnected to room: ${event.roomCode}, isHost: ${event.isHost}")
        try {

          val connection = playerConnection
          val player = connection?.player
          if (player != null && !playerListenerRegistered) {
            try {
              player.addListener(playerListener)
              playerListenerRegistered = true
              Timber.tag(TAG).d("Re-added player listener after reconnect")
            } catch (e: Exception) {
              Timber.tag(TAG).e(e, "Failed to re-add player listener after reconnect")
            }
          }

          if (event.isHost) {

            lastSyncedIsPlaying = player?.isPlaying
            lastSyncedTrackId = player?.currentMediaItem?.mediaId

            val currentMetadata = player?.currentMetadata
            if (currentMetadata != null) {

              val serverTrackId = event.state.currentTrack?.id
              if (serverTrackId != currentMetadata.id) {
                Timber.tag(TAG)
                  .d(
                    "Reconnected as host, server track ($serverTrackId) differs from local (${currentMetadata.id}), syncing"
                  )
                sendTrackChangeInternal(currentMetadata)
              } else {
                Timber.tag(TAG)
                  .d("Reconnected as host, server already has current track $serverTrackId")
              }

              scope.launch {
                delay(500)
                try {
                  val currentPlayer = playerConnection?.player
                  if (isHost && currentPlayer?.isPlaying == true) {
                    val pos = currentPlayer.currentPosition
                    Timber.tag(TAG).d("Reconnected host is playing, sending PLAY at $pos")
                    client.sendPlaybackAction(
                      PlaybackActions.PLAY,
                      trackId = currentPlayer.currentMediaItem?.mediaId,
                      position = pos
                    )
                  }
                } catch (e: Exception) {
                  Timber.tag(TAG).e(e, "Error sending play state after reconnect")
                }
              }
            }
          } else {

            Timber.tag(TAG).d("Reconnected as guest, syncing to host's current state")
            applyPlaybackState(
              currentTrack = event.state.currentTrack,
              anchor = event.state.playbackAnchor(event.receivedNs),
              queue = event.state.queue,
              bypassBuffer = true
            )
            applyHostVolumeIfNeeded(event.state.volume)

            scope.launch {
              delay(1000)
              if (isGuestPlaybackRestricted && smartResyncEnabled.value) {
                Timber.tag(TAG).d("Requesting fresh sync after reconnect (Smart Resync)")
                requestSync()
              }
            }
          }
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error handling Reconnected event")
        }
      }
      is ListenTogetherEvent.UserReconnected -> {
        Timber.tag(TAG).d("User reconnected: ${event.username}")
      }
      is ListenTogetherEvent.UserDisconnected -> {
        Timber.tag(TAG).d("User temporarily disconnected: ${event.username}")
      }
      is ListenTogetherEvent.HostChanged -> {
        Timber.tag(TAG).d("Host changed: new host is ${event.newHostName} (${event.newHostId})")
        refreshSyncCapabilities()

        val nowIsHost = event.newHostId == userId.value
        if (nowIsHost) {
          val player = playerConnection?.player
          val metadata = player?.currentMetadata
          if (metadata != null) {
            Timber.tag(TAG).d("New host sending current track: ${metadata.title}")
            sendTrackChangeInternal(metadata)

            if (player.isPlaying) {
              val position = player.currentPosition
              Timber.tag(TAG).d("New host is playing, sending PLAY at $position")
              sendIfAllowed {
                client.sendPlaybackAction(
                  PlaybackActions.PLAY,
                  trackId = player.currentMediaItem?.mediaId,
                  position = position
                )
              }
            }
          }
        }
      }
      is ListenTogetherEvent.JoinRequestReceived -> {
        Timber.tag(TAG).d("Join request received from ${event.username}")
      }
      is ListenTogetherEvent.LocalSuggestionApproved -> {
        try {
          val mediaMetadata = event.payload.trackInfo.toMediaMetadata()
          val mediaItem = mediaMetadata.toMediaItem()
          playerConnection?.playNext(mediaItem)
          Timber.tag(TAG).d("Approved suggestion added to queue: ${mediaMetadata.title}")
        } catch (e: Exception) {
          Timber.tag(TAG).e(e, "Error adding approved suggestion to queue")
        }
      }
      is ListenTogetherEvent.ConnectionError -> {
        Timber.tag(TAG).e("Connection error: ${event.error}")
        cleanup()
      }
      is ListenTogetherEvent.ChatMessageReceived -> {
        Timber.tag(TAG).d("Chat message received from ${event.payload.username}")
        _chatMessages.value = _chatMessages.value + event.payload
      }
      is ListenTogetherEvent.RoomSettingsChanged -> {
        Timber.tag(TAG)
          .d("Room settings changed: allowParticipantControl=${event.allowParticipantControl}")
        refreshSyncCapabilities()
      }
      else -> {}
    }
  }

  private fun cleanup() {
    if (lastRole == RoomRole.GUEST) {
      restoreGuestMuteState()
    }
    if (playerListenerRegistered) {
      playerConnection?.player?.removeListener(playerListener)
      playerListenerRegistered = false
    }
    stopQueueSyncObservation()
    stopHeartbeat()
    stopVolumeSyncObservation()

    lastSyncedIsPlaying = null
    lastSyncedTrackId = null
    clearTiming()
    isSyncing = false
    lastRole = RoomRole.NONE
    _chatMessages.value = emptyList()
  }

  private fun updateGuestMuteState() {

    val connection = playerConnection ?: return

    restoreGuestMuteState()
  }

  private fun saveMuteStateOnJoin() {
    val connection = playerConnection ?: return

    if (previousMuteState == null) {
      previousMuteState = connection.isMuted.value
      Timber.tag(TAG).d("Saved mute state on join: ${previousMuteState}")
    }
  }

  private fun restoreGuestMuteState() {
    val connection = playerConnection ?: return
    val savedState = previousMuteState

    if (savedState != null) {
      Timber.tag(TAG)
        .d(
          "Restoring mute state on leave: was muted=$savedState, currently muted=${connection.isMuted.value}"
        )
      connection.setMuted(savedState)
    } else {

      if (connection.isMuted.value) {
        Timber.tag(TAG).d("No saved mute state on leave, unmuting player as fallback")
        connection.setMuted(false)
      }
    }

    previousMuteState = null
    muteForcedByPreference = false
  }

  private fun applyHostVolumeIfNeeded(volume: Float?) {
    if (!syncHostVolumeEnabled.value || isHost || !isInRoom) return
    val connection = playerConnection ?: return
    val target = volume?.coerceIn(0f, 1f) ?: return
    connection.service.playerVolume.value = target
  }

  private fun clearBuffer() {
    waitingPlayer?.removeListener(readinessListener)
    waitingPlayer = null
    pendingPlayback = null
  }

  private fun stopSpeedCorrection() {
    speedResetJob?.cancel()
    speedResetJob = null
    restoreSpeed?.invoke()
    restoreSpeed = null
  }

  private fun clearTiming() {
    clearBuffer()
    stopSpeedCorrection()
  }

  private fun finishBufferingIfReady() {
    val pending = pendingPlayback ?: return
    val connection = playerConnection ?: return
    val player = connection.player
    if (waitingPlayer !== player || player.playbackState != Player.STATE_READY) return
    val anchor =
      pending.onPlayerReady(player.currentMediaItem?.mediaId, client::sendBufferReady) ?: return
    clearBuffer()
    applyingRemote { alignPlayback(connection, anchor, allowRateAdjustment = false) }
  }

  private fun alignPlayback(
    connection: PlayerConnection,
    anchor: PlaybackAnchor,
    allowRateAdjustment: Boolean
  ) {
    val player = connection.player
    val now = SystemClock.elapsedRealtimeNanos()
    val targetNs = anchor.positionNs(now, player.duration)
    val errorNs = targetNs - TimeUnit.MILLISECONDS.toNanos(player.currentPosition.coerceAtLeast(0))
    stopSpeedCorrection()
    val canAdjust =
      allowRateAdjustment &&
        anchor.playing &&
        player.isPlaying &&
        connection.service.castConnectionHandler?.isCasting?.value != true
    val correction =
      if (
        (!allowRateAdjustment || !player.playWhenReady) && kotlin.math.abs(errorNs) >= NS_PER_MS
      ) {
        DriftCorrection.Seek
      } else client.clock.driftCorrection(errorNs, now, canAdjust)
    if (!anchor.playing && player.playWhenReady) connection.pause()
    when (correction) {
      DriftCorrection.None -> Unit
      DriftCorrection.Seek -> connection.seekTo(targetNs / NS_PER_MS)
      is DriftCorrection.AdjustSpeed -> {
        val original = player.playbackParameters
        val adjusted = original.withSpeed(original.speed * correction.multiplier)
        player.playbackParameters = adjusted
        restoreSpeed = {
          if (player.playbackParameters == adjusted) player.playbackParameters = original
        }
        speedResetJob =
          scope.launch {
            delay(correction.durationMs)
            stopSpeedCorrection()
          }
      }
    }
    if (anchor.playing && !player.playWhenReady) connection.play()
    lastSyncedIsPlaying = anchor.playing
  }

  private fun handlePlaybackSync(action: PlaybackActionPayload, receivedNs: Long) {
    val connection = playerConnection ?: return
    val player = connection.player
    applyingRemote {
      when (action.action) {
        PlaybackActions.PLAY,
        PlaybackActions.PAUSE,
        PlaybackActions.SEEK -> {
          val pending = pendingPlayback
          val trackId = pending?.trackId ?: player.currentMediaItem?.mediaId
          if (action.trackId != null && action.trackId != trackId) return
          val playing =
            when (action.action) {
              PlaybackActions.PLAY -> true
              PlaybackActions.PAUSE -> false
              else -> pending?.anchor?.playing ?: player.playWhenReady
            }
          val anchor =
            client.clock.anchor(action.position ?: 0L, playing, action.serverTime, receivedNs)
          if (pending != null) {
            pending.anchor = anchor
            finishBufferingIfReady()
          } else {
            alignPlayback(
              connection,
              anchor,
              allowRateAdjustment = action.action == PlaybackActions.PLAY
            )
          }
        }
        PlaybackActions.CHANGE_TRACK -> {
          val track = action.trackInfo ?: return
          applyPlaybackState(
            track,
            PlaybackAnchor(0, false, receivedNs),
            action.queue,
            action.queueTitle
          )
        }
        PlaybackActions.SKIP_NEXT -> {
          Timber.tag(TAG).d("Guest: SKIP_NEXT")
          connection.seekToNext()
        }
        PlaybackActions.SKIP_PREV -> {
          Timber.tag(TAG).d("Guest: SKIP_PREV")
          connection.seekToPrevious()
        }
        PlaybackActions.QUEUE_ADD -> {
          val track = action.trackInfo
          if (track == null) {
            Timber.tag(TAG).w("QUEUE_ADD missing trackInfo")
          } else {
            Timber.tag(TAG)
              .d("Guest: QUEUE_ADD ${track.title}, insertNext=${action.insertNext == true}")
            scope.launch(Dispatchers.IO) {
              YouTube.queue(listOf(track.id))
                .onSuccess { list ->
                  val mediaItem =
                    list
                      .firstOrNull()
                      ?.toMediaMetadata()
                      ?.copy(suggestedBy = track.suggestedBy)
                      ?.toMediaItem()
                  if (mediaItem != null) {
                    launch(Dispatchers.Main) {
                      connection.allowInternalSync = true
                      if (action.insertNext == true) {
                        connection.playNext(mediaItem)
                      } else {
                        connection.addToQueue(mediaItem)
                      }
                      connection.allowInternalSync = false
                    }
                  } else {
                    Timber.tag(TAG).w("QUEUE_ADD failed to resolve media item for ${track.id}")
                  }
                }
                .onFailure { Timber.tag(TAG).e(it, "QUEUE_ADD metadata fetch failed") }
            }
          }
        }
        PlaybackActions.QUEUE_REMOVE -> {
          val removeId = action.trackId
          if (removeId.isNullOrEmpty()) {
            Timber.tag(TAG).w("QUEUE_REMOVE missing trackId")
          } else {

            val startIndex = player.currentMediaItemIndex + 1
            var removeIndex = -1
            val total = player.mediaItemCount
            for (i in startIndex until total) {
              val id = player.getMediaItemAt(i).mediaId
              if (id == removeId) {
                removeIndex = i
                break
              }
            }
            if (removeIndex >= 0) {
              Timber.tag(TAG).d("Guest: QUEUE_REMOVE index=$removeIndex id=$removeId")
              player.removeMediaItem(removeIndex)
            } else {
              Timber.tag(TAG).w("QUEUE_REMOVE id not found in queue: $removeId")
            }
          }
        }
        PlaybackActions.QUEUE_CLEAR -> {
          val currentIndex = player.currentMediaItemIndex
          val count = player.mediaItemCount
          val itemsAfter = count - (currentIndex + 1)
          if (itemsAfter > 0) {
            Timber.tag(TAG).d("Guest: QUEUE_CLEAR removing $itemsAfter items after current")
            player.removeMediaItems(currentIndex + 1, count - (currentIndex + 1))
          }
        }
        PlaybackActions.SET_VOLUME -> {
          applyHostVolumeIfNeeded(action.volume)
        }
        PlaybackActions.SYNC_QUEUE -> {
          val queue = action.queue
          val queueTitle = action.queueTitle
          if (queue != null) {
            Timber.tag(TAG).d("Guest: SYNC_QUEUE size=${queue.size}")

            val currentItem = player.currentMediaItem
            val items = queue.map { it.toMediaMetadata().toMediaItem() }
            val timeline =
              if (currentItem != null && items.none { it.mediaId == currentItem.mediaId }) {
                listOf(currentItem) + items
              } else items
            if (!sameTimeline(player, timeline)) {
              val index = timeline.indexOfFirst { it.mediaId == currentItem?.mediaId }
              if (index >= 0) player.setMediaItems(timeline, index, player.currentPosition)
              else player.setMediaItems(timeline)
            }
            connection.service.queueTitle = queueTitle
          }
        }
      }
    }
  }

  private fun RoomState.playbackAnchor(receivedNs: Long) =
    client.clock.anchor(position, isPlaying, lastUpdate, receivedNs, snapshot = true)

  private fun handleSyncState(state: SyncStatePayload, receivedNs: Long) {
    applyPlaybackState(
      state.currentTrack,
      client.clock.anchor(
        state.position,
        state.isPlaying,
        state.lastUpdate,
        receivedNs,
        snapshot = true
      ),
      state.queue,
      bypassBuffer = true
    )
    applyHostVolumeIfNeeded(state.volume)
  }

  private fun sameTimeline(player: Player, items: List<MediaItem>): Boolean =
    player.mediaItemCount == items.size &&
      items.indices.all { player.getMediaItemAt(it).mediaId == items[it].mediaId }

  private fun applyPlaybackState(
    currentTrack: TrackInfo?,
    anchor: PlaybackAnchor,
    queue: List<TrackInfo>?,
    queueTitle: String? = null,
    bypassBuffer: Boolean = false,
  ) {
    val connection = playerConnection ?: return
    val player = connection.player
    clearTiming()
    applyingRemote {
      connection.service.queueTitle = queueTitle ?: "Listen Together"
      if (currentTrack == null) {
        connection.pause()
        if (queue != null) player.setMediaItems(queue.map { it.toMediaMetadata().toMediaItem() })
        return
      }

      // Some servers send the full queue, others send only upcoming tracks.
      val tracks =
        if (queue?.any { it.id == currentTrack.id } == true) queue
        else listOf(currentTrack) + queue.orEmpty()
      val items = tracks.map { it.toMediaMetadata().toMediaItem() }
      val sameTrack = player.currentMediaItem?.mediaId == currentTrack.id
      val reload = !sameTrack || (queue != null && !sameTimeline(player, items))
      if (reload || !bypassBuffer) connection.pause()
      val start = anchor.positionNs(SystemClock.elapsedRealtimeNanos(), -1L) / NS_PER_MS
      if (reload) {
        player.setMediaItems(items, tracks.indexOfFirst { it.id == currentTrack.id }, start)
      } else if (player.playbackState == Player.STATE_ENDED) {
        player.seekTo(start)
      }
      lastSyncedTrackId = currentTrack.id
      pendingPlayback = BufferedPlayback(currentTrack.id, anchor, needsBarrier = !bypassBuffer)
      waitingPlayer = player
      player.addListener(readinessListener)
      if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED)
        player.prepare()
      finishBufferingIfReady()
    }
  }

  fun connect() {
    Timber.tag(TAG).d("Connecting to server")
    client.connect()
  }

  fun disconnect() {
    Timber.tag(TAG).d("Disconnecting from server")
    cleanup()
    client.disconnect()
  }

  fun createRoom(username: String) {
    Timber.tag(TAG).d("Creating room with username: $username")
    client.createRoom(username)
  }

  fun joinRoom(roomCode: String, username: String) {
    Timber.tag(TAG).d("Joining room $roomCode as $username")
    client.joinRoom(roomCode, username)
  }

  fun leaveRoom() {
    Timber.tag(TAG).d("Leaving room")
    cleanup()
    client.leaveRoom()
  }

  fun approveJoin(userId: String) = client.approveJoin(userId)

  fun rejectJoin(userId: String, reason: String? = null) = client.rejectJoin(userId, reason)

  fun kickUser(userId: String, reason: String? = null) = client.kickUser(userId, reason)

  fun blockUser(username: String) = client.blockUser(username)

  fun unblockUser(username: String) = client.unblockUser(username)

  fun getBlockedUsernames(): Set<String> = blockedUsernames.value

  fun transferHost(newHostId: String) = client.transferHost(newHostId)

  fun updateRoomSettings(allowParticipantControl: Boolean) {
    client.updateRoomSettings(allowParticipantControl)
    refreshSyncCapabilities()
  }

  fun sendTrackChange(metadata: MediaMetadata) {
    if (!canControlMusic || isSyncing) return
    sendTrackChangeInternal(metadata)
  }

  private fun sendTrackChangeInternal(metadata: MediaMetadata) {
    if (!canControlMusic) return

    val durationMs = if (metadata.duration > 0) metadata.duration.toLong() * 1000 else 180000L

    val trackInfo =
      TrackInfo(
        id = metadata.id,
        title = metadata.title,
        artist = metadata.artists.joinToString(", ") { it.name },
        album = metadata.album?.title,
        duration = durationMs,
        thumbnail = metadata.thumbnailUrl,
        suggestedBy = metadata.suggestedBy
      )

    Timber.tag(TAG).d("Sending track change: ${trackInfo.title}, duration: $durationMs")

    val currentQueue =
      try {
        playerConnection?.queueWindows?.value?.map { it.toTrackInfo() }
      } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Failed to get current queue")
        null
      }
    val currentTitle =
      try {
        playerConnection?.queueTitle?.value
      } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Failed to get current title")
        null
      }

    sendIfAllowed {
      client.sendPlaybackAction(
        PlaybackActions.CHANGE_TRACK,
        queueTitle = currentTitle,
        trackInfo = trackInfo,
        queue = currentQueue
      )
    }
  }

  private fun startQueueSyncObservation() {
    if (queueObserverJob?.isActive == true) return

    Timber.tag(TAG).d("Starting queue sync observation")
    queueObserverJob =
      scope.launch {
        playerConnection
          ?.queueWindows
          ?.map { windows -> windows.map { it.toTrackInfo() } }
          ?.distinctUntilChanged()
          ?.collectLatest { tracks ->
            if (!canControlMusic || !isInRoom || isSyncing) return@collectLatest

            delay(500)

            Timber.tag(TAG).d("Sending SYNC_QUEUE with ${tracks.size} items")
            val queueTitle =
              try {
                playerConnection?.queueTitle?.value
              } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to get queue title")
                null
              }
            sendIfAllowed {
              client.sendPlaybackAction(
                PlaybackActions.SYNC_QUEUE,
                queueTitle = queueTitle,
                queue = tracks
              )
            }
          }
      }
  }

  private fun startVolumeSyncObservation() {
    if (volumeObserverJob?.isActive == true) return

    Timber.tag(TAG).d("Starting volume sync observation")
    volumeObserverJob =
      scope.launch {
        playerConnection?.service?.playerVolume?.collectLatest { volume ->
          if (!isHost || !isInRoom || !syncHostVolumeEnabled.value) return@collectLatest

          val normalized = volume.coerceIn(0f, 1f)
          val last = lastSyncedVolume
          if (last != null && kotlin.math.abs(last - normalized) < 0.01f) return@collectLatest

          lastSyncedVolume = normalized
          client.sendPlaybackAction(PlaybackActions.SET_VOLUME, volume = normalized)
        }
      }
  }

  private fun stopVolumeSyncObservation() {
    volumeObserverJob?.cancel()
    volumeObserverJob = null
    lastSyncedVolume = null
  }

  private fun androidx.media3.common.Timeline.Window.toTrackInfo(): TrackInfo {
    val metadata =
      mediaItem.metadata ?: return TrackInfo("unknown", "Unknown", "Unknown", "", 0, "")
    val durationMs = if (metadata.duration > 0) metadata.duration.toLong() * 1000 else 180000L
    return TrackInfo(
      id = metadata.id,
      title = metadata.title,
      artist = metadata.artists.joinToString(", ") { it.name },
      album = metadata.album?.title,
      duration = durationMs,
      thumbnail = metadata.thumbnailUrl,
      suggestedBy = metadata.suggestedBy
    )
  }

  private fun stopQueueSyncObservation() {
    queueObserverJob?.cancel()
    queueObserverJob = null
  }

  private fun TrackInfo.toMediaMetadata(): MediaMetadata {
    return MediaMetadata(
      id = id,
      title = title,
      artists = listOf(Artist(id = "", name = artist)),
      album = if (album != null) Album(id = "", title = album) else null,
      duration = (duration / 1000).toInt(),
      thumbnailUrl = thumbnail,
      suggestedBy = suggestedBy
    )
  }

  fun requestSync() {
    if (!isGuestPlaybackRestricted) {
      Timber.tag(TAG)
        .d("requestSync: not applicable (isGuestPlaybackRestricted=$isGuestPlaybackRestricted)")
      return
    }
    Timber.tag(TAG).d("Requesting sync from server")
    client.requestSync()
  }

  fun clearLogs() = client.clearLogs()

  fun suggestTrack(track: TrackInfo) = client.suggestTrack(track)

  fun approveSuggestion(suggestionId: String) {
    if (!isHost) return

    client.approveSuggestion(suggestionId)
  }

  fun rejectSuggestion(suggestionId: String, reason: String? = null) =
    client.rejectSuggestion(suggestionId, reason)

  fun forceReconnect() {
    Timber.tag(TAG).d("Forcing reconnection")
    client.forceReconnect()
  }

  fun getPersistedRoomCode(): String? = client.getPersistedRoomCode()

  fun getSessionAge(): Long = client.getSessionAge()

  private var heartbeatJob: Job? = null

  private fun startHeartbeat() {
    if (heartbeatJob?.isActive == true) return
    heartbeatJob =
      scope.launch {
        while (isInRoom && isHost) {
          delay(HEARTBEAT_MS)
          if (!isInRoom || !isHost || connectionState.value != ConnectionState.CONNECTED) continue
          playerConnection?.player?.let { player ->
            if (player.isPlaying) {
              val pos = player.currentPosition
              Timber.tag(TAG).d("Host heartbeat: sending PLAY at pos $pos")
              client.sendPlaybackAction(
                PlaybackActions.PLAY,
                trackId = player.currentMediaItem?.mediaId,
                position = pos
              )
            }
          }
        }
      }
    Timber.tag(TAG).d("Host heartbeat started")
  }

  private fun stopHeartbeat() {
    heartbeatJob?.cancel()
    heartbeatJob = null
    Timber.tag(TAG).d("Host heartbeat stopped")
  }

  fun sendChatMessage(message: String, replyTo: RepliedMessage? = null) {
    if (message.isBlank()) return
    client.sendChatMessage(message, replyTo)
  }

  init {
    initialize()
    observePreferences()
  }
}
