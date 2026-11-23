import type { Track } from './domain.ts'
export type { Track } from './domain.ts'

export type PlaybackState =
  | 'IDLE'
  | 'BUFFERING'
  | 'PLAYING'
  | 'PAUSED'
  | 'SEEKING'
  | 'ENDED'
  | 'ERROR'
  | 'REQUIRES_USER_INTERACTION'

export type RepeatMode = 'OFF' | 'ALL' | 'ONE'

export type DeckId = 'A' | 'B'

export interface SponsorSegment {
  category: 'music_offtopic' | 'intro' | 'outro' | 'preview' | string
  start: number
  end: number
  uuid?: string
}

export interface QueueSnapshot {
  id: string
  timestamp: number
  history: Track[]
  currentTrack: Track | null
  upNext: Track[]
  sourceQueue: Track[]
  radioQueue: Track[]
  shuffledSourceQueue: Track[]
  currentTime: number
  activeDeck: DeckId
  repeatMode: RepeatMode
  isShuffle: boolean
  pauseAtEndOfItems: boolean
}

export type EqualizerFilterType =
  'lowpass' | 'highpass' | 'bandpass' | 'lowshelf' | 'highshelf' | 'peaking' | 'notch' | 'allpass'

export interface EqualizerBand {
  index: number
  frequency: number
  gain: number
  type: EqualizerFilterType
  q: number
}

export interface EqualizerPreset {
  name: string
  gains: number[]
}

export interface AudioEngineConfig {
  crossfadeDuration: number
  prebufferWindowMs: number
  sponsorBlockEnabled: boolean
  sponsorBlockCategories: string[]
  autoRadio: boolean
  volumeNormalization: boolean
  stereoWidth: number
}

export interface AudioEngineState {
  playbackState: PlaybackState
  currentTrack: Track | null
  currentTime: number
  duration: number
  bufferedTime: number
  volume: number
  muted: boolean
  repeatMode: RepeatMode
  isShuffle: boolean
  activeDeck: DeckId
  upNext: Track[]
  sourceQueue: Track[]
  sourceUpcoming: Track[]
  history: Track[]
  radioQueue: Track[]
  equalizerGains: number[]
  stereoWidth: number
  volumeNormalization: boolean
  crossfadeDuration: number
  playbackRate?: number
  sleepTimerRemaining?: number | null
  sleepTimerEndOfTrack?: boolean
}

export type AudioEngineEventMap = {
  playbackStateChange: (state: PlaybackState) => void
  trackChange: (track: Track | null) => void
  timeUpdate: (currentTime: number, duration: number) => void
  bufferedUpdate: (bufferedTime: number) => void
  volumeChange: (volume: number, muted: boolean) => void
  queueChange: () => void
  deckSwap: (activeDeck: DeckId) => void
  segmentSkip: (segment: SponsorSegment) => void
  error: (error: Error) => void
}

export type AudioEngineEvent = keyof AudioEngineEventMap
