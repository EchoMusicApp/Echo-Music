package echo.music.iad1tya.ui.player

import echo.music.iad1tya.canvas.CanvasArtwork

// Copied from Thumbnail.kt (a plain LRU cache); the rest of that file is the Android video thumbnail. Delete when it is ported.
object CanvasArtworkPlaybackCache {
  private const val defaultMaxSize = 256
  private val map = LinkedHashMap<String, CanvasArtwork>(defaultMaxSize, 0.75f, true)
  @Volatile private var maxSize = defaultMaxSize

  @Synchronized
  fun get(mediaId: String): CanvasArtwork? {
    if (maxSize <= 0) return null
    return map[mediaId]
  }

  @Synchronized
  fun put(mediaId: String, artwork: CanvasArtwork) {
    val limit = maxSize
    if (limit <= 0) return
    if (mediaId.isBlank()) return
    map[mediaId] = artwork
    while (map.size > limit) {
      val it = map.entries.iterator()
      if (it.hasNext()) {
        it.next()
        it.remove()
      }
    }
  }

  @Synchronized
  fun clear() {
    map.clear()
  }

  @Synchronized
  fun setMaxSize(value: Int) {
    maxSize = value.coerceAtLeast(0)
    if (maxSize == 0) {
      map.clear()
      return
    }
    while (map.size > maxSize) {
      val it = map.entries.iterator()
      if (it.hasNext()) {
        it.next()
        it.remove()
      } else {
        break
      }
    }
  }
}
