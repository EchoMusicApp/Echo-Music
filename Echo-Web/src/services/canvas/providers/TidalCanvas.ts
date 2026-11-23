import type { CanvasVideoResult } from './AppleCanvas.ts'

export class TidalCanvas {
  public static async resolve(
    isrc?: string,
    title?: string,
    artist?: string,
    signal?: AbortSignal,
  ): Promise<CanvasVideoResult | null> {
    try {
      if (isrc) {
        const url = new URL('/api/canvas/tidal', window.location.origin)
        url.searchParams.set('isrc', isrc)
        const res = await fetch(url.toString(), {
          ...(signal ? { signal } : {}),
        })
        if (res.ok) {
          const data = (await res.json()) as {
            success?: boolean
            videoUrl?: string
          }
          if (data.success && data.videoUrl) {
            return {
              provider: 'TIDAL',
              videoUrl: data.videoUrl,
            }
          }
        }
      }

      // If no ISRC or ISRC failed, resolve via central resolve endpoint
      if (title && artist) {
        const resolveUrl = new URL('/api/canvas/resolve', window.location.origin)
        resolveUrl.searchParams.set('title', title)
        resolveUrl.searchParams.set('artist', artist)
        if (isrc) resolveUrl.searchParams.set('isrc', isrc)

        const rRes = await fetch(resolveUrl.toString(), {
          ...(signal ? { signal } : {}),
        })
        if (rRes.ok) {
          const rData = (await rRes.json()) as {
            success?: boolean
            provider?: 'APPLE' | 'TIDAL' | 'ARTIST_VIDEO'
            videoUrl?: string
          }
          if (rData.success && rData.videoUrl && rData.provider === 'TIDAL') {
            return {
              provider: 'TIDAL',
              videoUrl: rData.videoUrl,
            }
          }
        }
      }

      return null
    } catch {
      return null
    }
  }
}
