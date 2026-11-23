import { Hono } from 'hono'

export const musicDnaRouter = new Hono()

interface MusicDnaResult {
  trackTitle: string
  artistName: string
  genius?:
    | {
        songId?: number | undefined
        description?: string | undefined
        releaseDate?: string | undefined
        producer?: string | undefined
        writer?: string | undefined
        annotationHighlights?: string[] | undefined
      }
    | undefined
  wikipedia?:
    | {
        title: string
        extract: string
        thumbnailUrl?: string | undefined
        pageUrl?: string | undefined
      }
    | undefined
  artistWikipedia?:
    | {
        title: string
        extract: string
        thumbnailUrl?: string | undefined
        pageUrl?: string | undefined
      }
    | undefined
}

musicDnaRouter.get('/', async (c) => {
  const title = c.req.query('title') || ''
  const artist = c.req.query('artist') || ''

  if (!title) {
    return c.json({ error: 'Missing title query parameter' }, 400)
  }

  const result: MusicDnaResult = {
    trackTitle: title,
    artistName: artist,
  }

  // 1. Wikipedia Summary for Track & Artist in parallel
  const cleanTitle = title.replace(/\s*\(.*?\)\s*/g, '').trim()
  const cleanArtist = artist.replace(/\s*\(.*?\)\s*/g, '').trim()

  const wikiTrackPromise = fetch(
    `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(cleanTitle + ' (song)')}`,
    { headers: { 'User-Agent': 'EchoMusicWeb/1.0 (https://echomusic.app)' } },
  )
    .then(async (r) => {
      if (r.ok) {
        const d = (await r.json()) as {
          title: string
          extract: string
          thumbnail?: { source: string }
          content_urls?: { desktop?: { page?: string } }
        }
        if (d.extract) {
          result.wikipedia = {
            title: d.title,
            extract: d.extract,
            thumbnailUrl: d.thumbnail?.source,
            pageUrl: d.content_urls?.desktop?.page,
          }
        }
      } else {
        // Try without "(song)"
        const alt = await fetch(
          `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(cleanTitle)}`,
          { headers: { 'User-Agent': 'EchoMusicWeb/1.0' } },
        )
        if (alt.ok) {
          const d2 = (await alt.json()) as {
            title: string
            extract: string
            thumbnail?: { source: string }
            content_urls?: { desktop?: { page?: string } }
          }
          if (d2.extract) {
            result.wikipedia = {
              title: d2.title,
              extract: d2.extract,
              thumbnailUrl: d2.thumbnail?.source,
              pageUrl: d2.content_urls?.desktop?.page,
            }
          }
        }
      }
    })
    .catch(() => {})

  const wikiArtistPromise = cleanArtist
    ? fetch(
        `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(cleanArtist)}`,
        { headers: { 'User-Agent': 'EchoMusicWeb/1.0' } },
      )
        .then(async (r) => {
          if (r.ok) {
            const d = (await r.json()) as {
              title: string
              extract: string
              thumbnail?: { source: string }
              content_urls?: { desktop?: { page?: string } }
            }
            if (d.extract) {
              result.artistWikipedia = {
                title: d.title,
                extract: d.extract,
                thumbnailUrl: d.thumbnail?.source,
                pageUrl: d.content_urls?.desktop?.page,
              }
            }
          }
        })
        .catch(() => {})
    : Promise.resolve()

  // 2. Query Genius public search for annotations & song story
  const geniusPromise = fetch(
    `https://genius.com/api/search/multi?q=${encodeURIComponent(`${cleanTitle} ${cleanArtist}`)}`,
    {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      },
    },
  )
    .then(async (r) => {
      if (r.ok) {
        const d = (await r.json()) as {
          response?: {
            sections?: Array<{
              type?: string
              hits?: Array<{
                result?: {
                  id?: number
                  title?: string
                  primary_artist?: { name?: string }
                  release_date_for_display?: string
                  description_preview?: string
                }
              }>
            }>
          }
        }
        const songSection = d.response?.sections?.find(
          (s) => s.type === 'song' || s.type === 'top_hit',
        )
        const hit = songSection?.hits?.[0]?.result
        if (hit) {
          result.genius = {
            songId: hit.id,
            description: hit.description_preview || undefined,
            releaseDate: hit.release_date_for_display || undefined,
            annotationHighlights: hit.description_preview ? [hit.description_preview] : undefined,
          }
        }
      }
    })
    .catch(() => {})

  await Promise.allSettled([wikiTrackPromise, wikiArtistPromise, geniusPromise])

  return c.json(result)
})
