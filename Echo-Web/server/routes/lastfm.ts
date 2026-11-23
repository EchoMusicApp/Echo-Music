import { Hono } from 'hono'
import { createHash } from 'node:crypto'
import process from 'node:process'

export const lastfmRouter = new Hono()

const LASTFM_ROOT = 'https://ws.audioscrobbler.com/2.0/'

function sign(params: Record<string, string>, sharedSecret: string): string {
  const sorted = Object.keys(params)
    .sort()
    .map((k) => `${k}${params[k]}`)
    .join('')
  return createHash('md5')
    .update(sorted + sharedSecret)
    .digest('hex')
}

// Last.fm requires the API key AND secret pair server-side; the browser gets a
// stateless passthrough that only knows the session key the user authenticated with.
lastfmRouter.post('/now-playing', async (c) => {
  const apiKey = process.env['LASTFM_API_KEY']
  const sharedSecret = process.env['LASTFM_SHARED_SECRET']
  if (!apiKey || !sharedSecret) {
    return c.json({ error: 'Last.fm is not configured on this server' }, 501)
  }

  const body = (await c.req.json().catch(() => ({}))) as {
    sessionKey?: string
    track?: string
    artist?: string
    album?: string
    duration?: number
  }

  if (!body.sessionKey || !body.track || !body.artist) {
    return c.json({ error: 'sessionKey, track and artist are required' }, 400)
  }

  const params: Record<string, string> = {
    method: 'track.updateNowPlaying',
    api_key: apiKey,
    sk: body.sessionKey,
    track: body.track,
    artist: body.artist,
  }
  if (body.album) params.album = body.album
  if (body.duration) params.duration = String(body.duration)
  params.api_sig = sign(params, sharedSecret)
  params.format = 'json'

  try {
    const res = await fetch(LASTFM_ROOT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(params).toString(),
    })
    const data = (await res.json().catch(() => ({}))) as { error?: number; message?: string }
    if (!res.ok || data.error) {
      return c.json({ error: data.message || `Last.fm API error (HTTP ${res.status})` }, 502)
    }
    return c.json({ ok: true })
  } catch {
    return c.json({ error: 'Could not reach Last.fm' }, 502)
  }
})

lastfmRouter.post('/scrobble', async (c) => {
  const apiKey = process.env['LASTFM_API_KEY']
  const sharedSecret = process.env['LASTFM_SHARED_SECRET']
  if (!apiKey || !sharedSecret) {
    return c.json({ error: 'Last.fm scrobbling is not configured on this server' }, 501)
  }

  const body = (await c.req.json().catch(() => ({}))) as {
    sessionKey?: string
    track?: string
    artist?: string
    album?: string
    timestamp?: number
    duration?: number
  }

  if (!body.sessionKey || !body.track || !body.artist) {
    return c.json({ error: 'sessionKey, track and artist are required' }, 400)
  }

  const params: Record<string, string> = {
    method: 'track.scrobble',
    api_key: apiKey,
    sk: body.sessionKey,
    'track[0]': body.track,
    'artist[0]': body.artist,
    timestamp: String(body.timestamp || Math.floor(Date.now() / 1000)),
  }
  if (body.album) params['album[0]'] = body.album
  if (body.duration) params['duration[0]'] = String(body.duration)
  params.api_sig = sign(params, sharedSecret)
  params.format = 'json'

  try {
    const res = await fetch(LASTFM_ROOT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(params).toString(),
    })
    const data = (await res.json().catch(() => ({}))) as { error?: number; message?: string }
    if (!res.ok || data.error) {
      return c.json({ error: data.message || `Last.fm API error (HTTP ${res.status})` }, 502)
    }
    return c.json({ ok: true })
  } catch {
    return c.json({ error: 'Could not reach Last.fm' }, 502)
  }
})

lastfmRouter.get('/auth-url', (c) => {
  const apiKey = process.env['LASTFM_API_KEY']
  if (!apiKey) {
    return c.json({ error: 'Last.fm is not configured on this server' }, 501)
  }
  const callback = c.req.query('callback') || 'http://localhost:5173/settings'
  const url = `https://www.last.fm/api/auth/?api_key=${encodeURIComponent(apiKey)}&cb=${encodeURIComponent(callback)}`
  return c.json({ url })
})

lastfmRouter.get('/session', async (c) => {
  const apiKey = process.env['LASTFM_API_KEY']
  const sharedSecret = process.env['LASTFM_SHARED_SECRET']
  if (!apiKey || !sharedSecret) {
    return c.json({ error: 'Last.fm is not configured on this server' }, 501)
  }
  const token = c.req.query('token')
  if (!token) {
    return c.json({ error: 'token query param is required' }, 400)
  }
  const params: Record<string, string> = {
    method: 'auth.getSession',
    api_key: apiKey,
    token,
  }
  params.api_sig = sign(params, sharedSecret)
  params.format = 'json'
  try {
    const res = await fetch(`${LASTFM_ROOT}?${new URLSearchParams(params).toString()}`)
    const data = (await res.json().catch(() => ({}))) as {
      session?: { key?: string; name?: string }
      error?: number
      message?: string
    }
    if (data.error || !data.session?.key) {
      return c.json({ error: data.message || 'Could not obtain a Last.fm session' }, 502)
    }
    return c.json({ sessionKey: data.session.key, username: data.session.name })
  } catch {
    return c.json({ error: 'Could not reach Last.fm' }, 502)
  }
})
