package echo.music.iad1tya.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import echo.music.iad1tya.cast.CastIconButton

@Composable
actual fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
) {
    CastIconButton(modifier = modifier, tint = tint)
}
