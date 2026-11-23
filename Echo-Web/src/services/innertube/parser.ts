import type { Album, Artist, HomeShelf, PlaylistSummary, Track } from '../../types/domain.ts'
import { artwork } from '../../utils/artwork.ts'

export function normalizeThumbnailUrl(url?: string, size = 544): string {
  if (!url || typeof url !== 'string' || !url.trim()) {
    return artwork('echo-placeholder', size)
  }

  let cleaned = url.trim()
  if (cleaned.startsWith('//')) {
    cleaned = `https:${cleaned}`
  }

  // Replace =w...-h... pattern
  if (/=w\d+-h\d+/i.test(cleaned)) {
    return cleaned.replace(/=w\d+-h\d+/i, `=w${size}-h${size}`)
  }

  // Replace =s... pattern
  if (/=s\d+/i.test(cleaned)) {
    return cleaned.replace(/=s\d+[^?#]*/i, `=s${size}`)
  }

  // Replace -w...-h... pattern
  if (/-w\d+-h\d+/i.test(cleaned)) {
    return cleaned.replace(/-w\d+-h\d+[^?#]*/i, `-w${size}-h${size}`)
  }

  return cleaned
}

export function parseDurationToSeconds(text?: string): number {
  if (!text || typeof text !== 'string') return 180
  const parts = text
    .trim()
    .split(':')
    .map((p) => Number.parseInt(p, 10))
  if (parts.some((n) => Number.isNaN(n))) return 180
  if (parts.length === 1) {
    return parts[0] ?? 180
  }
  if (parts.length === 2) {
    const mins = parts[0] ?? 0
    const secs = parts[1] ?? 0
    return mins * 60 + secs
  }
  if (parts.length === 3) {
    const hrs = parts[0] ?? 0
    const mins = parts[1] ?? 0
    const secs = parts[2] ?? 0
    return hrs * 3600 + mins * 60 + secs
  }
  return 180
}

interface RunItem {
  text?: string
  navigationEndpoint?: {
    browseEndpoint?: {
      browseId?: string
      browseEndpointContextSupportedConfigs?: {
        browseEndpointContextMusicConfig?: {
          pageType?: string
        }
      }
    }
    watchEndpoint?: {
      videoId?: string
      playlistId?: string
    }
  }
}

interface FlexColumnRenderer {
  musicResponsiveListItemFlexColumnRenderer?: {
    text?: {
      runs?: RunItem[]
    }
  }
}

interface FixedColumnRenderer {
  musicResponsiveListItemFixedColumnRenderer?: {
    text?: {
      runs?: RunItem[]
    }
  }
}

interface ResponsiveListItem {
  musicResponsiveListItemRenderer?: {
    trackingParams?: string
    thumbnail?: {
      musicThumbnailRenderer?: {
        thumbnail?: {
          thumbnails?: Array<{ url?: string; width?: number; height?: number }>
        }
      }
    }
    overlay?: {
      musicItemThumbnailOverlayRenderer?: {
        content?: {
          musicPlayButtonRenderer?: {
            playNavigationEndpoint?: {
              watchEndpoint?: {
                videoId?: string
              }
            }
          }
        }
      }
    }
    flexColumns?: FlexColumnRenderer[]
    fixedColumns?: FixedColumnRenderer[]
    navigationEndpoint?: {
      browseEndpoint?: {
        browseId?: string
        browseEndpointContextSupportedConfigs?: {
          browseEndpointContextMusicConfig?: {
            pageType?: string
          }
        }
      }
      watchEndpoint?: {
        videoId?: string
      }
    }
    playlistItemData?: {
      videoId?: string
    }
    badges?: Array<{
      musicInlineBadgeRenderer?: {
        icon?: {
          iconType?: string
        }
      }
    }>
  }
}

interface TwoRowItem {
  musicTwoRowItemRenderer?: {
    title?: {
      runs?: RunItem[]
    }
    subtitle?: {
      runs?: RunItem[]
    }
    thumbnailRenderer?: {
      musicThumbnailRenderer?: {
        thumbnail?: {
          thumbnails?: Array<{ url?: string; width?: number; height?: number }>
        }
      }
    }
    navigationEndpoint?: {
      browseEndpoint?: {
        browseId?: string
        browseEndpointContextSupportedConfigs?: {
          browseEndpointContextMusicConfig?: {
            pageType?: string
          }
        }
      }
      watchEndpoint?: {
        videoId?: string
        playlistId?: string
      }
    }
    thumbnailOverlay?: Record<string, unknown>
  }
}

export function parseResponsiveListItem(
  item: unknown,
): Track | Album | Artist | PlaylistSummary | null {
  if (!item || typeof item !== 'object') return null

  const row = (item as ResponsiveListItem).musicResponsiveListItemRenderer
  if (row) {
    return parseResponsiveListItemRenderer(row)
  }

  const twoRow = (item as TwoRowItem).musicTwoRowItemRenderer
  if (twoRow) {
    return parseTwoRowItemRenderer(twoRow)
  }

  return null
}

function extractThumbnailUrl(thumbnails?: Array<{ url?: string }>): string {
  if (!thumbnails || thumbnails.length === 0) return ''
  const highest = thumbnails[thumbnails.length - 1]
  return normalizeThumbnailUrl(highest?.url)
}

function parseResponsiveListItemRenderer(
  row: NonNullable<ResponsiveListItem['musicResponsiveListItemRenderer']>,
): Track | Album | Artist | PlaylistSummary | null {
  const flexCols = row.flexColumns || []
  const col0Runs = flexCols[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || []
  const col1Runs = flexCols[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || []

  const title = col0Runs[0]?.text?.trim() || 'Unknown'
  const videoId =
    row.playlistItemData?.videoId ||
    col0Runs[0]?.navigationEndpoint?.watchEndpoint?.videoId ||
    row.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer
      ?.playNavigationEndpoint?.watchEndpoint?.videoId ||
    row.navigationEndpoint?.watchEndpoint?.videoId

  const browseId =
    row.navigationEndpoint?.browseEndpoint?.browseId ||
    col0Runs[0]?.navigationEndpoint?.browseEndpoint?.browseId

  const pageType =
    row.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
      ?.browseEndpointContextMusicConfig?.pageType ||
    col0Runs[0]?.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
      ?.browseEndpointContextMusicConfig?.pageType

  const thumbnails = row.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
  const rawArtwork =
    extractThumbnailUrl(thumbnails) ||
    (videoId ? `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg` : '')

  const isExplicit = Boolean(
    row.badges?.some((b) =>
      b.musicInlineBadgeRenderer?.icon?.iconType?.toLowerCase().includes('explicit'),
    ),
  )

  // Check if this item is an Artist
  if (
    !videoId &&
    (pageType === 'MUSIC_PAGE_TYPE_ARTIST' ||
      browseId?.startsWith('UC') ||
      col1Runs.some((r) => {
        const txt = r.text?.trim().toLowerCase()
        return txt === 'artist' || txt?.startsWith('artist •')
      }))
  ) {
    const artistId = browseId || `artist-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
    const artist: Artist = {
      id: artistId,
      name: title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      verified: true,
    }
    return artist
  }

  // Check if this item is an Album
  if (
    pageType === 'MUSIC_PAGE_TYPE_ALBUM' ||
    (browseId?.startsWith('MPREb_') && !videoId) ||
    col1Runs.some((r) => {
      const t = r.text?.toLowerCase() || ''
      return t === 'album' || t === 'ep' || t === 'single'
    })
  ) {
    const albumId = browseId || `album-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
    const artists: Artist[] = []
    let artistName = 'Unknown Artist'
    let artistId = `artist-${albumId}`
    let year: number | undefined

    for (const r of col1Runs) {
      if (r.navigationEndpoint?.browseEndpoint?.browseId) {
        artistName = r.text?.trim() || artistName
        artistId = r.navigationEndpoint.browseEndpoint.browseId
        artists.push({
          id: artistId,
          name: artistName,
          thumbnailUrl: rawArtwork,
          image: rawArtwork,
        })
      } else if (r.text && /^\d{4}$/.test(r.text.trim())) {
        year = Number.parseInt(r.text.trim(), 10)
      }
    }

    const album: Album = {
      id: albumId,
      title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      artists:
        artists.length > 0
          ? artists
          : [{ id: artistId, name: artistName, thumbnailUrl: rawArtwork, image: rawArtwork }],
      artistName,
      artistId,
      year,
      type: 'album',
    }
    return album
  }

  // Check if this item is a Playlist
  if (pageType === 'MUSIC_PAGE_TYPE_PLAYLIST' || (browseId?.startsWith('VL') && !videoId)) {
    const playlist: PlaylistSummary = {
      id: browseId || `playlist-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
      title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      author: col1Runs[0]?.text?.trim(),
    }
    return playlist
  }

  // Default to Track (if videoId is present or assumed song)
  const trackId = videoId || browseId || `track-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
  const artists: Artist[] = []
  let artistName = 'Unknown Artist'
  let artistId = `artist-${trackId}`
  let albumTitle = 'Single'
  let albumId = `album-${trackId}`

  for (const r of col1Runs) {
    const endpoint = r.navigationEndpoint?.browseEndpoint
    const bId = endpoint?.browseId
    const pType =
      endpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
    const text = r.text?.trim()

    if (!text || text === '•') continue

    if (
      pType === 'MUSIC_PAGE_TYPE_ARTIST' ||
      (bId?.startsWith('UC') && !artistId.startsWith('UC'))
    ) {
      artistName = text
      artistId = bId || artistId
      artists.push({
        id: artistId,
        name: artistName,
        thumbnailUrl: rawArtwork,
        image: rawArtwork,
      })
    } else if (pType === 'MUSIC_PAGE_TYPE_ALBUM' || bId?.startsWith('MPREb_')) {
      albumTitle = text
      albumId = bId || albumId
    } else if (
      artists.length === 0 &&
      !text.includes('views') &&
      !text.includes('plays') &&
      !text.includes(':')
    ) {
      artistName = text
    }
  }

  if (artists.length === 0) {
    artists.push({
      id: artistId,
      name: artistName,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
    })
  }

  let durationSeconds = 180
  const fixedCol =
    row.fixedColumns?.[0]?.musicResponsiveListItemFixedColumnRenderer?.text?.runs?.[0]?.text
  if (fixedCol) {
    durationSeconds = parseDurationToSeconds(fixedCol)
  } else {
    for (const r of col1Runs) {
      if (r.text && /^\d+:\d+(:\d+)?$/.test(r.text.trim())) {
        durationSeconds = parseDurationToSeconds(r.text.trim())
        break
      }
    }
  }

  const track: Track = {
    id: trackId,
    title,
    artists,
    album: { id: albumId, title: albumTitle },
    durationSeconds,
    thumbnailUrl: rawArtwork,
    isExplicit,

    // Phase 1 AudioEngine & UI contracts compatibility
    artistId,
    artistName,
    albumId,
    albumTitle,
    albumImage: rawArtwork,
    durationMs: durationSeconds * 1000,
    artwork: rawArtwork,
    explicit: isExplicit,
    playCount: 0,
    videoId: trackId,
  }

  return track
}

function parseTwoRowItemRenderer(
  twoRow: NonNullable<TwoRowItem['musicTwoRowItemRenderer']>,
): Track | Album | Artist | PlaylistSummary | null {
  const title = twoRow.title?.runs?.[0]?.text?.trim() || 'Unknown'
  const endpoint = twoRow.navigationEndpoint

  const overlayPlay = (twoRow.thumbnailOverlay as Record<string, unknown> | undefined)?.[
    'musicItemThumbnailOverlayRenderer'
  ] as Record<string, unknown> | undefined
  const overlayContent = overlayPlay?.['content'] as Record<string, unknown> | undefined
  const overlayBtn = overlayContent?.['musicPlayButtonRenderer'] as
    Record<string, unknown> | undefined
  const overlayNav = overlayBtn?.['playNavigationEndpoint'] as Record<string, unknown> | undefined
  const overlayWatch = overlayNav?.['watchEndpoint'] as { videoId?: string } | undefined
  const overlayWatchPl = overlayNav?.['watchPlaylistEndpoint'] as
    { playlistId?: string } | undefined

  let browseId =
    endpoint?.browseEndpoint?.browseId ||
    (overlayWatchPl?.playlistId ? `VL${overlayWatchPl.playlistId}` : undefined)
  const videoId = endpoint?.watchEndpoint?.videoId || overlayWatch?.videoId
  const pageType =
    endpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
      ?.browseEndpointContextMusicConfig?.pageType

  const thumbnails = twoRow.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails
  const rawArtwork =
    extractThumbnailUrl(thumbnails) ||
    (videoId ? `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg` : '')
  const subtitleRuns = twoRow.subtitle?.runs || []

  // Check Artist
  if (pageType === 'MUSIC_PAGE_TYPE_ARTIST' || (browseId?.startsWith('UC') && !videoId)) {
    const artist: Artist = {
      id: browseId || `artist-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
      name: title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      verified: true,
    }
    return artist
  }

  // Check Album
  if (
    pageType === 'MUSIC_PAGE_TYPE_ALBUM' ||
    (browseId?.startsWith('MPREb_') && !videoId) ||
    subtitleRuns.some((r) => r.text?.toLowerCase().includes('album'))
  ) {
    let artistName = 'Unknown Artist'
    let artistId = `artist-${browseId || title}`
    let year: number | undefined

    for (const r of subtitleRuns) {
      if (r.navigationEndpoint?.browseEndpoint?.browseId) {
        artistName = r.text?.trim() || artistName
        artistId = r.navigationEndpoint.browseEndpoint.browseId
      } else if (r.text && /^\d{4}$/.test(r.text.trim())) {
        year = Number.parseInt(r.text.trim(), 10)
      }
    }

    const album: Album = {
      id: browseId || `album-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
      title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      artists: [{ id: artistId, name: artistName, thumbnailUrl: rawArtwork, image: rawArtwork }],
      artistName,
      artistId,
      year,
      type: 'album',
    }
    return album
  }

  // Check Playlist
  if (
    pageType === 'MUSIC_PAGE_TYPE_PLAYLIST' ||
    (browseId &&
      (browseId.startsWith('VL') || browseId.startsWith('PL') || browseId.startsWith('RD')))
  ) {
    const playlist: PlaylistSummary = {
      id: browseId || '',
      title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      author: subtitleRuns[0]?.text?.trim(),
      description: subtitleRuns.map((r) => r.text).join(''),
    }
    return playlist
  }

  // Check Track
  if (videoId) {
    let artistName = subtitleRuns[0]?.text?.trim() || 'Unknown Artist'
    const artistId =
      subtitleRuns[0]?.navigationEndpoint?.browseEndpoint?.browseId || `artist-${videoId}`
    const albumTitle =
      subtitleRuns.length > 2 ? subtitleRuns[2]?.text?.trim() || 'Single' : 'Single'
    const albumId = `album-${videoId}`

    const track: Track = {
      id: videoId,
      title,
      artists: [{ id: artistId, name: artistName, thumbnailUrl: rawArtwork, image: rawArtwork }],
      album: { id: albumId, title: albumTitle },
      durationSeconds: 180,
      thumbnailUrl: rawArtwork,
      isExplicit: false,

      artistId,
      artistName,
      albumId,
      albumTitle,
      albumImage: rawArtwork,
      durationMs: 180000,
      artwork: rawArtwork,
      explicit: false,
      playCount: 0,
      videoId,
    }
    return track
  }

  if (browseId) {
    const playlist: PlaylistSummary = {
      id: browseId || '',
      title,
      thumbnailUrl: rawArtwork,
      image: rawArtwork,
      author: subtitleRuns[0]?.text?.trim(),
    }
    return playlist
  }

  return null
}

export function parseHomeFeed(data: unknown): HomeShelf[] {
  if (!data || typeof data !== 'object') return []

  const raw = data as Record<string, unknown>
  const contents = raw['contents'] as Record<string, unknown> | undefined
  const singleCol = contents?.['singleColumnBrowseResultsRenderer'] as
    Record<string, unknown> | undefined
  const twoCol = contents?.['twoColumnBrowseResultsRenderer'] as
    Record<string, unknown> | undefined
  const tabs = (singleCol?.['tabs'] || twoCol?.['tabs']) as Array<Record<string, unknown>> | undefined
  const tab0 = tabs?.[0]?.['tabRenderer'] as Record<string, unknown> | undefined
  const tabContent = tab0?.['content'] as Record<string, unknown> | undefined
  const secondary = twoCol?.['secondaryContents'] as Record<string, unknown> | undefined
  const listRenderer = (tabContent?.['sectionListRenderer'] ||
    contents?.['sectionListRenderer'] ||
    secondary?.['sectionListRenderer']) as Record<string, unknown> | undefined
  const rawSections = (listRenderer?.['contents'] as Array<Record<string, unknown>>) || []

  const shelves: HomeShelf[] = []

  for (const sec of rawSections) {
    const carousel = sec['musicCarouselShelfRenderer'] as Record<string, unknown> | undefined
    const shelf = sec['musicShelfRenderer'] as Record<string, unknown> | undefined
    const playlistShelf = sec['musicPlaylistShelfRenderer'] as Record<string, unknown> | undefined
    const target = carousel || shelf || playlistShelf
    if (!target) continue

    const header = target['header'] as Record<string, unknown> | undefined
    const basicHeader = header?.['musicCarouselShelfBasicHeaderRenderer'] as
      Record<string, unknown> | undefined
    const titleRuns =
      (basicHeader?.['title'] as { runs?: RunItem[] })?.runs ||
      (target['title'] as { runs?: RunItem[] })?.runs ||
      []

    const shelfTitle = titleRuns[0]?.text?.trim() || 'Featured'
    const itemsRaw = (target['contents'] as unknown[]) || []

    const parsedItems: Array<Track | Album | PlaylistSummary> = []
    for (const item of itemsRaw) {
      const parsed = parseResponsiveListItem(item)
      if (parsed && 'title' in parsed) {
        parsedItems.push(parsed as Track | Album | PlaylistSummary)
      }
    }

    if (parsedItems.length > 0) {
      shelves.push({
        title: shelfTitle,
        items: parsedItems,
      })
    }
  }

  return shelves
}

export function parseAlbumDetails(data: unknown): { album: Album; tracks: Track[] } {
  if (!data || typeof data !== 'object') {
    throw new Error('Invalid album data payload')
  }

  const raw = data as Record<string, unknown>
  const contents = raw['contents'] as Record<string, unknown> | undefined
  const twoCol = contents?.['twoColumnBrowseResultsRenderer'] as Record<string, unknown> | undefined
  const singleCol = contents?.['singleColumnBrowseResultsRenderer'] as Record<string, unknown> | undefined

  const tabs = (twoCol?.['tabs'] || singleCol?.['tabs']) as Array<Record<string, unknown>> | undefined
  const tab0 = tabs?.[0]?.['tabRenderer'] as Record<string, unknown> | undefined
  const tabContent = tab0?.['content'] as Record<string, unknown> | undefined
  const secondary = twoCol?.['secondaryContents'] as Record<string, unknown> | undefined
  const sectionList = (tabContent?.['sectionListRenderer'] ||
    contents?.['sectionListRenderer'] ||
    secondary?.['sectionListRenderer']) as Record<string, unknown> | undefined
  const sectionContents = (sectionList?.['contents'] as Array<Record<string, unknown>>) || []

  const headerObj = raw['header'] as Record<string, unknown> | undefined
  const editableHeader = headerObj?.['musicEditablePlaylistDetailHeaderRenderer'] as
    Record<string, unknown> | undefined

  const headerRenderer = (
    sectionContents[0]?.['musicResponsiveHeaderRenderer'] ||
    headerObj?.['musicResponsiveHeaderRenderer'] ||
    headerObj?.['musicDetailHeaderRenderer'] ||
    headerObj?.['musicVisualHeaderRenderer'] ||
    (editableHeader?.['header'] as Record<string, unknown> | undefined)?.['musicResponsiveHeaderRenderer']
  ) as Record<string, unknown> | undefined

  const titleRuns = (headerRenderer?.['title'] as { runs?: RunItem[] })?.runs || []
  const albumTitle = titleRuns[0]?.text?.trim() || 'Unknown Album'

  const thumbnails = (
    headerRenderer?.['thumbnail'] as {
      musicThumbnailRenderer?: {
        thumbnail?: { thumbnails?: Array<{ url?: string }> }
      }
    }
  )?.musicThumbnailRenderer?.thumbnail?.thumbnails
  const albumArtwork = extractThumbnailUrl(thumbnails)

  const subtitleRuns = (headerRenderer?.['subtitle'] as { runs?: RunItem[] })?.runs || []
  const secondSubtitleRuns =
    (headerRenderer?.['secondSubtitle'] as { runs?: RunItem[] })?.runs || []

  let artistName = 'Unknown Artist'
  let artistId = `artist-${albumTitle.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
  let year: number | undefined
  let trackCount: number | undefined

  for (const r of subtitleRuns) {
    if (r.navigationEndpoint?.browseEndpoint?.browseId) {
      artistName = r.text?.trim() || artistName
      artistId = r.navigationEndpoint.browseEndpoint.browseId
    } else if (r.text && /^\d{4}$/.test(r.text.trim())) {
      year = Number.parseInt(r.text.trim(), 10)
    } else if (artistName === 'Unknown Artist' && r.text?.trim() && !/^\s*•\s*$/.test(r.text)) {
      artistName = r.text.trim()
    }
  }

  for (const r of secondSubtitleRuns) {
    if (r.text && r.text.includes('song')) {
      const match = /(\d+)\s+song/.exec(r.text)
      if (match?.[1]) trackCount = Number.parseInt(match[1], 10)
    }
  }

  // Find tracks shelf across secondaryContents or primary sectionList
  const secList = (secondary?.['sectionListRenderer'] || sectionList) as Record<string, unknown> | undefined
  const secContents = (secList?.['contents'] as Array<Record<string, unknown>>) || sectionContents

  let rawTracks: unknown[] = []
  for (const sec of secContents) {
    const shelf = (sec['musicShelfRenderer'] || sec['musicPlaylistShelfRenderer']) as
      Record<string, unknown> | undefined
    if (shelf?.['contents'] && Array.isArray(shelf['contents'])) {
      rawTracks = shelf['contents'] as unknown[]
      break
    }
  }

  const tracks: Track[] = []
  let totalDurationMs = 0

  for (const item of rawTracks) {
    const parsed = parseResponsiveListItem(item)
    if (parsed && 'durationSeconds' in parsed) {
      const t = parsed as Track
      t.album = { id: t.album?.id || `album-${albumTitle}`, title: albumTitle }
      t.albumTitle = albumTitle
      t.albumImage = albumArtwork || t.albumImage
      t.artwork =
        t.artwork ||
        t.thumbnailUrl ||
        (t.videoId ? `https://i.ytimg.com/vi/${t.videoId}/hqdefault.jpg` : '') ||
        albumArtwork
      t.thumbnailUrl = t.thumbnailUrl || t.artwork
      if (t.artistName === 'Unknown Artist') {
        t.artistName = artistName
        t.artistId = artistId
      }
      tracks.push(t)
      totalDurationMs += t.durationMs
    }
  }

  const album: Album = {
    id: `album-${albumTitle.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
    title: albumTitle,
    thumbnailUrl: albumArtwork,
    image: albumArtwork,
    artists: [{ id: artistId, name: artistName, thumbnailUrl: albumArtwork, image: albumArtwork }],
    artistName,
    artistId,
    year,
    trackCount: trackCount || tracks.length,
    durationMs: totalDurationMs,
    type: 'album',
  }

  return { album, tracks }
}

export function parseArtistDetails(data: unknown): {
  artist: Artist
  topTracks: Track[]
  albums: Album[]
  singles: Album[]
  relatedArtists: Artist[]
} {
  if (!data || typeof data !== 'object') {
    throw new Error('Invalid artist data payload')
  }

  const raw = data as Record<string, unknown>
  const header = raw['header'] as Record<string, unknown> | undefined
  const immersive = (header?.['musicImmersiveHeaderRenderer'] ||
    header?.['musicVisualHeaderRenderer']) as Record<string, unknown> | undefined

  const titleRuns = (immersive?.['title'] as { runs?: RunItem[] })?.runs || []
  const artistName = titleRuns[0]?.text?.trim() || 'Unknown Artist'

  const thumbnails = (
    immersive?.['thumbnail'] as {
      musicThumbnailRenderer?: {
        thumbnail?: { thumbnails?: Array<{ url?: string }> }
      }
    }
  )?.musicThumbnailRenderer?.thumbnail?.thumbnails
  const artistArtwork = extractThumbnailUrl(thumbnails)

  const artist: Artist = {
    id: `artist-${artistName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
    name: artistName,
    thumbnailUrl: artistArtwork,
    image: artistArtwork,
    verified: true,
  }

  const contents = raw['contents'] as Record<string, unknown> | undefined
  const singleCol = contents?.['singleColumnBrowseResultsRenderer'] as Record<string, unknown> | undefined
  const twoCol = contents?.['twoColumnBrowseResultsRenderer'] as Record<string, unknown> | undefined
  const tabs = (singleCol?.['tabs'] || twoCol?.['tabs']) as Array<Record<string, unknown>> | undefined
  const tab0 = tabs?.[0]?.['tabRenderer'] as Record<string, unknown> | undefined
  const tabContent = tab0?.['content'] as Record<string, unknown> | undefined
  const secondary = twoCol?.['secondaryContents'] as Record<string, unknown> | undefined
  const sectionList = (tabContent?.['sectionListRenderer'] ||
    contents?.['sectionListRenderer'] ||
    secondary?.['sectionListRenderer']) as Record<string, unknown> | undefined
  const sections = (sectionList?.['contents'] as Array<Record<string, unknown>>) || []

  const topTracks: Track[] = []
  const albums: Album[] = []
  const singles: Album[] = []
  const relatedArtists: Artist[] = []

  for (const sec of sections) {
    const shelf = sec['musicShelfRenderer'] as Record<string, unknown> | undefined
    const carousel = sec['musicCarouselShelfRenderer'] as Record<string, unknown> | undefined
    const descShelf = sec['musicDescriptionShelfRenderer'] as Record<string, unknown> | undefined

    if (descShelf) {
      const bioRuns = (descShelf['description'] as { runs?: RunItem[] })?.runs || []
      const bioText = bioRuns.map((r) => r.text).join('')
      if (bioText) artist.bio = bioText
      continue
    }

    const target = shelf || carousel
    if (!target) continue

    const headerObj = target['header'] as Record<string, unknown> | undefined
    const basicHeader = headerObj?.['musicCarouselShelfBasicHeaderRenderer'] as
      Record<string, unknown> | undefined
    const title =
      (basicHeader?.['title'] as { runs?: RunItem[] })?.runs?.[0]?.text?.toLowerCase() ||
      (target['title'] as { runs?: RunItem[] })?.runs?.[0]?.text?.toLowerCase() ||
      ''

    const items = (target['contents'] as unknown[]) || []

    if (title.includes('top songs') || title.includes('songs')) {
      for (const item of items) {
        const parsed = parseResponsiveListItem(item)
        if (parsed && 'durationSeconds' in parsed) {
          topTracks.push(parsed as Track)
        }
      }
    } else if (title.includes('album')) {
      for (const item of items) {
        const parsed = parseResponsiveListItem(item)
        if (parsed && 'artists' in parsed && !('durationSeconds' in parsed)) {
          const alb = parsed as Album
          alb.artistName = artistName
          alb.artistId = artist.id
          albums.push(alb)
        }
      }
    } else if (title.includes('singles') || title.includes('eps')) {
      for (const item of items) {
        const parsed = parseResponsiveListItem(item)
        if (parsed && 'artists' in parsed && !('durationSeconds' in parsed)) {
          const alb = parsed as Album
          alb.artistName = artistName
          alb.artistId = artist.id
          alb.type = 'single'
          singles.push(alb)
        }
      }
    } else if (title.includes('fans might also like') || title.includes('similar')) {
      for (const item of items) {
        const parsed = parseResponsiveListItem(item)
        if (parsed && 'name' in parsed && !('title' in parsed)) {
          relatedArtists.push(parsed as Artist)
        }
      }
    }
  }

  return { artist, topTracks, albums, singles, relatedArtists }
}

export function parseSearchResults(data: unknown): {
  tracks: Track[]
  albums: Album[]
  artists: Artist[]
  playlists: PlaylistSummary[]
} {
  const result: {
    tracks: Track[]
    albums: Album[]
    artists: Artist[]
    playlists: PlaylistSummary[]
  } = {
    tracks: [],
    albums: [],
    artists: [],
    playlists: [],
  }

  if (!data || typeof data !== 'object') return result

  const raw = data as Record<string, unknown>
  const contents = raw['contents'] as Record<string, unknown> | undefined
  const tabbed = contents?.['tabbedSearchResultsRenderer'] as Record<string, unknown> | undefined
  const tabs = tabbed?.['tabs'] as Array<Record<string, unknown>> | undefined
  const tab0 = tabs?.[0]?.['tabRenderer'] as Record<string, unknown> | undefined
  const tabContent = tab0?.['content'] as Record<string, unknown> | undefined
  const sectionList = tabContent?.['sectionListRenderer'] as Record<string, unknown> | undefined
  const rawSections = (sectionList?.['contents'] as Array<Record<string, unknown>>) || []

  for (const sec of rawSections) {
    const cardShelf = sec['musicCardShelfRenderer'] as Record<string, unknown> | undefined
    if (cardShelf) {
      const titleRuns = (cardShelf['title'] as { runs?: RunItem[] })?.runs || []
      const cardTitle = titleRuns[0]?.text?.trim() || ''
      const subtitleRuns = (cardShelf['subtitle'] as { runs?: RunItem[] })?.runs || []
      const isArtist = subtitleRuns.some((r) => r.text?.toLowerCase().includes('artist'))

      const thumbnails = (
        cardShelf['thumbnail'] as {
          musicThumbnailRenderer?: {
            thumbnail?: { thumbnails?: Array<{ url?: string }> }
          }
        }
      )?.musicThumbnailRenderer?.thumbnail?.thumbnails
      const art = extractThumbnailUrl(thumbnails)

      if (isArtist && cardTitle) {
        result.artists.push({
          id: `artist-${cardTitle.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
          name: cardTitle,
          thumbnailUrl: art,
          image: art,
          verified: true,
        })
      }
    }

    const itemSection = sec['itemSectionRenderer'] as Record<string, unknown> | undefined
    const items = (itemSection?.['contents'] as unknown[]) || []

    const shelf = sec['musicShelfRenderer'] as Record<string, unknown> | undefined
    const shelfItems = (shelf?.['contents'] as unknown[]) || []

    const allItems = [...items, ...shelfItems]

    for (const item of allItems) {
      const parsed = parseResponsiveListItem(item)
      if (!parsed) continue

      if ('durationSeconds' in parsed) {
        result.tracks.push(parsed as Track)
      } else if ('artists' in parsed && !('durationSeconds' in parsed)) {
        result.albums.push(parsed as Album)
      } else if ('author' in parsed) {
        result.playlists.push(parsed as PlaylistSummary)
      } else if ('name' in parsed && !('title' in parsed)) {
        if (!result.artists.some((a) => a.id === (parsed as Artist).id)) {
          result.artists.push(parsed as Artist)
        }
      }
    }
  }

  return result
}
