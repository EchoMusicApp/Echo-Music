package echo.music.iad1tya.desktop

import android.content.Context
import echo.music.iad1tya.db.InternalDatabase
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.di.DesktopGraph
import echo.music.iad1tya.lyrics.LyricsHelper
import echo.music.iad1tya.playback.DownloadUtil
import echo.music.iad1tya.playback.MusicService
import echo.music.iad1tya.playback.PlayerConnection
import echo.music.iad1tya.playback.QueuePlayer
import echo.music.iad1tya.ui.screens.settings.RingtoneViewModel
import echo.music.iad1tya.utils.SyncUtils

/** Builds the app's long-lived objects by hand (what Hilt did on Android). */
class DesktopRuntime {
  val context = Context()
  val database: MusicDatabase = InternalDatabase.newInstance(context.filesDir)
  val player = QueuePlayer(VlcBackend())
  val service = MusicService(player)
  val playerConnection = PlayerConnection(service, database)
  val downloadUtil = DownloadUtil()

  init {
    DesktopGraph.provide(Context::class.java, context)
    DesktopGraph.provide(MusicDatabase::class.java, database)
    DesktopGraph.provide(DownloadUtil::class.java, downloadUtil)
    DesktopGraph.lyricsHelper = DesktopGraph.resolve(LyricsHelper::class.java)
  }

  val syncUtils: SyncUtils = DesktopGraph.resolve(SyncUtils::class.java)
  val ringtoneViewModel = RingtoneViewModel()
}
