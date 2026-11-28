# Echo Music Desktop: feature roadmap

Goal: match the Android app (EchoMusicApp/Echo-Music) on Windows. Every fix is a new build, named
`EchoMusic-vN` (number in `desktop/VERSION`, shown in the window title). Build: GitHub Actions
"Desktop build" on branch `desktop-port`; download the artifact from the run page.

## Status of the open problem
Playback (v1-v4) failed with "VLC could not play this stream" although YouTube answers the stream
URL with HTTP 206. v5 downloads the song to a temp file first and plays the file. If v5 still fails,
read the red error text in the right-hand panel (it now shows VLC's own error lines) and the log
`%TEMP%\echomusic-vlc.log`.

## Done (v5)
Search (songs), home feed, open albums and playlists, quick picks, recent strip, queue with
next/previous/auto-advance, shuffle, repeat (off/all/one), liked songs (saved in
`%APPDATA%\EchoMusic\liked.tsv`), synced lyrics (LRCLIB), volume, now-playing panel, version in title.

## Next, in rough priority (all doable on Windows)
1. Search filters: albums, artists, playlists, videos; search suggestions.
2. Artist pages (`YouTube.artist`), album/playlist headers with Play all and Shuffle.
3. Queue view (reorder, remove, play next, add to queue); right-click or "..." menu on songs.
4. Library: saved playlists (create, add/remove songs), history, most played; persistence layer.
5. Downloads / offline: `AudioCache` already stores files; add a Downloads page and keep files.
6. Home mood chips (`HomePage.chips`), explore/charts/moods pages, podcasts.
7. Media keys and Windows media overlay (JNA or jpackage native hook), system tray, minimise to tray.
8. Settings: audio quality, content country/language, crossfade, cache size, theme (light/dark).
9. More lyrics providers (BetterLyrics, KuGou, SimpMusic, YouLyPlus modules are plain Kotlin, reuse
   them like `lrclib-jvm`), word-by-word lyrics, translation.
10. Login with YouTube account (cookie), sync library, Spotify import, Listen Together.
11. Packaging: signed installer (MSI/EXE with WiX) instead of a zipped folder.

## Android-only, no desktop equivalent
Set as ringtone, Cast, Bluetooth/headphone resume, pause on mute, dynamic island notification,
Echo Find (microphone song recognition needs a different audio capture path), USB audio, in-app OTA.

## Reuse notes
`desktop/*-jvm` modules compile the Android modules' sources unchanged (`srcDir`). The Android
modules `lyrics`, `kugou`, `betterlyrics`, `simpmusic`, `youlyplus` can be added the same way.
Stream URLs: `StreamResolver` (NewPipe extractor first, then InnerTube clients), each URL probed
with a ranged request. The Android app also uses an external library (innertubex) with PO tokens;
not portable as is.
