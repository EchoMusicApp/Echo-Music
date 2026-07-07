package iad1tya.echo.music.ui.ipod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * iPod Mode color schemes — 4 presets.
 * ponytail: data class + vals, no enum/factory/registry.
 */
data class IpodColorScheme(
    val name: String,
    val background: Color,
    val surface: Color,
    val screenBackground: Color,
    val screenText: Color,
    val screenTextSecondary: Color,
    val accent: Color,
    val wheelBackground: Color,
    val wheelButtonBackground: Color,
    val wheelText: Color,
    val selectedHighlight: Color,
    val divider: Color,
)

val ClassicWhite = IpodColorScheme(
    name = "Classic White",
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    screenBackground = Color(0xFFFFFFFF),
    screenText = Color(0xFF1D1D1F),
    screenTextSecondary = Color(0xFF86868B),
    accent = Color(0xFF007AFF),
    wheelBackground = Color(0xFFE8E8ED),
    wheelButtonBackground = Color(0xFFFFFFFF),
    wheelText = Color(0xFF1D1D1F),
    selectedHighlight = Color(0xFF007AFF),
    divider = Color(0xFFD2D2D7),
)

val ClassicBlack = IpodColorScheme(
    name = "Classic Black",
    background = Color(0xFF1D1D1F),
    surface = Color(0xFF2C2C2E),
    screenBackground = Color(0xFF000000),
    screenText = Color(0xFFFFFFFF),
    screenTextSecondary = Color(0xFF98989D),
    accent = Color(0xFF0A84FF),
    wheelBackground = Color(0xFF3A3A3C),
    wheelButtonBackground = Color(0xFF2C2C2E),
    wheelText = Color(0xFFFFFFFF),
    selectedHighlight = Color(0xFF0A84FF),
    divider = Color(0xFF48484A),
)

val SpaceGray = IpodColorScheme(
    name = "Space Gray",
    background = Color(0xFF2C2C2E),
    surface = Color(0xFF3A3A3C),
    screenBackground = Color(0xFF1C1C1E),
    screenText = Color(0xFFE5E5EA),
    screenTextSecondary = Color(0xFF8E8E93),
    accent = Color(0xFF64D2FF),
    wheelBackground = Color(0xFF48484A),
    wheelButtonBackground = Color(0xFF3A3A3C),
    wheelText = Color(0xFFE5E5EA),
    selectedHighlight = Color(0xFF64D2FF),
    divider = Color(0xFF545456),
)

val TransparentGlass = IpodColorScheme(
    name = "Transparent Glass",
    background = Color(0xCC1C1C1E),
    surface = Color(0x992C2C2E),
    screenBackground = Color(0xB3000000),
    screenText = Color(0xFFFFFFFF),
    screenTextSecondary = Color(0xCCAAAAB0),
    accent = Color(0xFFBF5AF2),
    wheelBackground = Color(0x66636366),
    wheelButtonBackground = Color(0x4D3A3A3C),
    wheelText = Color(0xFFFFFFFF),
    selectedHighlight = Color(0xFFBF5AF2),
    divider = Color(0x4DFFFFFF),
)

val IpodThemes = listOf(ClassicWhite, ClassicBlack, SpaceGray, TransparentGlass)

fun ipodThemeByName(name: String): IpodColorScheme =
    IpodThemes.find { it.name == name } ?: ClassicBlack

val LocalIpodColors = staticCompositionLocalOf { ClassicBlack }

@Composable
fun IpodTheme(
    colorScheme: IpodColorScheme = ClassicBlack,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIpodColors provides colorScheme) {
        content()
    }
}
