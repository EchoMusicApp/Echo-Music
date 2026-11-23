import type { Track } from '../../types/domain'

export interface MusicDnaData {
  trackTitle: string
  artistName: string
  genius?: {
    songId?: number
    description?: string
    releaseDate?: string
    producer?: string
    writer?: string
    annotationHighlights?: string[]
  }
  wikipedia?: {
    title: string
    extract: string
    thumbnailUrl?: string
    pageUrl?: string
  }
  artistWikipedia?: {
    title: string
    extract: string
    thumbnailUrl?: string
    pageUrl?: string
  }
}

// LRU-bounded cache (Map keeps insertion order)
const DNA_CACHE_MAX = 300
const dnaCache = new Map<string, MusicDnaData>()

export async function fetchMusicDna(track: Track): Promise<MusicDnaData> {
  const cacheKey = `${track.title}__${track.artistName}`.toLowerCase()
  const cached = dnaCache.get(cacheKey)
  if (cached) {
    dnaCache.delete(cacheKey)
    dnaCache.set(cacheKey, cached)
    return cached
  }

  const query = new URLSearchParams({
    title: track.title,
    artist: track.artistName || '',
  })

  const res = await fetch(`/api/music-dna?${query.toString()}`)
  if (!res.ok) {
    throw new Error(`Failed to fetch Music DNA (HTTP ${res.status})`)
  }

  const data = (await res.json()) as MusicDnaData

  // No synthesized "acoustic telemetry": BPM, key and loudness values derived from a title
  // hash would be fabricated. Real audio analysis would require decoding the track, which
  // this service does not do.

  dnaCache.set(cacheKey, data)
  if (dnaCache.size > DNA_CACHE_MAX) {
    const oldest = dnaCache.keys().next().value
    if (oldest !== undefined) dnaCache.delete(oldest)
  }
  return data
}
