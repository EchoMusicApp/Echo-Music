

package iad1tya.echo.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "podcast")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val bookmarkedAt: LocalDateTime? = null,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
) {
    fun toggleBookmark() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now(),
        lastUpdateTime = LocalDateTime.now(),
    )

    val inLibrary: Boolean get() = bookmarkedAt != null
}
