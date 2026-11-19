package echo.music.iad1tya.listentogether

/** Zero-based occurrence of the active item among entries with the same media ID. */
internal fun queueOccurrence(ids: List<String>, index: Int, trackId: String): Int {
  if (ids.getOrNull(index) != trackId) return 0
  return (0 until index).count { ids[it] == trackId }
}

internal fun activeQueueIndex(ids: List<String>, trackId: String, occurrence: Int): Int {
  var first = -1
  var seen = 0
  for ((index, id) in ids.withIndex()) {
    if (id != trackId) continue
    if (first == -1) first = index
    if (seen++ == occurrence) return index
  }
  return first
}
