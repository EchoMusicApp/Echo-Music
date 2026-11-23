import { Hono } from 'hono'
import { randomUUID } from 'node:crypto'

export const listenTogetherRouter = new Hono()

interface RoomMember {
  id: string
  name: string
  isHost: boolean
  lastSeen: number
  isBuffering: boolean
  // Secret per-participant token; sync requests must present it to act as this member
  token: string
}

interface RoomState {
  code: string
  hostId: string
  createdAt: number
  track: unknown | null
  isPlaying: boolean
  playbackPositionSeconds: number
  lastUpdatedServerTime: number
  members: Map<string, RoomMember>
  logs: Array<{ id: string; time: number; message: string }>
}

const activeRooms = new Map<string, RoomState>()

// Simple fixed-window rate limiting per IP to blunt code guessing and sync spam
const joinAttemptsByIp = new Map<string, { windowStart: number; count: number }>()
const JOIN_LIMIT_PER_WINDOW = 20
const JOIN_WINDOW_MS = 10 * 60 * 1000

function isJoinRateLimited(ip: string): boolean {
  const now = Date.now()
  const entry = joinAttemptsByIp.get(ip)
  if (!entry || now - entry.windowStart > JOIN_WINDOW_MS) {
    joinAttemptsByIp.set(ip, { windowStart: now, count: 1 })
    if (joinAttemptsByIp.size > 10000) {
      for (const [key, value] of joinAttemptsByIp.entries()) {
        if (now - value.windowStart > JOIN_WINDOW_MS) joinAttemptsByIp.delete(key)
      }
    }
    return false
  }
  entry.count++
  return entry.count > JOIN_LIMIT_PER_WINDOW
}

// Cleanup stale rooms every 5 minutes
const cleanupTimer = setInterval(
  () => {
    const now = Date.now()
    for (const [code, room] of activeRooms.entries()) {
      if (now - room.lastUpdatedServerTime > 30 * 60 * 1000) {
        activeRooms.delete(code)
      }
    }
  },
  5 * 60 * 1000,
)

if (typeof cleanupTimer?.unref === 'function') {
  cleanupTimer.unref()
}

// Room codes are 8 chars from a 32-symbol alphabet (8^... ~2^40 candidates). Clients never
// send the code alone: membership requires the secret token issued at create/join time.
function generateRoomCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let result = ''
  const bytes = randomUUID().replace(/-/g, '')
  for (let i = 0; i < 8; i++) {
    const nibble = Number.parseInt(bytes[i % bytes.length]!, 16)
    result += chars.charAt(nibble % chars.length)
  }
  return result
}

// Public room view: never exposes hostId or member tokens
function publicRoomView(room: RoomState, positionSeconds: number) {
  return {
    code: room.code,
    isPlaying: room.isPlaying,
    playbackPositionSeconds: positionSeconds,
    track: room.track,
    members: Array.from(room.members.values()).map(({ id, name, isHost, isBuffering }) => ({
      id,
      name,
      isHost,
      isBuffering,
    })),
    logs: room.logs.slice(-20),
  }
}

// Create a new listening room
listenTogetherRouter.post('/create', async (c) => {
  const body = (await c.req.json().catch(() => ({}))) as {
    hostName?: string
  }
  const hostId = `host_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
  const hostToken = randomUUID()
  const hostName = body.hostName || 'Host'

  let code = generateRoomCode()
  while (activeRooms.has(code)) {
    code = generateRoomCode()
  }

  const room: RoomState = {
    code,
    hostId,
    createdAt: Date.now(),
    track: null,
    isPlaying: false,
    playbackPositionSeconds: 0,
    lastUpdatedServerTime: Date.now(),
    members: new Map([
      [
        hostId,
        {
          id: hostId,
          name: hostName,
          isHost: true,
          lastSeen: Date.now(),
          isBuffering: false,
          token: hostToken,
        },
      ],
    ]),
    logs: [{ id: `log_${Date.now()}`, time: Date.now(), message: `Room created by ${hostName}` }],
  }

  activeRooms.set(code, room)

  return c.json({
    code,
    userId: hostId,
    token: hostToken,
    room: publicRoomView(room, room.playbackPositionSeconds),
  })
})

// Join an existing room
listenTogetherRouter.post('/join', async (c) => {
  const ip =
    c.req.header('x-forwarded-for')?.split(',')[0]?.trim() ||
    ((c.env as Record<string, unknown> | undefined)?.['remoteAddress'] as string | undefined) ||
    'unknown'
  if (isJoinRateLimited(ip)) {
    return c.json({ error: 'Too many join attempts. Try again later.' }, 429)
  }

  const body = (await c.req.json().catch(() => ({}))) as {
    code?: string
    userName?: string
  }
  const code = (body.code || '').trim().toUpperCase()
  const room = activeRooms.get(code)

  if (!room) {
    return c.json({ error: 'Room not found or expired' }, 404)
  }
  if (room.members.size >= 50) {
    return c.json({ error: 'Room is full' }, 409)
  }

  const userId = `user_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
  const userToken = randomUUID()
  const userName = body.userName || 'Guest'

  const member: RoomMember = {
    id: userId,
    name: userName,
    isHost: false,
    lastSeen: Date.now(),
    isBuffering: false,
    token: userToken,
  }

  room.members.set(userId, member)
  room.logs.push({
    id: `log_${Date.now()}`,
    time: Date.now(),
    message: `${userName} joined the room`,
  })

  // Estimate current position if playing
  const elapsed = room.isPlaying ? (Date.now() - room.lastUpdatedServerTime) / 1000 : 0
  const estimatedPosition = room.playbackPositionSeconds + elapsed

  return c.json({
    code: room.code,
    userId,
    isHost: false,
    token: userToken,
    room: publicRoomView(room, estimatedPosition),
  })
})

// Sync/Poll endpoint: receives host or member updates, returns current state
listenTogetherRouter.post('/sync', async (c) => {
  const body = (await c.req.json().catch(() => ({}))) as {
    code?: string
    userId?: string
    token?: string
    action?: 'update_state' | 'heartbeat' | 'leave'
    track?: unknown
    isPlaying?: boolean
    playbackPositionSeconds?: number
    isBuffering?: boolean
  }

  const code = (body.code || '').trim().toUpperCase()
  const room = activeRooms.get(code)

  if (!room) {
    return c.json({ error: 'Room not found' }, 404)
  }

  const userId = body.userId || ''
  const member = room.members.get(userId)
  // Membership is authenticated: a request that does not present the secret token issued at
  // create/join time is not a member, so it cannot act for (or observe) the room.
  if (!member || member.token !== body.token) {
    return c.json({ error: 'Not a room member' }, 403)
  }

  member.lastSeen = Date.now()
  if (typeof body.isBuffering === 'boolean') {
    member.isBuffering = body.isBuffering
  }

  if (body.action === 'leave') {
    room.members.delete(userId)
    room.logs.push({
      id: `log_${Date.now()}`,
      time: Date.now(),
      message: `${member.name} left the room`,
    })
    return c.json({ status: 'left' })
  }

  // Only the authenticated host can push playback state
  if (userId === room.hostId && body.action === 'update_state') {
    if (body.track !== undefined) {
      room.track = body.track
    }
    if (typeof body.isPlaying === 'boolean') {
      room.isPlaying = body.isPlaying
    }
    if (typeof body.playbackPositionSeconds === 'number') {
      room.playbackPositionSeconds = body.playbackPositionSeconds
    }
    room.lastUpdatedServerTime = Date.now()
  }

  // Calculate current interpolated playback position
  const elapsed = room.isPlaying ? (Date.now() - room.lastUpdatedServerTime) / 1000 : 0
  const estimatedPosition = room.playbackPositionSeconds + elapsed

  // Prune inactive members (> 45s without sync)
  const now = Date.now()
  for (const [id, m] of room.members.entries()) {
    if (id !== room.hostId && now - m.lastSeen > 45000) {
      room.members.delete(id)
    }
  }

  return c.json(publicRoomView(room, estimatedPosition))
})
