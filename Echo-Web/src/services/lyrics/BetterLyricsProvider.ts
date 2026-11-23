import type { SynchronizedLyrics } from '../../types/lyrics.ts'
import { TTMLParser } from './TTMLParser.ts'

export interface BetterLyricsResponse {
  ttml?: string
  lyrics?: string
}

export class BetterLyricsProvider {
  private static readonly BASE_URL = 'https://lyrics-api.boidu.dev/getLyrics'

  public static async fetchLyrics(
    title: string,
    artist: string,
    album?: string,
    durationSeconds?: number,
    signal?: AbortSignal,
  ): Promise<SynchronizedLyrics | null> {
    try {
      const url = new URL(this.BASE_URL)
      url.searchParams.set('s', title)
      url.searchParams.set('a', artist)
      if (album) {
        url.searchParams.set('al', album)
      }
      if (durationSeconds && durationSeconds > 0) {
        url.searchParams.set('d', Math.round(durationSeconds).toString())
      }

      const res = await fetch(url.toString(), {
        ...(signal ? { signal } : {}),
        headers: {
          Accept: 'application/json, text/plain, */*',
        },
      })

      if (!res.ok) {
        return null
      }

      const data = (await res.json()) as BetterLyricsResponse
      const ttml = data.ttml || data.lyrics
      if (!ttml) {
        return null
      }

      const lines = TTMLParser.parse(ttml)
      if (lines.length === 0) {
        return null
      }

      const hasWords = lines.some((l) => Boolean(l.words && l.words.length > 0))

      return {
        provider: 'BETTER_LYRICS',
        isSynced: true,
        isWordSynced: hasWords,
        lines,
      }
    } catch {
      return null
    }
  }
}
