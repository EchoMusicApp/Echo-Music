package iad1tya.echo.music.dpi

import android.content.Context
import android.util.Log

/**
 * DensityScaler - Main entry point for screen density scaling.
 *
 * Reads scale factor from user preferences with default of 1.0f (100% native).
 *
 * Supported scale factors:
 * - 1.0f (100%) - Native density (default)
 * - 0.85f (85%) - Slightly Compact
 * - 0.75f (75%) - Compact
 * - 0.65f (65%) - Very Compact
 * - 0.55f (55%) - Ultra Compact
 */
class DensityScaler : BaseLifecycleContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        val scaleFactor = getScaleFactorFromPreferences(context)
        DensityConfiguration(scaleFactor).applyDensityScaling(context)
        return true
    }

    companion object {
        private const val PREFS_NAME = "echo_music_settings"
        private const val KEY_DENSITY_SCALE = "density_scale_factor"
        private const val DEFAULT_SCALE_FACTOR = 1.0f

        private fun getScaleFactorFromPreferences(context: Context): Float {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getFloat(KEY_DENSITY_SCALE, DEFAULT_SCALE_FACTOR)
            } catch (e: Exception) {
                Log.w("DensityScaler", "Failed to read scale factor from preferences", e)
                DEFAULT_SCALE_FACTOR
            }
        }
    }
}
