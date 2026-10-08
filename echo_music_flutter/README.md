# Echo Music (Flutter / iOS & macOS)

Flutter port of the [Echo Music](../Echo-Music) Android app so it can run on iPhone, iPad and Mac.
It streams from YouTube Music ad-free, with synced lyrics, offline downloads, a local
library/history database, and background playback with lock-screen / Control Center controls.

## What is ported

| Area | Android source | Flutter port |
|---|---|---|
| YouTube Music API (InnerTube) | `:innertube` module | `lib/innertube/` — HTTP client, client identities, renderer parsers, page models, `YouTube` facade |
| Stream resolution | `InnerTubeXResolver` / `YTPlayerUtils` | `lib/stream/stream_resolver.dart` — direct-URL InnerTube clients (VISIONOS first), ranged-GET validation, `youtube_explode_dart` + Piped fallbacks |
| Playback / queue / radio | `MusicService`, `:playback` queues | `lib/playback/` — `audio_service` + `just_audio` handler, `ListQueue` / `YouTubeQueue` / `YouTubeAlbumRadio`, persistent queue, sleep timer, history |
| Database | Room (`:core` entities) | `lib/data/database.dart` — sqflite with reactive `watch()` queries |
| Settings | DataStore keys | `lib/data/settings.dart` |
| Lyrics | `:lyrics`, `:lrclib`, `:kugou`, `:betterlyrics` | `lib/lyrics/` — LRC/rich-sync parser, LRCLIB, KuGou, BetterLyrics (TTML), YouTube lyrics |
| Downloads | `DownloadUtil` | `lib/data/download_manager.dart` |
| Account | `LoginScreen` WebView + `SyncUtils` | `lib/ui/screens/login_screen.dart`, `lib/data/sync.dart` |
| UI | Compose screens | `lib/ui/` — Home, Explore (new releases, moods, charts), Search, Library, Album, Artist, Playlists (online + local), History, Stats, Settings, Account, full player, mini player, lyrics, queue |

Not ported (Android-only or out of scope for a first iOS build): Listen Together, Discord
Rich Presence, Echo Find (ShazamKit), canvas videos, equalizer/DSP, Spotify import,
local-media scanning, widgets, Last.fm scrobbling, AI lyric translation.

## Requirements

- Flutter 3.47.6, pinned with FVM (`.fvmrc`); run commands as `fvm flutter ...`
- Xcode 26 with iOS simulators, CocoaPods
- An Apple ID (free is fine) to run on a physical device

## Run

```bash
fvm flutter pub get
fvm flutter run -d "iPhone 17"      # iOS simulator
fvm flutter run -d macos            # Mac app
```

### On your Mac

`fvm flutter build macos --release` produces `build/macos/Build/Products/Release/Echo Music.app`.
The app is sandboxed with outgoing network access only (`macos/Runner/*.entitlements`);
downloads and the database live in its container under `~/Library/Containers/echo.music.iad1tya`.
The window opens at 1100×780 and can't shrink below 380×640, since the UI is the phone layout.

### On your iPhone

1. `open ios/Runner.xcworkspace`
2. Select the **Runner** target → *Signing & Capabilities* → pick your **Team**
   (the bundle id is `echo.music.iad1tya`; change it if Xcode reports a conflict).
3. Plug in / pair the phone, enable Developer Mode on it, then
   `flutter run -d <your iphone>` or press Run in Xcode.

A free Apple ID signs apps for 7 days at a time; a paid developer account removes that limit.

## Tests

```bash
flutter test test/innertube_live_test.dart        # live API parsing + stream + lyrics smoke test
flutter test integration_test/play_test.dart -d "iPhone 17"   # playback pipeline on the simulator (or -d macos)
flutter test integration_test/seek_test.dart -d "iPhone 17"   # seeking + end-of-track guard
flutter drive --driver=test_driver/integration_test.dart \
  --target=integration_test/app_test.dart -d "iPhone 17"   # UI walkthrough, screenshots in build/screenshots
```

## Notes on streaming

`AVPlayer` (iOS and macOS) cannot decode WebM/Opus, so the resolver always selects the `audio/mp4` (AAC)
stream. Of the InnerTube clients that return un-ciphered URLs, only **VISIONOS** currently
serves whole files (ANDROID_VR / IOS URLs 403 after the first bytes), which the resolver
detects with a last-byte range probe before handing a URL to the player.

AVPlayer reports twice the real duration for YouTube's fragmented-MP4 audio. The handler
therefore treats InnerTube's `lengthSeconds` as the authoritative duration for the UI and
lock screen, clamps seeks to it, and advances to the next track itself when playback reaches
the real end (`_endGuard` in `lib/playback/audio_handler.dart`).
