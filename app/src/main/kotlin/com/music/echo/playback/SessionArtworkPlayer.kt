package echo.music.iad1tya.playback

import android.content.Context
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.source.ForwardingTimeline
import echo.music.iad1tya.R
import echo.music.iad1tya.utils.AutomotiveArtwork.withAutomotiveArtworkUri

/**
 * The player exposed through the [androidx.media3.session.MediaSession].
 *
 * Android Auto and the instrument cluster read the now-playing cover and the
 * queue from the legacy session, which Media3 builds from this player's getters
 * and listener callbacks. The items carry `https://` cover URLs, which those
 * surfaces do not load, so this swaps them for the local
 * [echo.music.iad1tya.utils.AutomotiveArtworkProvider] ones at the session
 * boundary only: the app UI keeps using the ExoPlayer directly and is unaffected.
 *
 * It also gives the queue a title when the app has not set one, so Auto shows
 * the queue with a header instead of an unnamed list.
 */
class SessionArtworkPlayer(
  private val context: Context,
  player: Player,
) : ForwardingPlayer(player) {

  private val listeners = HashMap<Player.Listener, Player.Listener>()

  /** Last timeline seen and its wrapper, so repeated reads reuse mapped items. */
  private var lastSourceTimeline: Timeline? = null
  private var lastMappedTimeline: Timeline? = null

  override fun getMediaMetadata(): MediaMetadata = sessionMetadata(super.getMediaMetadata())

  /**
   * ExoPlayer merges the item's metadata with the metadata inside the stream, and
   * Media3 prefers embedded artwork bytes over the artwork URI. Addon streams
   * often embed a cover, so a few seconds into a track the session switched from
   * the item's cover to the stream's: the head unit and cluster redrew it, and an
   * undecodable embedded image left the track with no cover. When the item has
   * its own cover, the stream's is dropped; local files without a URI keep theirs.
   */
  private fun sessionMetadata(metadata: MediaMetadata): MediaMetadata {
    val mapped = metadata.withAutomotiveArtworkUri(context)
    if (mapped.artworkUri == null || mapped.artworkData == null) return mapped
    return mapped.buildUpon().setArtworkData(null, null).build()
  }

  override fun getCurrentMediaItem(): MediaItem? =
    super.getCurrentMediaItem()?.withAutomotiveArtworkUri(context)

  override fun getMediaItemAt(index: Int): MediaItem =
    super.getMediaItemAt(index).withAutomotiveArtworkUri(context)

  override fun getCurrentTimeline(): Timeline = mapTimeline(super.getCurrentTimeline())

  // After a playback error the player is idle, and a skip from Android Auto only
  // moved the position: the next track never started and Auto kept showing the
  // error. Skips from the session restart playback in that case.
  override fun seekToNext() = seekRecoveringFromError { super.seekToNext() }

  override fun seekToNextMediaItem() = seekRecoveringFromError { super.seekToNextMediaItem() }

  override fun seekToPrevious() = seekRecoveringFromError { super.seekToPrevious() }

  override fun seekToPreviousMediaItem() =
    seekRecoveringFromError { super.seekToPreviousMediaItem() }

  override fun seekTo(mediaItemIndex: Int, positionMs: Long) =
    seekRecoveringFromError { super.seekTo(mediaItemIndex, positionMs) }

  /** Restarts only when idle because of an error: a stopped player stays stopped. */
  private inline fun seekRecoveringFromError(seek: () -> Unit) {
    val afterError = playbackState == Player.STATE_IDLE && playerError != null
    seek()
    if (afterError && mediaItemCount > 0) {
      prepare()
      play()
    }
  }

  override fun getPlaylistMetadata(): MediaMetadata {
    val metadata = super.getPlaylistMetadata()
    if (metadata.title != null) return metadata
    return metadata.buildUpon().setTitle(context.getString(R.string.queue)).build()
  }

  override fun addListener(listener: Player.Listener) {
    val mapping = MappingListener(listener)
    listeners[listener] = mapping
    super.addListener(mapping)
  }

  override fun removeListener(listener: Player.Listener) {
    listeners.remove(listener)?.let { super.removeListener(it) }
  }

  private fun mapTimeline(timeline: Timeline): Timeline {
    if (timeline.isEmpty) return timeline
    if (timeline === lastSourceTimeline) return lastMappedTimeline ?: timeline
    val mapped = ArtworkTimeline(timeline)
    lastSourceTimeline = timeline
    lastMappedTimeline = mapped
    return mapped
  }

  /** Rewrites the artwork of every window's item; mapped items are cached per index. */
  private inner class ArtworkTimeline(timeline: Timeline) : ForwardingTimeline(timeline) {
    private val mappedItems = arrayOfNulls<MediaItem>(timeline.windowCount)

    override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
      super.getWindow(windowIndex, window, defaultPositionProjectionUs)
      val source = window.mediaItem
      val cached = mappedItems[windowIndex]
      window.mediaItem =
        if (cached != null && cached.mediaId == source.mediaId) {
          cached
        } else {
          source.withAutomotiveArtworkUri(context).also { mappedItems[windowIndex] = it }
        }
      return window
    }
  }

  /**
   * Media3 copies listener payloads (metadata, timeline) straight into what it
   * sends controllers, so they need the same mapping as the getters.
   *
   * Every callback is forwarded explicitly: Kotlin's `by` delegation does not
   * forward Java default methods, and all of [Player.Listener]'s are defaults,
   * so a delegated listener would silently drop playback-state updates.
   */
  @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
  private inner class MappingListener(private val delegate: Player.Listener) : Player.Listener {
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) =
      delegate.onMediaMetadataChanged(sessionMetadata(mediaMetadata))

    // Every timeline change must reach the session: source updates carry the
    // duration of a manually chosen track, which Auto needs for its progress bar.
    override fun onTimelineChanged(timeline: Timeline, reason: Int) =
      delegate.onTimelineChanged(mapTimeline(timeline), reason)

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) =
      delegate.onPlaylistMetadataChanged(playlistMetadata)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
      delegate.onMediaItemTransition(mediaItem?.withAutomotiveArtworkUri(context), reason)

    override fun onEvents(player: Player, events: Player.Events) = delegate.onEvents(player, events)
    override fun onTracksChanged(tracks: Tracks) = delegate.onTracksChanged(tracks)
    override fun onIsLoadingChanged(isLoading: Boolean) = delegate.onIsLoadingChanged(isLoading)
    override fun onLoadingChanged(isLoading: Boolean) = delegate.onLoadingChanged(isLoading)
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) =
      delegate.onAvailableCommandsChanged(availableCommands)
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
      delegate.onTrackSelectionParametersChanged(parameters)
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
      delegate.onPlayerStateChanged(playWhenReady, playbackState)
    override fun onPlaybackStateChanged(playbackState: Int) = delegate.onPlaybackStateChanged(playbackState)
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
      delegate.onPlayWhenReadyChanged(playWhenReady, reason)
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
      delegate.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
    override fun onIsPlayingChanged(isPlaying: Boolean) = delegate.onIsPlayingChanged(isPlaying)
    override fun onRepeatModeChanged(repeatMode: Int) = delegate.onRepeatModeChanged(repeatMode)
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
      delegate.onShuffleModeEnabledChanged(shuffleModeEnabled)
    override fun onPlayerError(error: PlaybackException) = delegate.onPlayerError(error)
    override fun onPlayerErrorChanged(error: PlaybackException?) = delegate.onPlayerErrorChanged(error)
    override fun onPositionDiscontinuity(reason: Int) = delegate.onPositionDiscontinuity(reason)
    override fun onPositionDiscontinuity(
      oldPosition: Player.PositionInfo,
      newPosition: Player.PositionInfo,
      reason: Int,
    ) = delegate.onPositionDiscontinuity(oldPosition.mapped(), newPosition.mapped(), reason)

    private fun Player.PositionInfo.mapped(): Player.PositionInfo =
      Player.PositionInfo(
        windowUid,
        mediaItemIndex,
        mediaItem?.withAutomotiveArtworkUri(context),
        periodUid,
        periodIndex,
        positionMs,
        contentPositionMs,
        adGroupIndex,
        adIndexInAdGroup,
      )
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
      delegate.onPlaybackParametersChanged(playbackParameters)
    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) =
      delegate.onSeekBackIncrementChanged(seekBackIncrementMs)
    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) =
      delegate.onSeekForwardIncrementChanged(seekForwardIncrementMs)
    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) =
      delegate.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
    override fun onAudioSessionIdChanged(audioSessionId: Int) = delegate.onAudioSessionIdChanged(audioSessionId)
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) =
      delegate.onAudioAttributesChanged(audioAttributes)
    override fun onVolumeChanged(volume: Float) = delegate.onVolumeChanged(volume)
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) =
      delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = delegate.onDeviceInfoChanged(deviceInfo)
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = delegate.onDeviceVolumeChanged(volume, muted)
    override fun onVideoSizeChanged(videoSize: VideoSize) = delegate.onVideoSizeChanged(videoSize)
    override fun onSurfaceSizeChanged(width: Int, height: Int) = delegate.onSurfaceSizeChanged(width, height)
    override fun onRenderedFirstFrame() = delegate.onRenderedFirstFrame()
    override fun onCues(cues: List<Cue>) = delegate.onCues(cues)
    override fun onCues(cueGroup: CueGroup) = delegate.onCues(cueGroup)
    override fun onMetadata(metadata: Metadata) = delegate.onMetadata(metadata)

    override fun equals(other: Any?): Boolean = other is MappingListener && other.delegate == delegate

    override fun hashCode(): Int = delegate.hashCode()
  }
}
