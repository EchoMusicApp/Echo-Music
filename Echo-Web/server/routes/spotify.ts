import { Hono } from 'hono'

export const spotifyRouter = new Hono()

interface SpotifyTrackInfo {
  title: string
  artist: string
  album?: string | undefined
  durationMs?: number | undefined
  isrc?: string | undefined
}

interface SpotifyResolvedData {
  title: string
  description?: string | undefined
  coverUrl?: string | undefined
  tracks: SpotifyTrackInfo[]
}

spotifyRouter.get('/resolve', async (c) => {
  const urlParam = c.req.query('url')
  if (!urlParam) {
    return c.json({ error: 'Missing url parameter' }, 400)
  }

  try {
    const parsedUrl = new URL(urlParam)
    if (!parsedUrl.hostname.includes('spotify.com')) {
      return c.json({ error: 'Not a valid Spotify URL' }, 400)
    }

    // Determine type: playlist, album, track
    const pathParts = parsedUrl.pathname.split('/').filter(Boolean)
    const type = pathParts[0] // 'playlist', 'album', 'track'
    const id = pathParts[1]

    if (!type || !id) {
      return c.json({ error: 'Could not parse Spotify entity ID' }, 400)
    }

    // 1. Fetch Spotify oEmbed for title and thumbnail
    let oEmbedTitle = 'Imported Spotify Playlist'
    let oEmbedThumbnail: string | undefined
    try {
      const oembedRes = await fetch(
        `https://open.spotify.com/oembed?url=${encodeURIComponent(urlParam)}`,
        { headers: { 'User-Agent': 'Mozilla/5.0 EchoMusicWeb/1.0' } },
      )
      if (oembedRes.ok) {
        const oembedData = (await oembedRes.json()) as { title?: string; thumbnail_url?: string }
        if (oembedData.title) oEmbedTitle = oembedData.title
        if (oembedData.thumbnail_url) oEmbedThumbnail = oembedData.thumbnail_url
      }
    } catch {
      // Continue even if oembed fails
    }

    // 2. Fetch Spotify Embed HTML page which contains embedded JSON data (window.__initial_state__ or __NEXT_DATA__)
    const embedUrl = `https://open.spotify.com/embed/${type}/${id}`
    const embedRes = await fetch(embedUrl, {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
      },
    })

    if (!embedRes.ok) {
      return c.json(
        {
          error: `Spotify returned HTTP ${embedRes.status}`,
          title: oEmbedTitle,
          coverUrl: oEmbedThumbnail,
          tracks: [],
        },
        502,
      )
    }

    const html = await embedRes.text()
    const tracks: SpotifyTrackInfo[] = []

    // Look for JSON payload in HTML
    const nextDataMatch = html.match(
      /<script id="__NEXT_DATA__" type="application\/json">([\s\S]*?)<\/script>/,
    )
    const initialStateMatch = html.match(
      /<script id="initial-state" type="text\/plain">([\s\S]*?)<\/script>/,
    )

    if (nextDataMatch && nextDataMatch[1]) {
      try {
        const parsed = JSON.parse(nextDataMatch[1])
        const entityData =
          parsed?.props?.pageProps?.state?.data?.entity || parsed?.props?.pageProps?.entity
        if (entityData?.trackList && Array.isArray(entityData.trackList)) {
          for (const item of entityData.trackList) {
            tracks.push({
              title: item.title || item.name || '',
              artist: item.subtitle || item.artists?.[0]?.name || '',
              durationMs: item.duration || 0,
            })
          }
        }
      } catch (err) {
        console.warn('Could not parse __NEXT_DATA__ from Spotify embed:', err)
      }
    } else if (initialStateMatch && initialStateMatch[1]) {
      try {
        const decoded = Buffer.from(initialStateMatch[1], 'base64').toString('utf8')
        const parsed = JSON.parse(decoded)
        const entity = parsed?.data?.entity
        if (entity?.trackList && Array.isArray(entity.trackList)) {
          for (const item of entity.trackList) {
            tracks.push({
              title: item.title || item.name || '',
              artist: item.subtitle || item.artists?.[0]?.name || '',
              durationMs: item.duration || 0,
            })
          }
        }
      } catch (err) {
        console.warn('Could not parse initial-state from Spotify embed:', err)
      }
    }

    // If script tags didn't match, scrape track items from HTML structure
    if (tracks.length === 0) {
      // Regex extraction of track name and artists from embed markup
      const trackRegex =
        /class="track-name[^>]*>([^<]+)<\/[^>]+>[\s\S]*?class="artist-name[^>]*>([^<]+)<\//g
      let match: RegExpExecArray | null
      while ((match = trackRegex.exec(html)) !== null) {
        const t = match[1]?.trim()
        const a = match[2]?.trim()
        if (t && a) {
          tracks.push({ title: t, artist: a })
        }
      }
    }

    const result: SpotifyResolvedData = {
      title: oEmbedTitle,
      coverUrl: oEmbedThumbnail,
      tracks,
    }

    return c.json(result)
  } catch (err) {
    return c.json(
      {
        error: 'Failed to resolve Spotify URL',
        message: err instanceof Error ? err.message : String(err),
      },
      500,
    )
  }
})
