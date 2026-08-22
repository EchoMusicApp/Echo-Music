package com.dpi

import android.content.Context
import timber.log.Timber


class DensityScaler : BaseLifecycleContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        val scaleFactor = getScaleFactorFromPreferences(context)
        DensityConfiguration(scaleFactor).applyDensityScaling(context)
        return true
    }

    companion object {
        private const val PREFS_NAME = "echomusic_settings"
        private const val KEY_DENSITY_SCALE = "density_scale_factor"
        private const val DEFAULT_SCALE_FACTOR = 1.0f

        
        private fun getScaleFactorFromPreferences(context: Context): Float {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getFloat(KEY_DENSITY_SCALE, DEFAULT_SCALE_FACTOR)
            } catch (e: Exception) {
                Timber.tag("DensityScaler").w(e, "Failed to read scale factor from preferences")
                DEFAULT_SCALE_FACTOR
            }
        }
    }
}
