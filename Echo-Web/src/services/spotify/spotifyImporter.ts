import { InnerTubeClient } from '../innertube/client'
import type { Track } from '../../types/domain'

export interface SpotifyImportProgress {
  current: number
  total: number
  currentTitle: string
  matchedCount: number
}

export interface SpotifyImportResult {
  title: string
  description: string
  coverUrl?: string | undefined
  totalSpotifyTracks: number
  matchedTracks: Track[]
  unmatchedTitles: string[]
}

export function isSpotifyUrl(url: string): boolean {
  try {
    const parsed = new URL(url.trim())
    return (
      (parsed.hostname === 'open.spotify.com' || parsed.hostname === 'spotify.link') &&
      Boolean(parsed.pathname)
    )
  } catch {
    return false
  }
}

export async function importFromSpotify(
  spotifyUrl: string,
  onProgress?: (progress: SpotifyImportProgress) => void,
  signal?: AbortSignal,
): Promise<SpotifyImportResult> {
  const resolveRes = await fetch(
    `/api/spotify/resolve?url=${encodeURIComponent(spotifyUrl.trim())}`,
    {
      ...(signal ? { signal } : {}),
    },
  )
  if (!resolveRes.ok) {
    const errData = await resolveRes.json().catch(() => ({}))
    throw new Error(
      errData.error ||
        errData.message ||
        `Failed to resolve Spotify URL (HTTP ${resolveRes.status})`,
    )
  }

  const spotifyData = (await resolveRes.json()) as {
    title: string
    description?: string | undefined
    coverUrl?: string | undefined
    tracks: Array<{ title: string; artist: string; album?: string; durationMs?: number }>
  }

  if (!spotifyData.tracks || spotifyData.tracks.length === 0) {
    throw new Error('No tracks found in the provided Spotify link')
  }

  const total = spotifyData.tracks.length
  const matchedTracks: Track[] = []
  const unmatchedTitles: string[] = []

  for (let i = 0; i < total; i++) {
    if (signal?.aborted) {
      throw new DOMException('Import cancelled', 'AbortError')
    }
    const item = spotifyData.tracks[i]!
    const query = `${item.title} ${item.artist}`.trim()

    onProgress?.({
      current: i + 1,
      total,
      currentTitle: `${item.title} - ${item.artist}`,
      matchedCount: matchedTracks.length,
    })

    try {
      const searchCatalog = await InnerTubeClient.search(query, 'songs')
      if (searchCatalog.tracks && searchCatalog.tracks.length > 0) {
        const best = searchCatalog.tracks[0]!
        matchedTracks.push(best)
      } else {
        unmatchedTitles.push(query)
      }
    } catch {
      if (signal?.aborted) {
        throw new DOMException('Import cancelled', 'AbortError')
      }
      unmatchedTitles.push(query)
    }

    if (i < total - 1) {
      await new Promise((r) => setTimeout(r, 120))
    }
  }

  return {
    title: spotifyData.title || 'Imported Spotify Playlist',
    description: `Imported from Spotify (${matchedTracks.length} of ${total} tracks matched)`,
    coverUrl: spotifyData.coverUrl,
    totalSpotifyTracks: total,
    matchedTracks,
    unmatchedTitles,
  }
}
