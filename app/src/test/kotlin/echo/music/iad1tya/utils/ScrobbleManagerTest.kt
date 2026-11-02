package echo.music.iad1tya.utils

import echo.music.iad1tya.models.MediaMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScrobbleManagerTest {

  @Test
  fun localMediaIsAcceptedForScrobbling() = runTest {
    val manager = ScrobbleManager(TestScope())
    manager.enableScrobbling = true

    val localMeta =
      MediaMetadata(
        id = "local_42",
        title = "Local Track",
        artists = listOf(MediaMetadata.Artist(id = "1", name = "Local Artist")),
        duration = 180,
      )

    manager.onSongStart(localMeta, duration = 180000L)

    // Ensure it didn't return early due to isLocalMediaId()
    // It should accept metadata
    val currentField = ScrobbleManager::class.java.getDeclaredField("currentMetadata").apply {
      isAccessible = true
    }
    val current = currentField.get(manager) as? MediaMetadata
    assertNotNull(current)
    assertEquals("local_42", current?.id)
  }

  @Test
  fun trackTransitionWhilePlayingSwitchesCurrentMetadata() = runTest {
    val manager = ScrobbleManager(TestScope())
    manager.enableScrobbling = true

    val song1 =
      MediaMetadata(
        id = "song_1",
        title = "Track One",
        artists = listOf(MediaMetadata.Artist(id = "a1", name = "Artist")),
        duration = 200,
      )

    val song2 =
      MediaMetadata(
        id = "song_2",
        title = "Track Two",
        artists = listOf(MediaMetadata.Artist(id = "a1", name = "Artist")),
        duration = 220,
      )

    // Song 1 starts
    manager.onPlayerStateChanged(isPlaying = true, metadata = song1, duration = 200000L)

    val currentField = ScrobbleManager::class.java.getDeclaredField("currentMetadata").apply {
      isAccessible = true
    }
    assertEquals("song_1", (currentField.get(manager) as? MediaMetadata)?.id)

    // Autoplay transition to Song 2 while isPlaying remains true
    manager.onPlayerStateChanged(isPlaying = true, metadata = song2, duration = 220000L)
    assertEquals("song_2", (currentField.get(manager) as? MediaMetadata)?.id)
  }
}

