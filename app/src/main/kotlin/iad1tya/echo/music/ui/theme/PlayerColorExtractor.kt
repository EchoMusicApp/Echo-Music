package iad1tya.echo.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Player color extraction system for generating gradients from album artwork
 * 
 * This system analyzes album artwork and extracts vibrant, dominant colors
 * to create visually appealing gradients for the music player interface.
 */
object PlayerColorExtractor {

    /**
     * Extracts colors from a palette and creates a gradient
     * 
     * @param palette The color palette extracted from album artwork
     * @param fallbackColor Fallback color to use if extraction fails
     * @return List of colors for gradient (primary, darker variant, black)
     */
    suspend fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int
    ): List<Color> = withContext(Dispatchers.Default) {
        
        // Extract all available colors with priority for dominant colors
        val colorCandidates = listOfNotNull(
            palette.dominantSwatch, // High priority for dominant color
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch
        )

        // Select best color based on weight (dominance + vibrancy)
        val bestSwatch = colorCandidates.maxByOrNull { calculateColorWeight(it) }
        val fallbackDominant = palette.dominantSwatch?.rgb?.let { Color(it) }
            ?: Color(palette.getDominantColor(fallbackColor))

        val primaryColor = if (bestSwatch != null) {
            val bestColor = Color(bestSwatch.rgb)
            // Ensure the color is suitable for use
            if (isColorVibrant(bestColor)) {
                enhanceColorVividness(bestColor, 1.5f)
            } else {
                // If not vibrant, use dominant color with slight enhancement
                enhanceColorVividness(fallbackDominant, 1.1f)
            }
        } else {
            enhanceColorVividness(fallbackDominant, 1.1f)
        }
        
        // Create sophisticated gradient with 3 color points for better contrast
        listOf(
            primaryColor, // Start: primary vibrant color
            primaryColor.copy(
                red = (primaryColor.red * 0.5f).coerceAtLeast(0f),
                green = (primaryColor.green * 0.5f).coerceAtLeast(0f),
                blue = (primaryColor.blue * 0.5f).coerceAtLeast(0f)
            ) // End: Darker version of primary color (50% brightness)
        )
    }

    /**
     * Extracts a rich palette of 4 colors for the animated gradient background
     */
    suspend fun extractRichGradientColors(
        palette: Palette,
        fallbackColor: Int
    ): List<Color> = withContext(Dispatchers.Default) {
        
        // Extract all available colors
        val dominant = palette.dominantSwatch?.let { Color(it.rgb) }
        val vibrant = palette.vibrantSwatch?.let { Color(it.rgb) }
        val darkVibrant = palette.darkVibrantSwatch?.let { Color(it.rgb) }
        val lightVibrant = palette.lightVibrantSwatch?.let { Color(it.rgb) }
        val muted = palette.mutedSwatch?.let { Color(it.rgb) }
        val darkMuted = palette.darkMutedSwatch?.let { Color(it.rgb) }
        
        val fallback = Color(fallbackColor)
        val defaultColor = dominant ?: fallback

        // Helper to get a valid color or fallback, possibly enhancing it
        fun getColor(color: Color?, enhance: Boolean = true): Color {
            return if (color != null) {
                if (enhance) enhanceColorVividness(color, 1.2f) else color
            } else {
                enhanceColorVividness(defaultColor, 1.1f)
            }
        }

        // We want a rich mix of colors. 
        // 1. Primary base (Dominant or Vibrant)
        // 2. Secondary accent (Vibrant or Light Vibrant)
        // 3. Tertiary depth (Dark Vibrant or Dark Muted)
        // 4. Quaternary highlight (Light Vibrant or Muted)
        
        val c1 = if (vibrant != null) getColor(vibrant) else getColor(dominant)
        val c2 = if (darkVibrant != null) getColor(darkVibrant) else getColor(muted).copy(alpha = 0.8f)
        val c3 = if (lightVibrant != null) getColor(lightVibrant) else getColor(dominant).copy(alpha = 0.6f)
        val c4 = if (dominant != null && dominant != c1) getColor(dominant) else c1.copy(red = c1.red * 0.8f)

        listOf(c1, c2, c3, c4)
    }

    /**
     * Determines if a color is vibrant enough for use in player UI
     * 
     * @param color The color to analyze
     * @return true if the color has sufficient saturation and brightness
     */
    private fun isColorVibrant(color: Color): Boolean {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1] // HSV[1] is saturation
        val brightness = hsv[2] // HSV[2] is brightness
        
        // Color is vibrant if it has sufficient saturation and appropriate brightness
        // Avoid colors that are too dark or too bright
        return saturation > 0.25f && brightness > 0.2f && brightness < 0.9f
    }
    
    /**
     * Enhances color vividness by adjusting saturation and brightness
     * 
     * @param color The color to enhance
     * @param saturationFactor Factor to multiply saturation by (default 1.4)
     * @return Enhanced color with improved vividness
     */
    private fun enhanceColorVividness(color: Color, saturationFactor: Float = 1.4f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        
        // Increase saturation for more vivid colors
        hsv[1] = (hsv[1] * saturationFactor).coerceAtMost(1.0f)
        // Adjust brightness for better visibility
        hsv[2] = (hsv[2] * 0.9f).coerceIn(0.4f, 0.85f)
        
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Calculates weight for color selection based on dominance and vibrancy
     * 
     * @param swatch The palette swatch to analyze
     * @return Weight value for color selection priority
     */
    private fun calculateColorWeight(swatch: Palette.Swatch?): Float {
        if (swatch == null) return 0f
        val population = swatch.population.toFloat()
        val color = Color(swatch.rgb)
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1]
        val brightness = hsv[2]
        
        // Give higher priority to dominance (population) while considering vibrancy
        val populationWeight = population * 2f // Double dominance weight
        val vibrancyBonus = if (saturation > 0.3f && brightness > 0.3f) 1.5f else 1f
        
        return populationWeight * vibrancyBonus * (saturation + brightness) / 2f
    }

    /**
     * Configuration constants for color extraction
     */
    object Config {
        const val MAX_COLOR_COUNT = 32
        const val BITMAP_AREA = 8000
        const val IMAGE_SIZE = 200
        
        // Color enhancement factors
        const val VIBRANT_SATURATION_THRESHOLD = 0.25f
        const val VIBRANT_BRIGHTNESS_MIN = 0.2f
        const val VIBRANT_BRIGHTNESS_MAX = 0.9f
        
        const val POPULATION_WEIGHT_MULTIPLIER = 2f
        const val VIBRANCY_THRESHOLD_SATURATION = 0.3f
        const val VIBRANCY_THRESHOLD_BRIGHTNESS = 0.3f
        const val VIBRANCY_BONUS = 1.5f
        
        const val DEFAULT_SATURATION_FACTOR = 1.4f
        const val VIBRANT_SATURATION_FACTOR = 1.3f
        const val FALLBACK_SATURATION_FACTOR = 1.1f
        
        const val BRIGHTNESS_MULTIPLIER = 0.9f
        const val BRIGHTNESS_MIN = 0.4f
        const val BRIGHTNESS_MAX = 0.85f
        
        const val DARKER_VARIANT_FACTOR = 0.6f
    }
}