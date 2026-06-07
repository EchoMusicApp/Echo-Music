

package iad1tya.echo.music.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import iad1tya.echo.music.applecanvas.AppleMusicCanvasProvider
import iad1tya.echo.music.echomusiccanvas.echomusicCanvasProvider
import iad1tya.echo.music.canvas.CanvasArtwork
import iad1tya.echo.music.canvas.MonochromeAlbumCanvas
import iad1tya.echo.music.canvas.MonochromeApiCanvas
import iad1tya.echo.music.ui.player.CanvasArtworkPlaybackCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun rememberAlbumCanvas(
    albumTitle: String?,
    artistName: String?,
    firstSongTitle: String? = null,
): CanvasArtwork? {
    val cacheKey = remember(albumTitle, artistName) {
        if (albumTitle != null && artistName != null) "album|$albumTitle|$artistName" else null
    }

    var canvasArtwork by remember(cacheKey) {
        mutableStateOf(cacheKey?.let { CanvasArtworkPlaybackCache.get(it) })
    }
    
    val storefront = remember {
        val country = Locale.getDefault().country
        if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }

    LaunchedEffect(albumTitle, artistName, firstSongTitle) {
        if (canvasArtwork != null || cacheKey == null) return@LaunchedEffect
        if (albumTitle.isNullOrBlank() || artistName.isNullOrBlank()) {
            canvasArtwork = null
            return@LaunchedEffect
        }

        val fetched = withContext(Dispatchers.IO) {
            val normalizedAlbumTitle = normalizeCanvasSongTitle(albumTitle)
            val normalizedFirstSongTitle = firstSongTitle?.let { normalizeCanvasSongTitle(it) }
            val normalizedArtistName = normalizeCanvasArtistName(artistName)

            
            val searchTasks = linkedSetOf(
                normalizedAlbumTitle to normalizedArtistName,
                albumTitle to normalizedArtistName,
                normalizedAlbumTitle to artistName,
                albumTitle to artistName,
            )
            
            if (normalizedFirstSongTitle != null) {
                searchTasks.add(normalizedFirstSongTitle to normalizedArtistName)
                searchTasks.add(firstSongTitle to normalizedArtistName)
            }

            searchTasks.filter { (s, a) -> s.isNotBlank() && a.isNotBlank() }
                .firstNotNullOfOrNull { (s, a) ->
                    echomusicCanvasProvider.getBySongArtist(
                        song = s,
                        artist = a
                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    ?: MonochromeAlbumCanvas.getByAlbumArtist(
                        album = s,
                        artist = a
                    ) ?: MonochromeApiCanvas.getBySongArtist(
                        song = s,
                        artist = a,
                        album = albumTitle
                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    ?: AppleMusicCanvasProvider.getByAlbumArtist(
                        album = s,
                        artist = a,
                        storefront = storefront
                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
        }

        
        val validated = fetched?.let { artwork ->
            val resultArtist = artwork.artist
            if (resultArtist != null && artistName.isNotBlank()) {
                if (resultArtist.contains(artistName, ignoreCase = true) || 
                    artistName.contains(resultArtist, ignoreCase = true)) {
                    artwork
                } else null
            } else artwork
        }

        if (validated != null) {
            canvasArtwork = validated
            CanvasArtworkPlaybackCache.put(cacheKey, validated)
        }
    }

    return canvasArtwork
}

private fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}
