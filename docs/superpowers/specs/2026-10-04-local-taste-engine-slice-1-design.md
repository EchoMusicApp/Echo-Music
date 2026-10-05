# Local Taste Engine — Slice 1 Design

Date: 2026-10-04
Status: approved (design stage)
Target: Echo-Music, branch `feat/haptic-feedback-settings-and-edge-fix`

## 1. Context

Echo-Music's playlist generation is entirely LLM-gated: `AiPlaylistGenerator` and
`AiRecommendationHelper` require an OpenRouter (or other) API key configured in
`ui/screens/settings/AiSettings.kt`. With no key, generation does not work.

The app already records listening history in three shapes — the `event` table
(`songId`, `timestamp`, `playTime`), `song.totalPlayTime`, and the monthly
`playCount` rollup — but none of them record **skips**, and none are keyed in a
form the recommendation pipeline can join on.

This slice adds an offline, account-free local taste engine: playlist generation
that works with no API key, no third-party account, and no network call beyond
YouTube Music itself.

## 2. Goals

1. Generate a playlist from local listening behaviour, offline, with no API key.
2. Record skip signals. None exist today: the `event` table has only
   `songId`/`timestamp`/`playTime`, and an earlier `play_event` table that did
   have a `skipped` column was dropped by a later migration (see §14).
3. Give existing users immediate value via a one-shot backfill from history.
4. Expose a **Never recommend again** action on the song long-press menu.
5. Add the first unit tests to the `app` module, and make CI run them.

## 3. Non-goals (deferred to Slice 2 / Slice 3)

| Slice | Contents |
|---|---|
| 2 | `RecommendationEngine` (Last.fm multi-source pipeline), `TasteProfileProvider` (1-hour taste snapshot cache) |
| 3 | `GenresRepository`, the `fetchTasteSignals` YouTube Music endpoint (no equivalent exists in `:innertube`), second reader for cross-screen generation status, integration into `CreateAiPlaylistDialog` |

Also explicitly out of scope for Slice 1:

- Removing or altering the existing LLM path (`ai/` package) — it stays as-is.
- Rule-based/saved-query playlists. Neither app has these; this is not that feature.
- Compose UI tests, screenshot tests, or `@Preview` conventions.

## 4. Slice roadmap

```
Slice 1 (this spec):  signals (DB) → local engine → Generate screen
Slice 2:              + Last.fm taste pipeline
Slice 3:              + genres, taste-signals endpoint, AI dialog integration
```

Each slice is independently shippable and testable.

## 5. Naming rule

No reference to any source application by name may appear in this feature's
**code**: not in identifiers, not in comments, not in string resources, not in
`docs/` entries the code points at. Ported comments must state the technical
fact rather than the genealogy — e.g. write `// skip is the heaviest weight at
3.0`, not `// ported from <other app>`.

Provenance for the implementer lives only in Appendix A of this document.

## 6. Architecture and components

### 6.1 New and changed files

| Concern | Path | Change |
|---|---|---|
| Entity | `core/src/main/kotlin/echo/music/iad1tya/db/entities/SongPlayStatsEntity.kt` | new |
| Entity | `core/src/main/kotlin/echo/music/iad1tya/db/entities/RecommendationExclusionEntity.kt` | new |
| DAO | `core/src/main/kotlin/echo/music/iad1tya/db/daos/SongPlayStatsDao.kt` | new |
| DAO | `core/src/main/kotlin/echo/music/iad1tya/db/daos/RecommendationExclusionDao.kt` | new |
| Database | `core/src/main/kotlin/echo/music/iad1tya/db/MusicDatabase.kt` | register entities, `version = 46 → 47`, `AutoMigration(46, 47, spec = ...)`, expose DAOs |
| Repository | `app/src/main/kotlin/com/music/echo/generate/SongPlayStatsRepository.kt` | new |
| Engine | `app/src/main/kotlin/com/music/echo/generate/LocalTasteEngine.kt` | new |
| DI | `app/src/main/kotlin/com/music/echo/di/AppModule.kt` | `@Provides` for repository + engine |
| Playback hooks | `app/src/main/kotlin/com/music/echo/playback/MusicService.kt` | 2 call sites (§8) |
| ViewModel | `app/src/main/kotlin/com/music/echo/viewmodels/GenerateViewModel.kt` | new |
| Screen | `app/src/main/kotlin/com/music/echo/ui/screens/generate/GenerateScreen.kt` | new |
| Menu action | `app/src/main/kotlin/com/music/echo/ui/menu/` long-press sheet | add **Never recommend again** |
| Navigation | `app/src/main/kotlin/com/music/echo/ui/screens/Screens.kt`, `NavigationBuilder.kt` | register destination |
| Tests | `app/src/test/…`, `core/src/test/…` | new (§12) |
| CI | `.github/workflows/android-build.yml` | add unit-test step (§12.4) |

Package: `echo.music.iad1tya.generate` for repository and engine — mirrors the
existing `ai/` convention of a feature-owned package in the app module.

> Note: in this repo the **directory** `app/src/main/kotlin/com/music/echo/…`
> does **not** match the **package** `echo.music.iad1tya.…`. This is the
> existing convention throughout (`viewmodels/` declares
> `echo.music.iad1tya.viewmodels`, `ui/menu/` declares
> `echo.music.iad1tya.ui.menu`). New files must follow the package side, not
> the path side.

Formatting follows `.editorconfig` / Spotless; run the pre-commit hook.

### 6.2 DAO wiring pattern

`InternalDatabase` exposes DAOs as abstract properties and `MusicDatabase`
delegates them. Follow the existing `speedDialDao` precedent exactly:

```kotlin
// InternalDatabase
abstract val songPlayStatsDao: SongPlayStatsDao
abstract val recommendationExclusionDao: RecommendationExclusionDao

// MusicDatabase
val songPlayStatsDao: SongPlayStatsDao get() = delegate.songPlayStatsDao
val recommendationExclusionDao: RecommendationExclusionDao
  get() = delegate.recommendationExclusionDao
```

### 6.3 Component boundaries

`LocalTasteEngine` depends only on:

1. the two DAOs,
2. `MusicDatabase.likedSongs` (the `LIKED_BONUS` source),
3. `YouTube.related(endpoint)` — the single network dependency.

It never references a ViewModel or a Composable.

Because Room entities are plain data classes, the pure scoring/filter functions
in §12.1 are unit-testable on the JVM with no Android runtime at all. The class
itself (DAO + network access) is tested with Robolectric plus hand-written fakes.

Scoring and filtering live in **internal top-level functions over plain data**,
not methods that read Room or `Context`. See §12.1.

### 6.4 GenerationStatus deferred

Cross-screen generation status is deferred to Slice 3, when a second reader
exists. Slice 1 holds `isGenerating` / `message` / `error` as `GenerateViewModel`
`StateFlow`. Porting a dedicated singleton later is ~30 lines.

## 7. Data model and migration

### 7.1 Entities

```kotlin
@Entity(tableName = "song_play_stats")
data class SongPlayStatsEntity(
  @PrimaryKey val trackKey: String,
  val title: String = "",
  val artist: String = "",
  val videoId: String? = null,
  val artworkUrl: String? = null,
  val totalPlayTimeMs: Long = 0L,
  val playCount: Int = 0,
  val skipCount: Int = 0,
  val lastPlayedAtMillis: Long = 0L,
)
```

```kotlin
@Entity(tableName = "recommendation_exclusions")
data class RecommendationExclusionEntity(
  @PrimaryKey val trackKey: String,
  val excludedAtMillis: Long,
  val trackName: String = "",
  val artistName: String = "",
)
```

### 7.2 Key invariant

```kotlin
internal fun trackKeyOf(title: String, artist: String): String =
  "${title.trim()}|${artist.trim()}".lowercase()
```

Keyed on `title|artist`, **not** video id, so duplicate YouTube Music uploads of
the same recording consolidate into one signal. This is the single invariant the
whole feature rests on: the write path, the exclusion path, and the engine's
candidate filter must all call this same function.

> **Case-folding mismatch, open for Task 2.** The backfill runs in SQL, where
> `lower()` is **ASCII-only**. Kotlin's `lowercase()` is Unicode-aware, so a CJK,
> Cyrillic or accented-Latin title produces different keys in a backfilled row
> than in a row written at runtime. Decision deferred to Task 2: either normalise keys
> once in Kotlin on read, or route all key emission through one shared
> normaliser. Left open here because it changes the engine's read path, which
> Task 1 does not touch.

### 7.3 Required DAO methods

`SongPlayStatsDao`: `find(trackKey)`, `upsert(entity)`,
`mostPlayedSince(sinceMillis, limit)` (`ORDER BY totalPlayTimeMs DESC`),
`recentlyPlayed(limit)` (`ORDER BY lastPlayedAtMillis DESC`),
`observeTopPlayed(limit)`: `Flow`, `getAllTrackKeys()`, `clearAll()`.

`RecommendationExclusionDao`: `upsert(entity)`, `upsertAll(entities)`, `getAll()`,
`observeAll()`: `Flow`, `count()`, `clear()`, `delete(trackKey)`.

### 7.4 Migration v46 → v47

`core/schemas/echo.music.iad1tya.db.InternalDatabase/46.json` is exported, so
`exportSchema = true` supports:

```kotlin
AutoMigration(from = 46, to = 47, spec = Migration46To47Spec::class)
```

Purely additive — Room generates `CREATE TABLE` for both new tables from the
exported schema. No existing table is touched.

Precedent and convention, so nobody is surprised by it:

- `AutoMigration(from = 36, to = 37, spec = Migration36To37Spec::class)` with
  `class Migration36To37Spec : AutoMigrationSpec` already exists
  (`MusicDatabase.kt:135`, `:906`). That is the pattern to copy.
- Migrations 42→46 were written **manually** (`MIGRATION_42_43` …
  `MIGRATION_45_46` in `addMigrations(...)`). Prefer the auto route here anyway:
  with `exportSchema = true` and `46.json` on disk, the annotation processor
  validates the generated DDL against the exported schema at **compile time**, so
  a schema mismatch fails the build instead of shipping.
- The backfill goes in `onPostMigrate` on the spec class, after Room has created
  the tables.

`Migration46To47Spec.onPostMigrate` runs the one-shot backfill:

```sql
INSERT INTO song_play_stats (trackKey, title, artist, videoId, artworkUrl,
                             totalPlayTimeMs, playCount, skipCount, lastPlayedAtMillis)
SELECT
  lower(trim(s.title) || '|' || trim(ifnull(x.artist, ''))),
  s.title,
  ifnull(x.artist, ''),
  s.id,
  s.thumbnailUrl,
  sum(e.playTime),
  count(*),
  0,
  max(e.timestamp) -- epoch millis, not a strftime value
FROM event e
JOIN song s ON s.id = e.songId
LEFT JOIN (
  SELECT songId, ifnull(group_concat(name, ', '), '') AS artist FROM (
    SELECT sam.songId AS songId, a.name AS name
    FROM song_artist_map sam
    LEFT JOIN artist a ON a.id = sam.artistId
    ORDER BY sam.songId, sam.position
  ) GROUP BY songId
) x ON x.songId = s.id
WHERE e.playTime > 0
GROUP BY lower(trim(s.title) || '|' || trim(ifnull(x.artist, '')))
```

**Why the ordered inner subquery.** `group_concat(a.name, ', ')` without
`ORDER BY` concatenates in arbitrary order — and the artist string is *part of
the key*, so the same track could backfill different keys on different
devices. Ordering by `song_artist_map.position` makes the concatenated string
deterministic. Note `ORDER BY` *inside* `group_concat()` is not used: it needs
SQLite 3.44+, which older devices (and the `sqlite-jdbc` test engine) predate.
The ordered-subquery form runs everywhere.

**Why the derived table `x` is mandatory.** The obvious one-pass form —
joining `song_artist_map`/`artist` directly and putting
`group_concat(...)` inside the outer `GROUP BY` — does not parse:

```
aggregate functions are not allowed in the GROUP BY clause
```

Artists are therefore aggregated per song in `x`, so the outer `GROUP BY`
references only plain columns. This also fixes two latent bugs in the naive
form: `count(*)` would be multiplied by the number of artists (per-event artist
expansion), and duplicate artist names across events would inflate the row.

Properties of this backfill:

- **`GROUP BY trackKey` consolidates duplicate uploads only when their joined
  artist string matches.** Two uploads with identical artist lists collapse to
  one row with summed play time; if one upload lists only the lead artist and
  the other lists everyone, they produce two distinct keys and thus two rows.
  This is a key-design consequence, not an SQL defect — and the runtime write
  path has the same property. See §14. Tier 2 pins all three behaviours:
  matching-list dupes collapse (`duplicateUploadsOfSameRecordingCollapseIntoOneRow`),
  differing-list dupes seed distinct rows (`sameTitleWithDifferentArtistsSeedsDistinctRows`),
  mapping-less songs seed an empty-artist row (`songWithNoArtistMappingSeedsEmptyArtistRow`).
- The chosen `videoId` is whichever row SQLite picks for the group — an
  arbitrary member of the consolidated set. Any of them plays the same
  recording, so this is acceptable; it is **not** "the upload with most plays".
- **`skipCount = 0` always.** Skip history cannot be reconstructed.
- **`event.timestamp` is stored as epoch milliseconds (INTEGER)**, via the
  `LocalDateTime` ↔ `Long` converters in `db/Converters.kt`. Existing queries
  compare it directly (`event.timestamp > (:now - 86400000 * 7)`,
  `DatabaseDao.kt:305`). Use `max(e.timestamp)` as-is — do **not** wrap it in
  `strftime`, and do not re-derive it.

## 8. Write path — two hooks in `MusicService.kt`

### 8.1 Hook 1: accumulate listened time

At `onPlaybackStatsReady` (~line 3515): when `playbackStats.totalPlayTimeMs`
increases, accumulate it into a per-track counter. This is *actual* listened
time, excluding seeks.

### 8.2 Hook 2: record on track exit

At `onMediaItemTransition` (line 2333; `reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO`
is already inspected at 2341):

Rule:

```
completedNaturally = reason == AUTO || ratio >= 0.85
wasSkip            = !completedNaturally
if (listenedMs <= 0L) return          // guard: nothing was heard
if (wasSkip) recordSkip(...)
```

**Deliberate deviation from a literal port.** Rather than reading the outgoing
track's position just before state overwrite, accumulate the window's
`playbackStats.totalPlayTimeMs` from Hook 1 and compare it against
`mediaMetadata.durationMs`. `totalPlayTimeMs` is *actual listened time and
excludes seeks*, which is a better signal than `positionMs / durationMs`, and it
removes ordering fragility. Both hooks run on `Dispatchers.IO` and must not
block the player.

### 8.3 Failure behaviour

Existing code at this site swallows database errors with
`catch (_: SQLException) {}`. Do **not** replicate that: log via `Timber.e` and
continue. Telemetry must never crash playback.

## 9. Read path — generation flow

1. User opens the Generate screen and taps **Generate**.
   `GenerateViewModel` sets `isGenerating = true`.
2. Seeds: `songPlayStatsDao.mostPlayedSince(sinceMillis = now - 30.days, limit = 100)` plus
   `recentlyPlayed(limit = 20)`.
3. Candidate filter (exact): `skipCount < 2 || totalPlayTimeMs > 45_000`.
4. Score each seed with these weights:

   | Signal | Weight |
   |---|---|
   | `PLAY_WEIGHT` — accumulated listened ms | `2.0f` |
   | `SKIP_PENALTY` — per skip (heaviest) | `-3.0f` |
   | `LIKED_BONUS` — track is in `likedSongs` | `+5.0f` |
   | `RECENCY_BONUS` — `lastPlayedAtMillis` | `+1.5f` |
   | `TIME_OF_DAY_BONUS` | `+1.3f` |

   Time-of-day buckets: morning 06–11, afternoon 12–17, evening 18–21, else night.

5. Drop any `trackKey` present in `recommendation_exclusions`.
6. Expand the top-scoring seeds via `YouTube.related(endpoint)` — the only
   network call in this slice.
7. Re-score candidates, drop excluded, dedupe, take the requested count.
8. Map results to `SongEntity` (inserting missing rows), create a
   `PlaylistEntity`, write ordered `PlaylistSongMap` rows, then navigate to the
   new playlist.

Generated playlists are ordinary Echo playlists — no new playlist type, no new
persistence layer.

## 10. UI

### 10.1 Generate screen

`ui/screens/generate/GenerateScreen.kt`, registered as one destination in
`Screens.kt` + `NavigationBuilder.kt` (the app currently has 61).

Contents:

- progress card driven by `isGenerating` / `message` from `GenerateViewModel`
- **Generate** primary action
- result playlist preview before navigation
- distinct empty states:
  - **no stats yet** → "Play some music first"
  - **no candidates after exclusions** → different copy, invites clearing exclusions
- cancel action while generating

Style follows `ui/screens/playlist/AutoPlaylistScreen.kt` (pull-to-refresh,
`TopAppBar`, `collectAsState`) and the `DESIGN.md` rules already in force.

### 10.2 Song menu action

Add **Never recommend again** to `ui/menu/SongMenu.kt`, the long-press sheet
that already carries the playlist actions. On confirm:

- `recommendationExclusionDao.upsert(trackKeyOf(title, artist), now, title, artist)`
- offer an undo affordance in a snackbar, since the action is otherwise silent

### 10.3 Access

Entry point from Library alongside the existing auto-playlist entries, using the
existing `AutoPlaylistButton` component.

## 11. Error handling

| Case | Behaviour |
|---|---|
| `YouTube.related` fails | `.getOrElse {}` → fall back to top-scored local tracks. Generation degrades, never fails |
| Stats table empty | Empty state, not an infinite spinner |
| User cancels | `viewModelScope` job cancelled; status reset in `finally` |
| DAO / DB write throws | `Timber.e`, then continue |
| No candidates after exclusions | Distinct empty state (see §10.1) |
| Backfill produces zero rows | Expected for fresh installs; UI handles it via the empty state |

## 12. Testing

### 12.1 Design constraint

`LocalTasteEngine` exposes scoring as internal top-level functions over plain
data:

```kotlin
internal fun scoreStats(stats: SongPlayStatsEntity, likedKeys: Set<String>, timeOfDay: String): Float
internal fun applyCandidateFilter(stats: SongPlayStatsEntity): Boolean
internal fun trackKeyOf(title: String, artist: String): String
```

The class wraps these with DAO access only.

### 12.2 Tiers

| Tier | Asserts | Location |
|---|---|---|
| 1 — pure logic **(must)** | all five weights; skip penalty is heaviest; liked bonus; recency; time-of-day buckets; `applyCandidateFilter` truth table; `trackKeyOf` normalization (`"  Title "` / `" Artist "` equals lowercase form) | `app/src/test` |
| 2 — migration backfill **(must)** | backfill row counts; duplicate `videoId`s consolidate into one `trackKey` with summed `totalPlayTimeMs`; `skipCount` seeded 0; `lastPlayedAtMillis` equals `max(event.timestamp)` (epoch millis round-trip) | `core/src/test` |
| 3 — DAO **(should)** | `mostPlayedSince` order + limit, `recentlyPlayed`, `observeTopPlayed`, key round-trip | `app/src/test` |
| 4 — flow **(should)** | excluded keys never survive to output; empty-stats state; cancel resets state | `app/src/test` |

Tier 2 is the non-negotiable test: the `GROUP BY` over `group_concat` in §7.4 is
exactly the kind of SQL that silently returns wrong counts.

**How Tier 2 runs.** No `MigrationTestHelper` — neither app has ever used one,
and it wants instrumentation. Two cheaper, stronger checks cover the real risk:

1. *Compile time* — the annotation processor validates `AutoMigration(46, 47)`
   against `46.json`. A schema mismatch fails the build. Nothing to test.
2. *Unit test* — open an in-memory Room database, insert fixture rows into
   `event` / `song` / `artist` / `song_artist_map`, execute the §7.4 SQL as a
   raw statement, then assert consolidation, summed play time, `skipCount = 0`
   and the `max(event.timestamp)` round-trip.

What is genuinely at risk is the SQL, and this tests exactly the SQL.

### 12.3 Dependencies

Add `testImplementation` to `app/build.gradle.kts` and `core/build.gradle.kts`:

- `junit:junit:4.13.2` (already in the version catalog)
- `org.jetbrains.kotlinx:kotlinx-coroutines-test`
- `org.robolectric:robolectric` (for Tiers 3 and 4)

**No mockk** — hand-written fakes for the two DAO interfaces (§7.3, seven
methods each) are smaller and clearer than mock expectations, and they keep Tier
4 runnable without a mocking framework. **No Truth** — JUnit assertions suffice.

### 12.4 CI

Add a step to `.github/workflows/android-build.yml`:

```bash
./gradlew :app:testDebugUnitTest :core:testDebugUnitTest
```

Echo's `app` module has never had tests; without this step they are decorative.
This is a five-line workflow change and is in scope for Slice 1.

## 13. Acceptance criteria

1. `song_play_stats` and `recommendation_exclusions` exist after upgrading from
   v46; existing tables and rows are untouched.
2. A user with prior listening history sees non-empty stats immediately after
   upgrading, without replaying anything.
3. Skipping a track before 85 % increments `skipCount`; playing to completion
   (or an `AUTO` transition) increments `playCount` only.
4. The Generate screen produces a playable playlist with no API key configured
   and no network other than YouTube Music.
5. "Never recommend again" excludes a track from subsequent generations, and the
   exclusion survives process restart.
6. Cancelling generation stops work and resets UI state.
7. All four test tiers exist and pass; CI runs them on push and PR.

## 14. Known limitations and risks

| Risk | Impact | Handling |
|---|---|---|
| Backfilled keys (DB artist strings) may not match runtime keys (player metadata) on multi-artist tracks | Scoring quality only — backfill is best-effort | Runtime writes are authoritative; exclusions are always runtime-created, so user intent is never lost |
| `skipCount` starts at 0 for all backfilled rows | Skip penalties apply only from install forward | Accepted; documented |
| Four stats tables now (`event`, `playCount`, `song.totalPlayTime`, `song_play_stats`) | Duplicated signal | Different reads and shapes: history UI vs engine scoring. Accepted |
| `MusicService.kt` is 4,817 lines | Merge risk | Hooks are additive at two already-identified anchors |
| `app` module test infrastructure does not exist | Upfront setup cost | Deps listed in §12.3; no new frameworks beyond Robolectric |
| An earlier taste experiment created `play_event` (with a `skipped` column), `taste_profile` and `brain_activity_log` in migration 37→38, then **dropped them all** via a later `@DeleteTable` spec. `46.json` has no trace of them | Slice 2 could wrongly assume `taste_profile` still exists | Confirmed absent from schema 46. Slice 2 must recreate what it needs on its own version bump, not reuse |
| Duplicate uploads whose artist lists differ produce two `song_play_stats` rows instead of one | Slightly diluted score for those recordings | Accepted consequence of `title|artist` keying; runtime has the same property. Tier 2 pins this (`sameTitleWithDifferentArtistsSeedsDistinctRows`) |
| `lower()` (SQL, ASCII-only) vs `lowercase()` (Kotlin, Unicode) can disagree on non-ASCII titles | Backfilled key ≠ runtime key for those tracks | Decision deferred to Task 2; resolve in Task 2 before the engine reads keys |

## Appendix A — provenance

Implementation references the sibling local repository
`LastWave-Native/` at these paths. This is documentation only; the names in
these paths must never be copied into source code (see §5).

| Ported from | File |
|---|---|
| Entity + DAO | `app/src/main/java/com/lastwave/app/data/local/db/SongPlayStatsEntity.kt` |
| Entity + DAO | `app/src/main/java/com/lastwave/app/data/local/db/RecommendationExclusionEntity.kt` |
| Repository | `app/src/main/java/com/lastwave/app/data/repository/SongPlayStatsRepository.kt` |
| Engine | `app/src/main/java/com/lastwave/app/data/generate/LocalTasteSuggestionEngine.kt` |
