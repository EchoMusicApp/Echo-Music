package iad1tya.echo.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * ArchiveTune Lyrics V2 compatibility wrapper.
 * This keeps the V2 toggle/path functional in Echo while reusing the robust Lyrics engine.
 */
@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    Lyrics(
        sliderPositionProvider = sliderPositionProvider,
        modifier = modifier,
        isVisible = true,
    )
}
