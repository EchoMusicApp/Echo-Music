export interface CanvasVideoResult {
  provider: 'APPLE' | 'TIDAL' | 'ARTIST_VIDEO'
  videoUrl: string
  rawUrl?: string | undefined
}

export class AppleCanvas {
  public static async resolve(
    title: string,
    artist: string,
    album?: string,
    signal?: AbortSignal,
  ): Promise<CanvasVideoResult | null> {
    try {
      const url = new URL('/api/canvas/resolve', window.location.origin)
      url.searchParams.set('title', title)
      url.searchParams.set('artist', artist)
      if (album) url.searchParams.set('album', album)

      const res = await fetch(url.toString(), {
        ...(signal ? { signal } : {}),
      })
      if (!res.ok) return null

      const data = (await res.json()) as {
        success?: boolean
        provider?: 'APPLE' | 'TIDAL' | 'ARTIST_VIDEO'
        videoUrl?: string
        rawUrl?: string
      }

      if (data.success && data.videoUrl && data.provider === 'APPLE') {
        const result: CanvasVideoResult = {
          provider: 'APPLE',
          videoUrl: data.videoUrl,
        }
        if (data.rawUrl) {
          result.rawUrl = data.rawUrl
        }
        return result
      }

      return null
    } catch {
      return null
    }
  }
}
