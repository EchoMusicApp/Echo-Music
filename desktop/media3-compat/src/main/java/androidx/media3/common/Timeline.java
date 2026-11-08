package androidx.media3.common;

import org.jetbrains.annotations.Nullable;

/** The queue as a list of windows (one per media item). Concrete timelines supply the playing order. */
public abstract class Timeline {
  public static final Timeline EMPTY = new Timeline() {
    @Override public int getWindowCount() { return 0; }
    @Override public Window getWindow(int windowIndex, Window window) { throw new IndexOutOfBoundsException(); }
    @Override public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) { return C.INDEX_UNSET; }
    @Override public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) { return C.INDEX_UNSET; }
  };

  public static final class Window {
    public Object uid = new Object();
    public MediaItem mediaItem = MediaItem.EMPTY;
    @Nullable public Object manifest;
    public long durationUs = C.TIME_UNSET;
    public boolean isSeekable = true;
    public boolean isDynamic = false;
    public int firstPeriodIndex = 0;
    public int lastPeriodIndex = 0;
    public long positionInFirstPeriodUs = 0;
  }

  public abstract int getWindowCount();
  public abstract Window getWindow(int windowIndex, Window window);
  public abstract int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled);
  public abstract int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled);

  public final boolean isEmpty() { return getWindowCount() == 0; }

  public final Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) { return getWindow(windowIndex, window); }
}
