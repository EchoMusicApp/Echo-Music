package echo.music.iad1tya.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import echo.music.iad1tya.constants.AppFont
import echo.music.iad1tya.constants.SelectedFontKey
import echo.music.iad1tya.utils.rememberPreference

// Pattukal brand violet.
val DefaultThemeColor = Color(0xFF9C67FF)

private val PattukalBackground = Color(0xFF090812)

/** Nature theme: dark forest greens. */
val NatureThemeColor = Color(0xFF3FAE6A)

/** Lava Black: true black surfaces with a molten-orange accent. */
val LavaThemeColor = Color(0xFFFF5A12)

private fun ColorScheme.lavaSurfaces(): ColorScheme =
  copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2B1710),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0D0705),
    surfaceContainer = Color(0xFF140B07),
    surfaceContainerHigh = Color(0xFF1C100A),
    surfaceContainerHighest = Color(0xFF261710),
    onBackground = Color(0xFFFFEDE3),
    onSurface = Color(0xFFFFEDE3),
    onSurfaceVariant = Color(0xFFD9B8A6),
    outlineVariant = Color(0xFF4A2A1B),
  )

private fun ColorScheme.natureSurfaces(): ColorScheme =
  copy(
    background = Color(0xFF04110A),
    surface = Color(0xFF04110A),
    surfaceDim = Color(0xFF030D08),
    surfaceBright = Color(0xFF1F3A2B),
    surfaceContainerLowest = Color(0xFF030C07),
    surfaceContainerLow = Color(0xFF08180F),
    surfaceContainer = Color(0xFF0C2015),
    surfaceContainerHigh = Color(0xFF12291C),
    surfaceContainerHighest = Color(0xFF193425),
    onBackground = Color(0xFFEAF7EE),
    onSurface = Color(0xFFEAF7EE),
    onSurfaceVariant = Color(0xFFB4CDBC),
    outlineVariant = Color(0xFF2F4A39),
  )

/** Deep violet-black surfaces that give the dark theme its Pattukal identity. */
private fun ColorScheme.pattukalSurfaces(): ColorScheme =
  copy(
    background = PattukalBackground,
    surface = PattukalBackground,
    surfaceDim = Color(0xFF070610),
    surfaceBright = Color(0xFF2A2640),
    surfaceContainerLowest = Color(0xFF06050D),
    surfaceContainerLow = Color(0xFF0E0C1A),
    surfaceContainer = Color(0xFF141222),
    surfaceContainerHigh = Color(0xFF1B182B),
    surfaceContainerHighest = Color(0xFF232034),
    onBackground = Color(0xFFF8F4FF),
    onSurface = Color(0xFFF8F4FF),
    onSurfaceVariant = Color(0xFFC4BDDB),
    outlineVariant = Color(0xFF3A3552),
  )

@Composable
fun echomusicTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  pureBlack: Boolean = false,
  themeColor: Color = DefaultThemeColor,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val selectedFontValue by rememberPreference(SelectedFontKey, AppFont.VESPER.value)
  val natureTheme by rememberPreference(echo.music.iad1tya.constants.NatureThemeKey, false)
  val lavaTheme by rememberPreference(echo.music.iad1tya.constants.LavaThemeKey, false)
  val customFontPathValue by rememberPreference(echo.music.iad1tya.constants.CustomFontPathKey, "")

  val brandFont =
    remember(selectedFontValue, customFontPathValue) {
      when (AppFont.fromValue(selectedFontValue)) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.VESPER -> VesperFraktur
        AppFont.GOOGLE_SANS -> GoogleSansFontFamily
        AppFont.SANS_FLEX -> SansFlexFontFamily
        AppFont.OUTFIT -> OutfitFontFamily
        AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
        AppFont.CUSTOM -> {
          try {
            if (customFontPathValue.isNotEmpty() && java.io.File(customFontPathValue).exists()) {
              val typeface = android.graphics.Typeface.createFromFile(customFontPathValue)
              FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
            } else {
              FontFamily.Default
            }
          } catch (e: Exception) {
            FontFamily.Default
          }
        }
        else -> FontFamily.Default
      }
    }

  val useSystemDynamicColor =
    false

  val baseColorScheme =
    if (useSystemDynamicColor) {

      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {

      rememberDynamicColorScheme(
        seedColor = if (lavaTheme && darkTheme) LavaThemeColor else if (natureTheme && darkTheme) NatureThemeColor else themeColor,
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot
      )
    }

  val colorScheme =
    remember(baseColorScheme, pureBlack, darkTheme, natureTheme, lavaTheme) {
      when {
        darkTheme && lavaTheme -> baseColorScheme.lavaSurfaces()
        darkTheme && pureBlack -> baseColorScheme.pureBlack(true)
        darkTheme && natureTheme -> baseColorScheme.natureSurfaces()
        darkTheme -> baseColorScheme.pattukalSurfaces()
        else -> baseColorScheme
      }
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = getTypography(brandFont),
    shapes =
      androidx.compose.material3.MaterialTheme.shapes.copy(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
      ),
    content = {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides echo.music.iad1tya.ui.utils.PattukalIndication,
        content = content
      )
    }
  )
}

fun Bitmap.extractThemeColor(): Color {
  val colorsToPopulation =
    Palette.from(this).maximumColorCount(8).generate().swatches.associate {
      it.rgb to it.population
    }
  val rankedColors = Score.score(colorsToPopulation)
  return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
  val extractedColors =
    Palette.from(this).maximumColorCount(64).generate().swatches.associate {
      it.rgb to it.population
    }

  val orderedColors =
    Score.score(extractedColors, 2, 0xff4285f4.toInt(), true).sortedByDescending {
      Color(it).luminance()
    }

  return if (orderedColors.size >= 2) listOf(Color(orderedColors[0]), Color(orderedColors[1]))
  else listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
  if (apply) copy(surface = Color.Black, background = Color.Black) else this

val ColorSaver =
  object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)

    override fun SaverScope.save(value: Color): Int = value.toArgb()
  }
