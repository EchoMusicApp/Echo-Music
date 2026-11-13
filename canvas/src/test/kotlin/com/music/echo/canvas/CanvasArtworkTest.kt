package echo.music.iad1tya.canvas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtworkTest {

  @Test
  fun testLegitimateCanvasIsNotYouTubeSnippet() {
    val tidalArtwork = CanvasArtwork(
      name = "Song Title",
      artist = "Artist Name",
      albumName = "Album Title",
      videoUrl = "https://resources.tidal.com/videos/00000000/0000/0000/0000/000000000000/1280x1280.mp4",
      animated = null,
      clipStartMs = null,
      clipEndMs = null,
    )
    assertFalse(tidalArtwork.isYouTubeSnippet)

    val spotifyArtwork = CanvasArtwork(
      name = "Song Title",
      artist = "Artist Name",
      animated = "https://canvaz.scdn.co/upload/artist/12345/canvas/xyz.mp4",
      videoUrl = null,
      clipStartMs = null,
      clipEndMs = null,
    )
    assertFalse(spotifyArtwork.isYouTubeSnippet)
  }

  @Test
  fun testClippedYouTubeSnippetIsDetected() {
    val clippedArtwork = CanvasArtwork(
      name = "Song Title",
      artist = "Artist Name",
      videoUrl = "https://rr3---sn-4g5edn6s.googlevideo.com/videoplayback?expire=123",
      animated = null,
      clipStartMs = 15000L,
      clipEndMs = 20000L,
    )
    assertTrue(clippedArtwork.isYouTubeSnippet)
  }

  @Test
  fun testGoogleVideoHostIsDetectedAsYouTubeSnippet() {
    val gvArtwork = CanvasArtwork(
      name = "Song Title",
      artist = "Artist Name",
      videoUrl = "https://manifest.googlevideo.com/api/manifest/hls_variant/...",
      animated = null,
      clipStartMs = null,
      clipEndMs = null,
    )
    assertTrue(gvArtwork.isYouTubeSnippet)
  }

  @Test
  fun testYouTubeHostIsDetectedAsYouTubeSnippet() {
    val ytArtwork = CanvasArtwork(
      name = "Song Title",
      artist = "Artist Name",
      animated = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      videoUrl = null,
      clipStartMs = null,
      clipEndMs = null,
    )
    assertTrue(ytArtwork.isYouTubeSnippet)
  }
}

