/*
 * Desktop (Skia) version of the Android-only file with the same name in the original app.
 * Original: Kyant0/backdrop v2.0.0, Apache License 2.0. Android RuntimeShader/RenderEffect -> Skia equivalents.
 */
package echo.music.iad1tya.ui.component.backdrop

/** Skia always supports render effects and runtime shaders. */
fun isRenderEffectSupported(): Boolean = true

fun isRuntimeShaderSupported(): Boolean = true
