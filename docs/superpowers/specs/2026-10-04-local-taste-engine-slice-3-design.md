# Local Taste Engine — Slice 3 Design

Date: 2026-10-04
Status: draft (pending design approval)

## 1. Context

Slice 2 (`2026-10-04-local-taste-engine-slice-2-design.md`) added a Last.fm
recommendation pipeline with a 1-hour taste-profile cache. Slice 3 completes
the local taste engine with:

1. **Genre awareness** — a `GenresRepository` scored from listening history and
   Last.fm tags.
2. **YouTube Music taste-signals endpoint** — a `fetchTasteSignals` call that
   does not exist in `:innertube` yet.
3. **Cross-screen generation status** — the `GenerationStatus` singleton that
   was deferred from Slice 1 §6.4.
4. **AI dialog integration** — the Generate flow surfaces in
   `CreateAiPlaylistDialog` alongside the existing LLM path.

## 2. Goals

1. `GenresRepository` derives the user's genre preferences from
   `song_play_stats` + Last.fm tags, feeding a genre dimension into scoring.
2. `fetchTasteSignals` — a new `:innertube` endpoint returning per-track genre
   and mood tags, so the engine doesn't depend solely on Last.fm for genre
   data.
3. `GenerationStatus` singleton — `isGenerating` / `message` / `error` readable
   from multiple screens (Generate screen, Library, CreateAiPlaylistDialog).
4. The Generate flow is reachable from `CreateAiPlaylistDialog` as an alternative
   to the LLM prompt path.

## 3. Non-goals

- Removing or altering the existing LLM path (`ai/` package) — it stays as-is.
- Compose UI tests, screenshot tests.
- Slice 1 and 2 behaviour changes (backwards-compatible).

## 4. Architecture and components

### 4.1 New and changed files

| Concern | Path | Change |
|---|---|---|
| Repository | `app/src/main/kotlin/com/music/echo/generate/GenresRepository.kt` | new |
| Status | `app/src/main/kotlin/com/music/echo/generate/GenerationStatus.kt` | new (singleton) |
| Innertube endpoint | `innertube/src/main/kotlin/com/music/innertube/YouTube.kt` | add `fetchTasteSignals(...)` |
| Innertube model | `innertube/src/main/kotlin/com/music/innertube/models/` | new request/response models |
| DI | `app/src/main/kotlin/com/music/echo/di/AppModule.kt` | `@Provides` for GenresRepository, GenerationStatus |
| Dialog | `app/src/main/kotlin/com/music/echo/ui/component/CreateAiPlaylistDialog.kt` | add "Generate from taste" action |
| Screen | `app/src/main/kotlin/com/music/echo/ui/screens/generate/GenerateScreen.kt` | read GenerationStatus instead of local StateFlow |
| ViewModel | `app/src/main/kotlin/com/music/echo/viewmodels/GenerateViewModel.kt` | publish to GenerationStatus |
| Tests | `app/src/test/…` | new (§7) |

### 4.2 GenresRepository

```kotlin
class GenresRepository @Inject constructor(
  private val database: MusicDatabase,
  private val tasteApi: LastFmTasteApi,
  private val tasteProfileProvider: TasteProfileProvider,
) {
  suspend fun topGenres(limit: Int = 10): List<GenreWeight>
  suspend fun genresForTrack(trackKey: String): List<String>
}
```

- `topGenres`: joins `song_play_stats` tracks against Last.fm
  `tag.getTopTags` for each; aggregates tag counts weighted by play time.
- `genresForTrack`: single-track lookup via Last.fm tag API, cached in-memory
  (1-hour TTL per track, matching Slice 2's cache pattern).
- On failure: returns empty list — genre bonus contributes `0`, generation
  still works.

### 4.3 Genre scoring

Added to Slice 2's scoring table as another additive bonus:

| Signal | Weight | Source |
|---|---|---|
| `GENRE_MATCH_BONUS` | `+1.0f` | candidate's genres ∩ user's top genres |

Maximum external bonus across all three slices: `+5.5f` per candidate
(`+4.5f` Slice 2 externals + `+1.0f` genre match).

### 4.4 GenerationStatus singleton

```kotlin
@Singleton
class GenerationStatus @Inject constructor() {
  val state: StateFlow<GenerationState>
  fun start(message: String)
  fun update(message: String)
  fun succeed(playlistId: String)
  fun fail(error: String)
  fun cancel()
}
```

`GenerationState` is a sealed class:

```kotlin
sealed class GenerationState {
  data object Idle : GenerationState()
  data class Running(val message: String) : GenerationState()
  data class Done(val playlistId: String) : GenerationState()
  data class Failed(val error: String) : GenerationState()
}
```

- `GenerateViewModel` writes to it (replacing its local `isGenerating` /
  `message` / `error` StateFlows from Slice 1 §6.4).
- `GenerateScreen` reads from it.
- `CreateAiPlaylistDialog` reads from it to show a progress indicator.

### 4.5 fetchTasteSignals endpoint

A new `YouTube.fetchTasteSignals(videoId)` call in `:innertube` returning:

```kotlin
data class TasteSignals(
  val videoId: String,
  val genres: List<String>,
  val moodTags: List<String>,
)
```

Built from the `next` response's `musicResponsiveListItemRenderer` metadata —
YouTube Music includes genre/mood shelf data that `:innertube` currently
discards.

### 4.6 CreateAiPlaylistDialog integration

Add a secondary action alongside the existing text-prompt path:

- **"Generate from taste"** — triggers the same `GenerationStatus`-driven
  generation flow.
- While running: dialog shows progress from `GenerationStatus.state`.
- On success: navigates to the new playlist (same as Generate screen).
- The LLM text-prompt path remains unchanged and available.

## 5. Data model

No new tables. `genres` are derived from existing `song_play_stats` +
Last.fm API + `fetchTasteSignals`, not persisted as a separate table in Slice 3.

If genre persistence becomes necessary (Slice 4+), it would be a
`track_genres` table — deferred.

## 6. Testing

| Tier | Asserts | Location |
|---|---|---|
| 1 — pure logic **(must)** | genre-match bonus arithmetic; `GenerationState` transitions; `GenresRepository` aggregation with fake tag data | `app/src/test` |
| 2 — endpoint parsing **(should)** | `fetchTasteSignals` parses a fixture `next` response into correct genres/moodTags | `innertube/src/test` |
| 3 — flow **(should)** | GenerationStatus flows correctly across multiple collectors; cancel resets state; dialog reads Running→Done | `app/src/test` |

Dependencies: same as Slice 1 §12.3. New: `innertube` module needs
`testImplementation(libs.junit)` if it doesn't have it already.

## 7. Acceptance criteria

1. `GenresRepository.topGenres()` returns non-empty when user has listening
   history and Last.fm connected.
2. Generation triggered from `CreateAiPlaylistDialog` produces the same
   playlist as generation from the Generate screen.
3. `GenerationStatus` is readable from both Generate screen and
   CreateAiPlaylistDialog simultaneously; updates propagate to all collectors.
4. `fetchTasteSignals` returns genres/moodTags from a real YouTube Music
   response; parses correctly on a fixture.
5. Genre bonus is additive — candidates with no genre match still score from
   local + Slice 2 signals.
6. All tests pass; CI runs them.

## 8. Known limitations and risks

| Risk | Impact | Handling |
|---|---|---|
| `fetchTasteSignals` response shape changes (YouTube Music API drift) | Parsing breaks silently | Parse defensively with `getOrElse`; genre bonus degrades to 0. Test with fixture. |
| `:innertube` module grows with taste-signal models | Module complexity | Keep models minimal — only `TasteSignals` and its nested types |
| GenerationStatus as `@Singleton` introduces a process-lifetime dependency | Harder to test | Expose via interface; tests use a fake implementation |
| Genre aggregation depends on Last.fm coverage of every track | Missing genres for niche/local tracks | Empty genres → no genre bonus, not an error |
| CreateAiPlaylistDialog has two generation paths (LLM + taste) | UI complexity | LLM path is default; taste path is secondary action, clearly labelled |

## Appendix A — provenance

| Ported from | File |
|---|---|
| Genres repository | `app/src/main/java/com/lastwave/app/data/repository/GenresRepository.kt` |
| Generation status | `app/src/main/java/com/lastwave/app/data/generate/GenerationStatus.kt` |
| Taste signals | `app/src/main/java/com/lastwave/app/data/generate/FetchTasteSignals.kt` |
