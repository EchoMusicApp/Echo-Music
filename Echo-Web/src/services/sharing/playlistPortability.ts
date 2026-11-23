import type { Track, Playlist } from '../../types/domain'

/**
 * Generate cross-platform Odesli / Song.link URL
 */
export function getSongLinkUrl(track: Track): string {
  const videoId = track.videoId || track.id
  if (videoId && !videoId.startsWith('local_')) {
    return `https://song.link/y/${encodeURIComponent(videoId)}`
  }
  return `https://song.link/s/${encodeURIComponent(`${track.title} ${track.artistName}`)}`
}

/**
 * Trigger native share or copy link
 */
export async function shareTrack(track: Track): Promise<'shared' | 'copied'> {
  const songLink = getSongLinkUrl(track)
  const shareData = {
    title: track.title,
    text: `Listen to "${track.title}" by ${track.artistName} on Echo`,
    url: songLink,
  }

  if (
    typeof navigator !== 'undefined' &&
    navigator.share &&
    navigator.canShare &&
    navigator.canShare(shareData)
  ) {
    try {
      await navigator.share(shareData)
      return 'shared'
    } catch (err) {
      if ((err as Error).name === 'AbortError') return 'shared'
    }
  }

  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    await navigator.clipboard.writeText(songLink)
    return 'copied'
  }

  return 'copied'
}

/**
 * Export playlist to standard Extended M3U (#EXTM3U) format
 */
export function exportPlaylistToM3U(playlist: Playlist, tracks: Track[]): string {
  const lines: string[] = ['#EXTM3U', `#PLAYLIST:${playlist.title}`]
  for (const track of tracks) {
    const duration = track.durationSeconds || 0
    lines.push(`#EXTINF:${duration},${track.artistName} - ${track.title}`)
    const url = track.videoId
      ? `https://music.youtube.com/watch?v=${track.videoId}`
      : `track://${track.id}`
    lines.push(url)
  }
  return lines.join('\n')
}

/**
 * Parse an uploaded M3U / M3U8 string
 */
export function parseM3UPlaylist(content: string): Array<{
  title: string
  artist: string
  duration?: number | undefined
  url?: string | undefined
}> {
  const lines = content.split(/\r?\n/)
  const result: Array<{
    title: string
    artist: string
    duration?: number | undefined
    url?: string | undefined
  }> = []

  let currentTitle = ''
  let currentArtist = ''
  let currentDuration: number | undefined

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue

    if (trimmed.startsWith('#EXTINF:')) {
      const match = trimmed.match(/#EXTINF:(-?\d+),(.*)/)
      if (match) {
        currentDuration = parseInt(match[1] || '0', 10)
        const namePart = (match[2] || '').trim()
        const dashIdx = namePart.indexOf(' - ')
        if (dashIdx !== -1) {
          currentArtist = namePart.substring(0, dashIdx).trim()
          currentTitle = namePart.substring(dashIdx + 3).trim()
        } else {
          currentTitle = namePart
          currentArtist = 'Unknown'
        }
      }
    } else if (!trimmed.startsWith('#')) {
      if (currentTitle) {
        result.push({
          title: currentTitle,
          artist: currentArtist,
          duration: currentDuration,
          url: trimmed,
        })
        currentTitle = ''
        currentArtist = ''
        currentDuration = undefined
      }
    }
  }

  return result
}

/**
 * Parse a CSV playlist (columns: Title, Artist, Album, Duration)
 */
export function parseCSVPlaylist(
  csvContent: string,
): Array<{ title: string; artist: string; album?: string | undefined }> {
  const lines = csvContent.split(/\r?\n/).filter(Boolean)
  if (lines.length <= 1) return []

  const header = lines[0]!
    .toLowerCase()
    .split(',')
    .map((h) => h.trim().replace(/^"|"$/g, ''))
  const titleIdx = header.findIndex(
    (h) => h.includes('title') || h.includes('track') || h.includes('name'),
  )
  const artistIdx = header.findIndex((h) => h.includes('artist'))
  const albumIdx = header.findIndex((h) => h.includes('album'))

  const result: Array<{ title: string; artist: string; album?: string | undefined }> = []
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i]!.split(',').map((c) => c.trim().replace(/^"|"$/g, ''))
    const title = titleIdx !== -1 ? cols[titleIdx] || '' : cols[0] || ''
    const artist = artistIdx !== -1 ? cols[artistIdx] || 'Unknown' : cols[1] || 'Unknown'
    const album = albumIdx !== -1 ? cols[albumIdx] : undefined
    if (title) {
      result.push({ title, artist, album })
    }
  }

  return result
}

/**
 * Download a file in the browser (for M3U, JSON, or exported audio)
 */
export function triggerFileDownload(
  content: Blob | string,
  fileName: string,
  mimeType = 'text/plain',
): void {
  const blob = typeof content === 'string' ? new Blob([content], { type: mimeType }) : content
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
