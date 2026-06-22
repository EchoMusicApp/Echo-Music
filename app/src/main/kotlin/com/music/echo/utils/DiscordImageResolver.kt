

package iad1tya.echo.music.utils

import android.content.Context
import iad1tya.echo.music.db.entities.Song
import timber.log.Timber

data class ResolvedDiscordImages(
    val thumbnailOriginalUrl: String?,
    val thumbnailResolvedId: String?,
    val artistOriginalUrl: String?,
    val artistResolvedId: String?,
)

object DiscordImageResolver {
    private const val TAG = "DiscordImageResolver"

    private var cachedSongId: String? = null
    private var cachedImages: ResolvedDiscordImages? = null

    @Synchronized
    fun getCachedImages(songId: String): ResolvedDiscordImages? = if (cachedSongId == songId) cachedImages else null

    @Synchronized
    private fun setCachedImages(
        songId: String,
        images: ResolvedDiscordImages,
    ) {
        cachedSongId = songId
        cachedImages = images
    }

    @Synchronized
    fun clearCache() {
        cachedSongId = null
        cachedImages = null
    }

    suspend fun resolveImagesForSong(
        context: Context,
        song: Song,
    ): ResolvedDiscordImages {
        val songId = song.song.id
        getCachedImages(songId)?.let { cached ->
            Timber.tag(TAG).d("Using cached images for song: %s", songId)
            return cached
        }

        val thumbnailUrl = song.song.thumbnailUrl?.asHttpUrl()
        val artistUrl =
            song.artists
                .firstOrNull()
                ?.thumbnailUrl
                ?.asHttpUrl()
        val thumbnail = thumbnailUrl
        val artist = artistUrl ?: thumbnail

        val images =
            ResolvedDiscordImages(
                thumbnailOriginalUrl = thumbnailUrl,
                thumbnailResolvedId = thumbnail,
                artistOriginalUrl = artistUrl,
                artistResolvedId = artist,
            )

        setCachedImages(songId, images)
        return images
    }

    fun buildImageUrl(
        imageType: String,
        customUrl: String?,
        resolvedImages: ResolvedDiscordImages,
        song: Song,
    ): String? =
        when (imageType.lowercase()) {
            "thumbnail", "song", "album" -> {
                resolvedImages.thumbnailResolvedId
                    ?: resolvedImages.thumbnailOriginalUrl
                    ?: song.song.thumbnailUrl?.asHttpUrl()
            }

            "artist" -> {
                resolvedImages.artistResolvedId
                    ?: resolvedImages.artistOriginalUrl
                    ?: song.artists
                        .firstOrNull()
                        ?.thumbnailUrl
                        ?.asHttpUrl()
                    ?: resolvedImages.thumbnailResolvedId
                    ?: resolvedImages.thumbnailOriginalUrl
                    ?: song.song.thumbnailUrl?.asHttpUrl()
            }

            "appicon" -> {
                "https://avatars.githubusercontent.com/u/258176326?s=200&v=4"
            }

            "custom" -> {
                customUrl?.asHttpUrl()
                    ?: resolvedImages.thumbnailResolvedId
                    ?: resolvedImages.thumbnailOriginalUrl
            }

            "dontshow", "none" -> {
                null
            }

            else -> {
                resolvedImages.thumbnailResolvedId ?: resolvedImages.thumbnailOriginalUrl
            }
        }

    private fun String.asHttpUrl(): String? {
        val trimmed = trim()
        return trimmed.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true)
        }
    }
}
