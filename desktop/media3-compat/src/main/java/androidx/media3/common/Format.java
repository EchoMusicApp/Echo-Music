package androidx.media3.common;

/** Audio format description (only the fields the player screen reads). */
public class Format {
  public int bitrate = -1;
  public int sampleRate = -1;
  public String sampleMimeType;
  public String codecs;
  public int channelCount = -1;
}
