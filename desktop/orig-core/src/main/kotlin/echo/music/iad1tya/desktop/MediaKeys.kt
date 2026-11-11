package echo.music.iad1tya.desktop

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import echo.music.iad1tya.playback.PlayerConnection
import javax.swing.SwingUtilities

/**
 * Keyboard media keys (play/pause, next, previous, stop) even when Echo Music is in the background.
 * shortcut: Windows only, plain hot keys; no Windows media overlay (the volume-flyout song card) yet.
 */
object MediaKeys {
  private const val WM_HOTKEY = 0x0312
  private const val MOD_NOREPEAT = 0x4000
  private const val VK_MEDIA_NEXT_TRACK = 0xB0
  private const val VK_MEDIA_PREV_TRACK = 0xB1
  private const val VK_MEDIA_STOP = 0xB2
  private const val VK_MEDIA_PLAY_PAUSE = 0xB3

  fun install(connection: PlayerConnection) {
    if (!System.getProperty("os.name").lowercase().contains("win")) return
    Thread {
      runCatching {
        val u = User32.INSTANCE
        // a hot key belongs to the thread that registered it, so this thread also runs the message loop
        mapOf(1 to VK_MEDIA_PLAY_PAUSE, 2 to VK_MEDIA_NEXT_TRACK, 3 to VK_MEDIA_PREV_TRACK, 4 to VK_MEDIA_STOP)
          .forEach { (id, vk) -> u.RegisterHotKey(null, id, MOD_NOREPEAT, vk) }
        val msg = WinUser.MSG()
        while (u.GetMessage(msg, null, 0, 0) > 0) {
          if (msg.message == WM_HOTKEY) {
            // the player is used from the UI thread only
            SwingUtilities.invokeLater {
              when (msg.wParam.toInt()) {
                1 -> connection.togglePlayPause()
                2 -> connection.seekToNext()
                3 -> connection.seekToPrevious()
                4 -> connection.player.pause()
              }
            }
          }
        }
      }
    }.apply { isDaemon = true; name = "media-keys" }.start()
  }
}
