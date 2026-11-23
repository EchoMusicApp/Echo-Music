package echo.music.iad1tya.playback

/** Bounds a relative seek within the track; unknown durations have no upper bound. */
fun seekButtonTarget(positionMs: Long, durationMs: Long, offsetMs: Long): Long {
  val position = positionMs.coerceAtLeast(0L)
  val target =
    if (offsetMs > 0 && position > Long.MAX_VALUE - offsetMs) Long.MAX_VALUE
    else (position + offsetMs).coerceAtLeast(0L)
  return if (durationMs >= 0L) target.coerceAtMost(durationMs) else target
}
