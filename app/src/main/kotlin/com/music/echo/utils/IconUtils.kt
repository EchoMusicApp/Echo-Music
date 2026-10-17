package echo.music.iad1tya.utils

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

enum class AppIconType(val value: Int, val alias: String) {
  DEFAULT(0, "MainActivityAlias"),
  HEADPHONES(1, "MainActivityHeadphones"),
  BILLIE_EILISH(2, "MainActivityBillie"),
  MICHAEL(3, "MainActivityMichael"),
  WAVE(4, "MainActivityWave")
}

object IconUtils {
  private fun component(context: Context, type: AppIconType) =
    ComponentName(context, "echo.music.iad1tya.${type.alias}")

  fun setIcon(context: Context, iconType: AppIconType) {
    val pm = context.packageManager
    // Enable the chosen alias first so there is always a launcher entry.
    pm.setComponentEnabledSetting(
      component(context, iconType),
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP
    )
    AppIconType.entries.filter { it != iconType }.forEach {
      pm.setComponentEnabledSetting(
        component(context, it),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
      )
    }
  }

  /** After an update, make sure exactly one launcher alias is on (older icons were removed). */
  fun repair(context: Context) {
    val pm = context.packageManager
    val enabled =
      AppIconType.entries.filter {
        val state = pm.getComponentEnabledSetting(component(context, it))
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
          (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && it == AppIconType.DEFAULT)
      }
    if (enabled.isEmpty()) setIcon(context, AppIconType.DEFAULT)
  }
}

class IconRepairReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) IconUtils.repair(context)
  }
}

/**
 * Status-bar / media-notification glyph that follows the launcher icon the user picked.
 * Android only draws notification icons as a single-colour silhouette, so photo icons use the
 * Pattukal headphones glyph instead of a picture.
 */
object NotificationIcon {
  @androidx.annotation.DrawableRes
  fun current(context: Context): Int {
    val pm = context.packageManager
    fun on(type: AppIconType): Boolean {
      val state =
        runCatching {
            pm.getComponentEnabledSetting(ComponentName(context, "echo.music.iad1tya.${type.alias}"))
          }
          .getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
      return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
    return when {
      on(AppIconType.WAVE) -> echo.music.iad1tya.R.drawable.ic_stat_wave
      on(AppIconType.HEADPHONES) || on(AppIconType.BILLIE_EILISH) || on(AppIconType.MICHAEL) ->
        echo.music.iad1tya.R.drawable.ic_stat_headphones
      else -> echo.music.iad1tya.R.drawable.ic_stat_official
    }
  }
}
