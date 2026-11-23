package echo.music.iad1tya.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerationStatusTest {

  private lateinit var status: RealGenerationStatus

  @Before
  fun setup() {
    status = RealGenerationStatus()
  }

  @Test
  fun initialStateIsIdle() {
    assertEquals(GenerationState.Idle, status.state.value)
  }

  @Test
  fun startTransitionsToRunning() {
    status.start("Starting generation...")
    val state = status.state.value
    assertTrue(state is GenerationState.Running)
    assertEquals("Starting generation...", (state as GenerationState.Running).message)
  }

  @Test
  fun updateChangesMessageWhenRunning() {
    status.start("Step 1")
    status.update("Step 2")
    val state = status.state.value
    assertTrue(state is GenerationState.Running)
    assertEquals("Step 2", (state as GenerationState.Running).message)
  }

  @Test
  fun updateIgnoredWhenNotRunning() {
    status.update("Step X")
    assertEquals(GenerationState.Idle, status.state.value)
  }

  @Test
  fun succeedTransitionsToDone() {
    status.start("Running")
    status.succeed("playlist_999")
    val state = status.state.value
    assertTrue(state is GenerationState.Done)
    assertEquals("playlist_999", (state as GenerationState.Done).playlistId)
  }

  @Test
  fun failTransitionsToFailed() {
    status.start("Running")
    status.fail("Network error")
    val state = status.state.value
    assertTrue(state is GenerationState.Failed)
    assertEquals("Network error", (state as GenerationState.Failed).error)
  }

  @Test
  fun cancelResetsToIdle() {
    status.start("Running")
    status.cancel()
    assertEquals(GenerationState.Idle, status.state.value)
  }
}
