/*
 * Desktop (Skia) version of the Android-only file with the same name in the original app.
 * Original: Kyant0/backdrop v2.0.0, Apache License 2.0. Android RuntimeShader/RenderEffect -> Skia equivalents.
 */
package echo.music.iad1tya.ui.component.backdrop.internal

import androidx.compose.ui.graphics.Paint
import echo.music.iad1tya.ui.component.backdrop.RuntimeShader
import echo.music.iad1tya.ui.component.backdrop.asComposeShader
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

internal fun Paint.blur(radius: Float) {
    // Android's blur radius -> Skia sigma
    this.asFrameworkPaint().maskFilter = if (radius > 0f) MaskFilter.makeBlur(FilterBlurMode.NORMAL, radius * 0.57735f + 0.5f) else null
}

internal fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.asFrameworkPaint().shader = runtimeShader?.asComposeShader()
}
