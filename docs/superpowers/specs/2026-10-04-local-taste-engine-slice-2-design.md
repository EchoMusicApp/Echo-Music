# Local Taste Engine — Slice 2 Design

Date: 2026-10-04
Status: draft (pending design approval)

## 1. Context

Slice 1 (`2026-10-04-local-taste-engine-slice-1-design.md`) established the
offline local taste foundation: `song_play_stats` and `recommendation_exclusions`
tables, a pure `LocalTasteEngine` scoring model, playback hooks recording skip
signals, and a Generate screen producing playlists from local history alone.

Slice 2 adds a **Last.fm multi-source recommendation pipeline** on top of that
foundation. It reuses the existing Last.fm authentication (scrobbling already
works — `LastFM.kt`, `ScrobbleManager.kt`) and adds taste-profile queries the
current code does not make.

The engine from Slice 1 scores local signals. Slice 2 adds external signals —
similar tracks, similar artists, top tags — so seeds with thin local history
still produce good candidates.

## 2. Goals

1. Enrich Slice 1's scoring with Last.fm signals: `track.getSimilar`,
   `artist.getSimilar`, `tag.getTopTags`.
2. Cache a taste snapshot (`TasteProfileProvider`) for 1 hour so repeated
   generations don't hammer the API.
3. Merge external scores into the existing candidate scoring without changing
   the local-only fallback path — the Generate screen must work without Last.fm
   connected.
4. Add the `taste_profile` table back (it was dropped in migration 37→38; see
   Slice 1 §14).

## 3. Non-goals (deferred to Slice 3)

- `GenresRepository` and genre-aware scoring.
- `fetchTasteSignals` YouTube Music endpoint.
- Cross-screen generation status (`GenerationStatus` singleton).
- Integration into `CreateAiPlaylistDialog`.

## 4. Architecture and components

### 4.1 New and changed files

| Concern | Path | Change |
|---|---|---|
| Entity | `core/src/main/kotlin/echo/music/iad1tya/db/entities/TasteProfileEntity.kt` | new |
| DAO | `core/src/main/kotlin/echo/music/iad1tya/db/daos/TasteProfileDao.kt` | new |
| Database | `core/src/main/kotlin/echo/music/iad1tya/db/MusicDatabase.kt` | register entity, `version = 47 → 48` |
| Engine | `app/src/main/kotlin/com/music/echo/generate/RecommendationEngine.kt` | new |
| Cache | `app/src/main/kotlin/com/music/echo/generate/TasteProfileProvider.kt` | new |
| Last.fm API | `app/src/main/kotlin/com/music/echo/utils/lastfm/LastFmTasteApi.kt` | new — adds taste queries to existing `LastFM.kt` |
| DI | `app/src/main/kotlin/com/music/echo/di/AppModule.kt` | `@Provides` for engine + provider |
| Integration | `app/src/main/kotlin/com/music/echo/generate/SongPlayStatsRepository.kt` | extend `generatePlaylist` with external candidates |
| Tests | `app/src/test/…` | new (§7) |

### 4.2 Last.fm API surface

Existing `LastFM.kt` handles authentication and scrobbling. New
`LastFmTasteApi.kt` adds:

```kotlin
suspend fun getSimilarTracks(artist: String, track: String): Result<List<SimilarTrack>>
suspend fun getSimilarArtists(artist: String): Result<List<SimilarArtist>>
suspend fun getTopTags(artist: String): Result<List<Tag>>
```

All methods return `Result` — never throw to callers. All honour the existing
`LastFM.sessionKey` for authenticated users; anonymous calls use API key only.

### 4.3 TasteProfileProvider

1-hour TTL cache holding a `TasteProfile` snapshot:

```kotlin
data class TasteProfile(
  val topArtistNames: List<String>,
  val topTrackKeys: List<String>,
  val topGenres: List<String>,
  val fetchedAtMillis: Long,
)
```

- On cache hit (within 1 hour): return immediately, zero network calls.
- On cache miss: fetch in parallel, update `taste_profile` table, return.
- On failure: return the stale cache if present; else return null (caller
  degrades to Slice 1 local-only behaviour).

### 4.4 Scoring integration

`RecommendationEngine` wraps Slice 1's `LocalTasteEngine` scoring and adds
external-signal weights:

| Signal | Weight | Source |
|---|---|---|
| Slice 1 local weights | unchanged (§9 of Slice 1) | local DB |
| `SIMILAR_TRACK_BONUS` | `+2.0f` | `track.getSimilar` for each seed |
| `SIMILAR_ARTIST_BONUS` | `+1.0f` | `artist.getSimilar` for each seed |
| `TAG_MATCH_BONUS` | `+1.5f` | `tag.getTopTags` ∩ taste profile top genres |

External signals are **additive** — a candidate with no external match still
scores from local signals alone. A candidate with all three externals gets a
maximum of `+4.5f` on top of local score.

If Last.fm is disconnected or fails, external bonuses contribute `0` —
generation degrades to Slice 1 behaviour, never fails.

## 5. Data model

### 5.1 TasteProfileEntity

```kotlin
@Entity(tableName = "taste_profile")
data class TasteProfileEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 1,
  val topArtistsJson: String = "",
  val topTracksJson: String = "",
  val topGenresJson: String = "",
  val confidence: Float = 0f,
  val updatedAtMillis: Long = 0L,
)
```

Single-row table (`id = 1`) — one profile at a time, overwritten on refresh.

Migration v47 → v48: additive `CREATE TABLE`. No backfill — the table is
empty until first fetch.

### 5.2 Dropped-table warning

Slice 1 §14 records that migration 37→38 created `taste_profile` and a later
migration dropped it; `46.json` has no trace. This Slice 2 spec **recreates**
the table under its own version bump (47→48), not reusing any old definition.

## 6. Testing

| Tier | Asserts | Location |
|---|---|---|
| 1 — pure logic **(must)** | external bonus arithmetic; tag-match intersection; degraded-mode scoring (Last.fm fail → local-only score) | `app/src/test` |
| 2 — cache **(must)** | `TasteProfileProvider` returns cached within TTL; refetches after TTL; returns stale on network failure | `app/src/test` |
| 3 — integration **(should)** | `RecommendationEngine` merges local + external candidates; excluded keys still dropped | `app/src/test` |

Dependencies: same as Slice 1 §12.3 (`junit`, `kotlinx-coroutines-test`,
`robolectric`). No mockk — hand-written fakes for `LastFmTasteApi`.

## 7. Acceptance criteria

1. With Last.fm connected, generated playlists include candidates that don't
   appear in local history (via similar tracks/artists).
2. With Last.fm disconnected or failing, generated playlists are identical to
   Slice 1 output (local-only).
3. Taste profile refreshes at most once per hour under repeated generation.
4. Migration 47→48 is additive; existing data untouched.
5. All new tests pass; CI runs them.

## 8. Known limitations and risks

| Risk | Impact | Handling |
|---|---|---|
| Last.fm API rate limits | Taste fetches throttled | 1-hour cache TTL (§4.3); degrade to local-only on 429 |
| `track.getSimilar` returns tracks absent from YouTube Music | Candidates that can't be played | Filter through `YouTube.search` existence check before offering; skip silently if not found |
| Taste profile is single-row | Concurrent generations overwrite each other | Acceptable — generations are user-triggered, not scheduled |
| `sessionKey` may expire | Authenticated requests fail | Catch auth errors, degrade to anonymous API-key mode; scrobbling already handles this pattern |

## Appendix A — provenance

| Ported from | File |
|---|---|
| Engine | `app/src/main/java/com/lastwave/app/data/generate/RecommendationEngine.kt` |
| Taste cache | `app/src/main/java/com/lastwave/app/data/generate/TasteProfileProvider.kt` |
