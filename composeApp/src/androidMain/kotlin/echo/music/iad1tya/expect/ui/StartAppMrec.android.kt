package echo.music.iad1tya.expect.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Mrec

@Composable
actual fun StartAppMrec(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Mrec(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                loadAd()
            }
        }
    )
}
