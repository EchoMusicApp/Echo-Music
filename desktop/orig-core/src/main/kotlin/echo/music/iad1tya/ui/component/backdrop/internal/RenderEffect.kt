/*
 * Desktop (Skia) version of the Android-only file with the same name in the original app.
 * Original: Kyant0/backdrop v2.0.0, Apache License 2.0. Android RuntimeShader/RenderEffect -> Skia equivalents.
 */
package echo.music.iad1tya.ui.component.backdrop.internal

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asSkiaColorFilter
import androidx.compose.ui.graphics.skiaImageFilter
import echo.music.iad1tya.ui.component.backdrop.RuntimeShader
import echo.music.iad1tya.ui.component.backdrop.asSkiaBuilder
import org.jetbrains.skia.ImageFilter

internal fun RenderEffect?.chain(other: RenderEffect): RenderEffect =
    if (this != null) ImageFilter.makeCompose(other.skiaImageFilter, this.skiaImageFilter).asComposeRenderEffect() else other

internal fun RuntimeShaderEffect(runtimeShader: RuntimeShader, uniformShaderName: String): RenderEffect =
    ImageFilter.makeRuntimeShader(runtimeShader.asSkiaBuilder(), uniformShaderName, null).asComposeRenderEffect()

internal fun ColorFilterEffect(renderEffect: RenderEffect?, colorFilter: ColorFilter): RenderEffect =
    ImageFilter.makeColorFilter(colorFilter.asSkiaColorFilter(), renderEffect?.skiaImageFilter, null).asComposeRenderEffect()
