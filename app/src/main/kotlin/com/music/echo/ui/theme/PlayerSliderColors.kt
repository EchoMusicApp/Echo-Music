

package iad1tya.echo.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import iad1tya.echo.music.constants.PlayerBackgroundStyle


object PlayerSliderColors {

    
    @Composable
    fun getSliderColors(
        activeColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ): SliderColors {
        val inactiveTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> {
                if (useDarkTheme) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            }
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> {
                Color.White.copy(alpha = 0.4f)
            }
        }
        
        return SliderDefaults.colors(
            activeTrackColor = activeColor,
            activeTickColor = activeColor,
            thumbColor = activeColor,
            inactiveTrackColor = inactiveTrackColor,
            disabledActiveTrackColor = activeColor,
            disabledInactiveTrackColor = inactiveTrackColor,
            disabledThumbColor = activeColor
        )
    }
}
