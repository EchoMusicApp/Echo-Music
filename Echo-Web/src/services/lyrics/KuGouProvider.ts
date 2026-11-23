import type { SynchronizedLyrics } from '../../types/lyrics.ts'

export class KuGouProvider {
  public static async fetchLyrics(
    title: string,
    artist: string,
    album?: string,
    durationSeconds?: number,
    signal?: AbortSignal,
  ): Promise<SynchronizedLyrics | null> {
    try {
      const url = new URL('/api/lyrics/kugou', window.location.origin)
      url.searchParams.set('title', title)
      url.searchParams.set('artist', artist)
      if (album) {
        url.searchParams.set('album', album)
      }
      if (durationSeconds && durationSeconds > 0) {
        url.searchParams.set('duration', Math.round(durationSeconds).toString())
      }

      const res = await fetch(url.toString(), {
        ...(signal ? { signal } : {}),
      })
      if (!res.ok) {
        return null
      }

      const data = (await res.json()) as {
        success?: boolean
        lyrics?: SynchronizedLyrics
      }

      if (data.success && data.lyrics && data.lyrics.lines.length > 0) {
        return data.lyrics
      }

      return null
    } catch {
      return null
    }
  }
}
