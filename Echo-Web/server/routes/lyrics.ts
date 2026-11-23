import { Hono } from 'hono'
import { Buffer } from 'node:buffer'
import type { LyricLine, SynchronizedLyrics } from '../../src/types/lyrics.ts'

export const lyricsRouter = new Hono()

export function parseLRC(lrcContent: string): LyricLine[] {
  const lines = lrcContent.split(/\r?\n/)
  const parsedLines: LyricLine[] = []
  const timeRegex = /\[(\d{1,2}):(\d{1,2})(?:\.(\d{2,3}))?\]/g

  for (const rawLine of lines) {
    const trimmed = rawLine.trim()
    if (!trimmed) continue

    const timestamps: number[] = []
    let match: RegExpExecArray | null
    timeRegex.lastIndex = 0

    while ((match = timeRegex.exec(trimmed)) !== null) {
      const minutes = parseInt(match[1] || '0', 10)
      const seconds = parseInt(match[2] || '0', 10)
      const fractionStr = match[3] || '0'
      const fractionMs =
        fractionStr.length === 2 ? parseInt(fractionStr, 10) * 10 : parseInt(fractionStr, 10)

      timestamps.push(minutes * 60 * 1000 + seconds * 1000 + fractionMs)
    }

    const text = trimmed.replace(timeRegex, '').trim()
    if (timestamps.length === 0 || !text) continue

    for (const timeMs of timestamps) {
      parsedLines.push({
        startTimeMs: timeMs,
        text,
      })
    }
  }

  parsedLines.sort((a, b) => a.startTimeMs - b.startTimeMs)

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

lyricsRouter.get('/kugou', async (c) => {
  const title = c.req.query('title') || ''
  const artist = c.req.query('artist') || ''
  const album = c.req.query('album') || ''
  const durationSec = parseInt(c.req.query('duration') || '0', 10)
  const durationMs = durationSec > 0 ? durationSec * 1000 : 0

  if (!title && !artist) {
    return c.json({ error: 'Missing title or artist' }, 400)
  }

  try {
    const searchQuery = album ? `${title} ${artist} ${album}`.trim() : `${title} ${artist}`.trim()
    let songHash = ''
    let songDuration = 0

    // Step 1: Search song to find candidate hash
    try {
      const searchUrl = new URL('https://mobileservice.kugou.com/api/v3/search/song')
      searchUrl.searchParams.set('version', '9108')
      searchUrl.searchParams.set('plat', '0')
      searchUrl.searchParams.set('pagesize', '8')
      searchUrl.searchParams.set('showtype', '0')
      searchUrl.searchParams.set('keyword', searchQuery)

      const searchRes = await fetch(searchUrl.toString(), {
        headers: { 'User-Agent': 'KuGouMusic/9108' },
      })
      if (searchRes.ok) {
        const searchJson = (await searchRes.json()) as {
          data?: { info?: Array<{ hash?: string; duration?: number }> }
        }
        const infoList = searchJson.data?.info || []
        if (infoList.length > 0) {
          const matched =
            durationSec > 0
              ? infoList.find((s) => Math.abs((s.duration || 0) - durationSec) <= 8) || infoList[0]
              : infoList[0]
          songHash = matched?.hash || ''
          songDuration = (matched?.duration || 0) * 1000
        }
      }
    } catch {
      // Ignore search error and continue to lyrics search
    }

    // Step 2: Search lyrics candidates
    let candidateId = ''
    let accessKey = ''

    if (songHash) {
      try {
        const hashLyricsUrl = `https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&hash=${songHash}`
        const hRes = await fetch(hashLyricsUrl, {
          headers: { 'User-Agent': 'KuGouMusic/9108' },
        })
        if (hRes.ok) {
          const hData = (await hRes.json()) as {
            candidates?: Array<{ id: string | number; accesskey: string }>
          }
          const cand = hData.candidates?.[0]
          if (cand) {
            candidateId = String(cand.id)
            accessKey = cand.accesskey
          }
        }
      } catch {
        // Fall back to keyword search
      }
    }

    if (!candidateId || !accessKey) {
      const keywordLyricsUrl = new URL('https://lyrics.kugou.com/search')
      keywordLyricsUrl.searchParams.set('ver', '1')
      keywordLyricsUrl.searchParams.set('man', 'yes')
      keywordLyricsUrl.searchParams.set('client', 'pc')
      keywordLyricsUrl.searchParams.set('keyword', searchQuery)
      if (durationMs > 0 || songDuration > 0) {
        keywordLyricsUrl.searchParams.set(
          'duration',
          String(durationMs > 0 ? durationMs : songDuration),
        )
      }

      const kwRes = await fetch(keywordLyricsUrl.toString(), {
        headers: { 'User-Agent': 'KuGouMusic/9108' },
      })
      if (kwRes.ok) {
        const kwData = (await kwRes.json()) as {
          candidates?: Array<{ id: string | number; accesskey: string }>
        }
        const cand = kwData.candidates?.[0]
        if (cand) {
          candidateId = String(cand.id)
          accessKey = cand.accesskey
        }
      }
    }

    if (!candidateId || !accessKey) {
      return c.json({ error: 'No KuGou lyrics candidate found' }, 404)
    }

    const lrcUrl = `https://lyrics.kugou.com/download?ver=1&client=pc&id=${encodeURIComponent(candidateId)}&accesskey=${encodeURIComponent(accessKey)}&fmt=lrc&charset=utf8`
    const lrcRes = await fetch(lrcUrl, {
      headers: { 'User-Agent': 'KuGouMusic/9108' },
    })

    if (!lrcRes.ok) {
      return c.json({ error: 'Failed to download KuGou lyrics' }, 404)
    }

    const lrcData = (await lrcRes.json()) as { content?: string }
    if (!lrcData.content) {
      return c.json({ error: 'KuGou lyrics content is empty' }, 404)
    }

    const lrcText = Buffer.from(lrcData.content, 'base64').toString('utf-8')
    const parsedLines = parseLRC(lrcText)

    if (parsedLines.length === 0) {
      return c.json({ error: 'Failed to parse KuGou lyrics' }, 404)
    }

    const payload: SynchronizedLyrics = {
      provider: 'KUGOU',
      isSynced: true,
      isWordSynced: false,
      lines: parsedLines,
    }

    return c.json({ success: true, lyrics: payload })
  } catch (error) {
    return c.json(
      {
        error: 'KuGou proxy failure',
        message: error instanceof Error ? error.message : String(error),
      },
      500,
    )
  }
})

// InnerTube Subtitles / Captions fallback
lyricsRouter.get('/youtube', async (c) => {
  const videoId = c.req.query('videoId') || ''
  if (!videoId) {
    return c.json({ error: 'Missing videoId' }, 400)
  }

  try {
    const watchUrl = `https://www.youtube.com/watch?v=${videoId}`
    const htmlRes = await fetch(watchUrl, {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
      },
    })
    const html = await htmlRes.text()

    const playerMatch = /ytInitialPlayerResponse\s*=\s*({.+?});/s.exec(html)
    if (!playerMatch || !playerMatch[1]) {
      return c.json({ error: 'Could not extract player response' }, 404)
    }

    const playerResponse = JSON.parse(playerMatch[1]) as {
      captions?: {
        playerCaptionsTracklistRenderer?: {
          captionTracks?: Array<{ baseUrl: string; languageCode?: string }>
        }
      }
    }

    const tracks = playerResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks || []
    if (tracks.length === 0 || !tracks[0]?.baseUrl) {
      return c.json({ error: 'No captions found' }, 404)
    }

    // Fetch caption XML
    const capRes = await fetch(tracks[0].baseUrl)
    const capXml = await capRes.text()

    // Parse transcript <text start="1.5" dur="3.0">Hello</text>
    const textRegex = /<text\b[^>]*start="([^"]+)"(?:\s+dur="([^"]+)")?[^>]*>([\s\S]*?)<\/text>/gi
    let match: RegExpExecArray | null
    const lines: LyricLine[] = []

    while ((match = textRegex.exec(capXml)) !== null) {
      const startSec = parseFloat(match[1] || '0')
      const durSec = parseFloat(match[2] || '3')
      const rawText = (match[3] || '')
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&#39;/g, "'")
        .replace(/&quot;/g, '"')
        .trim()

      if (rawText) {
        lines.push({
          startTimeMs: Math.round(startSec * 1000),
          endTimeMs: Math.round((startSec + durSec) * 1000),
          text: rawText,
        })
      }
    }

    if (lines.length === 0) {
      return c.json({ error: 'No lines in transcript' }, 404)
    }

    const payload: SynchronizedLyrics = {
      provider: 'YOUTUBE',
      isSynced: true,
      isWordSynced: false,
      lines,
    }

    return c.json({ success: true, lyrics: payload })
  } catch (error) {
    return c.json(
      {
        error: 'YouTube captions fetch error',
        message: error instanceof Error ? error.message : String(error),
      },
      500,
    )
  }
})
