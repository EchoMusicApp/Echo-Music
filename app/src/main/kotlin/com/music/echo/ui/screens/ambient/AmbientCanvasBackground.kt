package echo.music.iad1tya.ui.screens.ambient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.applecanvas.AppleMusicCanvasProvider
import echo.music.iad1tya.canvas.CanvasArtwork
import echo.music.iad1tya.canvas.TidalCanvasProvider
import echo.music.iad1tya.echomusiccanvas.echomusicCanvasProvider
import echo.music.iad1tya.ui.player.CanvasArtworkPlaybackCache
import echo.music.iad1tya.ui.player.CanvasArtworkPlayer
import echo.music.iad1tya.ui.player.normalizeCanvasArtistName
import echo.music.iad1tya.ui.player.normalizeCanvasSongTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AmbientCanvasBackground(modifier: Modifier = Modifier) {
  val playerConnection = LocalPlayerConnection.current ?: return
  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  val isPlaying by playerConnection.isPlaying.collectAsState()
  val metadata = mediaMetadata ?: return
  var canvasArtwork by remember(metadata.id) { mutableStateOf<CanvasArtwork?>(null) }

  val storefront = remember {
    val country = Locale.getDefault().country
    if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
  }

  LaunchedEffect(metadata.id) {
    canvasArtwork = CanvasArtworkPlaybackCache.get(metadata.id)
    if (canvasArtwork != null) return@LaunchedEffect

    val itemMetadata = playerConnection.player.currentMediaItem?.mediaMetadata
    val albumName = itemMetadata?.albumTitle?.toString().orEmpty()
    val songTitleRaw = itemMetadata?.title?.toString().orEmpty()
    val artistNameRaw = itemMetadata?.artist?.toString().orEmpty()

    val fetched =
      withContext(Dispatchers.IO) {
        val songTitle = normalizeCanvasSongTitle(songTitleRaw)
        val artistName = normalizeCanvasArtistName(artistNameRaw)

        linkedSetOf(
            songTitle to artistName,
            songTitleRaw to artistName,
            songTitle to artistNameRaw,
            songTitleRaw to artistNameRaw,
          )
          .filter { (song, artist) -> song.isNotBlank() && artist.isNotBlank() }
          .firstNotNullOfOrNull { (song, artist) ->
            if (albumName.isNotBlank()) {
              AppleMusicCanvasProvider.getByAlbumArtist(
                  album = albumName,
                  artist = artist,
                  storefront = storefront,
                )
                ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                ?.let { return@firstNotNullOfOrNull it }
            }

            echomusicCanvasProvider.getBySongArtist(song = song, artist = artist)
              ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
              ?: TidalCanvasProvider.getBySongArtist(
                  song = song,
                  artist = artist,
                  album = albumName,
                )
                ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
              ?: AppleMusicCanvasProvider.getBySongArtist(
                  song = song,
                  artist = artist,
                  album = albumName,
                  storefront = storefront,
                )
                ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
          }
      }

    val validated =
      fetched?.takeIf { artwork ->
        val resultArtist = artwork.artist
        val artistMatches =
          if (resultArtist != null && artistNameRaw.isNotBlank()) {
            val normalizedResult = normalizeCanvasArtistName(resultArtist)
            val normalizedRequested = normalizeCanvasArtistName(artistNameRaw)
            resultArtist.contains(artistNameRaw, ignoreCase = true) ||
              artistNameRaw.contains(resultArtist, ignoreCase = true) ||
              normalizedResult.contains(normalizedRequested, ignoreCase = true) ||
              normalizedRequested.contains(normalizedResult, ignoreCase = true)
          } else {
            true
          }

        val canvasAlbumName = artwork.albumName
        val canvasSongName = artwork.name
        val titleMatches =
          when {
            canvasAlbumName != null && albumName.isNotBlank() ->
              canvasAlbumName.contains(albumName, ignoreCase = true) ||
                albumName.contains(canvasAlbumName, ignoreCase = true)
            canvasSongName != null && songTitleRaw.isNotBlank() ->
              canvasSongName.contains(songTitleRaw, ignoreCase = true) ||
                songTitleRaw.contains(canvasSongName, ignoreCase = true)
            else -> true
          }

        artistMatches && titleMatches
      }

    canvasArtwork = validated
    validated?.let { CanvasArtworkPlaybackCache.put(metadata.id, it) }
  }

  canvasArtwork?.let { artwork ->
    CanvasArtworkPlayer(
      primaryUrl = artwork.animated,
      fallbackUrl = artwork.videoUrl,
      isPlaying = isPlaying,
      modifier = modifier,
    )
  }
}
