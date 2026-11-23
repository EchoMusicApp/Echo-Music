import { Hono } from 'hono'
import { getInnerTubeHeaders } from '../utils/headers.ts'
import { buildClientContext, fetchVisitorData, type ClientContextType } from '../utils/potoken.ts'

export const innertubeRouter = new Hono()

const CLIENT_ROTATION_LIST: ClientContextType[] = ['ANDROID_MUSIC', 'IOS', 'WEB_REMIX']

export async function forwardInnerTubeRequest(
  endpoint: string,
  bodyData: Record<string, unknown>,
  clientType: ClientContextType = 'WEB_REMIX',
  visitorHeaderOverride?: string,
): Promise<Response> {
  const url = `https://music.youtube.com/youtubei/v1/${endpoint}`
  const visitorData = visitorHeaderOverride || (await fetchVisitorData())

  const candidates: ClientContextType[] = [
    clientType,
    ...CLIENT_ROTATION_LIST.filter((c) => c !== clientType),
  ]

  let lastResponse: Response | null = null

  for (const currentClient of candidates) {
    const defaultContext = buildClientContext(currentClient, visitorData)

    const incomingContext =
      typeof bodyData['context'] === 'object' && bodyData['context'] !== null
        ? (bodyData['context'] as Record<string, unknown>)
        : {}
    const incomingClient =
      typeof incomingContext['client'] === 'object' && incomingContext['client'] !== null
        ? (incomingContext['client'] as Record<string, unknown>)
        : {}

    const mergedBody: Record<string, unknown> = {
      ...bodyData,
      context: {
        ...defaultContext,
        ...incomingContext,
        client: {
          ...(defaultContext['client'] as Record<string, unknown>),
          ...incomingClient,
        },
      },
    }

    const headers = getInnerTubeHeaders({
      clientType: currentClient,
      visitorData,
    })

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(mergedBody),
      })

      lastResponse = res
      if (res.ok || (res.status !== 403 && res.status !== 429)) {
        return res
      }
    } catch {
      // Rotate on network or upstream error
    }
  }

  if (lastResponse) {
    return lastResponse
  }

  throw new Error(`InnerTube request to ${endpoint} failed across all rotated client definitions`)
}

// 1. POST /api/innertube/browse
innertubeRouter.post('/browse', async (c) => {
  let bodyData: Record<string, unknown> = {}
  try {
    bodyData = (await c.req.json()) as Record<string, unknown>
  } catch {
    bodyData = {}
  }

  const visitorHeader = c.req.header('x-youtube-visitor-data')

  try {
    const upstreamRes = await forwardInnerTubeRequest(
      'browse',
      bodyData,
      'WEB_REMIX',
      visitorHeader,
    )
    const data = (await upstreamRes.json()) as Record<string, unknown>
    return c.json(data, upstreamRes.status as 200)
  } catch (err: unknown) {
    return c.json(
      {
        error: 'Upstream InnerTube browse error',
        message: err instanceof Error ? err.message : String(err),
      },
      502,
    )
  }
})

// 2. POST /api/innertube/search
innertubeRouter.post('/search', async (c) => {
  let bodyData: Record<string, unknown> = {}
  try {
    bodyData = (await c.req.json()) as Record<string, unknown>
  } catch {
    bodyData = {}
  }

  const visitorHeader = c.req.header('x-youtube-visitor-data')

  try {
    const upstreamRes = await forwardInnerTubeRequest(
      'search',
      bodyData,
      'WEB_REMIX',
      visitorHeader,
    )
    const data = (await upstreamRes.json()) as Record<string, unknown>
    return c.json(data, upstreamRes.status as 200)
  } catch (err: unknown) {
    return c.json(
      {
        error: 'Upstream InnerTube search error',
        message: err instanceof Error ? err.message : String(err),
      },
      502,
    )
  }
})

// 3. GET /api/innertube/suggestions?q=:query
innertubeRouter.get('/suggestions', async (c) => {
  const query = c.req.query('q') || c.req.query('input') || ''
  if (!query.trim()) {
    return c.json([])
  }

  try {
    const upstreamRes = await forwardInnerTubeRequest(
      'music/get_search_suggestions',
      { input: query.trim() },
      'WEB_REMIX',
    )

    if (upstreamRes.ok) {
      const data = (await upstreamRes.json()) as {
        contents?: Array<{
          searchSuggestionsSectionRenderer?: {
            contents?: Array<{
              searchSuggestionRenderer?: {
                suggestion?: {
                  runs?: Array<{ text: string }>
                }
                navigationEndpoint?: {
                  searchEndpoint?: {
                    query?: string
                  }
                }
              }
            }>
          }
        }>
      }

      const suggestions: string[] = []
      const sections = data.contents || []

      for (const section of sections) {
        const items = section.searchSuggestionsSectionRenderer?.contents || []
        for (const item of items) {
          const renderer = item.searchSuggestionRenderer
          if (!renderer) continue
          const text =
            renderer.navigationEndpoint?.searchEndpoint?.query ||
            renderer.suggestion?.runs?.map((r) => r.text).join('')
          if (text && !suggestions.includes(text)) {
            suggestions.push(text)
          }
        }
      }

      if (suggestions.length > 0) {
        return c.json(suggestions)
      }
    }
  } catch {
    // InnerTube suggestion failed, proceed to fallback
  }

  // Fallback to Google YouTube suggest
  try {
    const fallbackUrl = `https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&hl=en&gl=US&ds=yt&q=${encodeURIComponent(query.trim())}`
    const fallbackRes = await fetch(fallbackUrl)
    if (fallbackRes.ok) {
      const text = await fallbackRes.text()
      const match = /window\.google\.ac\.h\(\[.*?,(\[.*?\])\s*,\{/s.exec(text)
      if (match?.[1]) {
        const rawList = JSON.parse(match[1]) as Array<[string, ...unknown[]]>
        const suggestions = rawList
          .map((item) => (Array.isArray(item) ? item[0] : null))
          .filter((item): item is string => typeof item === 'string')
        return c.json(suggestions)
      }
    }
  } catch {
    // Non-fatal
  }

  return c.json([])
})

// 4. POST /api/innertube/next
innertubeRouter.post('/next', async (c) => {
  let bodyData: Record<string, unknown> = {}
  try {
    bodyData = (await c.req.json()) as Record<string, unknown>
  } catch {
    bodyData = {}
  }

  const visitorHeader = c.req.header('x-youtube-visitor-data')

  try {
    const upstreamRes = await forwardInnerTubeRequest('next', bodyData, 'WEB_REMIX', visitorHeader)
    const data = (await upstreamRes.json()) as Record<string, unknown>
    return c.json(data, upstreamRes.status as 200)
  } catch (err: unknown) {
    return c.json(
      {
        error: 'Upstream InnerTube next error',
        message: err instanceof Error ? err.message : String(err),
      },
      502,
    )
  }
})

// 5. Allowlisted passthrough: only InnerTube endpoints the client actually uses may be
// forwarded. An open relay would let any third-party site proxy arbitrary InnerTube calls
// through this server.
const ALLOWED_ENDPOINTS = new Set(['browse', 'search', 'next', 'music/get_search_suggestions'])

innertubeRouter.post('/:endpoint', async (c) => {
  const endpoint = c.req.param('endpoint')
  if (!endpoint || !ALLOWED_ENDPOINTS.has(endpoint)) {
    return c.json({ error: 'Endpoint not allowed' }, 403)
  }

  let bodyData: Record<string, unknown> = {}
  try {
    bodyData = (await c.req.json()) as Record<string, unknown>
  } catch {
    bodyData = {}
  }

  const visitorHeader = c.req.header('x-youtube-visitor-data')

  try {
    const upstreamRes = await forwardInnerTubeRequest(
      encodeURIComponent(endpoint),
      bodyData,
      'WEB_REMIX',
      visitorHeader,
    )
    const data = (await upstreamRes.json()) as Record<string, unknown>
    return c.json(data, upstreamRes.status as 200)
  } catch (err: unknown) {
    return c.json(
      {
        error: 'Upstream InnerTube error',
        message: err instanceof Error ? err.message : String(err),
      },
      502,
    )
  }
})
