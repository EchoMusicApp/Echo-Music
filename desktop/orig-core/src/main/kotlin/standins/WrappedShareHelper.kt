package com.music.echo.ui.screens.wrapped

import androidx.compose.ui.graphics.Color
import android.content.Context

/**
 * Authentic Spotify Wrapped colorway themes for recap posters.
 */
enum class WrappedPosterTheme(
  val displayName: String,
  val composeBg: Color,
  val composeAccent: Color,
  val composeHeadline: Color,
  val composeSecondary: Color,
  val bgInt: Int,
  val headlineInt: Int,
  val accentInt: Int,
  val secondaryInt: Int
) {
  VIOLET_MINT(
    displayName = "Neon Violet",
    composeBg = Color(0xFF2810C0),
    composeAccent = Color(0xFF00F5D4),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFD8B4FE),
    bgInt = 0xFF2810C0.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFF00F5D4.toInt(),
    secondaryInt = 0xFFD8B4FE.toInt()
  ),
  OBSIDIAN_GREEN(
    displayName = "Spotify Obsidian",
    composeBg = Color(0xFF121212),
    composeAccent = Color(0xFF1DB954),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFA7A7A7),
    bgInt = 0xFF121212.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFF1DB954.toInt(),
    secondaryInt = 0xFFA7A7A7.toInt()
  ),
  FIRE_RED(
    displayName = "Crimson Gold",
    composeBg = Color(0xFFC41E3A),
    composeAccent = Color(0xFFFFDF00),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFFECACA),
    bgInt = 0xFFC41E3A.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFFFFDF00.toInt(),
    secondaryInt = 0xFFFECACA.toInt()
  ),
  COBALT_BLUE(
    displayName = "Electric Cobalt",
    composeBg = Color(0xFF1E3A8A),
    composeAccent = Color(0xFF38BDF8),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFBFDBFE),
    bgInt = 0xFF1E3A8A.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFF38BDF8.toInt(),
    secondaryInt = 0xFFBFDBFE.toInt()
  ),
  SUNSET_ORANGE(
    displayName = "Sunset Tangerine",
    composeBg = Color(0xFFC2410C),
    composeAccent = Color(0xFFFFFBEB),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFFED7AA),
    bgInt = 0xFFC2410C.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFFFFFBEB.toInt(),
    secondaryInt = 0xFFFED7AA.toInt()
  )
}

// shortcut: sharing the recap as a poster image needs Android canvas drawing; not ported yet.
object WrappedShareHelper {
  suspend fun shareWrappedSummary(
    context: Context,
    data: WrappedData,
    posterTheme: WrappedPosterTheme = WrappedPosterTheme.VIOLET_MINT,
    isStoryFormat: Boolean = true,
  ) {}
}
