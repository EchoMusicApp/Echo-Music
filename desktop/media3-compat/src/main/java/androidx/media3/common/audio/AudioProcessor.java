package androidx.media3.common.audio;

import java.nio.ByteBuffer;

/** Same shape as Media3's AudioProcessor, so the original equalizer/widener/automix code compiles unchanged. */
public interface AudioProcessor {
  final class AudioFormat {
    public static final AudioFormat NOT_SET = new AudioFormat(-1, -1, -1);
    public final int sampleRate;
    public final int channelCount;
    public final int encoding;
    public final int bytesPerFrame;

    public AudioFormat(int sampleRate, int channelCount, int encoding) {
      this.sampleRate = sampleRate;
      this.channelCount = channelCount;
      this.encoding = encoding;
      this.bytesPerFrame = encoding == androidx.media3.common.C.ENCODING_PCM_16BIT ? channelCount * 2 : -1;
    }

    @Override public String toString() { return "AudioFormat[" + sampleRate + "Hz, " + channelCount + "ch, enc=" + encoding + "]"; }
  }

  final class UnhandledAudioFormatException extends Exception {
    public UnhandledAudioFormatException(AudioFormat inputAudioFormat) { super("Unhandled input format: " + inputAudioFormat); }
  }

  ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0).order(java.nio.ByteOrder.nativeOrder());

  AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException;
  boolean isActive();
  void queueInput(ByteBuffer inputBuffer);
  void queueEndOfStream();
  ByteBuffer getOutput();
  boolean isEnded();
  void flush();
  void reset();
}
