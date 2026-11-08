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
COIL_HW = (re.compile(r"\n\s*\.allowHardware\([^)]*\)|import coil3\.request\.allowHardware\n"), "")
PATCHES["com/music/echo/ui/component/Items.kt"] = [COIL]
for f in ("ui/component/AlbumGradient.kt", "ui/component/LyricsImageCard.kt", "ui/screens/ambient/AmbientGlowBackground.kt"):
    PATCHES["com/music/echo/" + f] = [COIL, COIL_LOADER, COIL_HW]
# Theme: no Material You on desktop, Palette works on skia bitmaps
PATCHES["com/music/echo/ui/theme/Theme.kt"] = [
    ("Build.VERSION.SDK_INT >= Build.VERSION_CODES.S", "false"),
    ("dynamicDarkColorScheme(context)", "androidx.compose.material3.darkColorScheme()"),
    ("dynamicLightColorScheme(context)", "androidx.compose.material3.lightColorScheme()"),
    (re.compile(r"import androidx\.compose\.material3\.dynamic\w+\n"), ""),
    ("import android.graphics.Bitmap", "import org.jetbrains.skia.Bitmap"),
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
