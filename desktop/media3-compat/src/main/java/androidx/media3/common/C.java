package androidx.media3.common;

/** Constants, same values as Media3's C (only the ones the app uses). */
public final class C {
  private C() {}
  public static final int INDEX_UNSET = -1;
  public static final long TIME_UNSET = Long.MIN_VALUE + 1;
  public static final long TIME_END_OF_SOURCE = Long.MIN_VALUE;
  public static final long MICROS_PER_SECOND = 1_000_000L;
  public static final long MILLIS_PER_SECOND = 1000L;
  public static final int USAGE_MEDIA = 1;
  public static final int AUDIO_CONTENT_TYPE_MUSIC = 2;
  public static final int RESULT_END_OF_INPUT = -1;
  public static final int LENGTH_UNSET = -1;

  public static long msToUs(long ms) { return ms == TIME_UNSET || ms == TIME_END_OF_SOURCE ? ms : ms * 1000; }
  public static long usToMs(long us) { return us == TIME_UNSET || us == TIME_END_OF_SOURCE ? us : us / 1000; }
}
