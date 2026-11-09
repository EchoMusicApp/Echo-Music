package androidx.media3.common;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/** The playback interface the app talks to (a subset of Media3's Player; implemented by the desktop player engine). */
public interface Player {
  int STATE_IDLE = 1;
  int STATE_BUFFERING = 2;
  int STATE_READY = 3;
  int STATE_ENDED = 4;

  int REPEAT_MODE_OFF = 0;
  int REPEAT_MODE_ONE = 1;
  int REPEAT_MODE_ALL = 2;

  int MEDIA_ITEM_TRANSITION_REASON_REPEAT = 0;
  int MEDIA_ITEM_TRANSITION_REASON_AUTO = 1;
  int MEDIA_ITEM_TRANSITION_REASON_SEEK = 2;
  int MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED = 3;

  int TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED = 0;
  int TIMELINE_CHANGE_REASON_SOURCE_UPDATE = 1;

  int PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST = 1;

  int DISCONTINUITY_REASON_AUTO_TRANSITION = 0;
  int DISCONTINUITY_REASON_SEEK = 1;
  int DISCONTINUITY_REASON_SEEK_ADJUSTMENT = 2;
  int DISCONTINUITY_REASON_INTERNAL = 5;

  final class PositionInfo {
    public final int mediaItemIndex;
    public final @Nullable MediaItem mediaItem;
    public final long positionMs;
    public final long contentPositionMs;
    public PositionInfo(@Nullable MediaItem mediaItem, int mediaItemIndex, long positionMs) {
      this.mediaItem = mediaItem; this.mediaItemIndex = mediaItemIndex; this.positionMs = positionMs; this.contentPositionMs = positionMs;
    }
  }

  interface Listener {
    default void onPlaybackStateChanged(int playbackState) {}
    default void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {}
    default void onIsPlayingChanged(boolean isPlaying) {}
    default void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {}
    default void onTimelineChanged(Timeline timeline, int reason) {}
    default void onRepeatModeChanged(int repeatMode) {}
    default void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}
    default void onPlayerError(PlaybackException error) {}
    default void onMediaMetadataChanged(MediaMetadata mediaMetadata) {}
    default void onPositionDiscontinuity(PositionInfo oldPosition, PositionInfo newPosition, int reason) {}
    default void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}
  }

  void addListener(Listener listener);
  void removeListener(Listener listener);

  int getPlaybackState();
  boolean getPlayWhenReady();
  void setPlayWhenReady(boolean playWhenReady);
  boolean isPlaying();
  int getRepeatMode();
  void setRepeatMode(int repeatMode);
  boolean getShuffleModeEnabled();
  void setShuffleModeEnabled(boolean shuffleModeEnabled);
  @Nullable PlaybackException getPlayerError();

  Timeline getCurrentTimeline();
  @Nullable MediaItem getCurrentMediaItem();
  MediaMetadata getMediaMetadata();
  int getCurrentMediaItemIndex();
  int getMediaItemCount();
  MediaItem getMediaItemAt(int index);
  int getPreviousMediaItemIndex();
  int getNextMediaItemIndex();
  boolean hasPreviousMediaItem();
  boolean hasNextMediaItem();

  long getCurrentPosition();
  long getDuration();
  long getBufferedPosition();
  float getVolume();
  void setVolume(float volume);
  PlaybackParameters getPlaybackParameters();
  void setPlaybackParameters(PlaybackParameters playbackParameters);

  void prepare();
  void play();
  void pause();
  void stop();
  void release();
  void seekTo(long positionMs);
  void seekTo(int mediaItemIndex, long positionMs);
  default void seekToDefaultPosition(int mediaItemIndex) { seekTo(mediaItemIndex, 0L); }
  void seekToNext();
  void seekToPrevious();
  void seekToNextMediaItem();
  void seekToPreviousMediaItem();

  void setMediaItem(MediaItem mediaItem);
  void setMediaItems(List<MediaItem> mediaItems);
  void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs);
  void addMediaItem(MediaItem mediaItem);
  void addMediaItem(int index, MediaItem mediaItem);
  void addMediaItems(List<MediaItem> mediaItems);
  void addMediaItems(int index, List<MediaItem> mediaItems);
  void removeMediaItem(int index);
  void removeMediaItems(int fromIndex, int toIndex);
  void moveMediaItem(int currentIndex, int newIndex);
  void replaceMediaItem(int index, MediaItem mediaItem);
  void clearMediaItems();
  boolean getPauseAtEndOfMediaItems();
  void setPauseAtEndOfMediaItems(boolean pauseAtEndOfMediaItems);
}
