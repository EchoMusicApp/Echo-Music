import { Hono } from 'hono'
import net from 'node:net'
import process from 'node:process'

export const discordRouter = new Hono()

const DEFAULT_CLIENT_ID = process.env['DISCORD_CLIENT_ID'] || '123456789012345678' // Application ID

interface PresencePayload {
  track?: string
  artist?: string
  album?: string
  durationMs?: number
  positionMs?: number
  isPlaying?: boolean
}

let activeSocket: net.Socket | null = null
let isConnected = false

function getIpcPath(id = 0): string {
  if (process.platform === 'win32') {
    return `\\\\?\\pipe\\discord-ipc-${id}`
  }
  const prefix =
    process.env['XDG_RUNTIME_DIR'] || process.env['TMPDIR'] || process.env['TMP'] || '/tmp'
  return `${prefix.replace(/\/$/, '')}/discord-ipc-${id}`
}

function encodeFrame(op: number, data: unknown): Buffer {
  const json = JSON.stringify(data)
  const len = Buffer.byteLength(json)
  const packet = Buffer.alloc(8 + len)
  packet.writeInt32LE(op, 0)
  packet.writeInt32LE(len, 4)
  packet.write(json, 8, len, 'utf8')
  return packet
}

async function connectDiscordIpc(): Promise<net.Socket | null> {
  if (activeSocket && isConnected) return activeSocket

  return new Promise((resolve) => {
    const socket = net.createConnection(getIpcPath(0), () => {
      // Handshake opcode 0
      const handshake = encodeFrame(0, {
        v: 1,
        client_id: DEFAULT_CLIENT_ID,
      })
      socket.write(handshake)
      activeSocket = socket
      isConnected = true
      resolve(socket)
    })

    socket.on('error', () => {
      isConnected = false
      activeSocket = null
      resolve(null)
    })

    socket.on('close', () => {
      isConnected = false
      activeSocket = null
    })
  })
}

discordRouter.post('/presence', async (c) => {
  const body = (await c.req.json().catch(() => ({}))) as PresencePayload
  if (!body.track || !body.artist) {
    return c.json({ ok: false, error: 'track and artist required' }, 400)
  }

  try {
    const socket = await connectDiscordIpc()
    if (!socket || !isConnected) {
      return c.json({
        ok: true,
        active: false,
        message: 'Discord desktop app is not running locally',
      })
    }

    const startTimestamp = body.isPlaying
      ? Math.floor((Date.now() - (body.positionMs || 0)) / 1000)
      : undefined

    const endTimestamp =
      body.isPlaying && body.durationMs && body.positionMs !== undefined
        ? Math.floor((Date.now() + (body.durationMs - body.positionMs)) / 1000)
        : undefined

    const activity = {
      cmd: 'SET_ACTIVITY',
      args: {
        pid: process.pid,
        activity: {
          details: body.track.slice(0, 128),
          state: body.artist.slice(0, 128),
          ...(startTimestamp ? { timestamps: { start: startTimestamp, end: endTimestamp } } : {}),
          assets: {
            large_image: 'echo_logo',
            large_text: body.album ? body.album.slice(0, 128) : 'Echo Music',
            small_image: body.isPlaying ? 'play' : 'pause',
            small_text: body.isPlaying ? 'Playing' : 'Paused',
          },
          type: 2, // Listening
        },
      },
      nonce: Math.random().toString(36).slice(2),
    }

    // Opcode 1 = Frame
    socket.write(encodeFrame(1, activity))
    return c.json({ ok: true, active: true })
  } catch (err) {
    return c.json({
      ok: true,
      active: false,
      error: err instanceof Error ? err.message : 'Failed to update Discord presence',
    })
  }
})

discordRouter.post('/clear', async (c) => {
  try {
    const socket = await connectDiscordIpc()
    if (socket && isConnected) {
      const clearActivity = {
        cmd: 'SET_ACTIVITY',
        args: {
          pid: process.pid,
          activity: null,
        },
        nonce: Math.random().toString(36).slice(2),
      }
      socket.write(encodeFrame(1, clearActivity))
    }
    return c.json({ ok: true })
  } catch {
    return c.json({ ok: true })
  }
})
