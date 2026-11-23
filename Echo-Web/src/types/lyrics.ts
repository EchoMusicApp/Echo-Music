export interface LyricWord {
  text: string
  startTimeMs: number
  endTimeMs: number
}

export interface LyricLine {
  startTimeMs: number
  endTimeMs?: number
  text: string
  translatedText?: string
  words?: LyricWord[]
  agent?: string
  isBackground?: boolean
}

export type LyricsProviderType = 'BETTER_LYRICS' | 'LRCLIB' | 'KUGOU' | 'YOUTUBE'

export interface SynchronizedLyrics {
  provider: LyricsProviderType
  isSynced: boolean
  isWordSynced: boolean
  lines: LyricLine[]
}
