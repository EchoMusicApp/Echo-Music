import { Hono } from 'hono'
import process from 'node:process'

export const recognitionRouter = new Hono()

// Echo Find records mic audio. When RECOGNITION_PROVIDER_URL is configured, the recording
// is forwarded to the upstream fingerprinting service; otherwise the endpoint reports honestly
// that recognition is unconfigured rather than returning fabricated hits.
recognitionRouter.post('/identify', async (c) => {
  const providerUrl = process.env['RECOGNITION_PROVIDER_URL']
  if (providerUrl) {
    try {
      const contentType = c.req.header('content-type') || ''
      const body = await c.req.arrayBuffer()
      const upstream = await fetch(providerUrl, {
        method: 'POST',
        headers: {
          'Content-Type': contentType,
        },
        body,
      })
      const data = (await upstream.json()) as Record<string, unknown>
      return c.json(data, upstream.status as 200)
    } catch (err) {
      return c.json(
        {
          matched: false,
          error: err instanceof Error ? err.message : 'Upstream recognition provider failed',
        },
        502,
      )
    }
  }

  const contentType = c.req.header('content-type') || ''
  if (contentType.includes('multipart/form-data')) {
    await c.req.parseBody()
  }

  return c.json({
    matched: false,
    message:
      'Audio recognition is not configured: set RECOGNITION_PROVIDER_URL on the server to enable external audio fingerprint matching.',
  })
})
