

package echo.music.iad1tya.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import echo.music.iad1tya.R


fun UriHandler.safeOpenUri(context: Context, uri: String) {
    if (uri.isBlank()) return
    
    runCatching {
        openUri(uri)
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.error_no_stream).replace("stream", "app"), 
            Toast.LENGTH_SHORT
        ).show()
    }
}
