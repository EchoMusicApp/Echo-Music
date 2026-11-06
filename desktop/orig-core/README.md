# orig-core: the original Echo Music files, compiled for Windows (JVM)

Native port, one piece at a time. `files.txt` lists the original app files that compile here.
CI (`Native port - compile original files`, branch `native-port`) prints which listed files still fail and why.

## Done
1. Files with no Android-only imports (121 found).
2. Stand-ins: `timber/` (logging), `BuildConfig`, and **R.string / R.plurals + stringResource()**
   generated from the original `strings.xml` by `tools/gen_r.py` (re-run it if strings change).

3. `desktop/core-jvm`: the original `core` + `domain` modules (65 files: db entities Song/Album/Artist/Playlist, MediaMetadata, models, constants). Deferred there: the Room database itself (`MusicDatabase`, repositories, StringExt) - needs Room KMP/SQLite.

## Next (one at a time)
3b. Room database on desktop (MusicDatabase, DAOs, repositories).
4. Drawables: `painterResource` / `R.drawable` (1566 uses).
5. Glass blur effects (RuntimeShader/RenderEffect) via Skia's runtime effects.
6. Android Material "expressive" controls that desktop Material lacks.
7. Player (Media3 -> desktop audio), audio lookup, navigation, ViewModels (Hilt), settings storage (DataStore).
8. Desktop layout and window.
