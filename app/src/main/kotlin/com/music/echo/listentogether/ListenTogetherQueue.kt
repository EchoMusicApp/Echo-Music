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

/** Returns the active slot in a full queue, or -1 when the current item must be prepended. */
internal fun incomingRoomQueueIndex(
  ids: List<String>,
  activeId: String,
  currentIds: List<String>,
  currentIndex: Int,
  upcomingQueue: Boolean,
): Int {
  if (upcomingQueue) return -1
  // An unmarked snapshot can still match our known upcoming queue, including
  // a later copy of the active track. Otherwise locate the active entry in it.
  if (currentIds.getOrNull(currentIndex) == activeId && currentIds.drop(currentIndex + 1) == ids) {
    return -1
  }
  return activeQueueIndex(ids, activeId, queueOccurrence(currentIds, currentIndex, activeId))
}
