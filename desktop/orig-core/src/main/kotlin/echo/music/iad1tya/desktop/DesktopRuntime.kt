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
import com.music.innertube.YouTube
import echo.music.iad1tya.constants.DiscordTokenKey
import echo.music.iad1tya.constants.EnableDiscordRPCKey
import echo.music.iad1tya.ui.screens.settings.DiscordPresenceManager
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.music.innertube.models.YouTubeLocale
import echo.music.iad1tya.utils.exceptionReporter

/** Builds the app's long-lived objects by hand (what Hilt did on Android). */
class DesktopRuntime {
  val context = Context()
  val database: MusicDatabase = InternalDatabase.newInstance(context.filesDir)
  val vlc = VlcBackend()
  val player = QueuePlayer(vlc).also { it.volume = savedVolume() }
  val dsp = echo.music.dsp.controller.DspControllerImpl()
  val service = MusicService(player)
  val playerConnection = PlayerConnection(service, database)
  val downloadUtil = DownloadUtil().also { it.attach(database) }

  init {
    // what Android's App.kt does at startup: YouTube needs a region and a visitor id, or Home/Explore requests fail
    val loc = java.util.Locale.getDefault()
    YouTube.locale = YouTubeLocale(gl = loc.country.ifBlank { "US" }, hl = loc.language.ifBlank { "en" })
    exceptionReporter = { t -> runCatching { java.io.File(System.getProperty("user.home"), ".echomusic/error.log").also { it.parentFile.mkdirs() }.appendText("${java.util.Date()} reported: ${t.stackTraceToString()}\n") } }
    // shortcut: visitor id is fetched every start and not cached; no login cookie is restored yet
    // blocking (max 8s) so Home does not request before it exists
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.withTimeoutOrNull(8000) { YouTube.visitorData().onSuccess { YouTube.visitorData = it }.onFailure { exceptionReporter?.invoke(it) } } }
    // Discord "listening to" status: follows the Discord settings (switch + token) like MusicService did on Android
    // shortcut: provider lambdas read the player from a background thread (read-only); fine for a status line
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default).launch {
      context.dataStore.data.map { (it[EnableDiscordRPCKey] ?: true) to (it[DiscordTokenKey] ?: "") }.distinctUntilChanged().collect { (on, token) ->
        runCatching {
          DiscordPresenceManager.stop()
          if (on && token.isNotBlank()) DiscordPresenceManager.start(
            context = context, token = token,
            songProvider = { player.currentMediaItem?.mediaId?.let { id -> kotlinx.coroutines.runBlocking { database.song(id).firstOrNull() } } },
            positionProvider = { player.currentPosition },
            isPausedProvider = { !player.isPlaying },
          )
        }.onFailure { exceptionReporter?.invoke(it) }
      }
    }
    DesktopGraph.provide(Context::class.java, context)
    DesktopGraph.provide(MusicDatabase::class.java, database)
    DesktopGraph.provide(DownloadUtil::class.java, downloadUtil)
    DesktopGraph.provide(echo.music.dsp.controller.DspController::class.java, dsp)
    // the profile that was active last time is switched on again
    CoroutineScope(Dispatchers.Default).launch {
      runCatching {
        kotlinx.coroutines.withTimeoutOrNull(3000) { DesktopGraph.resolve(echo.music.dsp.data.EQProfileRepository::class.java).activeProfile.first { it != null } }?.let { dsp.applyProfile(it) }
      }
    }
    // the equalizer page picks a profile in the DspController; VLC is told the matching 10-band curve
    CoroutineScope(Dispatchers.Default).launch {
      dsp.activeProfile.collect { p ->
        runCatching { if (p == null) vlc.setEqualizer(0f, null) else EqMapping.toVlc(p.parametricEQ).let { (pre, g) -> vlc.setEqualizer(pre, g) } }
      }
    }
    // Hilt bound this interface to its implementation; the reflection graph cannot guess that
    DesktopGraph.provide(echo.music.iad1tya.musicflow.MusicFlowFeedSource::class.java, DesktopGraph.resolve(echo.music.iad1tya.musicflow.MusicFlowRepository::class.java))
    DesktopGraph.lyricsHelper = DesktopGraph.resolve(LyricsHelper::class.java)
  }

  val syncUtils: SyncUtils = DesktopGraph.resolve(SyncUtils::class.java)
  val ringtoneViewModel = RingtoneViewModel()
}
