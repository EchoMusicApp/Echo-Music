package echo.music.iad1tya.playback

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import java.security.MessageDigest
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.pages.HomePage
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AutoplayOnAutomotiveConnectKey
import echo.music.iad1tya.constants.DataSyncIdKey
import echo.music.iad1tya.constants.HideExplicitKey
import echo.music.iad1tya.constants.HideVideoSongsKey
import echo.music.iad1tya.constants.MediaSessionConstants
import echo.music.iad1tya.constants.SongSortType
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.extensions.toggleRepeatMode
import echo.music.iad1tya.utils.AutomotiveArtwork.withAutomotiveArtworkUri
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import echo.music.iad1tya.utils.reportException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@androidx.annotation.OptIn(UnstableApi::class)
class MediaLibrarySessionCallback
@Inject
constructor(
  @ApplicationContext val context: Context,
  val database: MusicDatabase,
  val downloadUtil: DownloadUtil,
) : MediaLibrarySession.Callback {
  private val scope = CoroutineScope(Dispatchers.Main) + Job()
  lateinit var service: MusicService
  var toggleLike: () -> Unit = {}
  var toggleStartRadio: () -> Unit = {}
  var toggleLibrary: () -> Unit = {}

  /**
   * Invoked when an automotive controller connects. Used to embed the artwork of
   * the item already playing, which otherwise never goes through a media-item
   * transition while the car is connected.
   */
  var onAutomotiveConnected: () -> Unit = {}

  /** Cached YouTube Home snapshot (see [youtubeHome]) and when it was fetched. */
  private var cachedHome: HomePage? = null
  private var cachedHomeAtMs = 0L

  /** Disk copy of [cachedHome], so the shelves survive process restarts. */
  private val homeStore by lazy { AutoHomeSnapshotStore(context) }
  /** Account the cached snapshot belongs to (see [homeAccountKey]); null until first use. */
  private var homeAccount: String? = null
  /** True while [cachedHome] is the restored copy and still needs a fresh fetch. */
  private var cachedHomeFromDisk = false
  /** Wall-clock time each shelf was last returned by YouTube, by [homeSectionKey]. */
  private var homeLastSeenMs: Map<String, Long> = emptyMap()
  private val homeRefreshing = java.util.concurrent.atomic.AtomicBoolean(false)

  /** Serializes the Home snapshot refresh so concurrent misses cannot race. */
  private val homeMutex = Mutex()

  /**
   * Package names [MediaSession.isAutomotiveController] and
   * [MediaSession.isAutoCompanionController] treat as automotive/companion surfaces. Media3 1.7.1
   * documents both methods as comparing only [MediaSession.ControllerInfo.getPackageName] — a value
   * that isn't necessarily backed by the calling UID on every connection path — and explicitly says
   * neither is a security check. [isVerifiedAutomotiveController] re-derives the same intent but
   * cross-checks the claimed package name against [PackageManager]'s actual UID for it, which *is*
   * tied to the real calling process.
   */
  private val trustedAutomotivePackages =
    setOf(
      "com.google.android.projection.gearhead", // Android Auto
      "com.google.android.gms.car", // Android Auto (legacy package)
      "com.android.systemui", // Android Automotive OS system UI
    )

  /**
   * True only if [controller] is both claiming a trusted automotive/companion package name *and* is
   * actually running as that package's UID — closing the gap left by
   * [MediaSession.isAutomotiveController]/[MediaSession.isAutoCompanionController], which Media3
   * documents as non-authoritative string comparisons an unverified controller could satisfy by
   * merely reporting one of these package names.
   */
  private fun isVerifiedAutomotiveController(controller: MediaSession.ControllerInfo): Boolean {
    val claimedPackage = controller.packageName
    if (claimedPackage !in trustedAutomotivePackages) return false
    val actualUid =
      try {
        context.packageManager.getPackageUid(claimedPackage, 0)
      } catch (e: PackageManager.NameNotFoundException) {
        return false
      }
    return actualUid == controller.uid
  }

  /**
   * Whether [controller] is a surface allowed to mutate playback state via the toggle custom
   * commands (like/library/shuffle/repeat/radio): a trusted controller (the app itself, systemui,
   * or an app holding MEDIA_CONTENT_CONTROL / an enabled notification listener), the system media
   * notification, or a UID-verified Android Auto / Auto companion controller. [MusicService] is an
   * exported [MediaLibraryService][androidx.media3.session.MediaLibraryService], so without this
   * check any app on the device could bind to the session and invoke these state-changing commands.
   */
  private fun isAuthorizedController(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
  ): Boolean =
    controller.isTrusted ||
      session.isMediaNotificationController(controller) ||
      isVerifiedAutomotiveController(controller)

  /**
   * Negotiates connection capabilities for an incoming controller.
   *
   * Exposes the toggle custom commands ([SessionCommand]s for like, start-radio, library, shuffle
   * and repeat) only to [isAuthorizedController] controllers — the system notification / mini
   * player, the lock screen, Wear, and Android Auto all drive playback exclusively through these
   * commands, so they still need to reach those surfaces, but an arbitrary unauthorized app must
   * not be able to invoke them.
   *
   * @param session the media session receiving the connection
   * @param controller the connecting controller
   * @return accepted [MediaSession.ConnectionResult] with the toggle commands added for authorized
   *   controllers
   */
  override fun onConnect(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
  ): MediaSession.ConnectionResult {
    val connectionResult = super.onConnect(session, controller)
    val availableSessionCommands =
      if (isAuthorizedController(session, controller)) {
        connectionResult.availableSessionCommands
          .buildUpon()
          .add(MediaSessionConstants.CommandToggleLike)
          .add(MediaSessionConstants.CommandToggleStartRadio)
          .add(MediaSessionConstants.CommandToggleLibrary)
          .add(MediaSessionConstants.CommandToggleShuffle)
          .add(MediaSessionConstants.CommandToggleRepeatMode)
          .add(MediaSessionConstants.CommandItemLike)
          .add(MediaSessionConstants.CommandItemPlayNext)
          .build()
      } else {
        connectionResult.availableSessionCommands
      }

    // The reported package name alone is not verified; require the UID check
    // so an arbitrary controller cannot trigger the artwork refresh.
    if (isVerifiedAutomotiveController(controller)) {
      onAutomotiveConnected()
    }
    if (isAutomotiveBrowser(controller)) {
      prefetchHome()
    }

    // The platform session (read by Android Auto) mirrors the commands of the
    // media notification controller. Without COMMAND_GET_TIMELINE it publishes no
    // queue, so Auto drops its queue button: that screen opened at the top of the
    // queue instead of the current song and kept jumping back while scrolling.
    val availablePlayerCommands =
      if (session.isMediaNotificationController(controller)) {
        connectionResult.availablePlayerCommands
          .buildUpon()
          .remove(androidx.media3.common.Player.COMMAND_GET_TIMELINE)
          .build()
      } else {
        connectionResult.availablePlayerCommands
      }

    return MediaSession.ConnectionResult.accept(
      availableSessionCommands,
      availablePlayerCommands,
    )
  }

  /**
   * Handles custom session commands such as toggle-like, toggle-shuffle and toggle-repeat.
   *
   * Toggle commands are re-checked against [isAuthorizedController] as defense in depth —
   * [onConnect] should already keep them out of an unauthorized controller's available-commands
   * set, but a controller that somehow still sends one is rejected here.
   *
   * @param session the host [MediaSession]
   * @param controller the controller that sent the command
   * @param customCommand the [SessionCommand] with `customAction` to dispatch
   * @param args optional arguments (unused)
   * @return immediate [ListenableFuture] with [SessionResult]
   */
  override fun onCustomCommand(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle,
  ): ListenableFuture<SessionResult> {
    val isToggleCommand =
      customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_LIKE ||
        customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_START_RADIO ||
        customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_LIBRARY ||
        customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_SHUFFLE ||
        customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE
    val isItemCommand =
      customCommand.customAction == MediaSessionConstants.ACTION_ITEM_LIKE ||
        customCommand.customAction == MediaSessionConstants.ACTION_ITEM_PLAY_NEXT
    val authorized = isAuthorizedController(session, controller)
    if ((isToggleCommand || isItemCommand) && !authorized) {
      return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
    }
    if (isItemCommand) {
      val mediaId =
        args.getString(MediaConstants.EXTRA_KEY_MEDIA_ID)
          ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
      return scope.future {
        val handled =
          when (customCommand.customAction) {
            MediaSessionConstants.ACTION_ITEM_LIKE -> likeItem(mediaId)
            else -> playItemNext(session, mediaId)
          }
        SessionResult(if (handled) SessionResult.RESULT_SUCCESS else SessionError.ERROR_BAD_VALUE)
      }
    }
    when (customCommand.customAction) {
      MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
      MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> toggleStartRadio()
      MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> toggleLibrary()
      MediaSessionConstants.ACTION_TOGGLE_SHUFFLE ->
        session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
      MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> session.player.toggleRepeatMode()
    }
    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
  }

  /** Song id at the end of a browse item's media id ("home_section/x/<id>"). */
  private fun songIdOf(mediaId: String): String = mediaId.substringAfterLast('/')

  /**
   * Makes sure the song behind [mediaId] is in the database (online Home or
   * search items may not be yet), so actions on it have a row to update.
   */
  private suspend fun ensureSongInDatabase(mediaId: String): Song? {
    val songId = songIdOf(mediaId)
    database.song(songId).first()?.let { return it }
    withContext(Dispatchers.IO) { getMediaItem(mediaId) }
    return database.song(songId).first()
  }

  /** Browse action: like / unlike the song behind [mediaId]. */
  private suspend fun likeItem(mediaId: String): Boolean {
    val song = ensureSongInDatabase(mediaId) ?: return false
    service.toggleLike(song.id)
    return true
  }

  /** Browse action: queue the song behind [mediaId] right after the current one. */
  private suspend fun playItemNext(session: MediaSession, mediaId: String): Boolean {
    val item = ensureSongInDatabase(mediaId)?.toMediaItem() ?: return false
    withContext(Dispatchers.Main) {
      val player = session.player
      if (player.mediaItemCount == 0) {
        player.setMediaItem(item)
        player.prepare()
        player.play()
      } else {
        player.addMediaItem(player.currentMediaItemIndex + 1, item)
      }
    }
    return true
  }

  /**
   * Handles playback resumption requests (Android Auto / Automotive OS reconnect).
   *
   * Previously this always returned a never-completing future, so opening Android
   * Auto never started Echo and the user had to open the app manually. Now, when
   * the [AutoplayOnAutomotiveConnectKey] setting is on and the caller is an
   * automotive controller, the current player queue (restored from disk with
   * playWhenReady=false) is offered back so Auto can resume it without manual taps.
   * Every other case keeps the old never-completing behavior so nothing can
   * unexpectedly start playback on controller reconnect.
   */
  @Deprecated("Deprecated in MediaLibrarySession.Callback")
  override fun onPlaybackResumption(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo
  ): ListenableFuture<MediaItemsWithStartPosition> {
    if (!isAutomotiveBrowser(controller) && !isVerifiedAutomotiveController(controller)) {
      return SettableFuture.create<MediaItemsWithStartPosition>()
    }
    return scope.future(Dispatchers.Main) {
      val autoplay = context.dataStore.get(AutoplayOnAutomotiveConnectKey, false)
      if (!autoplay) {
        // Never complete, exactly like before: no implicit playback.
        awaitCancellation()
      }
      if (!::service.isInitialized) {
        return@future MediaItemsWithStartPosition(emptyList(), 0, C.TIME_UNSET)
      }
      val player = service.player
      if (player.mediaItemCount <= 0) {
        return@future MediaItemsWithStartPosition(emptyList(), 0, C.TIME_UNSET)
      }
      val index = player.currentMediaItemIndex
        .takeIf { it != C.INDEX_UNSET }
        ?.coerceIn(0, player.mediaItemCount - 1) ?: 0
      val items = List(player.mediaItemCount) { player.getMediaItemAt(it) }
      MediaItemsWithStartPosition(items, index, player.currentPosition)
    }
  }

  /**
   * Provides the library root and browsing hints for Automotive and other browsers.
   *
   * Always advertises `CONTENT_STYLE_SUPPORTED` and `SEARCH_SUPPORTED`. For automotive browsers,
   * forwards the caller-requested artwork size hint
   * ([MediaConstants.EXTRAS_KEY_MEDIA_ART_SIZE_PIXELS]) from
   * [MediaLibraryService.LibraryParams.extras] into the returned root extras via `getInt` (integer
   * key, not parcelable). Non-automotive callers receive only the base browsing flags. Result
   * extras are normalized with [withContentStyleHints].
   *
   * @param session the [MediaLibrarySession]
   * @param browser the browsing controller
   * @param params incoming [MediaLibraryService.LibraryParams] that may carry the art-size hint
   * @return [LibraryResult] containing the root [MediaItem] and decorated
   *   [MediaLibraryService.LibraryParams]
   */
  override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: MediaLibraryService.LibraryParams?,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    val isAutomotive =
      browser.packageName == "com.google.android.projection.gearhead" ||
        browser.packageName == "com.google.android.gms.car" ||
        browser.packageName == "com.android.systemui"

    val rootExtras =
      Bundle().apply {
        putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
        putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
        if (isAutomotive) {
          params?.extras?.let { extras ->
            if (extras.containsKey(MediaConstants.EXTRAS_KEY_MEDIA_ART_SIZE_PIXELS)) {
              putInt(
                MediaConstants.EXTRAS_KEY_MEDIA_ART_SIZE_PIXELS,
                extras.getInt(MediaConstants.EXTRAS_KEY_MEDIA_ART_SIZE_PIXELS)
              )
            }
          }
        }
      }

    val rootParams =
      MediaLibraryService.LibraryParams.Builder()
        .setOffline(params?.isOffline ?: false)
        .setRecent(params?.isRecent ?: false)
        .setSuggested(params?.isSuggested ?: false)
        .setExtras(rootExtras)
        .build()

    return Futures.immediateFuture(
      LibraryResult.ofItem(
        rootMediaItem(),
        rootParams.withContentStyleHints(isAutomotive),
      ),
    )
  }

  /**
   * Returns children for a given browsable parent. Supports root and standard parents
   * (song/artist/album/playlist) with pagination via [paginate]; always applies
   * [withContentStyleHints] so Automotive OS renders lists with correct styles.
   *
   * @param session the [MediaLibrarySession]
   * @param browser the browsing controller
   * @param parentId the browsable parent id
   * @param page zero-based page index
   * @param pageSize requested page size
   * @param params optional browsing params
   * @return [LibraryResult] with paginated [MediaItem] children
   */
  override fun onGetChildren(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: MediaLibraryService.LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
    val isAutomotive = isAutomotiveBrowser(browser)
    return scope.future(Dispatchers.IO) {
      // Automotive "Suggested" / "Recent" tabs arrive as LibraryParams flags on the
      // root, not as distinct parentIds. Serve them from YT Home / local recents
      // instead of an empty list so Auto shows recommended music.
      if (parentId == MusicService.ROOT && params?.isSuggested == true) {
        return@future LibraryResult.ofItemList(
          // Suggested/Recent bypassed artwork embedding, so their covers were
          // URI-only and unreliable on automotive surfaces.
          withAutomotiveArtworkIfNeeded(isAutomotive, suggestedHomeSongs().paginate(page, pageSize)),
          params.withContentStyleHints(isAutomotive),
        )
      }
      if (parentId == MusicService.ROOT && params?.isRecent == true) {
        val recent = database.songsByCreateDateAsc().first().take(50)
          .map { it.toMediaItem(MusicService.SONG) }
        return@future LibraryResult.ofItemList(
          withAutomotiveArtworkIfNeeded(isAutomotive, recent.paginate(page, pageSize)),
          params.withContentStyleHints(isAutomotive),
        )
      }
      val children =
        when (parentId) {
          MusicService.ROOT -> rootChildren()
          MusicService.HOME -> homeShelves()
          MusicService.LIBRARY -> libraryChildren()
          MusicService.HISTORY -> historySongs()
          MusicService.SONG ->
            database.songsByCreateDateAsc().first().map { it.toMediaItem(parentId) }
          MusicService.ARTIST ->
            database.artistsByCreateDateAsc().first().map { artist ->
              browsableMediaItem(
                "${MusicService.ARTIST}/${artist.id}",
                artist.artist.name,
                context.resources.getQuantityString(
                  R.plurals.n_song,
                  artist.songCount,
                  artist.songCount
                ),
                artist.artist.thumbnailUrl?.toUri(),
                MediaMetadata.MEDIA_TYPE_ARTIST,
                singleItemStyle = artistItemStyle(isAutomotive),
              )
            }
          MusicService.ALBUM ->
            database.albumsByCreateDateAsc().first().map { album ->
              browsableMediaItem(
                "${MusicService.ALBUM}/${album.id}",
                album.album.title,
                album.artists.joinToString { it.name },
                album.album.thumbnailUrl?.toUri(),
                MediaMetadata.MEDIA_TYPE_ALBUM,
                singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
              )
            }
          MusicService.PLAYLIST -> {
            val likedSongCount = database.likedSongsCount().first()
            val downloadedSongCount = downloadUtil.downloads.value.size
            val youtubePlaylists =
              try {
                youtubeHome()
                  ?.sections
                  ?.flatMap { it.items }
                  ?.filterIsInstance<PlaylistItem>()
                  ?.take(20) ?: emptyList()
              } catch (e: Exception) {
                reportException(e)
                emptyList()
              }

            listOf(
              likedSongsMediaItem(likedSongCount),
              downloadedSongsMediaItem(downloadedSongCount),
            ) +
              database.playlistsByCreateDateAsc().first().map { playlist ->
                playlist.toBrowsableMediaItem()
              } +
              youtubePlaylists.map { ytPlaylist -> ytPlaylist.toBrowsableMediaItem() }
          }
          else ->
            when {
              parentId == MusicService.HOME_SECTION ||
                parentId.startsWith("${MusicService.HOME_SECTION}/") -> {
                val sectionId = parentId.removePrefix(MusicService.HOME_SECTION).removePrefix("/")
                listOf(shuffleMediaItem(parentId)) + homeSectionSongs(sectionId, parentId)
              }
              parentId.startsWith("${MusicService.ARTIST}/") ->
                database
                  .artistSongsByCreateDateAsc(parentId.removePrefix("${MusicService.ARTIST}/"))
                  .first()
                  .map { it.toMediaItem(parentId) }
              parentId.startsWith("${MusicService.ALBUM}/") ->
                database.albumSongs(parentId.removePrefix("${MusicService.ALBUM}/")).first().map {
                  it.toMediaItem(parentId)
                }
              parentId.startsWith("${MusicService.PLAYLIST}/") -> {
                val playlistId = parentId.removePrefix("${MusicService.PLAYLIST}/")
                val songs =
                  when (playlistId) {
                    PlaylistEntity.LIKED_PLAYLIST_ID ->
                      database.likedSongs(SongSortType.CREATE_DATE, true)
                    PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                      val downloads = downloadUtil.downloads.value
                      database
                        .allSongs()
                        .flowOn(Dispatchers.IO)
                        .map { songs ->
                          songs.filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                        }
                        .map { songs ->
                          songs
                            .map { it to downloads[it.id] }
                            .sortedBy { it.second?.updateTimeMs ?: 0L }
                            .map { it.first }
                        }
                    }
                    else -> database.playlistSongs(playlistId).map { list -> list.map { it.song } }
                  }.first()

                listOf(shuffleMediaItem(parentId)) + songs.map { it.toMediaItem(parentId) }
              }
              parentId.startsWith("${MusicService.YOUTUBE_PLAYLIST}/") -> {
                val playlistId = parentId.removePrefix("${MusicService.YOUTUBE_PLAYLIST}/")
                try {
                  val songs =
                    YouTube.playlist(playlistId)
                      .getOrNull()
                      ?.songs
                      ?.take(100)
                      ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                      ?.filterVideoSongs(
                        context.dataStore.get(HideVideoSongsKey, false) ||
                          context.dataStore.get(
                            echo.music.iad1tya.constants.DataSaverEnabledKey,
                            false
                          )
                      ) ?: emptyList()

                  listOf(shuffleMediaItem(parentId)) + songs.map { it.toMediaItem(parentId) }
                } catch (e: Exception) {
                  reportException(e)
                  emptyList()
                }
              }
              else -> emptyList()
            }
        }

      val paged = children.paginate(page, pageSize)
      LibraryResult.ofItemList(
        withAutomotiveArtworkIfNeeded(isAutomotive, paged),
        params.withContentStyleHints(isAutomotive),
      )
    }
  }

  /**
   * Points each item's artwork at [AutomotiveArtworkProvider][echo.music.iad1tya.utils.AutomotiveArtworkProvider].
   * Android Auto ignores `https://` icon URIs, and embedding the bytes only fit
   * a bounded part of each page, so covers were missing on later items and on
   * some surfaces. A `content://` URI lets every surface load every cover lazily.
   */
  private fun withAutomotiveArtworkIfNeeded(
    isAutomotive: Boolean,
    items: List<MediaItem>
  ): List<MediaItem> = if (isAutomotive) withAutomotiveArtwork(items) else items

  private fun withAutomotiveArtwork(items: List<MediaItem>): List<MediaItem> =
    items.map { it.withAutomotiveArtworkUri(context) }

  private fun isAutomotiveBrowser(browser: MediaSession.ControllerInfo): Boolean =
    browser.packageName == "com.google.android.projection.gearhead" ||
      browser.packageName == "com.google.android.gms.car" ||
      browser.packageName == "com.android.systemui"

  override fun onGetItem(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    mediaId: String,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    val isAutomotive = isAutomotiveBrowser(browser)
    return scope.future(Dispatchers.IO) {
      getMediaItem(mediaId, isAutomotive)
        ?.let { item -> if (isAutomotive) withAutomotiveArtwork(listOf(item)).first() else item }
        ?.let { LibraryResult.ofItem(it, null) }
        ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
    }
  }

  /**
   * Handles media search requests by notifying the controller that results changed. Actual results
   * are provided via [onGetSearchResult]; this keeps search lightweight and avoids blocking the
   * caller thread.
   *
   * @param session the [MediaLibrarySession]
   * @param browser the controller initiating the search
   * @param query the user search query
   * @param params optional library params
   * @return void [LibraryResult]
   */
  override fun onSearch(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    query: String,
    params: MediaLibraryService.LibraryParams?
  ): ListenableFuture<LibraryResult<Void>> {
    session.notifySearchResultChanged(browser, query, 1, params)
    return Futures.immediateFuture(LibraryResult.ofVoid())
  }

  /**
   * Returns search results matching [query] from local DB and YouTube, merged and deduped, with
   * pagination and content-style hints. Filters explicit/video songs per data-store settings and
   * paginates via [paginate].
   *
   * @param session the [MediaLibrarySession]
   * @param browser the controller requesting results
   * @param query the search query
   * @param page zero-based page index
   * @param pageSize page size
   * @param params optional browsing params
   * @return [LibraryResult] with paginated [MediaItem] results
   */
  override fun onGetSearchResult(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    query: String,
    page: Int,
    pageSize: Int,
    params: MediaLibraryService.LibraryParams?
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
    val isAutomotive =
      browser.packageName == "com.google.android.projection.gearhead" ||
        browser.packageName == "com.google.android.gms.car" ||
        browser.packageName == "com.android.systemui"
    return scope.future(Dispatchers.IO) {
      if (query.isEmpty()) {
        return@future LibraryResult.ofItemList(
          emptyList(),
          params.withContentStyleHints(isAutomotive)
        )
      }

      try {
        val searchResults = mutableListOf<MediaItem>()

        val localSongs =
          database.allSongs().first().filter { song ->
            song.song.title.contains(query, ignoreCase = true) ||
              song.artists.any { it.name.contains(query, ignoreCase = true) } ||
              song.album?.title?.contains(query, ignoreCase = true) == true
          }

        val artistSongs =
          database.searchArtists(query).first().flatMap { artist ->
            database.artistSongsByCreateDateAsc(artist.id).first()
          }

        val albumSongs =
          database.searchAlbums(query).first().flatMap { album ->
            database.albumSongs(album.id).first()
          }

        val playlistSongs =
          database.searchPlaylists(query).first().flatMap { playlist ->
            database.playlistSongs(playlist.id).first().map { it.song }
          }

        val allLocalSongs =
          (localSongs + artistSongs + albumSongs + playlistSongs).distinctBy { it.id }

        val libraryGroup = context.getString(R.string.filter_library)
        allLocalSongs.take(SEARCH_LOCAL_LIMIT).forEach { song ->
          searchResults.add(
            song.toMediaItem(
              path = "${MusicService.SEARCH}/$query",
              isPlayable = true,
              isBrowsable = false
            ).withGroupTitle(libraryGroup)
          )
        }

        // Albums and playlists are searched alongside the songs, so Auto shows
        // grouped results like the app instead of one flat list of songs.
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val albumsSearch = async { onlineCollections(query, YouTube.SearchFilter.FILTER_ALBUM, hideExplicit) }
        val playlistsSearch =
          async { onlineCollections(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST, hideExplicit) }

        try {
          val onlineResults =
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
              .getOrNull()
              ?.items
              ?.filterIsInstance<SongItem>()
              ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
              ?.filterVideoSongs(
                context.dataStore.get(HideVideoSongsKey, false) ||
                  context.dataStore.get(echo.music.iad1tya.constants.DataSaverEnabledKey, false)
              )
              ?.filter { onlineSong ->
                !allLocalSongs.any { localSong ->
                  localSong.id == onlineSong.id ||
                    (localSong.song.title.equals(onlineSong.title, ignoreCase = true) &&
                      localSong.artists.any { artist ->
                        onlineSong.artists.any { it.name.equals(artist.name, ignoreCase = true) }
                      })
                }
              } ?: emptyList()

          onlineResults.forEach { songItem ->
            try {
              database.query { insert(songItem.toMediaMetadata()) }
            } catch (e: Exception) {}

            searchResults.add(
              songItem.toMediaItem("${MusicService.SEARCH}/$query")
                .withGroupTitle(context.getString(R.string.songs))
            )
          }
        } catch (e: Exception) {
          reportException(e)
        }
        searchResults += albumsSearch.await().map { it.withGroupTitle(context.getString(R.string.albums)) }
        searchResults += playlistsSearch.await().map { it.withGroupTitle(context.getString(R.string.playlists)) }

        LibraryResult.ofItemList(
          withAutomotiveArtworkIfNeeded(isAutomotive, searchResults.paginate(page, pageSize)),
          params.withContentStyleHints(isAutomotive)
        )
      } catch (e: Exception) {
        reportException(e)
        LibraryResult.ofItemList(emptyList(), params.withContentStyleHints(isAutomotive))
      }
    }
  }

  /**
   * Albums or playlists matching [query] on YouTube, as browsable playlist nodes
   * (albums open through their playlist id). Failures yield no results.
   */
  private suspend fun onlineCollections(
    query: String,
    filter: YouTube.SearchFilter,
    hideExplicit: Boolean,
  ): List<MediaItem> =
    try {
      YouTube.search(query, filter).getOrNull()?.items.orEmpty()
        .filterExplicit(hideExplicit)
        .mapNotNull { item ->
          when (item) {
            is PlaylistItem -> item.toBrowsableMediaItem()
            is AlbumItem ->
              browsableMediaItem(
                "${MusicService.YOUTUBE_PLAYLIST}/${item.playlistId}",
                item.title,
                item.artists?.joinToString { it.name },
                item.thumbnail.toUri(),
                MediaMetadata.MEDIA_TYPE_ALBUM,
                singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
              )
            else -> null
          }
        }
        .take(SEARCH_COLLECTION_LIMIT)
    } catch (e: Exception) {
      reportException(e)
      emptyList()
    }

  override fun onSetMediaItems(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ): ListenableFuture<MediaItemsWithStartPosition> =
    scope.future(Dispatchers.IO) {
      val defaultResult = MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
      val path = mediaItems.firstOrNull()?.mediaId?.split("/") ?: return@future defaultResult

      when (path.firstOrNull()) {
        MusicService.HISTORY -> {
          val songId = path.getOrNull(1) ?: return@future defaultResult
          val songs = recentlyPlayedSongs()
          MediaItemsWithStartPosition(
            songs.map { it.toMediaItem() },
            songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
            startPositionMs
          )
        }
        MusicService.SONG -> {
          val songId = path.getOrNull(1) ?: return@future defaultResult
          val allSongs = database.songsByCreateDateAsc().first()
          val index = allSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0
          val start = maxOf(0, index - 100)
          val end = minOf(allSongs.size, index + 100)
          MediaItemsWithStartPosition(
            allSongs.subList(start, end).map { it.toMediaItem() },
            index - start,
            startPositionMs
          )
        }
        MusicService.ARTIST -> {
          val songId = path.getOrNull(2) ?: return@future defaultResult
          val artistId = path.getOrNull(1) ?: return@future defaultResult
          val songs = database.artistSongsByCreateDateAsc(artistId).first()
          MediaItemsWithStartPosition(
            songs.map { it.toMediaItem() },
            songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
            startPositionMs
          )
        }
        MusicService.ALBUM -> {
          val songId = path.getOrNull(2) ?: return@future defaultResult
          val albumId = path.getOrNull(1) ?: return@future defaultResult
          val albumWithSongs =
            database.albumWithSongs(albumId).first() ?: return@future defaultResult
          MediaItemsWithStartPosition(
            albumWithSongs.songs.map { it.toMediaItem() },
            albumWithSongs.songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
            startPositionMs
          )
        }
        MusicService.PLAYLIST -> {
          val songId = path.getOrNull(2) ?: return@future defaultResult
          val playlistId = path.getOrNull(1) ?: return@future defaultResult
          val songs =
            when (playlistId) {
              PlaylistEntity.LIKED_PLAYLIST_ID ->
                database.likedSongs(SongSortType.CREATE_DATE, descending = true)
              PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                val downloads = downloadUtil.downloads.value
                database
                  .allSongs()
                  .flowOn(Dispatchers.IO)
                  .map { songs ->
                    songs.filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                  }
                  .map { songs ->
                    songs
                      .map { it to downloads[it.id] }
                      .sortedBy { it.second?.updateTimeMs ?: 0L }
                      .map { it.first }
                  }
              }
              else -> database.playlistSongs(playlistId).map { list -> list.map { it.song } }
            }.first()

          if (songId == MusicService.SHUFFLE_ACTION) {
            MediaItemsWithStartPosition(
              songs.shuffled().take(200).map { it.toMediaItem() },
              0,
              C.TIME_UNSET
            )
          } else {
            val index = songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0
            val start = maxOf(0, index - 100)
            val end = minOf(songs.size, index + 100)
            MediaItemsWithStartPosition(
              songs.subList(start, end).map { it.toMediaItem() },
              index - start,
              startPositionMs
            )
          }
        }
        MusicService.YOUTUBE_PLAYLIST -> {
          val songId = path.getOrNull(2) ?: return@future defaultResult
          val playlistId = path.getOrNull(1) ?: return@future defaultResult

          val songs =
            try {
              YouTube.playlist(playlistId).getOrNull()?.songs?.map { it.toMediaItem() }
                ?: emptyList()
            } catch (e: Exception) {
              reportException(e)
              return@future defaultResult
            }

          if (songId == MusicService.SHUFFLE_ACTION) {
            MediaItemsWithStartPosition(songs.shuffled(), 0, C.TIME_UNSET)
          } else {
            MediaItemsWithStartPosition(
              songs,
              songs.indexOfFirst { it.mediaId.endsWith(songId) }.takeIf { it != -1 } ?: 0,
              C.TIME_UNSET
            )
          }
        }
        MusicService.HOME_SECTION -> {
          val sectionId = path.getOrNull(1) ?: return@future defaultResult
          val songId = path.lastOrNull() ?: return@future defaultResult
          val parentId = mediaItems.firstOrNull()?.mediaId?.removeSuffix("/$songId")
            ?: "${MusicService.HOME_SECTION}/$sectionId"
          // Drop the shuffle marker itself: it is a command, not a playable queue item.
          val sectionSongs =
            if (sectionId == SUGGESTED_SECTION_ID) suggestedHomeSongs() else homeSectionSongs(sectionId, parentId)
          val songs = sectionSongs.filter { it.mediaMetadata.isPlayable == true && !it.mediaId.endsWith(MusicService.SHUFFLE_ACTION) }
          if (songs.isEmpty()) return@future defaultResult
          val playable = songs.map { song ->
            val id = song.mediaId.substringAfterLast("/")
            database.song(id).first()?.toMediaItem() ?: song
          }
          if (songId == MusicService.SHUFFLE_ACTION) {
            MediaItemsWithStartPosition(playable.shuffled(), 0, C.TIME_UNSET)
          } else {
            MediaItemsWithStartPosition(
              playable,
              playable.indexOfFirst { it.mediaId.endsWith(songId) }.takeIf { it != -1 } ?: 0,
              C.TIME_UNSET
            )
          }
        }
        MusicService.HOME -> {
          // Reached via the shuffle marker under suggested songs: play shuffled.
          val songs = suggestedHomeSongs()
            .filter { it.mediaMetadata.isPlayable == true && !it.mediaId.endsWith(MusicService.SHUFFLE_ACTION) }
            .map { song ->
              val id = song.mediaId.substringAfterLast("/")
              database.song(id).first()?.toMediaItem() ?: song
            }
          if (songs.isEmpty()) return@future defaultResult
          MediaItemsWithStartPosition(songs.shuffled(), 0, C.TIME_UNSET)
        }
        MusicService.SEARCH -> {
          val songId = path.lastOrNull() ?: return@future defaultResult
          val searchQuery =
            mediaItems
              .firstOrNull()
              ?.mediaId
              ?.removePrefix("${MusicService.SEARCH}/")
              ?.removeSuffix("/$songId")
              ?.takeIf { it.isNotBlank() } ?: return@future defaultResult

          val searchResults = mutableListOf<Song>()

          val localSongs =
            database.allSongs().first().filter { song ->
              song.song.title.contains(searchQuery, ignoreCase = true) ||
                song.artists.any { it.name.contains(searchQuery, ignoreCase = true) } ||
                song.album?.title?.contains(searchQuery, ignoreCase = true) == true
            }

          val artistSongs =
            database.searchArtists(searchQuery).first().flatMap { artist ->
              database.artistSongsByCreateDateAsc(artist.id).first()
            }

          val albumSongs =
            database.searchAlbums(searchQuery).first().flatMap { album ->
              database.albumSongs(album.id).first()
            }

          val playlistSongs =
            database.searchPlaylists(searchQuery).first().flatMap { playlist ->
              database.playlistSongs(playlist.id).first().map { it.song }
            }

          val allLocalSongs =
            (localSongs + artistSongs + albumSongs + playlistSongs).distinctBy { it.id }

          searchResults.addAll(allLocalSongs)

          try {
            val onlineResults =
              YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                ?.filterVideoSongs(
                  context.dataStore.get(HideVideoSongsKey, false) ||
                    context.dataStore.get(echo.music.iad1tya.constants.DataSaverEnabledKey, false)
                )
                ?.filter { onlineSong ->
                  !allLocalSongs.any { localSong ->
                    localSong.id == onlineSong.id ||
                      (localSong.song.title.equals(onlineSong.title, ignoreCase = true) &&
                        localSong.artists.any { artist ->
                          onlineSong.artists.any { it.name.equals(artist.name, ignoreCase = true) }
                        })
                  }
                } ?: emptyList()

            onlineResults.forEach { songItem ->
              try {
                database.query { insert(songItem.toMediaMetadata()) }
                database.song(songItem.id).first()?.let { newSong -> searchResults.add(newSong) }
              } catch (e: Exception) {}
            }
          } catch (e: Exception) {
            reportException(e)
          }

          if (searchResults.isEmpty()) {
            return@future defaultResult
          }

          val targetIndex = searchResults.indexOfFirst { it.id == songId }

          MediaItemsWithStartPosition(
            searchResults.map { it.toMediaItem() },
            if (targetIndex >= 0) targetIndex else 0,
            C.TIME_UNSET
          )
        }
        else -> defaultResult
      }
    }

  private fun drawableUri(
    @DrawableRes id: Int,
  ) =
    Uri.Builder()
      .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
      .authority(context.resources.getResourcePackageName(id))
      .appendPath(context.resources.getResourceTypeName(id))
      .appendPath(context.resources.getResourceEntryName(id))
      .build()

  private fun rootMediaItem() =
    MediaItem.Builder()
      .setMediaId(MusicService.ROOT)
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(context.getString(R.string.app_name))
          .setIsPlayable(false)
          .setIsBrowsable(true)
          .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
          .build(),
      )
      .build()

  /**
   * The tabs Android Auto shows across the top. Auto displays at most four, so
   * Songs / Artists / Albums are grouped under Library (the fifth root item used
   * to be dropped).
   */
  private fun rootChildren() =
    listOf(
      browsableMediaItem(
        MusicService.HOME,
        context.getString(R.string.home),
        context.getString(R.string.home_desc),
        drawableUri(R.drawable.home_filled),
        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
        browsableStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
      ),
      browsableMediaItem(
        MusicService.LIBRARY,
        context.getString(R.string.filter_library),
        null,
        drawableUri(R.drawable.library_music),
        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
      ),
      browsableMediaItem(
        MusicService.PLAYLIST,
        context.getString(R.string.playlists),
        null,
        drawableUri(R.drawable.queue_music),
        MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
        browsableStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
      ),
      browsableMediaItem(
        MusicService.HISTORY,
        context.getString(R.string.history),
        null,
        drawableUri(R.drawable.history),
        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
      ),
    )

  /** Contents of the Library tab. */
  private fun libraryChildren() =
    listOf(
      browsableMediaItem(
        MusicService.SONG,
        context.getString(R.string.songs),
        null,
        drawableUri(R.drawable.music_note),
        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
      ),
      browsableMediaItem(
        MusicService.ARTIST,
        context.getString(R.string.artists),
        null,
        drawableUri(R.drawable.artist),
        MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
      ),
      browsableMediaItem(
        MusicService.ALBUM,
        context.getString(R.string.albums),
        null,
        drawableUri(R.drawable.album),
        MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
        browsableStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
      ),
    )

  /** The most recently played songs, newest first, without repeats. */
  private suspend fun recentlyPlayedSongs(): List<Song> =
    database.events().first().map { it.song }.distinctBy { it.id }.take(HISTORY_SIZE)

  private suspend fun historySongs(): List<MediaItem> =
    recentlyPlayedSongs().map { it.toMediaItem(MusicService.HISTORY) }

  /**
   * The Home tab as one page, like the phone app: each YouTube shelf becomes a
   * titled group of its first songs and playlists. Songs keep their shelf in the
   * media id, so playing one queues the rest of that shelf.
   */
  private suspend fun homeShelves(): List<MediaItem> =
    try {
      val home = youtubeHome()
        ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
        ?.filterVideoSongs(
          context.dataStore.get(HideVideoSongsKey, false) ||
            context.dataStore.get(echo.music.iad1tya.constants.DataSaverEnabledKey, false)
        )
      home?.sections.orEmpty().flatMap { section ->
        val parentId = "${MusicService.HOME_SECTION}/${homeSectionKey(section)}"
        section.items
          .mapNotNull { item ->
            when (item) {
              is SongItem -> item.toMediaItem(parentId)
              is PlaylistItem -> item.toBrowsableMediaItem()
              else -> null
            }
          }
          .take(HOME_SHELF_PREVIEW_SIZE)
          .map { it.withGroupTitle(section.title) }
      }
    } catch (e: Exception) {
      reportException(e)
      emptyList()
    }

  /** Puts [this] under a titled group header in Android Auto lists. */
  private fun MediaItem.withGroupTitle(title: String): MediaItem {
    val extras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
      putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, title)
    }
    return buildUpon().setMediaMetadata(mediaMetadata.buildUpon().setExtras(extras).build()).build()
  }

  private fun artistItemStyle(isAutomotive: Boolean) =
    if (isAutomotive) {
      MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM
    } else {
      MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
    }

  /**
   * One Home snapshot per [HOME_CACHE_TTL_MS], shared by every automotive browse
   * call.
   *
   * YouTube's Home feed is personalized per request: two consecutive fetches can
   * return different shelves, or the same shelves in a different order, which
   * made shelves such as "Mixtape per te" appear and disappear between visits.
   * Every fetch is therefore merged into one snapshot ([mergeHomeSnapshots])
   * that is also persisted ([AutoHomeSnapshotStore]), so the set of shelves and
   * their order survive both refreshes and process restarts.
   *
   * The refresh is serialized by [homeMutex] because `onGetChildren` and
   * `onSetMediaItems` run on independent IO coroutines: without it, two
   * concurrent misses could install two different snapshots. The TTL uses
   * [SystemClock.elapsedRealtime] so a wall-clock adjustment cannot expire it.
   */
  private suspend fun youtubeHome(): HomePage? {
    val account = homeAccountKey()
    val (home, isFresh) =
      homeMutex.withLock {
        useHomeAccount(account)
        cachedHome to isCachedHomeFresh()
      }
    if (isFresh) return home
    // A stale snapshot (restored from disk, or past its TTL) is served right
    // away so Auto never waits on several Home pages; the refresh only adds
    // shelves or updates their content, positions stay put.
    if (home != null) {
      refreshHomeInBackground()
      return home
    }
    return refreshHome()
  }

  /**
   * Identifies the signed-in account, so one account's personalized shelves are
   * never shown to another. Read from DataStore rather than [YouTube.dataSyncId],
   * which is still unset for a moment after a cold start.
   */
  private fun homeAccountKey(): String =
    MessageDigest.getInstance("SHA-256")
      .digest(context.dataStore.get(DataSyncIdKey, "").toByteArray())
      .joinToString("") { "%02x".format(it) }

  /**
   * Makes [cachedHome] belong to [account]: on first use, or after a sign-out or
   * account switch inside this process, the snapshot is replaced by the one
   * stored for [account] (if any). Callers hold [homeMutex].
   */
  private fun useHomeAccount(account: String) {
    if (homeAccount == account) return
    homeAccount = account
    cachedHome = null
    cachedHomeAtMs = 0L
    cachedHomeFromDisk = false
    homeLastSeenMs = emptyMap()
    val snapshot = homeStore.load(account) ?: return
    // The age limit applies to the restored copy too, or a shelf could outlive
    // it whenever the refresh keeps failing.
    val wallNow = System.currentTimeMillis()
    val sections =
      snapshot.sections.filter {
        wallNow - (snapshot.lastSeenMs[homeSectionKey(it)] ?: 0L) < HOME_SECTION_MAX_AGE_MS
      }
    if (sections.isEmpty()) return
    val keptKeys = sections.map(::homeSectionKey).toSet()
    cachedHome = HomePage(chips = null, sections = sections)
    homeLastSeenMs = snapshot.lastSeenMs.filterKeys { it in keptKeys }
    cachedHomeFromDisk = true
  }

  private fun isCachedHomeFresh(): Boolean =
    cachedHome != null &&
      !cachedHomeFromDisk &&
      SystemClock.elapsedRealtime() - cachedHomeAtMs < HOME_CACHE_TTL_MS

  private fun refreshHomeInBackground() {
    if (!homeRefreshing.compareAndSet(false, true)) return
    scope.launch(Dispatchers.IO) {
      try {
        refreshHome()
      } finally {
        homeRefreshing.set(false)
      }
    }
  }

  /**
   * Fetches Home, merges it into the snapshot and persists the result.
   *
   * [homeMutex] is held only to check freshness and to install the result, not
   * during the network fetch, so browse calls keep getting the stale snapshot
   * immediately while a refresh is running.
   */
  private suspend fun refreshHome(): HomePage? {
    val account = homeAccountKey()
    homeMutex.withLock {
      useHomeAccount(account)
      if (isCachedHomeFresh()) return cachedHome
    }
    val fetched = fetchFullHome()
    return homeMutex.withLock {
      useHomeAccount(account)
      if (fetched == null || fetched.sections.isEmpty()) {
        // Offline or YouTube failing: the restored snapshot still gives Auto its shelves.
        return@withLock cachedHome
      }
      // Another caller may have installed a fresh snapshot during the fetch.
      if (isCachedHomeFresh()) return@withLock cachedHome
      val wallNow = System.currentTimeMillis()
      val lastSeen = homeLastSeenMs.toMutableMap()
      fetched.sections.forEach { lastSeen[homeSectionKey(it)] = wallNow }
      // Keep shelves YouTube has not returned for a while from piling up forever.
      val merged =
        mergeHomeSnapshots(cachedHome, fetched).let { page ->
          page.copy(
            sections =
              page.sections.filter {
                wallNow - (lastSeen[homeSectionKey(it)] ?: wallNow) < HOME_SECTION_MAX_AGE_MS
              }
          )
        }
      val keptKeys = merged.sections.map(::homeSectionKey).toSet()
      homeLastSeenMs = lastSeen.filterKeys { it in keptKeys }
      cachedHome = merged
      cachedHomeAtMs = SystemClock.elapsedRealtime()
      cachedHomeFromDisk = false
      homeStore.save(account, merged.sections, ::homeSectionKey, homeLastSeenMs)
      merged
    }
  }

  /**
   * The first Home page plus its continuations.
   *
   * `YouTube.home()` returns only the first batch of shelves; the rest (often
   * including "Mixtape per te" / "Mixed for you") arrive through continuations,
   * and which shelves land in the first batch varies per request. Serving only
   * the first page made shelves swap in and out between visits.
   */
  private suspend fun fetchFullHome(): HomePage? {
    // Bounded, or a stalled connection would keep the refresh (and the
    // homeRefreshing flag) active forever.
    val first =
      try {
        withTimeoutOrNull(HOME_FIRST_PAGE_TIMEOUT_MS) { YouTube.home().getOrNull() }
      } catch (e: Exception) {
        reportException(e)
        null
      } ?: return null
    val sections = first.sections.toMutableList()
    var continuation = first.continuation
    withTimeoutOrNull(HOME_CONTINUATION_TIMEOUT_MS) {
      var pages = 1
      while (continuation != null && pages < HOME_MAX_PAGES) {
        val next = YouTube.home(continuation).getOrNull() ?: break
        sections += next.sections
        continuation = next.continuation
        pages++
      }
    }
    return first.copy(sections = sections.distinctBy(::homeSectionKey), continuation = null)
  }

  /**
   * Keeps every shelf seen during this process alive and in a stable position:
   * shelves already shown keep their slot (with fresh content when the new fetch
   * has them), new shelves are appended, and a shelf missing from one fetch is
   * not dropped. Auto then always shows the same full set of categories.
   */
  private fun mergeHomeSnapshots(previous: HomePage?, fresh: HomePage): HomePage {
    if (previous == null) return fresh
    val freshByKey = fresh.sections.associateBy(::homeSectionKey)
    val previousKeys = previous.sections.map(::homeSectionKey).toSet()
    val sections =
      previous.sections.map { freshByKey[homeSectionKey(it)] ?: it } +
        fresh.sections.filter { homeSectionKey(it) !in previousKeys }
    return fresh.copy(sections = sections)
  }

  /**
   * Stable id of a shelf inside the Auto tree. It used to be the shelf's index in
   * the feed, which pointed at a different shelf as soon as the feed changed.
   * Encoded, so a "/" in a title cannot split the media id.
   */
  private fun homeSectionKey(section: HomePage.Section): String = Uri.encode(section.title.trim())

  /** Resolves a shelf by [homeSectionKey], or by index for ids from older builds. */
  private suspend fun homeSection(sectionId: String): HomePage.Section? {
    val sections = youtubeHome()?.sections ?: return null
    return sections.firstOrNull { homeSectionKey(it) == sectionId }
      ?: sectionId.toIntOrNull()?.let { sections.getOrNull(it) }
  }

  /** Fetches Home ahead of the first browse so Auto does not wait for every page. */
  private fun prefetchHome() {
    scope.launch(Dispatchers.IO) { youtubeHome() }
  }

  /**
   * YouTube Home sections (Quick picks, Keep listening, charts...) as browsable
   * Auto nodes. Previously Auto only showed local Songs/Artists/Albums/Playlists,
   * so YT-generated playlists and recommendations were invisible.
   */
  private suspend fun homeSections(): List<MediaItem> {
    return try {
      val home = youtubeHome()
        ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
        ?.filterVideoSongs(
          context.dataStore.get(HideVideoSongsKey, false) ||
            context.dataStore.get(echo.music.iad1tya.constants.DataSaverEnabledKey, false)
        ) ?: return emptyList()
      home.sections.mapNotNull { section ->
        val artwork = sectionArtwork(section)
        // A shelf whose children homeSectionSongs() cannot render (album-only,
        // artist-only, ...) is a dead end on Automotive: it opens to nothing.
        // Artwork alone does not make a shelf browsable, so require usable
        // children instead of artwork.
        if (section.items.none { it is SongItem || it is PlaylistItem }) {
          return@mapNotNull null
        }
        browsableMediaItem(
          "${MusicService.HOME_SECTION}/${homeSectionKey(section)}",
          section.title,
          section.label ?: "${section.items.size} items",
          artwork?.toUri(),
          MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
          singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        )
      }
    } catch (e: Exception) {
      reportException(e)
      emptyList()
    }
  }

  /**
   * Header thumbnail when present, otherwise the first item's artwork. Home
   * shelves commonly ship without a header thumbnail, which is why Auto showed
   * a warning icon instead of a cover.
   */
  private fun sectionArtwork(section: HomePage.Section): String? =
    section.thumbnail
      ?: when (val first = section.items.firstOrNull()) {
        is SongItem -> first.thumbnail
        is PlaylistItem -> first.thumbnail
        is AlbumItem -> first.thumbnail
        is ArtistItem -> first.thumbnail
        else -> null
      }

  private suspend fun homeSectionSongs(sectionId: String, parentId: String): List<MediaItem> {
    return try {
      val section = homeSection(sectionId) ?: return emptyList()
      val hideExplicit = context.dataStore.get(HideExplicitKey, false)
      val hideVideos = context.dataStore.get(HideVideoSongsKey, false) ||
        context.dataStore.get(echo.music.iad1tya.constants.DataSaverEnabledKey, false)
      // Filter video songs on the YTItem list (same predicate as elsewhere);
      // filtering MediaItems afterwards would be a no-op.
      section.items.filterVideoSongs(hideVideos).flatMap { item ->
        when (item) {
          is SongItem -> {
            if (hideExplicit && item.explicit) return@flatMap emptyList()
            // Cache online metadata so playback resolves even if DB missed it
            // (same stale-trackedMediaItem class of bugs as the addon queue).
            try { database.query { insert(item.toMediaMetadata()) } } catch (_: Exception) {}
            listOf(item.toMediaItem(parentId))
          }
          is PlaylistItem -> listOf(item.toBrowsableMediaItem())
          else -> emptyList()
        }
      }.take(100)
    } catch (e: Exception) {
      reportException(e)
      emptyList()
    }
  }

  private suspend fun suggestedHomeSongs(): List<MediaItem> {
    return try {
      val home = youtubeHome()
        ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
        ?.filterVideoSongs(
          context.dataStore.get(HideVideoSongsKey, false) ||
            context.dataStore.get(echo.music.iad1tya.constants.DataSaverEnabledKey, false)
        ) ?: return emptyList()
      val songs = home.sections.flatMap { it.items }.filterIsInstance<SongItem>().take(50)
      songs.forEach { item ->
        try { database.query { insert(item.toMediaMetadata()) } } catch (_: Exception) {}
      }
      listOf(shuffleMediaItem(MusicService.HOME)) +
        songs.map { it.toMediaItem("${MusicService.HOME_SECTION}/$SUGGESTED_SECTION_ID") }
    } catch (e: Exception) {
      reportException(e)
      emptyList()
    }
  }

  private suspend fun getMediaItem(mediaId: String, isAutomotive: Boolean = false): MediaItem? {
    val path = mediaId.split("/")
    val type = path.firstOrNull() ?: return null

    return when (type) {
      MusicService.ROOT -> rootMediaItem()
      MusicService.HOME -> browsableMediaItem(
        MusicService.HOME,
        context.getString(R.string.home),
        context.getString(R.string.home_desc),
        drawableUri(R.drawable.home_filled),
        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
      )
      MusicService.HOME_SECTION -> {
        val sectionId = path.getOrNull(1) ?: return null
        if (path.size == 2) {
          homeSections().firstOrNull { it.mediaId == mediaId }
        } else {
          val songId = path.lastOrNull() ?: return null
          if (songId == MusicService.SHUFFLE_ACTION) {
            shuffleMediaItem(mediaId.removeSuffix("/$songId"))
          } else {
            database.song(songId).first()?.toMediaItem(mediaId.removeSuffix("/$songId"))
              ?: homeSectionOnlineSong(sectionId, songId, mediaId.removeSuffix("/$songId"))
          }
        }
      }
      MusicService.LIBRARY -> rootChildren().firstOrNull { it.mediaId == MusicService.LIBRARY }
      MusicService.HISTORY ->
        if (path.size == 1) {
          rootChildren().firstOrNull { it.mediaId == MusicService.HISTORY }
        } else {
          database.song(path[1]).first()?.toMediaItem(MusicService.HISTORY)
        }
      MusicService.SONG -> {
        if (path.size == 1) {
          libraryChildren().first { it.mediaId == MusicService.SONG }
        } else {
          database.song(path[1]).first()?.toMediaItem(MusicService.SONG)
        }
      }
      MusicService.ARTIST -> {
        val artistId = path.getOrNull(1) ?: return null
        if (path.size == 2) {
          database.artist(artistId).first()?.let { artist ->
            browsableMediaItem(
              "${MusicService.ARTIST}/${artist.id}",
              artist.artist.name,
              context.resources.getQuantityString(
                R.plurals.n_song,
                artist.songCount,
                artist.songCount
              ),
              artist.artist.thumbnailUrl?.toUri(),
              MediaMetadata.MEDIA_TYPE_ARTIST,
              singleItemStyle = artistItemStyle(isAutomotive),
            )
          }
        } else {
          val songId = path.lastOrNull() ?: return null
          database.song(songId).first()?.toMediaItem("${MusicService.ARTIST}/$artistId")
        }
      }
      MusicService.ALBUM -> {
        val albumId = path.getOrNull(1) ?: return null
        if (path.size == 2) {
          database.album(albumId).first()?.let { album ->
            browsableMediaItem(
              "${MusicService.ALBUM}/${album.id}",
              album.album.title,
              album.artists.joinToString { it.name },
              album.album.thumbnailUrl?.toUri(),
              MediaMetadata.MEDIA_TYPE_ALBUM,
              singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            )
          }
        } else {
          val songId = path.lastOrNull() ?: return null
          database.song(songId).first()?.toMediaItem("${MusicService.ALBUM}/$albumId")
        }
      }
      MusicService.PLAYLIST -> {
        val playlistId = path.getOrNull(1) ?: return null
        if (path.size == 2) {
          playlistMediaItem(playlistId)
        } else {
          val songId = path.lastOrNull() ?: return null
          if (songId == MusicService.SHUFFLE_ACTION) {
            shuffleMediaItem("${MusicService.PLAYLIST}/$playlistId")
          } else {
            database.song(songId).first()?.toMediaItem("${MusicService.PLAYLIST}/$playlistId")
          }
        }
      }
      MusicService.YOUTUBE_PLAYLIST -> {
        val playlistId = path.getOrNull(1) ?: return null
        if (path.size == 2) {
          youtubePlaylistMediaItem(playlistId)
        } else {
          val songId = path.lastOrNull() ?: return null
          if (songId == MusicService.SHUFFLE_ACTION) {
            shuffleMediaItem("${MusicService.YOUTUBE_PLAYLIST}/$playlistId")
          } else {
            database
              .song(songId)
              .first()
              ?.toMediaItem("${MusicService.YOUTUBE_PLAYLIST}/$playlistId")
              ?: youtubePlaylistSongMediaItem(playlistId, songId)
          }
        }
      }
      MusicService.SEARCH -> {
        val songId = path.lastOrNull() ?: return null
        database.song(songId).first()?.toMediaItem(mediaId.removeSuffix("/$songId"))
      }
      else -> database.song(mediaId).first()?.toMediaItem()
    }
  }

  private suspend fun playlistMediaItem(playlistId: String): MediaItem? =
    when (playlistId) {
      PlaylistEntity.LIKED_PLAYLIST_ID -> likedSongsMediaItem(database.likedSongsCount().first())
      PlaylistEntity.DOWNLOADED_PLAYLIST_ID ->
        downloadedSongsMediaItem(downloadUtil.downloads.value.size)
      else -> database.playlist(playlistId).first()?.toBrowsableMediaItem()
    }

  private suspend fun youtubePlaylistMediaItem(playlistId: String): MediaItem =
    try {
      YouTube.playlist(playlistId).getOrNull()?.playlist?.toBrowsableMediaItem()
    } catch (e: Exception) {
      reportException(e)
      null
    }
      ?: browsableMediaItem(
        "${MusicService.YOUTUBE_PLAYLIST}/$playlistId",
        playlistId,
        "YouTube Music",
        null,
        MediaMetadata.MEDIA_TYPE_PLAYLIST,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
      )

  private suspend fun youtubePlaylistSongMediaItem(
    playlistId: String,
    songId: String,
  ): MediaItem? =
    try {
      YouTube.playlist(playlistId)
        .getOrNull()
        ?.songs
        ?.firstOrNull { it.id == songId }
        ?.toMediaItem("${MusicService.YOUTUBE_PLAYLIST}/$playlistId")
    } catch (e: Exception) {
      reportException(e)
      null
    }

  private suspend fun homeSectionOnlineSong(
    sectionId: String,
    songId: String,
    parentId: String,
  ): MediaItem? {
    if (sectionId == SUGGESTED_SECTION_ID) {
      return suggestedHomeSongs()
        .firstOrNull { it.mediaId.endsWith("/$songId") }
    }
    return try {
      val item = homeSection(sectionId)
        ?.items?.filterIsInstance<SongItem>()
        ?.firstOrNull { it.id == songId } ?: return null
      try { database.query { insert(item.toMediaMetadata()) } } catch (_: Exception) {}
      item.toMediaItem(parentId)
    } catch (e: Exception) {
      reportException(e)
      null
    }
  }

  private fun likedSongsMediaItem(songCount: Int) =
    browsableMediaItem(
      "${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}",
      context.getString(R.string.liked_songs),
      context.resources.getQuantityString(R.plurals.n_song, songCount, songCount),
      drawableUri(R.drawable.favorite),
      MediaMetadata.MEDIA_TYPE_PLAYLIST,
      singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
    )

  private fun downloadedSongsMediaItem(songCount: Int) =
    browsableMediaItem(
      "${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}",
      context.getString(R.string.downloaded_songs),
      context.resources.getQuantityString(R.plurals.n_song, songCount, songCount),
      drawableUri(R.drawable.download),
      MediaMetadata.MEDIA_TYPE_PLAYLIST,
      singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
    )

  private fun echo.music.iad1tya.db.entities.Playlist.toBrowsableMediaItem() =
    browsableMediaItem(
      "${MusicService.PLAYLIST}/$id",
      playlist.name,
      context.resources.getQuantityString(R.plurals.n_song, songCount, songCount),
      thumbnails.firstOrNull()?.toUri(),
      MediaMetadata.MEDIA_TYPE_PLAYLIST,
      singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
    )

  private fun PlaylistItem.toBrowsableMediaItem() =
    browsableMediaItem(
      "${MusicService.YOUTUBE_PLAYLIST}/$id",
      title,
      author?.name ?: "YouTube Music",
      thumbnail?.toUri(),
      MediaMetadata.MEDIA_TYPE_PLAYLIST,
      singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
    )

  private fun shuffleMediaItem(parentId: String) =
    MediaItem.Builder()
      .setMediaId("$parentId/${MusicService.SHUFFLE_ACTION}")
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(context.getString(R.string.shuffle))
          .setArtworkUri(drawableUri(R.drawable.shuffle))
          .setIsPlayable(true)
          .setIsBrowsable(false)
          .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
          .setExtras(playableMediaItemExtras())
          .build()
      )
      .build()

  private fun browsableMediaItem(
    id: String,
    title: String,
    subtitle: String?,
    iconUri: Uri?,
    mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
    singleItemStyle: Int = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
    browsableStyle: Int = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
  ) =
    MediaItem.Builder()
      .setMediaId(id)
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(title)
          .setSubtitle(subtitle)
          .setArtist(subtitle)
          .setArtworkUri(iconUri)
          .setIsPlayable(false)
          .setIsBrowsable(true)
          .setMediaType(mediaType)
          .setExtras(
            contentStyleExtras(
              browsableStyle = browsableStyle,
              playableStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
              singleItemStyle = singleItemStyle,
            )
          )
          .build(),
      )
      .build()

  private fun Song.toMediaItem(
    path: String,
    isPlayable: Boolean = true,
    isBrowsable: Boolean = false
  ): MediaItem {
    return MediaItem.Builder()
      .setMediaId("$path/$id")
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(song.title)
          .setSubtitle(artists.joinToString { it.name })
          .setArtist(artists.joinToString { it.name })
          .setArtworkUri(song.thumbnailUrl?.toUri())
          .setIsPlayable(isPlayable)
          .setIsBrowsable(isBrowsable)
          .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
          .setExtras(songExtras(id, song.explicit))
          .apply { if (isPlayable) setSupportedCommands(SONG_ACTIONS) }
          .build(),
      )
      .build()
  }

  private fun SongItem.toMediaItem(path: String) =
    MediaItem.Builder()
      .setMediaId("$path/$id")
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(title)
          .setSubtitle(artists.joinToString(", ") { it.name })
          .setArtist(artists.joinToString(", ") { it.name })
          .setArtworkUri(thumbnail.toUri())
          .setIsPlayable(true)
          .setIsBrowsable(false)
          .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
          .setExtras(songExtras(id, explicit))
          .setSupportedCommands(SONG_ACTIONS)
          .build()
      )
      .build()

  /**
   * Ensures consistent content-style hints for automotive browsing responses.
   *
   * Copies any incoming [MediaLibraryService.LibraryParams] and, when [isAutomotive] is true,
   * injects [MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE] and
   * [MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE] as `EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM`.
   * Used by [onGetLibraryRoot], [onGetChildren] and [onGetSearchResult] so Automotive OS renders
   * lists correctly.
   *
   * @param isAutomotive when true, automotive content-style extras are applied
   * @return new [MediaLibraryService.LibraryParams] with style extras applied
   * @receiver nullable library params to decorate; `null` yields defaults plus style hints when
   *   automotive
   */
  private fun MediaLibraryService.LibraryParams?.withContentStyleHints(
    isAutomotive: Boolean
  ): MediaLibraryService.LibraryParams {
    val extras =
      Bundle(this?.extras ?: Bundle()).apply {
        if (isAutomotive) {
          putInt(
            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
          )
          putInt(
            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
          )
        }
      }

    return MediaLibraryService.LibraryParams.Builder()
      .setOffline(this?.isOffline ?: false)
      .setRecent(this?.isRecent ?: false)
      .setSuggested(this?.isSuggested ?: false)
      .setExtras(extras)
      .build()
  }

  /**
   * Song extras: list style plus the badges Auto draws on the row (explicit,
   * downloaded).
   */
  private fun songExtras(songId: String, explicit: Boolean) =
    playableMediaItemExtras().apply {
      if (explicit) {
        putLong(MediaConstants.EXTRAS_KEY_IS_EXPLICIT, MediaConstants.EXTRAS_VALUE_ATTRIBUTE_PRESENT)
      }
      if (downloadUtil.downloads.value[songId]?.state == Download.STATE_COMPLETED) {
        putLong(MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS, MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED)
      }
    }

  private fun playableMediaItemExtras() =
    contentStyleExtras(
      singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
    )

  private fun contentStyleExtras(
    browsableStyle: Int? = null,
    playableStyle: Int? = null,
    singleItemStyle: Int? = null,
  ) =
    Bundle().apply {
      browsableStyle?.let { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, it) }
      playableStyle?.let { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, it) }
      singleItemStyle?.let { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, it) }
    }

  private fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
    if (page < 0 || pageSize < 1 || isEmpty()) return emptyList()
    if (pageSize == Int.MAX_VALUE) return this

    val fromIndex = page.toLong() * pageSize
    if (fromIndex >= size) return emptyList()

    return subList(fromIndex.toInt(), minOf(fromIndex.toInt() + pageSize, size))
  }

  private companion object {
    /**
     * How long a YouTube Home snapshot is reused for automotive browse calls
     * before it is refreshed (and merged, see [mergeHomeSnapshots]).
     */
    const val HOME_CACHE_TTL_MS = 5 * 60 * 1000L
    const val HOME_MAX_PAGES = 6
    const val HOME_FIRST_PAGE_TIMEOUT_MS = 10_000L
    /** A shelf YouTube has not returned for this long is dropped from the snapshot. */
    const val HOME_SECTION_MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000
    const val HOME_CONTINUATION_TIMEOUT_MS = 12_000L
    const val SUGGESTED_SECTION_ID = "suggested"
    /** Items of each shelf shown on the Home tab; the full shelf plays from any of them. */
    const val HOME_SHELF_PREVIEW_SIZE = 8
    const val HISTORY_SIZE = 50
    const val SEARCH_LOCAL_LIMIT = 20
    const val SEARCH_COLLECTION_LIMIT = 6
    /** Browse actions offered on every playable song. */
    val SONG_ACTIONS = listOf(MediaSessionConstants.ACTION_ITEM_LIKE, MediaSessionConstants.ACTION_ITEM_PLAY_NEXT)
  }
}
