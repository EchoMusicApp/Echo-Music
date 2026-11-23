import type { Track } from '../../types/domain'

declare global {
  interface Window {
    __onGCastApiAvailable?: (isAvailable: boolean) => void
    cast?: {
      framework: {
        CastContext: {
          getInstance: () => {
            setOptions: (options: {
              receiverApplicationId: string
              autoJoinPolicy: unknown
            }) => void
            requestSession: () => Promise<unknown>
            getCastState: () => string
            getCurrentSession: () => {
              getSessionObj: () => unknown
              getMediaSession: () => {
                play: (request: unknown, success: () => void, error: (err: unknown) => void) => void
                pause: (
                  request: unknown,
                  success: () => void,
                  error: (err: unknown) => void,
                ) => void
                seek: (request: unknown, success: () => void, error: (err: unknown) => void) => void
                stop: (request: unknown, success: () => void, error: (err: unknown) => void) => void
              } | null
              loadMedia: (loadRequest: unknown) => Promise<unknown>
            } | null
            addEventListener: (event: string, handler: (e: { castState?: string }) => void) => void
          }
        }
        CastState: {
          NO_DEVICES_AVAILABLE: string
          NOT_CONNECTED: string
          CONNECTING: string
          CONNECTED: string
        }
        CastContextEventType: {
          CAST_STATE_CHANGED: string
          SESSION_STATE_CHANGED: string
        }
      }
    }
    chrome?: {
      cast?: {
        media?: {
          MediaInfo: new (
            contentId: string,
            contentType: string,
          ) => {
            metadata: {
              metadataType: number
              title: string
              subtitle: string
              images: Array<{ url: string }>
            }
            streamType: string
          }
          LoadRequest: new (mediaInfo: unknown) => {
            currentTime: number
            autoplay: boolean
          }
          MetadataType: {
            MUSIC_TRACK: number
          }
          StreamType: {
            BUFFERED: string
          }
        }
        AutoJoinPolicy: {
          ORIGIN_SCOPED: unknown
        }
        mediaSessionId?: unknown
      }
    }
  }
}

class GoogleCastService {
  private static instance: GoogleCastService | null = null
  private initialized = false
  private isLoaded = false
  private isConnected = false
  private listeners = new Set<(connected: boolean) => void>()

  public static getInstance(): GoogleCastService {
    if (!GoogleCastService.instance) {
      GoogleCastService.instance = new GoogleCastService()
    }
    return GoogleCastService.instance
  }

  public init(): void {
    if (this.initialized || typeof window === 'undefined') return
    this.initialized = true

    window.__onGCastApiAvailable = (isAvailable: boolean) => {
      if (isAvailable && window.cast?.framework) {
        this.isLoaded = true
        try {
          const context = window.cast.framework.CastContext.getInstance()
          context.setOptions({
            receiverApplicationId: 'CC1AD845', // Default Media Receiver
            autoJoinPolicy: window.chrome?.cast?.AutoJoinPolicy?.ORIGIN_SCOPED,
          })

          context.addEventListener(
            window.cast.framework.CastContextEventType.CAST_STATE_CHANGED,
            (event) => {
              const connected = event.castState === window.cast?.framework.CastState.CONNECTED
              this.isConnected = connected
              this.listeners.forEach((l) => l(connected))
            },
          )
        } catch {
          // Ignore
        }
      }
    }

    // Lazy load the cast sender framework
    const script = document.createElement('script')
    script.src = 'https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1'
    script.async = true
    document.head.appendChild(script)
  }

  public subscribe(listener: (connected: boolean) => void): () => void {
    this.listeners.add(listener)
    listener(this.isConnected)
    return () => this.listeners.delete(listener)
  }

  public getIsConnected(): boolean {
    return this.isConnected
  }

  public async requestSession(): Promise<boolean> {
    if (!this.isLoaded || !window.cast?.framework) {
      this.init()
      return false
    }

    try {
      const context = window.cast.framework.CastContext.getInstance()
      await context.requestSession()
      this.isConnected = true
      return true
    } catch {
      return false
    }
  }

  public async castTrack(track: Track, streamUrl: string, currentTime = 0): Promise<boolean> {
    if (!this.isConnected || !window.cast?.framework || !window.chrome?.cast?.media) return false

    try {
      const context = window.cast.framework.CastContext.getInstance()
      const session = context.getCurrentSession()
      if (!session) return false

      const fullStreamUrl = streamUrl.startsWith('/')
        ? window.location.origin + streamUrl
        : streamUrl

      const mediaInfo = new window.chrome.cast.media.MediaInfo(fullStreamUrl, 'audio/webm')
      mediaInfo.metadata = {
        metadataType: window.chrome.cast.media.MetadataType.MUSIC_TRACK,
        title: track.title,
        subtitle: track.artistName || 'Unknown Artist',
        images: track.artwork ? [{ url: track.artwork }] : [],
      }
      mediaInfo.streamType = window.chrome.cast.media.StreamType.BUFFERED

      const request = new window.chrome.cast.media.LoadRequest(mediaInfo)
      request.autoplay = true
      request.currentTime = currentTime

      await session.loadMedia(request)
      return true
    } catch {
      return false
    }
  }

  public pause(): void {
    const session = window.cast?.framework?.CastContext?.getInstance()?.getCurrentSession()
    session?.getMediaSession()?.pause(
      {},
      () => {},
      () => {},
    )
  }

  public play(): void {
    const session = window.cast?.framework?.CastContext?.getInstance()?.getCurrentSession()
    session?.getMediaSession()?.play(
      {},
      () => {},
      () => {},
    )
  }

  public seek(seconds: number): void {
    const session = window.cast?.framework?.CastContext?.getInstance()?.getCurrentSession()
    session?.getMediaSession()?.seek(
      { currentTime: seconds },
      () => {},
      () => {},
    )
  }
}

export const castService = GoogleCastService.getInstance()
