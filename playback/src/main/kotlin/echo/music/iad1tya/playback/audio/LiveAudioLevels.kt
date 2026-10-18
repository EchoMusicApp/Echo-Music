package echo.music.iad1tya.playback.audio

/**
 * Real levels measured from the PCM the player is actually outputting.
 * Written by [AudioLevelTapProcessor] on the audio thread, read by the visualisers every frame.
 */
object LiveAudioLevels {
  /** Overall loudness 0..1 (RMS, lightly compressed). */
  @Volatile var level: Float = 0f
  /** Low (<~200 Hz) energy 0..1. */
  @Volatile var bass: Float = 0f
  /** Mid (~200 Hz – 2.5 kHz) energy 0..1. */
  @Volatile var mid: Float = 0f
  /** High (>~2.5 kHz) energy 0..1. */
  @Volatile var treble: Float = 0f
  /** Increments on every detected bass onset, so a reader can fire a pulse once per beat. */
  @Volatile var beatCount: Int = 0
  /** Uptime ms of the last audio buffer; if old, the numbers are stale (paused / unsupported format). */
  @Volatile var lastUpdateMs: Long = 0L

  fun isLive(nowMs: Long = android.os.SystemClock.uptimeMillis()): Boolean = nowMs - lastUpdateMs < 400L

  fun clear() {
    level = 0f; bass = 0f; mid = 0f; treble = 0f
  }
}
