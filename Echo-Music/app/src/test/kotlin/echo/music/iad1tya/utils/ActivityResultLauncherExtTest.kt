package echo.music.iad1tya.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ActivityResultLauncherExtTest {
  @Test
  fun successfulLaunchForwardsInputOnceWithoutFailureCallback() {
    val input = "playlist_export.csv"
    val launcher = FakeLauncher<String>()
    var failureCalls = 0

    assertTrue(launcher.launchSafely(input) { failureCalls++ })

    assertEquals(1, launcher.launchCalls)
    assertSame(input, launcher.lastInput)
    assertNull(launcher.lastOptions)
    assertEquals(0, failureCalls)
  }

  @Test
  fun nullTreeInputIsForwardedUnchanged() {
    val launcher = FakeLauncher<Uri?>()

    assertTrue(launcher.launchSafely(null) { throw AssertionError("Unexpected failure", it) })

    assertEquals(1, launcher.launchCalls)
    assertNull(launcher.lastInput)
  }

  @Test
  fun mimeArrayIsForwardedUnchanged() {
    val input = arrayOf("text/csv", "text/plain")
    val launcher = FakeLauncher<Array<String>>()

    assertTrue(launcher.launchSafely(input) { throw AssertionError("Unexpected failure", it) })

    assertEquals(1, launcher.launchCalls)
    assertSame(input, launcher.lastInput)
  }

  @Test
  fun photoRequestIsForwardedUnchanged() {
    val input = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val launcher = FakeLauncher<PickVisualMediaRequest>()

    assertTrue(launcher.launchSafely(input) { throw AssertionError("Unexpected failure", it) })

    assertEquals(1, launcher.launchCalls)
    assertSame(input, launcher.lastInput)
  }

  @Test
  fun missingActivityInvokesFailureCallbackOnceWithoutRetrying() {
    assertHandled(ActivityNotFoundException("No document picker"))
  }

  @Test
  fun securityDenialInvokesFailureCallbackOnceWithoutRetrying() {
    assertHandled(SecurityException("Picker access denied"))
  }

  @Test
  fun unrelatedLaunchExceptionPropagatesWithoutFailureCallback() {
    val failure = IllegalStateException("Launcher is not registered")
    val launcher = FakeLauncher<String>(failure)
    var failureCalls = 0

    val thrown = assertThrows(IllegalStateException::class.java) {
      launcher.launchSafely("text/plain") { failureCalls++ }
    }

    assertSame(failure, thrown)
    assertEquals(1, launcher.launchCalls)
    assertEquals(0, failureCalls)
  }

  @Test
  fun failureCallbackExceptionPropagatesWithoutBeingHandledAgain() {
    val launcher = FakeLauncher<String>(ActivityNotFoundException("No document picker"))
    val callbackFailure = SecurityException("Fallback failed")
    var failureCalls = 0

    val thrown = assertThrows(SecurityException::class.java) {
      launcher.launchSafely("text/plain") {
        failureCalls++
        throw callbackFailure
      }
    }

    assertSame(callbackFailure, thrown)
    assertEquals(1, launcher.launchCalls)
    assertEquals(1, failureCalls)
  }

  private fun assertHandled(failure: Exception) {
    val launcher = FakeLauncher<String>(failure)
    var failureCalls = 0
    var reportedFailure: Exception? = null

    val launched = launcher.launchSafely("text/plain") {
      failureCalls++
      reportedFailure = it
    }

    assertFalse(launched)
    assertEquals(1, launcher.launchCalls)
    assertEquals(1, failureCalls)
    assertSame(failure, reportedFailure)
  }

  private class FakeLauncher<I>(private val failure: Exception? = null) : ActivityResultLauncher<I>() {
    var launchCalls = 0
      private set
    var lastInput: I? = null
      private set
    var lastOptions: ActivityOptionsCompat? = null
      private set

    override fun launch(input: I, options: ActivityOptionsCompat?) {
      launchCalls++
      lastInput = input
      lastOptions = options
      failure?.let { throw it }
    }

    override fun unregister() = Unit

    override val contract: ActivityResultContract<I, *> =
      object : ActivityResultContract<I, Unit>() {
        override fun createIntent(context: Context, input: I): Intent = Intent()

        override fun parseResult(resultCode: Int, intent: Intent?) = Unit
      }
  }
}
