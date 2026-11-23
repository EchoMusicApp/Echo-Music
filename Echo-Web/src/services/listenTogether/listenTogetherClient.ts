import type { Track } from '../../types/domain'

export interface RoomParticipant {
  id: string
  name: string
  isHost: boolean
  isBuffering: boolean
  lastSeen: number
}

export interface RoomEventLog {
  id: string
  time: number
  message: string
}

export interface SynchronizedRoomState {
  code: string
  isHost: boolean
  userId: string
  // Secret token issued by the server at create/join; required for every sync call
  token: string
  userName: string
  track: Track | null
  isPlaying: boolean
  playbackPositionSeconds: number
  members: RoomParticipant[]
  logs: RoomEventLog[]
}

type RoomListener = (state: SynchronizedRoomState) => void

export class ListenTogetherClient {
  private static instance: ListenTogetherClient | null = null

  private state: SynchronizedRoomState | null = null
  private listeners = new Set<RoomListener>()
  private syncTimer: number | null = null
  private broadcastChannel: BroadcastChannel | null = null

  private constructor() {
    if (typeof window !== 'undefined' && 'BroadcastChannel' in window) {
      try {
        this.broadcastChannel = new BroadcastChannel('echo_listen_together')
        this.broadcastChannel.onmessage = (e) => {
          if (e.data && e.data.type === 'SYNC_STATE' && this.state) {
            this.handleLocalBroadcast(e.data.payload)
          }
        }
      } catch {
        // BroadcastChannel unavailable
      }
    }
  }

  public static getInstance(): ListenTogetherClient {
    if (!ListenTogetherClient.instance) {
      ListenTogetherClient.instance = new ListenTogetherClient()
    }
    return ListenTogetherClient.instance
  }

  public getState(): SynchronizedRoomState | null {
    return this.state
  }

  public subscribe(listener: RoomListener): () => void {
    this.listeners.add(listener)
    if (this.state) {
      listener(this.state)
    }
    return () => {
      this.listeners.delete(listener)
    }
  }

  private notify() {
    if (!this.state) return
    for (const listener of this.listeners) {
      listener(this.state)
    }
  }

  public async createRoom(hostName = 'Host'): Promise<SynchronizedRoomState> {
    this.leaveRoom()

    const res = await fetch('/api/listen-together/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hostName }),
    })

    if (!res.ok) {
      throw new Error(`Failed to create listening room (HTTP ${res.status})`)
    }

    const data = (await res.json()) as {
      code: string
      userId: string
      token: string
      room: {
        code: string
        isPlaying: boolean
        playbackPositionSeconds: number
        track: Track | null
        members: RoomParticipant[]
        logs: RoomEventLog[]
      }
    }

    this.state = {
      code: data.code,
      isHost: true,
      userId: data.userId,
      token: data.token,
      userName: hostName,
      track: data.room.track,
      isPlaying: data.room.isPlaying,
      playbackPositionSeconds: data.room.playbackPositionSeconds,
      members: data.room.members,
      logs: data.room.logs,
    }

    this.startSyncLoop()
    this.notify()
    return this.state
  }

  public async joinRoom(code: string, userName = 'Guest'): Promise<SynchronizedRoomState> {
    this.leaveRoom()

    const cleanCode = code.trim().toUpperCase()

    const res = await fetch('/api/listen-together/join', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: cleanCode, userName }),
    })

    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.error || `Failed to join room ${cleanCode}`)
    }

    const data = (await res.json()) as {
      code: string
      userId: string
      isHost: boolean
      token: string
      room: {
        code: string
        isPlaying: boolean
        playbackPositionSeconds: number
        track: Track | null
        members: RoomParticipant[]
        logs: RoomEventLog[]
      }
    }

    this.state = {
      code: data.code,
      isHost: data.isHost,
      userId: data.userId,
      token: data.token,
      userName,
      track: data.room.track,
      isPlaying: data.room.isPlaying,
      playbackPositionSeconds: data.room.playbackPositionSeconds,
      members: data.room.members,
      logs: data.room.logs,
    }

    this.startSyncLoop()
    this.notify()
    return this.state
  }

  public leaveRoom(): void {
    if (this.syncTimer !== null) {
      window.clearInterval(this.syncTimer)
      this.syncTimer = null
    }

    if (this.state) {
      // Fire and forget leave
      fetch('/api/listen-together/sync', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: this.state.code,
          userId: this.state.userId,
          token: this.state.token,
          action: 'leave',
        }),
      }).catch(() => {})
    }

    this.state = null
    for (const listener of this.listeners) {
      listener({
        code: '',
        isHost: false,
        userId: '',
        token: '',
        userName: '',
        track: null,
        isPlaying: false,
        playbackPositionSeconds: 0,
        members: [],
        logs: [],
      })
    }
  }

  public updateHostPlayback(
    track: Track | null,
    isPlaying: boolean,
    positionSeconds: number,
  ): void {
    if (!this.state || !this.state.isHost) return

    this.state.track = track
    this.state.isPlaying = isPlaying
    this.state.playbackPositionSeconds = positionSeconds

    // Push local broadcast
    this.broadcastChannel?.postMessage({
      type: 'SYNC_STATE',
      payload: {
        code: this.state.code,
        track,
        isPlaying,
        playbackPositionSeconds: positionSeconds,
      },
    })

    // Immediately sync to server
    this.performSync('update_state')
  }

  private startSyncLoop() {
    if (this.syncTimer !== null) {
      window.clearInterval(this.syncTimer)
    }

    this.syncTimer = window.setInterval(() => {
      this.performSync(this.state?.isHost ? 'heartbeat' : 'heartbeat')
    }, 1500)
  }

  private async performSync(action: 'update_state' | 'heartbeat') {
    if (!this.state) return

    try {
      const res = await fetch('/api/listen-together/sync', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: this.state.code,
          userId: this.state.userId,
          token: this.state.token,
          action,
          track: this.state.track,
          isPlaying: this.state.isPlaying,
          playbackPositionSeconds: this.state.playbackPositionSeconds,
        }),
      })

      if (!res.ok) return

      const data = (await res.json()) as {
        isPlaying: boolean
        playbackPositionSeconds: number
        track: Track | null
        members: RoomParticipant[]
        logs: RoomEventLog[]
      }

      if (!this.state.isHost) {
        this.state.isPlaying = data.isPlaying
        this.state.playbackPositionSeconds = data.playbackPositionSeconds
        this.state.track = data.track
      }

      this.state.members = data.members || []
      this.state.logs = data.logs || []
      this.notify()
    } catch {
      // Ignore transient network errors
    }
  }

  private handleLocalBroadcast(payload: {
    code: string
    track: Track | null
    isPlaying: boolean
    playbackPositionSeconds: number
  }) {
    if (!this.state || this.state.isHost || this.state.code !== payload.code) return

    this.state.track = payload.track
    this.state.isPlaying = payload.isPlaying
    this.state.playbackPositionSeconds = payload.playbackPositionSeconds
    this.notify()
  }
}

export const listenTogether = ListenTogetherClient.getInstance()
