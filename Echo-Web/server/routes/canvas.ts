import { Hono } from 'hono'
import process from 'node:process'

export const canvasRouter = new Hono()

// Tokens come from env only. The Apple fallback JWT that used to be hard-coded here has been
// removed: it was committed to source and must be treated as compromised (rotate it).
const APPLE_TOKEN = process.env['APPLE_MUSIC_TOKEN'] || ''
const TIDAL_PUBLIC_TOKEN = process.env['TIDAL_TOKEN'] || ''

let cachedAppleToken: string | null = null
let tokenExpiry = 0

function isAllowedAppleDomain(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr)
    const host = parsed.hostname.toLowerCase()
    return (
      host === 'music.apple.com' ||
      host === 'beta.music.apple.com' ||
      host.endsWith('.apple.com') ||
      host.endsWith('.mzstatic.com') ||
      host.endsWith('.apple-dns.net')
    )
  } catch {
    return false
  }
}

function isAllowedTidalDomain(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr)
    const host = parsed.hostname.toLowerCase()
    return host === 'tidal.com' || host.endsWith('.tidal.com')
  } catch {
    return false
  }
}

async function getAppleMusicToken(): Promise<string> {
  if (cachedAppleToken && Date.now() < tokenExpiry) {
    return cachedAppleToken
  }

  try {
    const htmlRes = await fetch('https://beta.music.apple.com', {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
      },
    })
    const html = await htmlRes.text()
    const indexMatch = /src="(\/assets\/index-[^"]+\.js)"/.exec(html)

    if (indexMatch && indexMatch[1]) {
      const jsRes = await fetch(`https://beta.music.apple.com${indexMatch[1]}`)
      const js = await jsRes.text()
      const tokenMatch = /eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+/.exec(js)
      if (tokenMatch && tokenMatch[0]) {
        cachedAppleToken = tokenMatch[0]
        tokenExpiry = Date.now() + 1000 * 60 * 60 * 12
        return cachedAppleToken
      }
    }
  } catch {
    // Fall through to env token below
  }

  if (!APPLE_TOKEN) {
    throw new Error('APPLE_MUSIC_TOKEN env var is not set and scraping failed')
  }
  cachedAppleToken = APPLE_TOKEN
  tokenExpiry = Date.now() + 1000 * 60 * 60 * 6
  return cachedAppleToken
}

// 1. Apple Music Proxy: rewrites m3u8 playlists and proxies video TS chunks with domain validation
canvasRouter.get('/apple', async (c) => {
  const targetUrl = c.req.query('url')
  if (!targetUrl) {
    return c.text('Missing url parameter', 400)
  }

  if (!isAllowedAppleDomain(targetUrl)) {
    return c.text('Forbidden: target domain is not permitted', 403)
  }

  try {
    const rangeHeader = c.req.header('range')
    const headers: Record<string, string> = {
      Origin: 'https://music.apple.com',
      Referer: 'https://music.apple.com/',
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    }
    if (rangeHeader) {
      headers['Range'] = rangeHeader
    }

    const upstreamRes = await fetch(targetUrl, { headers })
    if (!upstreamRes.ok && upstreamRes.status !== 206) {
      return c.text(`Upstream error: ${upstreamRes.statusText}`, upstreamRes.status as 400)
    }

    const contentType = upstreamRes.headers.get('content-type') || ''
    const isM3U8 =
      targetUrl.includes('.m3u8') ||
      contentType.includes('mpegurl') ||
      contentType.includes('application/x-mpegURL')

    if (isM3U8) {
      const rawM3U8 = await upstreamRes.text()
      const baseUrl = new URL(targetUrl)

      // Rewrite each segment line in the m3u8 manifest to proxy through /api/canvas/apple?url=...
      const rewrittenLines = rawM3U8.split(/\r?\n/).map((line) => {
        const trimmed = line.trim()
        if (!trimmed) return line

        if (trimmed.startsWith('#')) {
          // Check for URI="..." attribute inside tags (e.g. #EXT-X-MAP:URI="init.mp4")
          return line.replace(/URI=["']([^"']+)["']/g, (_all, uri: string) => {
            const abs = new URL(uri, baseUrl).toString()
            return `URI="/api/canvas/apple?url=${encodeURIComponent(abs)}"`
          })
        }

        // Segment URL line
        const absUrl = new URL(trimmed, baseUrl).toString()
        return `/api/canvas/apple?url=${encodeURIComponent(absUrl)}`
      })

      const rewrittenContent = rewrittenLines.join('\n')

      return new Response(rewrittenContent, {
        status: 200,
        headers: {
          'Content-Type': 'application/vnd.apple.mpegurl',
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
          'Access-Control-Allow-Headers': '*',
          'Cache-Control': 'public, max-age=3600',
        },
      })
    }

    // Binary segment stream (TS, MP4, AAC)
    const resHeaders = new Headers()
    resHeaders.set('Access-Control-Allow-Origin', '*')
    resHeaders.set('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS')
    resHeaders.set('Access-Control-Allow-Headers', '*')
    resHeaders.set('Content-Type', contentType || 'video/mp2t')

    const contentLength = upstreamRes.headers.get('content-length')
    if (contentLength) resHeaders.set('Content-Length', contentLength)

    const contentRange = upstreamRes.headers.get('content-range')
    if (contentRange) resHeaders.set('Content-Range', contentRange)

    const acceptRanges = upstreamRes.headers.get('accept-ranges')
    if (acceptRanges) resHeaders.set('Accept-Ranges', acceptRanges)

    return new Response(upstreamRes.body, {
      status: upstreamRes.status,
      headers: resHeaders,
    })
  } catch (error) {
    return c.json(
      {
        error: 'Apple canvas proxy error',
        message: error instanceof Error ? error.message : String(error),
      },
      500,
    )
  }
})

// 2. Tidal Proxy / Video Resolver
canvasRouter.get('/tidal', async (c) => {
  const isrc = c.req.query('isrc')
  const videoUrl = c.req.query('url')

  if (videoUrl) {
    if (!isAllowedTidalDomain(videoUrl)) {
      return c.text('Forbidden: target domain is not permitted', 403)
    }

    try {
      const range = c.req.header('range')
      const headers: Record<string, string> = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
      }
      if (range) headers['Range'] = range

      const res = await fetch(videoUrl, { headers })
      const resHeaders = new Headers()
      resHeaders.set('Access-Control-Allow-Origin', '*')
      resHeaders.set('Content-Type', res.headers.get('content-type') || 'video/mp4')
      if (res.headers.get('content-length')) {
        resHeaders.set('Content-Length', res.headers.get('content-length')!)
      }
      if (res.headers.get('content-range')) {
        resHeaders.set('Content-Range', res.headers.get('content-range')!)
      }
      if (res.headers.get('accept-ranges')) {
        resHeaders.set('Accept-Ranges', res.headers.get('accept-ranges')!)
      }

      return new Response(res.body, {
        status: res.status,
        headers: resHeaders,
      })
    } catch (err) {
      return c.json({ error: String(err) }, 500)
    }
  }

  if (isrc) {
    try {
      const searchRes = await fetch(
        `https://api.tidal.com/v1/tracks?isrc=${encodeURIComponent(isrc)}&countryCode=US`,
        {
          headers: {
            'X-Tidal-Token': TIDAL_PUBLIC_TOKEN,
          },
        },
      )

      if (searchRes.ok) {
        const data = (await searchRes.json()) as {
          items?: Array<{
            album?: { videoCover?: string; title?: string }
            videoCover?: string
          }>
        }
        const item = data.items?.[0]
        const videoCover = item?.videoCover || item?.album?.videoCover

        if (videoCover) {
          const parts = videoCover.split('-')
          if (parts.length === 5) {
            const mp4Url = `https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4`
            return c.json({
              success: true,
              provider: 'TIDAL',
              videoUrl: mp4Url,
            })
          }
        }
      }
      return c.json({ error: 'No Tidal canvas found for ISRC' }, 404)
    } catch (err) {
      return c.json({ error: String(err) }, 500)
    }
  }

  return c.text('Provide either isrc or url', 400)
})

// 3. Central Canvas Video Resolver (Apple -> Tidal -> Artist Video)
canvasRouter.get('/resolve', async (c) => {
  const title = c.req.query('title') || ''
  const artist = c.req.query('artist') || ''
  const album = c.req.query('album') || ''
  const isrc = c.req.query('isrc') || ''

  if (!title && !artist && !isrc) {
    return c.json({ error: 'Missing metadata for canvas search' }, 400)
  }

  // 1. Try Tidal by ISRC if available
  if (isrc) {
    try {
      const tidalRes = await fetch(
        `https://api.tidal.com/v1/tracks?isrc=${encodeURIComponent(isrc)}&countryCode=US`,
        { headers: { 'X-Tidal-Token': TIDAL_PUBLIC_TOKEN } },
      )
      if (tidalRes.ok) {
        const tData = (await tidalRes.json()) as {
          items?: Array<{
            album?: { videoCover?: string }
            videoCover?: string
          }>
        }
        const item = tData.items?.[0]
        const cover = item?.videoCover || item?.album?.videoCover
        if (cover) {
          const parts = cover.split('-')
          if (parts.length === 5) {
            const rawMp4 = `https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4`
            return c.json({
              success: true,
              provider: 'TIDAL',
              videoUrl: rawMp4,
            })
          }
        }
      }
    } catch {
      // Continue to next provider
    }
  }

  // 2. Try Apple Music Motion Editorial Video
  try {
    const token = await getAppleMusicToken()
    const query = `${artist} ${title}`.trim()
    const ampUrl = `https://amp-api.music.apple.com/v1/catalog/us/search?term=${encodeURIComponent(query)}&types=songs,albums&limit=5&extend=editorialVideo`

    const ampRes = await fetch(ampUrl, {
      headers: {
        Authorization: `Bearer ${token}`,
        Origin: 'https://music.apple.com',
        Referer: 'https://music.apple.com/',
      },
    })

    if (ampRes.ok) {
      const ampData = (await ampRes.json()) as {
        results?: {
          albums?: {
            data?: Array<{
              attributes?: {
                editorialVideo?: {
                  motionDetailSquare?: { video?: string }
                  motionSquare?: { video?: string }
                  motionDetailTall?: { video?: string }
                }
              }
            }>
          }
          songs?: {
            data?: Array<{
              attributes?: {
                editorialVideo?: {
                  motionDetailSquare?: { video?: string }
                  motionSquare?: { video?: string }
                  motionDetailTall?: { video?: string }
                }
              }
            }>
          }
        }
      }

      const songData = ampData.results?.songs?.data || []
      const albumData = ampData.results?.albums?.data || []
      const allEntries = [...songData, ...albumData]

      for (const entry of allEntries) {
        const ev = entry.attributes?.editorialVideo
        const rawVideo =
          ev?.motionDetailSquare?.video || ev?.motionSquare?.video || ev?.motionDetailTall?.video

        if (rawVideo) {
          const proxiedUrl = `/api/canvas/apple?url=${encodeURIComponent(rawVideo)}`
          return c.json({
            success: true,
            provider: 'APPLE',
            videoUrl: proxiedUrl,
            rawUrl: rawVideo,
          })
        }
      }
    }
  } catch {
    // Continue to artist video fallback
  }

  // 3. Fallback to Artist Video Archive
  try {
    const artistArchiveUrl = new URL('https://artwork-archivetune.koiiverse.cloud/')
    artistArchiveUrl.searchParams.set('s', title)
    artistArchiveUrl.searchParams.set('a', artist)
    if (album) artistArchiveUrl.searchParams.set('al', album)

    const aRes = await fetch(artistArchiveUrl.toString())
    if (aRes.ok) {
      const aData = (await aRes.json()) as { videoUrl?: string; url?: string }
      const url = aData.videoUrl || aData.url
      if (url) {
        return c.json({
          success: true,
          provider: 'ARTIST_VIDEO',
          videoUrl: url,
        })
      }
    }
  } catch {
    // No video canvas found
  }

  return c.json({ success: false, message: 'No canvas video available for track' }, 404)
})
