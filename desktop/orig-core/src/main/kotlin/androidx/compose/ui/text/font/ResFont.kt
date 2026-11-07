package androidx.compose.ui.text.font

import echo.music.iad1tya.RTable

// Desktop version of Android's Font(resId, ...): loads the .ttf from the classpath (see tools/gen_res.py).
// shortcut: variable-font settings are ignored, so weights/width come from the font's default instance.
@Suppress("UNUSED_PARAMETER")
fun Font(
    resId: Int,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings? = null,
): Font = Font("font/" + (RTable.fonts[resId] ?: error("no font with id $resId")), weight, style)
