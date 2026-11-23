import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { fileURLToPath } from 'node:url'
import process from 'node:process'
import { innertubeRouter } from './routes/innertube.ts'
import { radioRouter } from './routes/radio.ts'
import { sponsorblockRouter } from './routes/sponsorblock.ts'
import { streamRouter } from './routes/stream.ts'
import { canvasRouter } from './routes/canvas.ts'
import { lyricsRouter } from './routes/lyrics.ts'
import { spotifyRouter } from './routes/spotify.ts'
import { listenTogetherRouter } from './routes/listenTogether.ts'
import { recognitionRouter } from './routes/recognition.ts'
import { musicDnaRouter } from './routes/musicDna.ts'
import { lastfmRouter } from './routes/lastfm.ts'
import { discordRouter } from './routes/discord.ts'

export const app = new Hono()

// Browsers on the same origin never trigger CORS (same-origin requests are exempt), so the
// allowlist below only has to cover cross-origin deployments. Set CORS_ORIGINS as a comma-
// separated list of deploy origins (e.g. "https://echo.example,https://staging.echo.example").
// The default mirrors the Vite dev server so direct origin access keeps working in development.
const allowedOrigins = (process.env['CORS_ORIGINS'] || 'http://localhost:5173')
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean)

app.use(
  '*',
  cors({
    origin: (origin) => (origin && allowedOrigins.includes(origin) ? origin : undefined),
    allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Range', 'Authorization', 'X-YouTube-Visitor-Data'],
    exposeHeaders: ['Content-Length', 'Content-Range', 'Accept-Ranges'],
  }),
)

app.onError((err, c) => {
  // Log full details server-side; never surface raw upstream/internal error
  // messages to clients (they can leak proxy targets, tokens or stack info).
  console.error(`[proxy] ${c.req.method} ${c.req.path} failed:`, err)
  return c.json({ error: 'Internal Server Error' }, 500)
})

app.get('/api/health', (c) => {
  return c.json({
    status: 'ok',
    timestamp: Date.now(),
    service: 'echo-audio-proxy',
  })
})

app.route('/api/innertube', innertubeRouter)
app.route('/api/stream', streamRouter)
app.route('/api/radio', radioRouter)
app.route('/api/sponsorblock', sponsorblockRouter)
app.route('/api/canvas', canvasRouter)
app.route('/api/lyrics', lyricsRouter)
app.route('/api/spotify', spotifyRouter)
app.route('/api/listen-together', listenTogetherRouter)
app.route('/api/recognition', recognitionRouter)
app.route('/api/music-dna', musicDnaRouter)
app.route('/api/lastfm', lastfmRouter)
app.route('/api/discord', discordRouter)

const port = Number.parseInt(process.env['PORT'] || '3000', 10)

const isDirectRun =
  Boolean(process.argv[1]) &&
  fileURLToPath(import.meta.url)
    .toLowerCase()
    .replace(/\\/g, '/') === process.argv[1].toLowerCase().replace(/\\/g, '/')

if (isDirectRun) {
  process.on('uncaughtException', (err) => {
    console.error('Server uncaught exception:', err)
  })

  process.on('unhandledRejection', (reason) => {
    console.error('Server unhandled rejection:', reason)
  })

  serve(
    {
      fetch: app.fetch,
      port,
    },
    (info) => {
      console.log(`Echo Audio Proxy Server running on port ${info.port}`)
    },
  )
}
