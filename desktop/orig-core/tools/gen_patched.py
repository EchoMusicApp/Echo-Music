"""Writes desktop-patched copies of a few original files into build/gen-src/patched/ (originals stay untouched).
Run from desktop/orig-core."""
import os
BASE = "../../app/src/main/kotlin/"
PATCHES = {
    # the library's SliderDefaults.Track(trackCornerSize=...) is internal on desktop; DesktopSliderTrack is the same call
    "com/music/echo/ui/component/VolumeSlider.kt": [("SliderDefaults.Track(", "androidx.compose.material3.DesktopSliderTrack(")],
}
out = "build/gen-src/patched"
os.makedirs(out, exist_ok=True)
for path, subs in PATCHES.items():
    t = open(BASE + path, encoding="utf-8").read()
    for a, b in subs:
        assert a in t, f"{a} not found in {path}"
        t = t.replace(a, b)
    open(f"{out}/{os.path.basename(path)}", "w", encoding="utf-8").write(t)
