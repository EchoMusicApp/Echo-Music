import {
  db,
  type ListeningHistoryRecord,
  type RecommendationExclusionRecord,
} from '../db/echoDatabase'
import type { Track } from '../../types/domain'

export interface ScoredTrack {
  trackId: string
  trackTitle: string
  artistName: string
  artistId?: string | undefined
  albumTitle?: string | undefined
  artwork?: string | undefined
  durationSeconds: number
  score: number
  lastPlayedAt: number
  playCount: number
}

export function getCurrentTimeOfDay(): 'morning' | 'afternoon' | 'evening' | 'night' {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 12) return 'morning'
  if (hour >= 12 && hour < 17) return 'afternoon'
  if (hour >= 17 && hour < 22) return 'evening'
  return 'night'
}

/**
 * Record a listening event into the Local Taste Engine history
 */
export async function recordListeningEvent(params: {
  track: Track
  durationPlayedMs: number
  trackDurationMs: number
  completed: boolean
  skipped: boolean
}): Promise<void> {
  const timeOfDay = getCurrentTimeOfDay()
  const record: ListeningHistoryRecord = {
    trackId: params.track.id,
    trackTitle: params.track.title,
    artistName: params.track.artistName,
    artistId: params.track.artistId,
    albumTitle: params.track.albumTitle || params.track.album?.title,
    artwork: params.track.artwork || params.track.thumbnailUrl,
    playedAt: Date.now(),
    durationPlayedMs: params.durationPlayedMs,
    trackDurationMs: params.trackDurationMs,
    completed: params.completed,
    skipped: params.skipped,
    timeOfDay,
  }

  await db.history.add(record)
}

/**
 * Exclude a track from future recommendations
 */
export async function excludeTrackFromRecommendations(
  track: Track,
  reason = 'User excluded',
): Promise<void> {
  const record: RecommendationExclusionRecord = {
    trackId: track.id,
    trackTitle: track.title,
    artistName: track.artistName,
    excludedAt: Date.now(),
    reason,
  }
  await db.recommendationExclusions.put(record)
}

/**
 * Restore an excluded track back into recommendations
 */
export async function restoreExcludedTrack(trackId: string): Promise<void> {
  await db.recommendationExclusions.delete(trackId)
}

/**
 * Get all excluded tracks
 */
export async function getExcludedTracks(): Promise<RecommendationExclusionRecord[]> {
  return await db.recommendationExclusions.toArray()
}

/**
 * Compute Taste Scores for all tracks in local history
 */
export async function computeTasteScores(likedTrackIds: Set<string>): Promise<ScoredTrack[]> {
  const exclusions = await db.recommendationExclusions.toArray()
  const excludedSet = new Set(exclusions.map((e) => e.trackId))

  const history = await db.history.toArray()
  const now = Date.now()
  const sevenDaysMs = 7 * 24 * 60 * 60 * 1000
  const currentTimeOfDay = getCurrentTimeOfDay()

  const trackMap = new Map<
    string,
    {
      record: ListeningHistoryRecord
      score: number
      playCount: number
      lastPlayedAt: number
    }
  >()

  for (const item of history) {
    if (excludedSet.has(item.trackId)) continue

    let entry = trackMap.get(item.trackId)
    if (!entry) {
      entry = {
        record: item,
        score: 0,
        playCount: 0,
        lastPlayedAt: item.playedAt,
      }
      trackMap.set(item.trackId, entry)
    }

    entry.playCount++
    if (item.playedAt > entry.lastPlayedAt) {
      entry.lastPlayedAt = item.playedAt
      entry.record = item
    }

    // Additive scoring mechanics based on Echo Music's Local Taste Engine:
    entry.score += 1.0

    if (item.completed) {
      entry.score += 2.0
    }

    if (item.skipped && item.durationPlayedMs < 30000) {
      entry.score -= 1.5
    }

    if (now - item.playedAt < sevenDaysMs) {
      entry.score += 1.5
    }

    if (item.timeOfDay === currentTimeOfDay) {
      entry.score += 1.0
    }
  }

  // Liked track bonus
  for (const [trackId, entry] of trackMap.entries()) {
    if (likedTrackIds.has(trackId)) {
      entry.score += 3.0
    }
  }

  const results: ScoredTrack[] = []
  for (const [trackId, entry] of trackMap.entries()) {
    results.push({
      trackId,
      trackTitle: entry.record.trackTitle,
      artistName: entry.record.artistName,
      artistId: entry.record.artistId,
      albumTitle: entry.record.albumTitle,
      artwork: entry.record.artwork,
      durationSeconds: Math.round(entry.record.trackDurationMs / 1000) || 180,
      score: Math.max(0, entry.score),
      lastPlayedAt: entry.lastPlayedAt,
      playCount: entry.playCount,
    })
  }

  return results.sort((a, b) => b.score - a.score)
}

function scoredToTrack(st: ScoredTrack): Track {
  const art = st.artwork || ''
  return {
    id: st.trackId,
    title: st.trackTitle,
    artistName: st.artistName,
    artistId: st.artistId || '',
    albumId: `album_${st.trackId}`,
    albumTitle: st.albumTitle || 'Unknown Album',
    albumImage: art,
    artwork: art,
    thumbnailUrl: art,
    durationMs: st.durationSeconds * 1000,
    durationSeconds: st.durationSeconds,
    explicit: false,
    playCount: st.playCount,
    videoId: st.trackId,
  }
}

/**
 * Generate "Quick Picks" from top scored tracks
 */
export async function getQuickPicks(likedTrackIds: string[], limit = 12): Promise<Track[]> {
  const scored = await computeTasteScores(new Set(likedTrackIds))
  return scored.slice(0, limit).map(scoredToTrack)
}

/**
 * Generate "Forgotten Favorites"
 */
export async function getForgottenFavorites(likedTrackIds: string[], limit = 10): Promise<Track[]> {
  const scored = await computeTasteScores(new Set(likedTrackIds))
  const now = Date.now()
  const twentyOneDaysMs = 21 * 24 * 60 * 60 * 1000

  const forgotten = scored.filter(
    (st) => st.playCount >= 2 && now - st.lastPlayedAt > twentyOneDaysMs,
  )

  return forgotten.slice(0, limit).map(scoredToTrack)
}

/**
 * Generate a personalized taste playlist
 */
export async function generateTastePlaylist(
  likedTrackIds: string[],
  catalogFallback: Track[] = [],
  targetCount = 25,
): Promise<{ title: string; description: string; tracks: Track[] }> {
  const scored = await computeTasteScores(new Set(likedTrackIds))
  const timeOfDay = getCurrentTimeOfDay()
  const timeLabel =
    timeOfDay === 'morning'
      ? 'Morning Light'
      : timeOfDay === 'afternoon'
        ? 'Midday Flow'
        : timeOfDay === 'evening'
          ? 'Evening Waves'
          : 'Late Night Echoes'

  const userTracks: Track[] = scored.map(scoredToTrack)

  const combined = [...userTracks]
  for (const t of catalogFallback) {
    if (!combined.some((item) => item.id === t.id)) {
      combined.push(t)
    }
  }

  const pool = combined.slice(0, targetCount * 2)
  const selected = pool
    .map((value) => ({ value, sort: Math.random() }))
    .sort((a, b) => a.sort - b.sort)
    .map(({ value }) => value)
    .slice(0, targetCount)

  return {
    title: `Local Taste: ${timeLabel}`,
    description: `Auto-generated by Echo's Local Taste Engine based on your listening patterns for ${timeOfDay}.`,
    tracks: selected,
  }
}

/**
 * Generate "Heavy Rotation" from tracks with high recent play counts
 */
export async function getHeavyRotation(likedTrackIds: string[], limit = 8): Promise<Track[]> {
  const scored = await computeTasteScores(new Set(likedTrackIds))
  const now = Date.now()
  const fourteenDaysMs = 14 * 24 * 60 * 60 * 1000

  const active = scored.filter(
    (st) => st.playCount >= 2 && now - st.lastPlayedAt <= fourteenDaysMs,
  )
  return active.slice(0, limit).map(scoredToTrack)
}

/**
 * Get top artists based on user listening history
 */
export async function getTopArtists(
  limit = 6,
): Promise<Array<{ id: string; name: string; count: number; artwork?: string | undefined }>> {
  const history = await db.history.toArray()
  const artistMap = new Map<
    string,
    { id: string; name: string; count: number; artwork?: string | undefined }
  >()

  for (const item of history) {
    if (!item.artistName || item.artistName === 'Unknown Artist') continue
    const key = item.artistId || item.artistName.toLowerCase()
    let entry = artistMap.get(key)
    if (!entry) {
      entry = {
        id: item.artistId || `artist-${item.artistName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
        name: item.artistName,
        count: 0,
        artwork: item.artwork,
      }
      artistMap.set(key, entry)
    }
    entry.count++
    if (!entry.artwork && item.artwork) {
      entry.artwork = item.artwork
    }
  }

  return [...artistMap.values()].sort((a, b) => b.count - a.count).slice(0, limit)
}
