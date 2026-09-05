

package echo.music.iad1tya.playback.queues

import androidx.media3.common.MediaItem
import echo.music.iad1tya.extensions.metadata
import echo.music.iad1tya.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) {
                withFilteredItems(items.filterExplicit())
            } else {
                this
            }

        fun filterVideoSongs(disableVideos: Boolean = false) =
            if (disableVideos) {
                withFilteredItems(items.filterVideoSongs(true))
            } else {
                this
            }

        /**
         * Re-points [mediaItemIndex] at the item it originally selected (matched by
         * mediaId) after [newItems] has had entries removed, instead of leaving it as
         * a raw position into a now-shorter list — which would silently select and
         * play a different song than the one the user tapped.
         */
        private fun withFilteredItems(newItems: List<MediaItem>): Status {
            val currentItem = items.getOrNull(mediaItemIndex)
            val newIndex = currentItem?.let { item -> newItems.indexOfFirst { it.mediaId == item.mediaId } } ?: -1
            return copy(
                items = newItems,
                mediaItemIndex = if (newIndex >= 0) newIndex else mediaItemIndex.coerceIn(0, (newItems.size - 1).coerceAtLeast(0)),
            )
        }
    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.metadata?.isVideoSong == true }
    } else {
        this
    }
