package echo.music.iad1tya.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Banner

@Composable
actual fun StartAppBanner(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Banner(context)
        }
    )
}
