"""Writes desktop-patched copies of a few original files into build/gen-src/patched/ (originals stay untouched).
Run from desktop/orig-core."""
import os, re
BASE = "../../app/src/main/kotlin/"
PATCHES = {
    # the library's SliderDefaults.Track(trackCornerSize=...) is internal on desktop; DesktopSliderTrack is the same call
    "com/music/echo/ui/component/VolumeSlider.kt": [("SliderDefaults.Track(", "androidx.compose.material3.DesktopSliderTrack(")],
}
# coil3's ImageRequest.Builder takes a PlatformContext on desktop, not our Context shim
COIL = (re.compile(r"ImageRequest\.Builder\(\s*[\w.]+\s*\)"), "ImageRequest.Builder(coil3.PlatformContext.INSTANCE)")
# coil on desktop: the image loader hangs off PlatformContext, and allowHardware does not exist
COIL_LOADER = (re.compile(r"(?<![\w.])(?<!import )[\w.]+\.imageLoader\b"), "coil3.SingletonImageLoader.get(coil3.PlatformContext.INSTANCE)")
COIL_HW = (re.compile(r"\s*\.allowHardware\([^)]*\)|import coil3\.request\.allowHardware\n|import coil3\.imageLoader\n"), "")
PATCHES["com/music/echo/ui/component/Items.kt"] = [COIL]
for f in ("ui/player/Player.kt","ui/player/Thumbnail.kt","ui/component/Lyrics.kt","ui/player/MiniPlayer.kt","ui/screens/HomeScreen.kt","ui/screens/playlist/OnlinePlaylistScreen.kt","ui/screens/playlist/SpotifyPlaylistDetailScreen.kt","ui/screens/settings/DiscordSettings.kt","ui/screens/settings/StorageSettings.kt","ui/component/AlbumGradient.kt", "ui/component/LyricsImageCard.kt", "ui/screens/ambient/AmbientGlowBackground.kt"):
    PATCHES["com/music/echo/" + f] = [COIL, COIL_LOADER, COIL_HW]
# suspend DAO functions cannot be passed as a plain function reference to let()
PATCHES["com/music/echo/viewmodels/LyricsMenuViewModel.kt"] = [("lyricsEntity?.let(::delete)", "lyricsEntity?.let { delete(it) }")]
# MetroLyrics: the word glow is drawn with Android's Paint/BlurMaskFilter on the native canvas; same drawing with Skia
GLOW = (re.compile(r"glowPaint\.maskFilter = BlurMaskFilter\(.*?glowPaint\s*\)", re.S), """run {
                    val skPaint = org.jetbrains.skia.Paint().apply {
                      isAntiAlias = true
                      color = expressiveAccent.copy(alpha = glowAlpha).toArgb()
                      maskFilter = org.jetbrains.skia.MaskFilter.makeBlur(org.jetbrains.skia.FilterBlurMode.NORMAL, baseGlowRadius * 0.57735f + 0.5f)
                    }
                    val skFont = org.jetbrains.skia.Font().apply { size = lyricStyle.fontSize.toPx(); isEmboldened = true }
                    canvas.nativeCanvas.drawString(letterLayouts[i].layoutInput.text.text, 0f, letterLayouts[i].firstBaseline, skFont, skPaint)
                  }""")
PATCHES["com/music/echo/ui/component/MetroLyrics.kt"] = [
    GLOW,
    ("platformStyle = PlatformTextStyle(includeFontPadding = false),", ""),  # Android-only option; desktop has no font padding
    ("val glowPaint = remember { android.graphics.Paint().apply { isAntiAlias = true } }", ""),
    ("import android.graphics.BlurMaskFilter\n", ""),
]
# Hilt entry point replaced by the hand-wired graph
PATCHES["com/music/echo/ui/player/PlayerSyncedLyricsView.kt"] = [
    (re.compile(r"val entryPoint =\s*EntryPointAccessors\.fromApplication\(.*?\)\s*val lyricsHelper = entryPoint\.lyricsHelper\(\)", re.S), "val lyricsHelper = echo.music.iad1tya.di.DesktopGraph.lyricsHelper"),
    ("import dagger.hilt.android.EntryPointAccessors\n", ""),
]
# LocalSongScanner: the database half is unchanged; Android's MediaStore query is replaced by a walk over ~/Music reading tags with jaudiotagger
SCAN = (re.compile(r"    val projection =\n.*?        queryCompleted = true\n      \}\n", re.S), """    val unknownArtist = context.getString(R.string.unknown_artist)
    val unknownTitle = context.getString(R.string.unknown)
    val tracks = mutableListOf<LocalTrackRecord>()
    var totalDeviceRows = 0
    var matchedFolderRowsCount = 0
    var queryCompleted = false
    // shortcut: only scans ~/Music (override with -Dechomusic.musicDirs=dir1;dir2); a folder picker can feed this later.
    val roots = (System.getProperty("echomusic.musicDirs")?.split(';') ?: listOf(System.getProperty("user.home") + "/Music"))
      .map(::File).filter { it.isDirectory }
    java.util.logging.Logger.getLogger("org.jaudiotagger").level = java.util.logging.Level.OFF
    roots.forEach { root ->
      root.walkTopDown().filter { it.isFile }.forEach files@{ file ->
        totalDeviceRows++
        val normalizedFolderPath = resolveNormalizedFolderPath(relativePath = null, absolutePath = file.absolutePath)
        if (isIncludeMode) {
          if (sanitizedIncludedFolders.isEmpty() || !matchesFolder(normalizedFolderPath, sanitizedIncludedFolders)) return@files
        } else if (matchesFolder(normalizedFolderPath, sanitizedExcludedFolders)) return@files
        matchedFolderRowsCount++
        val displayName = file.name
        val mimeType = java.nio.file.Files.probeContentType(file.toPath())?.takeIf(String::isNotBlank) ?: "audio/*"
        if (!SupportedLocalAudio.isSupported(displayName, mimeType)) return@files
        val audio = runCatching { org.jaudiotagger.audio.AudioFileIO.read(file) }.getOrNull() ?: return@files
        val tag = audio.tag
        fun field(key: org.jaudiotagger.tag.FieldKey) = runCatching { tag?.getFirst(key) }.getOrNull()
        val durationMs = audio.audioHeader.trackLength * 1000L
        if (durationMs <= 0L || (sanitizedMinimumDurationMs > 0L && durationMs < sanitizedMinimumDurationMs)) return@files
        val artistValue = normalizeArtistName(field(org.jaudiotagger.tag.FieldKey.ARTIST), unknownArtist)
        val splitArtists = splitArtistNames(artistValue).ifEmpty { listOf(unknownArtist) }
        val artists = splitArtists.mapIndexed { index, name ->
          LocalArtistRecord(id = buildArtistId(null, name, index, splitArtists.size), name = name)
        }
        val albumName = normalizeAlbumName(field(org.jaudiotagger.tag.FieldKey.ALBUM))
        val title = normalizeTitle(field(org.jaudiotagger.tag.FieldKey.TITLE), displayName, unknownTitle)
        val uri = file.toURI().toString()
        tracks += LocalTrackRecord(
          id = uri,
          title = title,
          artists = artists,
          albumId = albumName?.let { buildAlbumId(null, it, artists.firstOrNull()?.id) },
          albumName = albumName,
          durationSeconds = (durationMs / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
          year = field(org.jaudiotagger.tag.FieldKey.YEAR)?.take(4)?.toIntOrNull()?.takeIf { it > 0 },
          dateModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()),
          sizeBytes = file.length(),
          mimeType = mimeType,
          thumbnailUrl = null,
        )
      }
    }
    queryCompleted = true
""")
PATCHES["com/music/echo/localmedia/LocalSongScanner.kt"] = [
    SCAN,
    ("removedIds.chunked(SqlBatchSize).forEach(::deleteSongsByIds)", "removedIds.chunked(SqlBatchSize).forEach { deleteSongsByIds(it) }"),
    (".forEach(::deleteAlbumArtistMapsByAlbumIds)", ".forEach { deleteAlbumArtistMapsByAlbumIds(it) }"),
    ("import android.content.ContentUris\n", "import java.io.File\n"),
    ("import android.os.Build\nimport android.provider.MediaStore\n", ""),
    ('Uri.parse("content://media/external/audio/albumart")', 'Uri.parse("file:///albumart")'),
    (re.compile(r"  private fun android\.database\.Cursor\.\w+\(columnIndex: Int\): \w+\? \{.*?\n  \}\n\n?", re.S), ""),
]
# Backup/restore: the Android "restart the activity" step becomes "exit; start the app again"
PATCHES["com/music/echo/viewmodels/BackupRestoreViewModel.kt"] = [
    (re.compile(r"\s*context\.stopService\(Intent\(context, MusicService::class\.java\)\)"), ""),
    (re.compile(r"\s*val restartIntent =.*?context\.startActivity\(restartIntent\)", re.S), ""),
    ("import echo.music.iad1tya.MainActivity\n", ""),
]
# Theme: no Material You on desktop, Palette works on skia bitmaps
PATCHES["com/music/echo/ui/theme/Theme.kt"] = [
    ("Build.VERSION.SDK_INT >= Build.VERSION_CODES.S", "false"),
    ("dynamicDarkColorScheme(context)", "androidx.compose.material3.darkColorScheme()"),
    ("dynamicLightColorScheme(context)", "androidx.compose.material3.lightColorScheme()"),
    (re.compile(r"import androidx\.compose\.material3\.dynamic\w+\n"), ""),
    ("import android.graphics.Bitmap", "import org.jetbrains.skia.Bitmap"),
    ("android.graphics.Typeface.createFromFile(customFontPathValue)", "org.jetbrains.skia.FontMgr.default.makeFromFile(customFontPathValue)!!"),
    ("androidx.compose.ui.text.font.Typeface(typeface)", "androidx.compose.ui.text.platform.Typeface(typeface)"),
]
# Room on desktop has suspend transactions; the menu click handler is not suspending, so block (short DB write)
PATCHES["com/music/echo/ui/menu/ArtistMenu.kt"] = [("database.transaction { update(artist.artist.toggleLike()) }", "kotlinx.coroutines.runBlocking { database.transaction { update(artist.artist.toggleLike()) } }")]
# JB navigation arguments are a plain map on desktop, not a Bundle
NAVARG = (re.compile(r"arguments\?\.get(?:String|Boolean)\(\"([^\"]+)\"\)"), None)
NAV = [
    (re.compile(r"\.arguments\?\.getString\((\"[^\"]+\")\)"), r".arguments.let { echo.music.iad1tya.desktop.navString(it, \1) }"),
    (re.compile(r"\.arguments\?\.getBoolean\((\"[^\"]+\")\)"), r".arguments.let { echo.music.iad1tya.desktop.navBoolean(it, \1) }"),
]
PATCHES["com/music/echo/ui/screens/NavigationBuilder.kt"] = NAV
PATCHES["com/music/echo/ui/screens/search/OnlineSearchResult.kt"] = NAV
# Queue: no ExoPlayer shuffle-order API (QueuePlayer shuffles itself); Material3 expressive ToggleButton checked colors are missing
PATCHES["com/music/echo/ui/player/Queue.kt"] = [
    (re.compile(r"import androidx\.media3\.exoplayer\.source\.ShuffleOrder\.DefaultShuffleOrder\n"), ""),
    (re.compile(r"playerConnection\.player\.setShuffleOrder\(\s*DefaultShuffleOrder\(.*?System\.currentTimeMillis\(\)\s*\)\s*\)", re.S), "Unit"),
    (re.compile(r"\n\s*checked(?:Container|Content)Color\s*=[^\n]*"), ""),
]
LOADING = (re.compile(r"PullToRefreshDefaults\.LoadingIndicator\("), "androidx.compose.material3.DesktopPullLoadingIndicator(")
for f in ("ui/screens/library/LibraryMixScreen.kt","ui/screens/search/suggestions/TabNewsSuggestion.kt","echomusic/commitscreen/commitscreen.kt","echomusic/changelog/changelogscreen.kt"):
    PATCHES["com/music/echo/" + f] = [LOADING]
PATCHES["com/music/echo/ui/screens/HomeScreen.kt"].append(LOADING)
FNREF1 = (re.compile(r"(forEach|onEach)\(::(\w+)\)"), r"\1 { \2(it) }")
FNREF2 = (re.compile(r"\.let\(::(\w+)\)"), r".let { \1(it) }")
# desktop Typeface from a font file (same as Theme.kt)
PATCHES["com/music/echo/ui/screens/settings/FontSelectionScreen.kt"] = [
    ("android.graphics.Typeface.createFromFile(customFontPath)", "org.jetbrains.skia.FontMgr.default.makeFromFile(customFontPath)!!"),
    ("androidx.compose.ui.text.font.Typeface(typeface)", "androidx.compose.ui.text.platform.Typeface(typeface)"),
]
# suspend DAO functions cannot be function references; ToggleButton has no contentPadding on desktop
for f in ("ui/menu/YouTubePlaylistMenu.kt","ui/menu/YouTubeAlbumMenu.kt"):
    PATCHES["com/music/echo/" + f] = [FNREF1, FNREF2]
PATCHES["com/music/echo/ui/screens/equalizer/axion/AxionEqScreen.kt"] = [(re.compile(r"\n\s*contentPadding = PaddingValues\(horizontal = 4\.dp\)\n"), "\n")]
# misc desktop fixes
PATCHES["com/music/echo/ui/screens/WelcomeDialog.kt"] = [("R.mipmap.ic_launcher", "R.drawable.ic_launcher_foreground")]
PATCHES["com/music/echo/ui/screens/MoodAndGenresScreen.kt"] = [("import android.content.res.Configuration.ORIENTATION_LANDSCAPE", "import android.content.res.Configuration.Companion.ORIENTATION_LANDSCAPE")]
HOMEFN = [FNREF1, FNREF2]
PATCHES["com/music/echo/ui/screens/playlist/LocalPlaylistScreen.kt"] = HOMEFN
# liquid glass needs Android RenderEffect and an app-wide backdrop; on desktop glass is off so every liquidGlass() returns the plain modifier
PATCHES["com/music/echo/ui/component/GlassEffect.kt"] = [("fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S", "fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = false")]
# Spotify import: Android logs in through a WebView and reads the sp_dc cookie; desktop opens the browser and takes the pasted cookie instead
SPOTIFY_LOGIN = """@Composable
private fun SpotifyLoginSheet(
  onDismiss: () -> Unit,
  onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
  // mode: null = asking permission, "mine" = person's own browser tab + pasted cookie, "auto" = clean browser window read by the app
  var mode by remember { mutableStateOf<String?>(null) }
  var problem by remember { mutableStateOf<String?>(null) }
  var spDc by remember { mutableStateOf("") }
  var spKey by remember { mutableStateOf("") }
  if (mode == "auto") {
    androidx.compose.runtime.DisposableEffect(Unit) {
      val close = echo.music.iad1tya.desktop.BrowserLogin.open(SpotifyAuth.LOGIN_URL, "spotify.com", { it.containsKey("sp_dc") },
        { c -> javax.swing.SwingUtilities.invokeLater { onCookiesCaptured(c.getValue("sp_dc"), c["sp_key"].orEmpty()) } },
        { msg -> problem = msg })
      onDispose { close() }
    }
  }
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Connect Spotify") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
          problem != null -> Text(problem!!)
          mode == null -> Text("Echo Music can open Spotify's sign-in page as a new tab in your own browser, where you are probably already signed in. Browsers do not let apps read your logins, so after signing in you copy one value (sp_dc) back here.\\n\\nOr use the automatic way: a clean browser window opens, you sign in once, and the app connects by itself.")
          mode == "mine" -> {
            Text("1. Sign in in the tab that just opened (use your saved login).\\n2. On open.spotify.com press F12 > Application (Chrome/Brave/Edge) > Cookies > open.spotify.com.\\n3. Copy the value of sp_dc (and sp_key if shown) and paste below.")
            androidx.compose.material3.OutlinedTextField(spDc, { spDc = it }, label = { Text("sp_dc") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            androidx.compose.material3.OutlinedTextField(spKey, { spKey = it }, label = { Text("sp_key (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          }
          else -> Text("A clean browser window has opened. Sign in to Spotify there; this connects on its own.")
        }
      }
    },
    confirmButton = {
      when {
        problem != null -> {}
        mode == null -> androidx.compose.material3.TextButton(onClick = {
          runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(SpotifyAuth.LOGIN_URL)) }
          mode = "mine"
        }) { Text("Open in my browser") }
        mode == "mine" -> androidx.compose.material3.TextButton(onClick = { onCookiesCaptured(spDc.trim(), spKey.trim()) }, enabled = spDc.isNotBlank()) { Text("Connect") }
      }
    },
    dismissButton = {
      Row {
        if (mode == null) androidx.compose.material3.TextButton(onClick = { mode = "auto" }) { Text("Automatic") }
        androidx.compose.material3.TextButton(onClick = onDismiss) { Text(if (mode == "auto" && problem == null) "Cancel" else "Close") }
      }
    },
  )
}

"""
PATCHES["com/music/echo/ui/screens/SpotifyImportScreen.kt"] = [
    (re.compile(r"@Composable\nprivate fun SpotifyLoginSheet\(.*?(?=@Composable\nprivate fun SpotifyAddByLinkDialog)", re.S), SPOTIFY_LOGIN.replace("\\", "\\\\")),
    (re.compile(r"import android\.webkit\.\w+\n|import androidx\.compose\.ui\.viewinterop\.AndroidView\n"), ""),
]
PATCHES["com/music/echo/ui/player/MiniPlayer.kt"].append(("playerConnection.service.toggleLike()", "playerConnection.toggleLike()"))
PATCHES["com/music/echo/ui/screens/HomeScreen.kt"] += HOMEFN
PATCHES["com/music/echo/ui/component/Lyrics.kt"] += [(re.compile(r",?\s*decorFitsSystemWindows = false"), "")]
PATCHES["com/music/echo/ui/screens/settings/DiscordSettings.kt"] += [(re.compile(r"\s*shapes = ButtonDefaults\.shapes\(\),?"), " ")]
# Player.kt: the animated cover video uses Android's ExoPlayer views; drop its body, and the imports it needed
PATCHES["com/music/echo/ui/player/Player.kt"] += [
    (re.compile(r"  var isVideoReady by remember\(videoUrl\).*?modifier = modifier\.alpha\(alpha\)\n  \)\n\}", re.S), "  // desktop: animated cover video is not ported yet\n}"),
    (re.compile(r"^import (android\.view\.(TextureView|ViewGroup).*|androidx\.compose\.ui\.viewinterop\.AndroidView|androidx\.media3\.(common\.MimeTypes|exoplayer\.(DefaultLoadControl|ExoPlayer|trackselection\..*)|ui\..*))\n", re.M), ""),
]
PATCHES["com/music/echo/ui/screens/settings/DiscordSettings.kt"].append((re.compile(r"\bLinearWavyProgressIndicator\("), "androidx.compose.material3.LinearProgressIndicator("))
# desktop windows are wide: a hero card as wide as the window crops the cover to a strip, so cap it and let the carousel show neighbours
PATCHES["com/music/echo/ui/screens/HomeScreen.kt"].append(("val heroWidth = configuration.screenWidthDp.dp - 32.dp", "val heroWidth = minOf(configuration.screenWidthDp.dp - 32.dp, 300.dp)"))
PATCHES["com/music/echo/ui/screens/HomeScreen.kt"] += [
    ("modifier = Modifier.fillMaxWidth().height(360.dp).animateItem()", "modifier = Modifier.fillMaxWidth().height(420.dp).animateItem().carouselWheel(carouselState, songs.size)"),
    (re.compile(r"modifier = Modifier\.fillMaxWidth\(\)\.height\(320\.dp\)\n(\s*\) \{ i ->)"), r"modifier = Modifier.fillMaxWidth().height(320.dp).carouselWheel(carouselState, discoverList.size)\n\1"),
]
# app icon: on desktop the window icon follows the preference live, so no "restart to apply" (Android launcher intent) is needed
PATCHES["com/music/echo/ui/screens/settings/AppIconSettingsScreen.kt"] = [
    # icon previews: Android drew adaptive-icon Drawables; the mipmap PNGs are loaded as plain images
    (re.compile(r"    val context = LocalContext\.current\n    val bitmap =\n      remember\(option\.iconRes\) \{.*?\n      \}\n", re.S), "    val painter = painterResource(option.iconRes)\n"),
    ("if (bitmap != null) {\n        Image(\n          bitmap = bitmap,", "run {\n        Image(\n          painter = painter,"),
    (re.compile(r"import androidx\.core\.(content\.ContextCompat|graphics\.drawable\.toBitmap)\n"), ""),
    (re.compile(r"    coroutineScope\.launch \{\n      val result =.*?\n    \}\n  \}\n", re.S), "    coroutineScope.launch { snackbarHostState.showSnackbar(\"Icon updated\") }\n  }\n"),
]
# Home's hero cards: centre card big, neighbours smaller (standins/Wheel.kt); the Daily Discover row keeps the Material carousel
PATCHES["com/music/echo/ui/screens/HomeScreen.kt"] += [
    ("val carouselState = rememberCarouselState { currentSongs.size }", "val carouselState = rememberWheelState { currentSongs.size }"),
    ("HorizontalCenteredHeroCarousel(\n", "WheelCarousel(\n"),
    ("return@HorizontalCenteredHeroCarousel", "return@WheelCarousel"),
    (re.compile(r"\.maskClip\(MaterialTheme\.shapes\.extraLarge\)\n(\s*)\.maskBorder\("), r".clip(MaterialTheme.shapes.extraLarge)\n\1.border("),
    ("import androidx.compose.ui.draw.clip\n", "import androidx.compose.ui.draw.clip\nimport androidx.compose.foundation.border\n"),
]
# hero card: a portrait card would crop the square cover, so show the whole cover (Fit) over a blurred copy of itself
_ind = " " * 24
PATCHES["com/music/echo/ui/screens/HomeScreen.kt"] += [
    (re.compile(r"(contentScale = ContentScale\.Crop,\n\s*)modifier = Modifier\.fillMaxSize\(\)\n(\s*\)\n\n\s*Box\(\n\s*modifier =\n\s*Modifier\.fillMaxSize\(\)\n\s*\.background\(\n\s*Brush\.verticalGradient)"),
     lambda m: m.group(1) + "modifier = Modifier.fillMaxSize().blur(28.dp)\n" + _ind + ")\n" + _ind + "AsyncImage(\n" + _ind + "  model = ImageRequest.Builder(coil3.PlatformContext.INSTANCE).data(localSong?.thumbnailUrl ?: song.thumbnailUrl).crossfade(true).build(),\n" + _ind + "  contentDescription = null,\n" + _ind + "  contentScale = ContentScale.Fit,\n" + _ind + "  alignment = Alignment.Center,\n" + _ind + "  modifier = Modifier.fillMaxSize()\n" + m.group(2)),
    ("import androidx.compose.ui.draw.clip\n", "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.blur\n"),
]
# Spotify import: if restoring the saved session hangs (token refresh with no answer), give up after 15 s instead of leaving every row greyed out
PATCHES["com/music/echo/spotifyimport/SpotifyImportViewModel.kt"] = [
    ("runCatching { repository.restoreSession() }", "runCatching { kotlinx.coroutines.withTimeoutOrNull(15_000) { repository.restoreSession() } ?: SpotifyImportSession() }"),
]
# Spotify import: a YouTube search that fails once (busy / rate-limited with many parallel searches) dropped the track for good; retry twice, and match fewer tracks at a time
PATCHES["com/music/echo/spotifyimport/SpotifyImportRepository.kt"] = [
    (re.compile(r"    val searchResult =\n      YouTube\.search\(\n\s*query = SpotifyMapper\.buildSearchQuery\(track\),\n\s*filter = YouTube\.SearchFilter\.FILTER_SONG,\n\s*\)\n\s*\.getOrElse \{ error ->\n\s*if \(error is CancellationException\) \{\n\s*throw error\n\s*\}\n\s*return null\n\s*\}\n"),
     """    suspend fun trySearch() =
      YouTube.search(query = SpotifyMapper.buildSearchQuery(track), filter = YouTube.SearchFilter.FILTER_SONG)
        .getOrElse { error -> if (error is CancellationException) throw error; null }
    var searchResult = trySearch()
    var attempt = 0
    while (searchResult == null && attempt < 2) {
      kotlinx.coroutines.delay(700L * (attempt + 1))
      searchResult = trySearch()
      attempt++
    }
    if (searchResult == null) return null
"""),
    (re.compile(r"MAX_CONCURRENT_MATCHES = \d+"), "MAX_CONCURRENT_MATCHES = 3"),
]
PATCHES["com/music/echo/ui/screens/musicflow/MusicFlowScreen.kt"] = [(".let(SongEntity::toggleLike)", ".let { it.toggleLike() }"), (".also(::update)", ".also { update(it) }")]
PATCHES["com/music/echo/ui/player/Thumbnail.kt"].append(("SingletonImageLoader.get(context)", "SingletonImageLoader.get(coil3.PlatformContext.INSTANCE)"))
# the real MaterialShapes class exists in the library at run time, so a stand-in with the same name clashes: patch the use instead
PATCHES["com/music/echo/ui/player/Thumbnail.kt"] += [
    ("MaterialShapes.Clover8Leaf.toShape()", "androidx.compose.foundation.shape.RoundedCornerShape(35)"),
    ("import androidx.compose.material3.MaterialShapes\n", ""),
    ("import androidx.compose.material3.toShape\n", ""),
]
out = "build/gen-src/patched"
os.makedirs(out, exist_ok=True)
for path, subs in PATCHES.items():
    t = open(BASE + path, encoding="utf-8").read()
    for a, b in subs:
        if isinstance(a, str):
            assert a in t, f"{a} not found in {path}"
            t = t.replace(a, b)
        else:
            t, n = a.subn(b, t)
            assert n or a in (COIL[0], COIL_LOADER[0], COIL_HW[0]) or a in [x[0] for x in NAV] or a is LOADING[0] or a is FNREF1[0] or a is FNREF2[0], f"pattern not found in {path}"
    open(f"{out}/{os.path.basename(path)}", "w", encoding="utf-8").write(t)
