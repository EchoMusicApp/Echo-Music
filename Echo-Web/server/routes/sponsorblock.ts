import { Hono } from 'hono'

export const sponsorblockRouter = new Hono()

export interface SponsorBlockSegment {
  category: string
  start: number
  end: number
  uuid?: string
}

sponsorblockRouter.get('/:videoId', async (c) => {
  const videoId = c.req.param('videoId')
  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return c.json({ error: 'Valid 11-character videoId is required' }, 400)
  }

  const categories = JSON.stringify(['music_offtopic', 'intro', 'outro', 'preview'])
  const url = `https://sponsor.ajay.app/api/skipSegments?videoID=${encodeURIComponent(videoId)}&categories=${encodeURIComponent(categories)}`

  try {
    const res = await fetch(url, {
      headers: {
        'User-Agent': 'Echo-Music-Web/1.0',
        Accept: 'application/json',
      },
    })

    if (res.status === 404) {
      return c.json([])
    }

    if (!res.ok) {
      return c.json([])
    }

    const data = (await res.json()) as Array<{
      category: string
      segment: [number, number]
      UUID?: string
    }>

    const segments: SponsorBlockSegment[] = (Array.isArray(data) ? data : [])
      .filter(
        (item) =>
          Array.isArray(item.segment) &&
          item.segment.length === 2 &&
          item.segment[1] > item.segment[0],
      )
      .map((item) => ({
        category: item.category,
        start: item.segment[0],
        end: item.segment[1],
        ...(item.UUID ? { uuid: item.UUID } : {}),
      }))
      .sort((a, b) => a.start - b.start)

    return c.json(segments)
  } catch {
    return c.json([])
  }
})
