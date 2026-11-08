package echo.music.iad1tya

import android.content.Context
import androidx.datastore.preferences.core.edit
import echo.music.iad1tya.constants.AccountChannelHandleKey
import echo.music.iad1tya.constants.AccountEmailKey
import echo.music.iad1tya.constants.AccountNameKey
import echo.music.iad1tya.constants.DataSyncIdKey
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.VisitorDataKey
import echo.music.iad1tya.innertube.YouTube
import echo.music.iad1tya.utils.dataStore

// shortcut: desktop version of the Android Application class; only the account sign-out lives here (no WebView cookies to clear).
class App {
  companion object {
    suspend fun forgetAccount(context: Context) {
      context.dataStore.edit { settings ->
        settings.remove(InnerTubeCookieKey)
        settings.remove(VisitorDataKey)
        settings.remove(DataSyncIdKey)
        settings.remove(AccountNameKey)
        settings.remove(AccountEmailKey)
        settings.remove(AccountChannelHandleKey)
      }
      YouTube.cookie = null
      YouTube.visitorData = null
      YouTube.dataSyncId = null
    }
  }
}
