package echo.music.iad1tya.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import echo.music.iad1tya.R
import timber.log.Timber

fun <I> ActivityResultLauncher<I>.launchSafely(
  input: I,
  onUnavailable: (Exception) -> Unit,
): Boolean {
  val failure =
    try {
      launch(input)
      return true
    } catch (e: ActivityNotFoundException) {
      e
    } catch (e: SecurityException) {
      e
    }

  Timber.w(failure, "System picker launch unavailable")
  onUnavailable(failure)
  return false
}

fun <I> ActivityResultLauncher<I>.launchSafely(input: I, context: Context): Boolean =
  launchSafely(input) {
    Toast.makeText(context, R.string.system_picker_unavailable, Toast.LENGTH_LONG).show()
  }
