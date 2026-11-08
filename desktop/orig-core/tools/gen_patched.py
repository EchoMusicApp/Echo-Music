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
for f in ("ui/component/AlbumGradient.kt", "ui/component/LyricsImageCard.kt", "ui/screens/ambient/AmbientGlowBackground.kt"):
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
            assert n, f"pattern not found in {path}"
    open(f"{out}/{os.path.basename(path)}", "w", encoding="utf-8").write(t)
