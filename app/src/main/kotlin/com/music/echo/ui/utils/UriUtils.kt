package echo.music.iad1tya.ui.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import echo.music.iad1tya.R

fun UriHandler.safeOpenUri(context: Context, uri: String) {
  if (uri.isBlank()) return

  runCatching { openUri(uri) }
    .onFailure {
      context.safeOpenUri(uri)
    }
}

fun Context.safeOpenUri(uri: Uri) {
  safeOpenUri(uri.toString())
}

fun Context.safeOpenUri(url: String) {
  if (url.isBlank()) return
  try {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    try {
      val chooser =
        Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(url)), null).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      startActivity(chooser)
    } catch (_: Exception) {
      copyUrlToClipboardAndToast(url)
    }
  } catch (_: Exception) {
    copyUrlToClipboardAndToast(url)
  }
}

private fun Context.copyUrlToClipboardAndToast(url: String) {
  runCatching {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("URL", url)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(this, getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
  }
    .onFailure {
      Toast.makeText(
          this,
          getString(R.string.error_no_stream).replace("stream", "app"),
          Toast.LENGTH_SHORT,
        )
        .show()
    }
}
