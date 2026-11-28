package androidx.media3.common;

public final class PlaybackParameters {
  public static final PlaybackParameters DEFAULT = new PlaybackParameters(1f);
  public final float speed;
  public final float pitch;
  public PlaybackParameters(float speed) { this(speed, 1f); }
  public PlaybackParameters(float speed, float pitch) { this.speed = speed; this.pitch = pitch; }
}
