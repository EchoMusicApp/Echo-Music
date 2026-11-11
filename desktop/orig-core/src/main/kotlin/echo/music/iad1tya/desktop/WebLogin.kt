package echo.music.iad1tya.desktop

import java.net.CookieHandler
import java.net.CookieManager
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.WindowConstants

/**
 * Desktop stand-in for Android's login WebView: opens [url] in a small browser window and, once
 * [ready] says the wanted cookies exist, closes it and hands them to [onDone].
 * Returns a function that closes the window early.
 */
object WebLogin {
  private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

  fun open(url: String, title: String, domain: String, ready: (Map<String, String>) -> Boolean, onDone: (Map<String, String>) -> Unit): () -> Unit {
    // JavaFX keeps its cookies in the default handler; install one we can read (HttpOnly cookies included)
    val jar = (CookieHandler.getDefault() as? CookieManager) ?: CookieManager().also { CookieHandler.setDefault(it) }
    jar.cookieStore.removeAll() // fresh sign-in every time
    var frame: JFrame? = null
    var timer: Timer? = null
    val close = { SwingUtilities.invokeLater { timer?.stop(); frame?.dispose(); frame = null } }
    SwingUtilities.invokeLater {
      val panel = JFXPanel() // also starts the JavaFX runtime
      Platform.setImplicitExit(false)
      frame = JFrame(title).apply {
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        add(panel); setSize(520, 760); setLocationRelativeTo(null); isVisible = true
      }
      Platform.runLater {
        val web = WebView()
        web.engine.userAgent = UA
        web.engine.load(url)
        panel.scene = Scene(web)
      }
      timer = Timer(1000) {
        val got = jar.cookieStore.cookies.filter { domain in it.domain }.associate { it.name to it.value }
        if (ready(got)) { close(); onDone(got) }
      }.also { it.start() }
    }
    return close
  }
}
