package androidx.media3.exoplayer.offline;

import androidx.media3.common.MediaItem;

/** Download record; only the state is used by the UI. */
public final class Download {
  public static final int STATE_QUEUED = 0;
  public static final int STATE_STOPPED = 1;
  public static final int STATE_DOWNLOADING = 2;
  public static final int STATE_COMPLETED = 3;
  public static final int STATE_FAILED = 4;
  public static final int STATE_REMOVING = 5;
  public static final int STATE_RESTARTING = 7;

  public final String id;
  public final int state;
  public final long contentLength;

  public Download(String id, int state, long contentLength) {
    this.id = id; this.state = state; this.contentLength = contentLength;
  }
}
