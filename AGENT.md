# AGENT.md - Echo Music

Context file for AI agents (Antigravity, Claude Code, etc.) working in this repo.
Keep this file up to date as the project evolves — it's the fastest way to give
the agent full context without re-scanning the whole codebase every session.

## Development Rules & Guidelines

> **Maintenance rule:** whenever you add or change a feature, structure,
> module, build config, or convention in this repo, **update this file in
> the same PR/commit**. This file is only useful if it stays accurate —
> stale context is worse than no context, because the agent will act on it
> confidently and be wrong. Treat an out-of-date `AGENT.md` as a bug.

> **Attribution rule:** if a feature is ported from, adapted from, or
> inspired by another open-source project (even partially — a UI pattern,
> an algorithm, a parsing approach, a whole file), you **must** add or
> update an entry in `README.md`'s **Special Thanks** section in the same
> commit. Follow the existing table format there:
> `| **[Project Name](repo URL)** | What was taken/inspired, described specifically |`.
> Be specific in the description — not "inspiration" alone, but what part
> (e.g. "Material You UI inspiration", "Lyrics implementation reference",
> "Decryption handling and backup playback engine"), matching how existing
> entries like Metrolist, ArchiveTune, and SimpMusic are described. If code
> is copied or closely adapted rather than just conceptually inspired, say
> so plainly in the description, and check the source project's license is
> compatible before merging. Never port/adapt code without adding this
> credit — this applies even for small snippets, not just whole features.

> **Do not push without explicit instruction.** Making code changes
> (editing files, committing locally) is fine whenever asked, but **never
> run `git push` — or otherwise publish changes to GitHub — unless I
> explicitly tell you to push** in that message (e.g. "push this",
> "push it", "commit and push"). Finishing a change, fixing a bug, or being
> asked to "commit" is not by itself permission to push. If you're unsure
> whether "push" was actually meant, stop and ask rather than assuming.
> This applies to every session, not just the current one.

> **Pre-push checklist — run through this every time before pushing:**
>
> 1. **Remove all temporary files.** Before committing and pushing, delete any
>    scratch/debug/automation scripts that were created during the session.
>    Common culprits:
>    - One-off Python/shell scripts (`fix_*.py`, `fix_*.sh`, `patch_*.py`, etc.)
>    - Debug log files (`build.log`, `*.log` — already in `.gitignore` but
>      double-check nothing slipped through)
>    - Temporary data files (`.json`, `.txt` scratch files created during
>      investigation)
>    Check with `git status` and `git ls-files | grep -E '\.(py|sh|log)'`
>    before staging. If a script is genuinely reusable project tooling, move
>    it to `scripts/` with a clear name and docstring; otherwise delete it.
> 2. **Verify the build is clean.** Run
>    `./gradlew :app:compileUniversalGmsDebugKotlin` and confirm
>    `BUILD SUCCESSFUL` with no errors before committing.
> 3. **Write a proper commit message.** Title + body in Conventional Commits
>    format (see "Commit message format" section below).
> 4. **Update `AGENT.md` and docs** if module structure, conventions, or
>    architecture changed — stale context is a bug.

### UI rule: Custom Echo Music Aesthetic (over Material You)

All UI work — new screens, new components, and edits to existing ones —
**must** match the established custom visual style of Echo Music rather than blindly
applying standard Material Design 3 guidelines.

We maintain a dedicated **[DESIGN.md](DESIGN.md)** file which serves as the central
hub for all design decisions, component usage, and style choices in the Echo Music codebase.

**Key Requirements:**
- **Match existing patterns:** Look at the existing UI (like the custom original Settings or Listen Together styles) and replicate their translucent cards, rounded corners, and spacing.
- **Dynamic color with translucency:** Colors come from `MaterialTheme.colorScheme`, but are often modified (e.g. `surfaceVariant.copy(alpha = 0.3f)`) rather than using solid M3 containers.
- **Do not refactor to strict M3:** Do not replace custom top bars with `LargeTopAppBar` or custom grouped cards with standard M3 cards unless explicitly requested by the user.

If a new feature needs a UI pattern not covered in `DESIGN.md`, copy an existing custom pattern from the app rather than pulling from the official Material 3 guidelines.

### Conventions worth following

- Kotlin official style: `val` over `var`, data classes for simple holders,
  sealed classes/interfaces for UI/playback state.
- New dependency versions go in `gradle/libs.versions.toml`
  (`[versions]` + `[libraries]`), referenced via `libs.xxx` — don't hardcode
  version strings in module `build.gradle.kts` files except for one-off
  Google/Firebase classpath deps in the root `build.gradle.kts`.
- New cross-cutting integrations (lyrics source, canvas provider, metadata
  source) → new Gradle module, not a package inside `:app`.
- Feature flags / build-time toggles go through `buildConfigField` in
  `app/build.gradle.kts` (see `CAST_AVAILABLE`, `IS_NIGHTLY` patterns).
- Room schema changes require a migration in `db/` and the schema JSON is
  version-controlled under `app/schemas/` — don't skip migrations.
- **Networking:** Use Ktor for all new network requests. Retrofit is maintained for legacy endpoints but should not be used for new features.

### Things to double check before assuming

- Exact current contents of `com/music/echo/echomusic/` (contains core app-level initializers/application class) and `:unison` (cross-cutting/shared utilities and common base classes) — check source before editing.

## What this app is

Echo Music is a native **Android** music streaming client (Kotlin + Jetpack
Compose) that streams from YouTube Music's catalog ad-free, and layers on
offline downloads, real-time synced lyrics, music recognition ("Echo Find"),
podcasts, local media playback, Spotify import, "Listen Together" (synced
group listening), Discord Rich Presence, and more. It ships as a single
**GMS** build variant (Google Cast + Firebase enabled) — the previous FOSS
(no-Google-services) flavor has been removed. See "Build variants" below.

Package/namespace: `echo.music.iad1tya` (app module). Application ID matches.

## Tech stack

- **Language:** Kotlin (JVM target 21), Gradle Kotlin DSL (`.kts`)
- **UI:** Jetpack Compose (Material 3, `material3 = 1.5.0-alpha18`), adaptive
  layouts (`androidx.compose.material3.adaptive`), `haze` for blur effects,
  `materialKolor` for Material-You-style dynamic color, Coil 3 for image
  loading, Lottie for animations, `shimmer` for loading placeholders,
  `smoothCorner` for custom shapes.
- **Architecture:** MVVM — ViewModels (`viewmodels/`, 30 files) + Compose
  screens (`ui/screens/`) + Repository-ish data layer under `data/`/`db/`.
  Hilt (`dagger.hilt`) is used throughout for DI (see `di/AppModule.kt`,
  `di/NetworkModule.kt`).
- **Persistence:** Room (`db/entities/`, `db/daos/`), DataStore Preferences
  (`utils/dataStore`).
- **Playback:** Media3 / ExoPlayer (`media3`, `media3-session`, `media3-hls`,
  `media3-ui`, `media3-okhttp`) — core logic in `playback/` (`MusicService.kt`,
  `PlayerConnection.kt`, `ExoDownloadService.kt`, `ChunkingDataSource.kt`,
  `SleepTimer.kt`, `audio/`, `queues/`). FFmpegKit is used for audio export.
- **Networking:** Ktor client + Retrofit (both present — Retrofit mainly for
  legacy/simple calls, Ktor for newer code), OkHttp, Jsoup for scraping,
  kotlinx.serialization + Gson.
- **Other:** Protobuf (lite, for message serialization), Guava +
  coroutines-guava, WorkManager, Firebase Analytics/Crashlytics
  (**GMS flavor only**), Google Drive API (GMS flavor, for backup/sync),
  Play Services Cast (GMS flavor).
- **Build system:** Gradle version catalog at `gradle/libs.versions.toml`
  (always add new deps here, not inline). AGP `9.0.0`, Kotlin `2.3.10`,
  KSP for annotation processing (Room, Hilt).

## Module map (multi-module Gradle project)

Root `:app` depends on all of these library modules — each is a focused,
mostly-independent feature/integration:

| Module | Purpose |
|---|---|
| `:app` | Main application — UI, ViewModels, MusicService, DB, DI, app-level orchestration |
| `:core` | Shared models, constants, Room database, DataStore, and common utilities |
| `:playback` | Pure ExoPlayer/Media3 logic — Queues, Equalizer, ChunkingDataSource, SleepTimer, BeatAnalyzer |
| `:lyrics` | Lyrics orchestration — LyricsHelper, LyricsEntry, LyricsUtils, all provider impls, AI translation |
| `:innertube` | YouTube Music InnerTube API client (the core music source) |
| `:kugou`, `:lrclib`, `:betterlyrics`, `:youlyplus`, `:paxsenixlyrics`, `:simpmusic` | Individual lyrics source providers (each consumed by `:lyrics`) |
| `:shazamkit` | Music recognition ("Echo Find") |
| `:canvas`, `:echomusiccanvas`, `:applecanvas` | Canvas-style looping video backgrounds for tracks (different providers) |
| `:artistvideo` | Artist video features |
| `:unison` | Cross-cutting shared utility module (check source before editing) |

When adding a new external integration (a new lyrics source, a new canvas
provider, etc.), the existing pattern is: **new Gradle module**, register it
in `settings.gradle.kts`, add it as an `implementation(project(":name"))` in
`app/build.gradle.kts`, wire it up via Hilt in `di/`.

## App module internal structure

Path: `app/src/main/kotlin/com/music/echo/` (note: source dir is `kotlin/`,
not `java/` — despite what older internal docs may say).

```
ai/             AI features (lyrics translation providers, etc.)
api/            External API clients not covered by a dedicated module
constants/      Preference keys / constant definitions (DataStore keys live here)
data/           Data layer glue
db/
  entities/     Room entities (Song, Album, Artist, Playlist, Lyrics, etc.)
  daos/         Room DAOs
di/             Hilt modules (AppModule, NetworkModule, Qualifiers, entry points)
discord/        Discord Rich Presence integration
echomusic/      Core app-level classes (Application class, core initializers)
eq/             Equalizer
extensions/     Kotlin extension functions
listentogether/ "Listen Together" synced group listening feature
localmedia/     Local on-device media file playback
lyrics/         Lyrics orchestration (aggregates the lyrics provider modules)
models/         Shared data models
playback/       Media3/ExoPlayer service, download manager, queueing, audio
quicksettings/  Android quick settings tile
recognition/    Music recognition (Echo Find) app-side logic
spotify/        Spotify API integration
spotifyimport/  Import playlists/tracks from Spotify
ui/
  component/    Reusable Compose components (backdrop, floating tab bar, shimmer, etc.)
  menu/         Context/dropdown menus
  player/       Now-playing / player UI
  screens/      Top-level screens (Home, Search, Library, Album, Artist, Settings, etc.)
  theme/        Theme.kt, Type.kt, Font.kt, color extraction, dynamic color
  utils/        UI-specific utilities
utils/          General utilities (largest non-UI folder, 44 files)
viewmodels/     ViewModels, one (or a few) per screen/feature (30 files)
widget/         Home-screen widget
```

## UI / design system notes

- Material 3 with **dynamic color**: on Android 12+, uses system dynamic
  color by default; otherwise generates a scheme from `DefaultThemeColor`
  (`0xFFED5564`) via `materialKolor`'s `rememberDynamicColorScheme`
  (spec `SPEC_2025`, style `TonalSpot`). See `ui/theme/Theme.kt`.
  A custom "pure black" dark mode variant is supported.
  Users can override the seed color from settings.
- Adaptive layouts (`androidx.compose.material3.adaptive`) are used —
  design new screens to work across phone/tablet/foldable widths.
  A "floating tab bar" custom component exists at
  `ui/component/floatingtabbar/` — prefer reusing it over building new nav UI.
- Icons: partly Material Symbols/Icons Extended
  (`androidx.compose.material.material-icons-extended`), plus custom SVG
  drawables generated via `scripts/compose_svg_drawable.py` and
  `scripts/download_material_icons.py` — check those scripts before manually
  adding new vector assets.
- Shimmer loading placeholders (`ui/component/shimmer/`, `libs.shimmer`) are
  the standard loading-state pattern — use them for new async-loading UI
  instead of spinners.

## Commit message format (required)

Every commit/PR title **must** follow Conventional Commits style, matching
this repo's actual history:

```
<type>(<scope>): <short, imperative summary>
```

Examples from this repo's own log:
- `feat(ui): implement Material You pill-shaped inputs and dialog buttons`
- `fix(db): prevent SQLiteConstraintException crash on map inserts`
- `fix(spotify): handle SSO popups in Spotify login by intercepting multiple windows`
- `fix(datastore): gracefully handle ClassCastException on corrupted preference keys`
- `build(deps): upgrade safe non-compose dependencies`

Rules:
- **type**: `feat`, `fix`, `build`, `chore`, `refactor`, `docs`, `perf`,
  `test`, or `ci` — pick the one that actually matches the change.
- **scope**: the module or area touched, lowercase, e.g. `ui`, `db`,
  `spotify`, `datastore`, `deps`, `playback`, `lyrics`, `di`. Keep it short
  and specific — this is what makes the log scannable.
- **summary**: imperative mood ("implement", "fix", "prevent" — not
  "implemented" or "fixes"), no trailing period, concise enough to read as
  a single line in `git log --oneline`.
- **Body** (commit description / PR description): a short paragraph
  explaining *what changed and why*, specific enough that someone reading
  it later understands the change without opening the diff — e.g. naming
  the exact components/files affected and the concrete before→after
  behavior, the way the pill-shaped-inputs commit names `OutlinedTextField`,
  `CircleShape (24.dp)`, and the `TextButton`→`Button` swap explicitly.
  Avoid vague bodies like "UI improvements" or "bug fixes."

When pushing a new feature or fix, generate both the title and body in this
format, and don't omit the body for anything beyond a trivial one-line fix.

## Build variants

**GMS only.** The `foss` product flavor has been removed — do not add code,
docs, or CI steps that reference a `foss` build, and do not re-introduce a
`foss` flavor without an explicit decision to do so. `variant` dimension now
has a single flavor: `gms` (Google Cast + Firebase enabled). The `abi`
dimension is unchanged: `universal`, `arm64`, `armeabi`, `x86`, `x86_64`.

```bash
# Debug build
./gradlew assembleUniversalGmsDebug

# Release build (needs signing env vars: STORE_PASSWORD, KEY_ALIAS,
# KEY_PASSWORD and app/keystore/release.keystore)
./gradlew assembleUniversalGmsRelease
```

Release builds are also GMS only — there is no separate FOSS release
artifact anymore. `gmsImplementation`-scoped dependencies (Firebase, Cast,
Play Services Auth, Google Drive API) are effectively always active now;
new Google-Play-Services-dependent code no longer needs flavor gating, but
keep using `gmsImplementation` in `build.gradle.kts` for consistency and in
case a FOSS flavor is reintroduced later.
min/target/compile SDK: `minSdk 26`, `targetSdk 36`, `compileSdk 36`.
NDK `27.0.12077973`. JDK 21 (kotlin/java toolchain).

## Config & secrets

- `local.properties` (from `local.properties.template`) — Android SDK path.
  Never commit.
- `app/google-services.json` — Firebase config, **optional**; app builds fine
  without it (GMS-only feature).
- Build-time secrets read from `local.properties` first, then env vars:
  `LASTFM_API_KEY`, `LASTFM_SECRET`, `GH_CLIENT_ID`, `GH_CLIENT_SECRET`.
  Also `FLOW_NEURO_BASE_URL` / `FLOW_NEURO_API_KEY` (defaults to
  `https://api.flowneuroengine.com`) as Gradle properties.
- AI lyrics translation is configured **in-app** (Settings → AI Settings),
  not at build time — supports OpenRouter (default) or custom
  OpenAI/Anthropic/Gemini-compatible providers.
- Never commit: `local.properties`, `*.keystore`, real `google-services.json`,
  any `gradle.properties` containing signing credentials.

## Testing

Test coverage is currently minimal — only a handful of unit tests exist under
`app/src/test`, and no `androidTest` (instrumented) tests. When adding
non-trivial logic (parsers, repository logic, playback queue logic), prefer
adding a unit test alongside it rather than assuming existing coverage will
catch regressions. CI (`.github/workflows/android-build.yml`) builds the app
but there's no dedicated test-run gate to rely on — verify manually.

## CI

- `.github/workflows/android-build.yml` — build check
- `.github/workflows/codeql.yml` — static analysis / security scanning