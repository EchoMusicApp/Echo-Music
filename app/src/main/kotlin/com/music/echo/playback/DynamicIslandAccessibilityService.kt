/**
 * Echo Music
 * Optional AccessibilityService enabling TYPE_ACCESSIBILITY_OVERLAY so the floating
 * Origin / Dynamic Island capsule sits above the system status bar and receives taps everywhere.
 */

package echo.music.iad1tya.playback

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class DynamicIslandAccessibilityService : AccessibilityService() {

  companion object {
    @Volatile var instance: DynamicIslandAccessibilityService? = null
      private set

    @Volatile var onServiceStateChanged: (() -> Unit)? = null

    fun isServiceEnabled(context: Context): Boolean {
      if (instance != null) return true
      return try {
        val enabledServices =
          Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
          ) ?: return false
        enabledServices.contains("${context.packageName}/${DynamicIslandAccessibilityService::class.java.name}")
      } catch (_: Exception) {
        false
      }
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    onServiceStateChanged?.invoke()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Only used for TYPE_ACCESSIBILITY_OVERLAY window placement over status bar.
  }

  override fun onInterrupt() {
    // Nothing to clean up on interrupt.
  }

  override fun onUnbind(intent: Intent?): Boolean {
    instance = null
    onServiceStateChanged?.invoke()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    instance = null
    onServiceStateChanged?.invoke()
    super.onDestroy()
  }
}
