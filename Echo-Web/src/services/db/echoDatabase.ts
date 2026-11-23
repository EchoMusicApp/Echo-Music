import Dexie, { type EntityTable } from 'dexie'
import type { Track } from '../../types/domain'
import type { SynchronizedLyrics } from '../../types/lyrics'

export interface StoredPlaylistRecord {
  id: string
  title: string
  description: string
  coverUrl?: string | undefined
  coverBlob?: Blob | undefined
  trackIds: string[]
  createdAt: number
  updatedAt: number
  isSystem?: boolean | undefined
}

export interface ListeningHistoryRecord {
  id?: number | undefined
  trackId: string
  trackTitle: string
  artistName: string
  artistId?: string | undefined
  albumTitle?: string | undefined
  artwork?: string | undefined
  playedAt: number
  durationPlayedMs: number
  trackDurationMs: number
  completed: boolean
  skipped: boolean
  timeOfDay: 'morning' | 'afternoon' | 'evening' | 'night'
}

export interface LocalFileRecord {
  id: string
  fileName: string
  title: string
  artist: string
  album: string
  durationSeconds: number
  year?: string | undefined
  genre?: string | undefined
  artworkBlob?: Blob | undefined
  audioBlob: Blob
  mimeType: string
  addedAt: number
  fileSize: number
}

export interface PodcastProgressRecord {
  episodeId: string
  showTitle: string
  episodeTitle: string
  positionMs: number
  durationMs: number
  completed: boolean
  dismissed: boolean
  updatedAt: number
}

export interface RecommendationExclusionRecord {
  trackId: string
  trackTitle: string
  artistName: string
  excludedAt: number
  reason?: string | undefined
}

export interface SyncOffsetRecord {
  trackId: string
  offsetMs: number
  updatedAt: number
}

export interface CachedLyricsRecord {
  trackId: string
  query: string
  lyrics: SynchronizedLyrics
  provider: string
  fetchedAt: number
}

export interface MetadataCacheRecord {
  key: string
  kind: 'shelf' | 'album' | 'artist' | 'playlist' | 'search' | 'suggestions'
  data: unknown
  updatedAt: number
  expiresAt: number
}

export class EchoDatabase extends Dexie {
  playlists!: EntityTable<StoredPlaylistRecord, 'id'>
  history!: EntityTable<ListeningHistoryRecord, 'id'>
  localFiles!: EntityTable<LocalFileRecord, 'id'>
  podcastProgress!: EntityTable<PodcastProgressRecord, 'episodeId'>
  recommendationExclusions!: EntityTable<RecommendationExclusionRecord, 'trackId'>
  cachedLyrics!: EntityTable<CachedLyricsRecord, 'trackId'>
  syncOffsets!: EntityTable<SyncOffsetRecord, 'trackId'>
  metadataCache!: EntityTable<MetadataCacheRecord, 'key'>

  constructor() {
    super('EchoMusicWebDB')
    this.version(1).stores({
      tracks: 'id, videoId, title, artistName, downloadedAt',
      playlists: 'id, title, updatedAt, createdAt',
      history: '++id, trackId, artistName, playedAt, timeOfDay, completed, skipped',
      localFiles: 'id, title, artist, album, addedAt',
      podcastProgress: 'episodeId, showTitle, completed, dismissed, updatedAt',
      recommendationExclusions: 'trackId, artistName, excludedAt',
      cachedLyrics: 'trackId, query, provider, fetchedAt',
    })
    // Offline song downloads were removed; drop the orphaned table for existing users.
    this.version(2).stores({ tracks: null })
    this.version(3).stores({ syncOffsets: 'trackId, updatedAt' })
    this.version(4).stores({ metadataCache: 'key, kind, updatedAt, expiresAt' })
  }
}

export const db = new EchoDatabase()

export async function getStorageUsage(): Promise<{
  localFilesCount: number
  localFilesSizeBytes: number
  historyCount: number
  totalEstimateBytes?: number | undefined
}> {
  const localFiles = await db.localFiles.toArray()
  const localFilesSizeBytes = localFiles.reduce((acc, cur) => acc + (cur.fileSize || 0), 0)

  const historyCount = await db.history.count()

  let totalEstimateBytes: number | undefined
  if (navigator.storage && navigator.storage.estimate) {
    try {
      const estimate = await navigator.storage.estimate()
      totalEstimateBytes = estimate.usage
    } catch {
      // Ignore if unsupported
    }
  }

  return {
    localFilesCount: localFiles.length,
    localFilesSizeBytes,
    historyCount,
    totalEstimateBytes,
  }
}

export async function clearAllLocalFiles(): Promise<void> {
  await db.localFiles.clear()
}

export async function clearListeningHistory(): Promise<void> {
  await db.history.clear()
}

/**
 * Full Library Backup & Restore
 */
export interface FullEchoBackup {
  version: 1
  exportedAt: number
  preferences: Record<string, unknown>
  playlists: StoredPlaylistRecord[]
  likedTrackIds: string[]
  history: ListeningHistoryRecord[]
  exclusions: RecommendationExclusionRecord[]
}

export async function exportFullBackup(
  preferences: Record<string, unknown>,
  likedTrackIds: string[],
): Promise<string> {
  const playlists = await db.playlists.toArray()
  const history = await db.history.limit(500).toArray()
  const exclusions = await db.recommendationExclusions.toArray()

  const backup: FullEchoBackup = {
    version: 1,
    exportedAt: Date.now(),
    preferences,
    playlists: playlists.map((p) => ({ ...p, coverBlob: undefined })),
    likedTrackIds,
    history,
    exclusions,
  }

  return JSON.stringify(backup, null, 2)
}

export async function importFullBackup(
  jsonString: string,
  mode: 'merge' | 'replace' = 'merge',
): Promise<{
  playlistsRestored: number
  historyRestored: number
  exclusionsRestored: number
  likedTrackIds: string[]
  preferences?: Record<string, unknown> | undefined
}> {
  const data = JSON.parse(jsonString) as Partial<FullEchoBackup>
  if (!data || data.version !== 1) {
    throw new Error('Invalid Echo backup format')
  }

  if (mode === 'replace') {
    await db.playlists.clear()
    await db.history.clear()
    await db.recommendationExclusions.clear()
  }

  let playlistsRestored = 0
  if (Array.isArray(data.playlists)) {
    for (const p of data.playlists) {
      await db.playlists.put(p)
      playlistsRestored++
    }
  }

  let historyRestored = 0
  if (Array.isArray(data.history)) {
    for (const h of data.history) {
      delete h.id
      await db.history.add(h)
      historyRestored++
    }
  }

  let exclusionsRestored = 0
  if (Array.isArray(data.exclusions)) {
    for (const e of data.exclusions) {
      await db.recommendationExclusions.put(e)
      exclusionsRestored++
    }
  }

  return {
    playlistsRestored,
    historyRestored,
    exclusionsRestored,
    likedTrackIds: Array.isArray(data.likedTrackIds) ? data.likedTrackIds : [],
    preferences: data.preferences,
  }
}

/**
 * Resolves track metadata for a given track or video ID from IndexedDB caches
 * (downloads, history, local files) or the server stream resolution endpoint.
 */
export async function resolveMissingTrack(id: string): Promise<Track | null> {
  if (!id) return null

  try {
    // 1. Check listening history records in IndexedDB
    const history = await db.history.where('trackId').equals(id).first()
    if (history) {
      const art = history.artwork || `https://i.ytimg.com/vi/${history.trackId}/hqdefault.jpg`
      return {
        id: history.trackId,
        title: history.trackTitle,
        artistName: history.artistName,
        artistId: history.artistId || `yt_${history.artistName}`,
        albumTitle: history.albumTitle || history.trackTitle,
        albumId: `album_${history.trackId}`,
        albumImage: art,
        durationMs: history.trackDurationMs,
        artwork: art,
        explicit: false,
        playCount: 1,
        videoId: history.trackId,
      }
    }

    // 3. Check local imported audio files
    const local = await db.localFiles.get(id)
    if (local) {
      const art = local.artworkBlob
        ? URL.createObjectURL(local.artworkBlob)
        : `https://i.ytimg.com/vi/${id}/hqdefault.jpg`
      return {
        id: local.id,
        title: local.title,
        artistName: local.artist,
        artistId: `local_${local.artist}`,
        albumTitle: local.album,
        albumId: `local_${local.album}`,
        albumImage: art,
        durationMs: local.durationSeconds * 1000,
        artwork: art,
        explicit: false,
        playCount: 0,
      }
    }

    // 4. Resolve metadata from audio stream resolution endpoint
    const res = await fetch(`/api/stream/resolve?videoId=${encodeURIComponent(id)}`)
    if (res.ok) {
      const data = (await res.json()) as {
        videoId?: string
        title?: string
        artist?: string
        durationMs?: number
      }
      const vid = data.videoId || id
      const art = `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`
      return {
        id: vid,
        title: data.title || 'Unknown Title',
        artistName: data.artist || 'Unknown Artist',
        artistId: `artist_${vid}`,
        albumTitle: data.title || 'Single',
        albumId: `album_${vid}`,
        albumImage: art,
        durationMs: data.durationMs || 180000,
        artwork: art,
        explicit: false,
        playCount: 0,
        videoId: vid,
      }
    }
  } catch {
    // Network or server resolution error
  }

  // 5. Explicit unresolved fallback: no fabricated title or duration. Callers can detect this
  // via `unresolved` and surface an honest "unresolved" state.
  const fallbackArt = `https://i.ytimg.com/vi/${id}/hqdefault.jpg`
  return {
    id,
    title: 'Unresolved Track',
    artistName: 'Metadata unavailable',
    artistId: `artist_${id}`,
    albumTitle: 'Unknown Release',
    albumId: `unresolved_${id}`,
    albumImage: fallbackArt,
    durationMs: 0,
    artwork: fallbackArt,
    explicit: false,
    playCount: 0,
    videoId: id,
    unresolved: true,
  }
}
