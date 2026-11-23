import type { Track } from '../types/audio'

export interface MediaSessionCallbacks {
  onPlay: () => void
  onPause: () => void
  onPreviousTrack: () => void
  onNextTrack: () => void
  onSeekTo: (seconds: number) => void
  onSeekBackward?: (seconds: number) => void
  onSeekForward?: (seconds: number) => void
}

export class MediaSession {
  private isAvailable = false
  private callbacks: MediaSessionCallbacks | null = null

  constructor(callbacks: MediaSessionCallbacks) {
    this.callbacks = callbacks
    this.isAvailable = typeof navigator !== 'undefined' && 'mediaSession' in navigator

    if (this.isAvailable) {
      this.registerActionHandlers()
    }
  }

  public updateShuffleActive(enabled: boolean): void {
    if (!this.isAvailable || !navigator.mediaSession) return
    try {
      ;(navigator.mediaSession as { setShuffleActive?: (v: boolean) => void }).setShuffleActive?.(
        enabled,
      )
    } catch {
      // Ignore unsupported session action in this browser
    }
  }

  public updateRepeatMode(mode: 'off' | 'all' | 'one'): void {
    if (!this.isAvailable || !navigator.mediaSession) return
    try {
      ;(
        navigator.mediaSession as { setRepeatMode?: (m: 'none' | 'all' | 'one') => void }
      ).setRepeatMode?.(mode === 'off' ? 'none' : mode)
    } catch {
      // Ignore unsupported session action in this browser
    }
  }

  public updateTrack(track: Track | null): void {
    if (!this.isAvailable || !navigator.mediaSession) return

    if (!track) {
      navigator.mediaSession.metadata = null
      return
    }

    const artworkSources: MediaImage[] = []
    if (track.artwork) {
      artworkSources.push(
        { src: track.artwork, sizes: '96x96', type: 'image/jpeg' },
        { src: track.artwork, sizes: '128x128', type: 'image/jpeg' },
        { src: track.artwork, sizes: '192x192', type: 'image/jpeg' },
        { src: track.artwork, sizes: '256x256', type: 'image/jpeg' },
        { src: track.artwork, sizes: '512x512', type: 'image/jpeg' },
      )
    }

    navigator.mediaSession.metadata = new MediaMetadata({
      title: track.title,
      artist: track.artistName,
      album: track.albumTitle,
      artwork: artworkSources,
    })
  }

  public updatePlaybackState(state: 'playing' | 'paused' | 'none'): void {
    if (!this.isAvailable || !navigator.mediaSession) return
    navigator.mediaSession.playbackState = state
  }

  public updatePositionState(
    durationSeconds: number,
    positionSeconds: number,
    playbackRate = 1.0,
  ): void {
    if (
      !this.isAvailable ||
      !navigator.mediaSession ||
      typeof navigator.mediaSession.setPositionState !== 'function'
    ) {
      return
    }

    if (
      !Number.isFinite(durationSeconds) ||
      durationSeconds <= 0 ||
      !Number.isFinite(positionSeconds) ||
      positionSeconds < 0
    ) {
      return
    }

    const safeRate = Number.isFinite(playbackRate) && playbackRate > 0 ? playbackRate : 1.0

    try {
      navigator.mediaSession.setPositionState({
        duration: durationSeconds,
        playbackRate: safeRate,
        position: Math.min(positionSeconds, durationSeconds),
      })
    } catch {
      // Ignore unsupported or out-of-range position errors
    }
  }

  public destroy(): void {
    if (!this.isAvailable || !navigator.mediaSession) return

    const actions: MediaSessionAction[] = [
      'play',
      'pause',
      'previoustrack',
      'nexttrack',
      'seekto',
      'seekbackward',
      'seekforward',
    ]

    for (const action of actions) {
      try {
        navigator.mediaSession.setActionHandler(action, null)
      } catch {
        // Ignore unsupported action types in browser
      }
    }

    navigator.mediaSession.metadata = null
    this.callbacks = null
  }

  private registerActionHandlers(): void {
    if (!this.isAvailable || !navigator.mediaSession || !this.callbacks) return

    const cb = this.callbacks

    const safeSetHandler = (
      action: MediaSessionAction,
      handler: MediaSessionActionHandler | null,
    ) => {
      try {
        navigator.mediaSession.setActionHandler(action, handler)
      } catch {
        // Ignore browser-unsupported action
      }
    }

    safeSetHandler('play', () => cb.onPlay())
    safeSetHandler('pause', () => cb.onPause())
    safeSetHandler('previoustrack', () => cb.onPreviousTrack())
    safeSetHandler('nexttrack', () => cb.onNextTrack())

    safeSetHandler('seekto', (details) => {
      if (typeof details.seekTime === 'number') {
        cb.onSeekTo(details.seekTime)
      }
    })

    safeSetHandler('seekbackward', (details) => {
      const skipSeconds = details.seekOffset ?? 10
      if (cb.onSeekBackward) {
        cb.onSeekBackward(skipSeconds)
      } else {
        cb.onSeekTo(Math.max(0, (details.seekTime ?? 0) - skipSeconds))
      }
    })

    safeSetHandler('seekforward', (details) => {
      const skipSeconds = details.seekOffset ?? 10
      if (cb.onSeekForward) {
        cb.onSeekForward(skipSeconds)
      } else {
        cb.onSeekTo((details.seekTime ?? 0) + skipSeconds)
      }
    })
  }
}
