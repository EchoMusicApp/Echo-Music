import type { Album, Artist, HomeShelf, PlaylistSummary, Track } from '../../types/domain.ts'
import type { SponsorSegment } from '../../types/audio.ts'
import type { StreamResolutionResult } from '../../types/innertube.ts'
import { apiFetch } from '../api.ts'
import {
  parseAlbumDetails,
  parseArtistDetails,
  parseHomeFeed,
  parseSearchResults,
} from './parser.ts'
import { fetchWithCache } from '../cache/metadataCache.ts'
import { cacheTracks } from '../../stores/useEchoStore.ts'

export const SEARCH_FILTERS = {
  songs: 'EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D',
  albums: 'EgWKAQIBAWoKEAkQBRAKEAMQBA%3D%3D',
  artists: 'EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D',
  playlists: 'EgWKAQIoAWoKEAkQBRAKEAMQBA%3D%3D',
  videos: 'EgWKAQIQAWoKEAkQBRAKEAMQBA%3D%3D',
  podcasts: 'EgWKAQJQAWoKEAkQBRAKEAMQBA%3D%3D',
} as const

export type SearchFilterKey = keyof typeof SEARCH_FILTERS | 'all'

export interface SearchResultCatalog {
  tracks: Track[]
  albums: Album[]
  artists: Artist[]
  playlists: PlaylistSummary[]
}

export class InnerTubeClient {
  public static async fetchHomeFeed(): Promise<HomeShelf[]> {
    return await fetchWithCache(
      'shelf:home',
      'shelf',
      async () => {
        const data = await apiFetch<Record<string, unknown>>('/api/innertube/browse', {
          method: 'POST',
          body: JSON.stringify({ browseId: 'FEmusic_home' }),
        })
        const shelves = parseHomeFeed(data)
        for (const shelf of shelves) {
          const tracks = shelf.items.filter((i): i is Track => 'durationSeconds' in i)
          if (tracks.length > 0) cacheTracks(tracks)
        }
        return shelves
      },
      30 * 60 * 1000, // 30 mins
    )
  }

  public static async fetchCharts(countryCode?: string): Promise<HomeShelf[]> {
    const key = `shelf:charts:${countryCode || 'default'}`
    return await fetchWithCache(
      key,
      'shelf',
      async () => {
        const data = await apiFetch<Record<string, unknown>>('/api/innertube/browse', {
          method: 'POST',
          body: JSON.stringify({
            browseId: 'FEmusic_charts',
            ...(countryCode ? { formData: { selectedValues: [countryCode] } } : {}),
          }),
        })
        const shelves = parseHomeFeed(data)
        for (const shelf of shelves) {
          const tracks = shelf.items.filter((i): i is Track => 'durationSeconds' in i)
          if (tracks.length > 0) cacheTracks(tracks)
        }
        return shelves
      },
      60 * 60 * 1000, // 1 hour
    )
  }

  public static async fetchMoodsAndGenres(): Promise<HomeShelf[]> {
    return await fetchWithCache(
      'shelf:moods',
      'shelf',
      async () => {
        const data = await apiFetch<Record<string, unknown>>('/api/innertube/browse', {
          method: 'POST',
          body: JSON.stringify({ browseId: 'FEmusic_moods_and_genres' }),
        })
        return parseHomeFeed(data)
      },
      120 * 60 * 1000, // 2 hours
    )
  }

  public static async fetchTrending(): Promise<HomeShelf[]> {
    return await fetchWithCache(
      'shelf:trending',
      'shelf',
      async () => {
        const data = await apiFetch<Record<string, unknown>>('/api/innertube/browse', {
          method: 'POST',
          body: JSON.stringify({ browseId: 'FEmusic_trending' }),
        })
        const shelves = parseHomeFeed(data)
        for (const shelf of shelves) {
          const tracks = shelf.items.filter((i): i is Track => 'durationSeconds' in i)
          if (tracks.length > 0) cacheTracks(tracks)
        }
        return shelves
      },
      30 * 60 * 1000,
    )
  }

  public static async search(
    query: string,
    filter?: SearchFilterKey,
  ): Promise<SearchResultCatalog> {
    const trimmed = query.trim()
    if (!trimmed) {
      return { tracks: [], albums: [], artists: [], playlists: [] }
    }

    const key = `search:${trimmed.toLowerCase()}:${filter || 'all'}`
    return await fetchWithCache(
      key,
      'search',
      async () => {
        const payload: { query: string; params?: string } = { query: trimmed }
        if (filter && filter !== 'all' && SEARCH_FILTERS[filter]) {
          payload.params = SEARCH_FILTERS[filter]
        }

        const data = await apiFetch<Record<string, unknown>>('/api/innertube/search', {
          method: 'POST',
          body: JSON.stringify(payload),
        })

        const catalog = parseSearchResults(data)
        if (catalog.tracks.length > 0) {
          cacheTracks(catalog.tracks)
        }
        return catalog
      },
      15 * 60 * 1000, // 15 mins
    )
  }

  public static async fetchSuggestions(query: string): Promise<string[]> {
    const trimmed = query.trim()
    if (!trimmed) return []

    const key = `suggestions:${trimmed.toLowerCase()}`
    return await fetchWithCache(
      key,
      'suggestions',
      async () => {
        return await apiFetch<string[]>(`/api/innertube/suggestions?q=${encodeURIComponent(trimmed)}`)
      },
      10 * 60 * 1000,
    )
  }

  public static async fetchAlbum(browseId: string): Promise<{ album: Album; tracks: Track[] }> {
    const key = `album:${browseId}`
    return await fetchWithCache(
      key,
      'album',
      async () => {
        const data = await apiFetch<Record<string, unknown>>('/api/innertube/browse', {
          method: 'POST',
          body: JSON.stringify({ browseId }),
        })
        const result = parseAlbumDetails(data)
        if (result.tracks.length > 0) {
          cacheTracks(result.tracks)
        }
        return result
      },
      24 * 60 * 60 * 1000, // 24 hours
    )
  }

  public static async fetchArtist(browseId: string): Promise<{
    artist: Artist
    topTracks: Track[]
    albums: Album[]
    singles: Album[]
    relatedArtists: Artist[]
  }> {
    const key = `artist:${browseId}`
    return await fetchWithCache(
      key,
      'artist',
      async () => {
        const data = await apiFetch<Record<string, unknown>>('/api/innertube/browse', {
          method: 'POST',
          body: JSON.stringify({ browseId }),
        })
        const result = parseArtistDetails(data)
        if (result.topTracks.length > 0) {
          cacheTracks(result.topTracks)
        }
        return result
      },
      24 * 60 * 60 * 1000, // 24 hours
    )
  }

  public static async resolveStream(videoId: string): Promise<StreamResolutionResult> {
    return await apiFetch<StreamResolutionResult>(
      `/api/stream/resolve?videoId=${encodeURIComponent(videoId)}`,
    )
  }

  public static async fetchSponsorBlock(videoId: string): Promise<SponsorSegment[]> {
    return await apiFetch<SponsorSegment[]>(`/api/sponsorblock/${encodeURIComponent(videoId)}`)
  }

  public static async fetchRadioNext(
    videoId?: string,
    continuation?: string,
  ): Promise<{ tracks: Track[]; continuationToken?: string }> {
    const queryParams = new URLSearchParams()
    if (videoId) queryParams.set('videoId', videoId)
    if (continuation) queryParams.set('continuation', continuation)

    return await apiFetch<{ tracks: Track[]; continuationToken?: string }>(
      `/api/radio/continuation?${queryParams.toString()}`,
    )
  }
}
