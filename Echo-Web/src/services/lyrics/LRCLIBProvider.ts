import type { LyricLine, SynchronizedLyrics } from '../../types/lyrics.ts'

export interface LRCLIBResponse {
  id?: number
  trackName?: string
  artistName?: string
  albumName?: string
  duration?: number
  instrumental?: boolean
  plainLyrics?: string
  syncedLyrics?: string
}

export function parseLRC(lrcContent: string): LyricLine[] {
  if (!lrcContent || typeof lrcContent !== 'string') {
    return []
  }

  const lines = lrcContent.split(/\r?\n/)
  const parsedLines: LyricLine[] = []
  const timeRegex = /\[(\d{1,2}):(\d{1,2})(?:\.(\d{2,3}))?\]/g

  for (const rawLine of lines) {
    const trimmed = rawLine.trim()
    if (!trimmed) continue

    // Extract all timestamps in this line
    const timestamps: number[] = []
    let match: RegExpExecArray | null
    timeRegex.lastIndex = 0

    while ((match = timeRegex.exec(trimmed)) !== null) {
      const minutes = parseInt(match[1] || '0', 10)
      const seconds = parseInt(match[2] || '0', 10)
      const fractionStr = match[3] || '0'
      const fractionMs =
        fractionStr.length === 2 ? parseInt(fractionStr, 10) * 10 : parseInt(fractionStr, 10)

      const timeMs = minutes * 60 * 1000 + seconds * 1000 + fractionMs
      timestamps.push(timeMs)
    }

    // Strip timestamps to obtain lyrics text
    const text = trimmed.replace(timeRegex, '').trim()

    // Skip empty lines or header tags like [ar:Artist], [ti:Title]
    if (timestamps.length === 0) continue

    for (const timeMs of timestamps) {
      parsedLines.push({
        startTimeMs: timeMs,
        text,
      })
    }
  }

  // Sort chronologically
  parsedLines.sort((a, b) => a.startTimeMs - b.startTimeMs)

  // Deduce endTimeMs for each line based on start time of the next line
  for (let i = 0; i < parsedLines.length; i++) {
    const current = parsedLines[i]
    if (!current) continue
    const next = parsedLines[i + 1]
    if (next) {
      current.endTimeMs = next.startTimeMs
    }
  }

  return parsedLines
}

export class LRCLIBProvider {
  private static readonly BASE_URL = 'https://lrclib.net/api/get'

  public static async fetchLyrics(
    title: string,
    artist: string,
    album?: string,
    durationSeconds?: number,
    signal?: AbortSignal,
    preferRomanized?: boolean,
  ): Promise<SynchronizedLyrics | null> {
    try {
      if (preferRomanized) {
        try {
          const searchUrl = new URL('https://lrclib.net/api/search')
          searchUrl.searchParams.set('q', `${title} ${artist} romanized`)
          const searchRes = await fetch(searchUrl.toString(), {
            ...(signal ? { signal } : {}),
            headers: {
              'User-Agent': 'Echo-Music/1.0.0 (https://github.com/echo-music)',
            },
          })
          if (searchRes.ok) {
            const results = (await searchRes.json()) as LRCLIBResponse[]
            const match = results.find(
              (r) =>
                Boolean(r.syncedLyrics) &&
                (r.trackName?.toLowerCase().includes('roman') ||
                  r.syncedLyrics?.toLowerCase().includes('roman')),
            )
            if (match?.syncedLyrics) {
              const lines = parseLRC(match.syncedLyrics)
              if (lines.length > 0) {
                return {
                  provider: 'LRCLIB',
                  isSynced: true,
                  isWordSynced: false,
                  lines,
                }
              }
            }
          }
        } catch {
          // Fall through to exact fetch
        }
      }

      const url = new URL(this.BASE_URL)
      url.searchParams.set('track_name', title)
      url.searchParams.set('artist_name', artist)
      if (album) {
        url.searchParams.set('album_name', album)
      }
      if (durationSeconds && durationSeconds > 0) {
        url.searchParams.set('duration', Math.round(durationSeconds).toString())
      }

      const res = await fetch(url.toString(), {
        ...(signal ? { signal } : {}),
        headers: {
          'User-Agent': 'Echo-Music/1.0.0 (https://github.com/echo-music)',
        },
      })

      if (!res.ok) {
        return null
      }

      const data: LRCLIBResponse = await res.json()

      if (data.syncedLyrics && data.syncedLyrics.trim().length > 0) {
        const lines = parseLRC(data.syncedLyrics)
        if (lines.length > 0) {
          return {
            provider: 'LRCLIB',
            isSynced: true,
            isWordSynced: false,
            lines,
          }
        }
      }

      if (data.plainLyrics && data.plainLyrics.trim().length > 0) {
        const plainLines: LyricLine[] = data.plainLyrics
          .split(/\r?\n/)
          .map((line) => line.trim())
          .filter((line) => line.length > 0)
          .map((text, idx) => ({
            startTimeMs: idx * 4000,
            text,
          }))

        return {
          provider: 'LRCLIB',
          isSynced: false,
          isWordSynced: false,
          lines: plainLines,
        }
      }

      return null
    } catch {
      return null
    }
  }
}
