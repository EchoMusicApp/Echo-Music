package iad1tya.echo.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconUtils {
    fun setIcon(context: Context, dynamicEnabled: Boolean) {
        val pm = context.packageManager
        val dynamicAlias = ComponentName(context, "iad1tya.echo.music.MainActivityAlias")
        val staticAlias = ComponentName(context, "iad1tya.echo.music.MainActivityStatic")

        pm.setComponentEnabledSetting(
            dynamicAlias,
            if (dynamicEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            staticAlias,
            if (dynamicEnabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
