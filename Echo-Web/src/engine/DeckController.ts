import type { DeckId, PlaybackState, Track } from '../types/audio'
import type { WebAudioGraph } from './WebAudioGraph'

export interface DeckControllerOptions {
  crossfadeDuration?: number
  prebufferWindowMs?: number
}

export class DeckController {
  private readonly graph: WebAudioGraph
  private readonly deckAElement: HTMLAudioElement
  private readonly deckBElement: HTMLAudioElement
  private readonly abortController = new AbortController()

  private deckASource: MediaElementAudioSourceNode | null = null
  private deckBSource: MediaElementAudioSourceNode | null = null

  private activeDeckId: DeckId = 'A'
  private crossfadeDurationSeconds = 3.0
  private prebufferWindowMs = 15000

  private activeTrackDeckA: Track | null = null
  private activeTrackDeckB: Track | null = null

  private isPrebufferingTriggered = false
  private isCrossfading = false
  private crossfadeTimer: number | null = null

  private playbackState: PlaybackState = 'IDLE'
  private currentPlayOperationId = 0
  private currentLoadOperationId = 0

  public onPlaybackStateChange?: (state: PlaybackState) => void
  public onTimeUpdate?: (currentTime: number, duration: number) => void
  public onBufferedUpdate?: (bufferedTime: number) => void
  public onTrackEnded?: (endedDeckId: DeckId) => void
  public onPrebufferNeeded?: (standbyDeckId: DeckId) => void
  public onCrossfadeStart?: (fromDeck: DeckId, toDeck: DeckId) => void
  public onCrossfadeComplete?: (newActiveDeck: DeckId) => void
  public onError?: (error: Error) => void

  constructor(graph: WebAudioGraph, options: DeckControllerOptions = {}) {
    this.graph = graph
    this.crossfadeDurationSeconds = options.crossfadeDuration ?? 3.0
    this.prebufferWindowMs = options.prebufferWindowMs ?? 15000

    this.deckAElement = new Audio()
    this.deckBElement = new Audio()

    this.setupAudioElement(this.deckAElement, 'A')
    this.setupAudioElement(this.deckBElement, 'B')

    this.attachAudioSources()
  }

  public get activeDeck(): DeckId {
    return this.activeDeckId
  }

  public get standbyDeck(): DeckId {
    return this.activeDeckId === 'A' ? 'B' : 'A'
  }

  public get state(): PlaybackState {
    return this.playbackState
  }

  public get crossfadeDuration(): number {
    return this.crossfadeDurationSeconds
  }

  public setCrossfadeDuration(seconds: number): void {
    this.crossfadeDurationSeconds = Math.max(0, Math.min(12, seconds))
  }

  public setPrebufferWindowMs(ms: number): void {
    this.prebufferWindowMs = Math.max(5000, ms)
  }

  public getActiveTrack(): Track | null {
    return this.activeDeckId === 'A' ? this.activeTrackDeckA : this.activeTrackDeckB
  }

  public getStandbyTrack(): Track | null {
    return this.activeDeckId === 'A' ? this.activeTrackDeckB : this.activeTrackDeckA
  }

  public getCurrentTime(): number {
    const el = this.getActiveElement()
    return Number.isFinite(el.currentTime) ? el.currentTime : 0
  }

  public getDuration(): number {
    const el = this.getActiveElement()
    return Number.isFinite(el.duration) ? el.duration : 0
  }

  public getBufferedTime(): number {
    const el = this.getActiveElement()
    if (el.buffered.length > 0) {
      return el.buffered.end(el.buffered.length - 1)
    }
    return 0
  }

  public async loadActive(track: Track, url: string, autoPlay = true): Promise<void> {
    const loadId = ++this.currentLoadOperationId
    const deck = this.activeDeckId
    if (deck === 'A') {
      this.activeTrackDeckA = track
    } else {
      this.activeTrackDeckB = track
    }

    this.isPrebufferingTriggered = false
    if (this.crossfadeTimer !== null) {
      window.clearTimeout(this.crossfadeTimer)
      this.crossfadeTimer = null
    }
    this.isCrossfading = false

    // Halt and release standby deck if it was active during an in-flight crossfade
    const standbyEl = this.getStandbyElement()
    this.releaseMediaElement(standbyEl)
    if (this.standbyDeck === 'A') {
      this.activeTrackDeckA = null
    } else {
      this.activeTrackDeckB = null
    }

    const now = this.graph.context.currentTime
    this.graph.deckAGain.gain.cancelScheduledValues(now)
    this.graph.deckBGain.gain.cancelScheduledValues(now)

    this.setDeckGain(deck, 1.0)
    this.setDeckGain(this.standbyDeck, 0.0)

    const el = this.getActiveElement()
    el.pause()
    el.src = url
    el.load()

    if (loadId !== this.currentLoadOperationId) {
      return
    }

    if (autoPlay) {
      await this.play()
    } else {
      this.setPlaybackState('PAUSED')
    }
  }

  public prebufferStandby(track: Track, url: string): void {
    const deck = this.standbyDeck
    if (deck === 'A') {
      this.activeTrackDeckA = track
    } else {
      this.activeTrackDeckB = track
    }

    this.setDeckGain(deck, 0.0)
    const el = this.getStandbyElement()
    el.src = url
    el.preload = 'auto'
    el.load()
  }

  public async play(): Promise<void> {
    const playOpId = ++this.currentPlayOperationId
    await this.graph.resume()
    const el = this.getActiveElement()

    try {
      this.setPlaybackState('BUFFERING')
      await el.play()

      // Guard: If another play/pause/load operation started while el.play() was pending, ignore
      if (playOpId !== this.currentPlayOperationId) {
        return
      }

      if (el.paused) {
        this.setPlaybackState('PAUSED')
      } else {
        this.setPlaybackState('PLAYING')
      }
    } catch (err: unknown) {
      if (playOpId !== this.currentPlayOperationId) {
        return
      }

      if (err instanceof DOMException && err.name === 'NotAllowedError') {
        this.setPlaybackState('REQUIRES_USER_INTERACTION')
      } else if (err instanceof DOMException && err.name === 'AbortError') {
        // Ignored: interruption by rapid skip or subsequent load/pause
      } else {
        this.setPlaybackState('ERROR')
        this.onError?.(err instanceof Error ? err : new Error(String(err)))
      }
    }
  }

  public pause(): void {
    ++this.currentPlayOperationId
    this.getActiveElement().pause()
    if (this.isCrossfading) {
      this.getStandbyElement().pause()
    }
    this.setPlaybackState('PAUSED')
  }

  public toggle(): Promise<void> {
    if (this.playbackState === 'PLAYING') {
      this.pause()
      return Promise.resolve()
    }
    return this.play()
  }

  public seek(seconds: number): void {
    const el = this.getActiveElement()
    const duration = this.getDuration()
    const target = duration > 0 ? Math.max(0, Math.min(seconds, duration)) : Math.max(0, seconds)
    el.currentTime = target
    this.onTimeUpdate?.(target, duration)
  }

  public setPlaybackRate(rate: number): void {
    const clamped = Math.max(0.5, Math.min(2.0, rate))
    this.deckAElement.playbackRate = clamped
    this.deckBElement.playbackRate = clamped
    const a = this.deckAElement as unknown as { preservesPitch?: boolean }
    const b = this.deckBElement as unknown as { preservesPitch?: boolean }
    if ('preservesPitch' in a) a.preservesPitch = true
    if ('preservesPitch' in b) b.preservesPitch = true
  }

  public async setAudioSink(sinkId: string): Promise<boolean> {
    let success = true
    const elA = this.deckAElement as unknown as { setSinkId?: (id: string) => Promise<void> }
    const elB = this.deckBElement as unknown as { setSinkId?: (id: string) => Promise<void> }
    if (typeof elA.setSinkId === 'function') {
      try {
        await elA.setSinkId(sinkId)
      } catch {
        success = false
      }
    }
    if (typeof elB.setSinkId === 'function') {
      try {
        await elB.setSinkId(sinkId)
      } catch {
        success = false
      }
    }
    return success
  }

  public async crossfadeToStandby(): Promise<void> {
    if (this.isCrossfading) return
    const standbyEl = this.getStandbyElement()

    if (!standbyEl.src) {
      this.handleTrackEnded(this.activeDeckId)
      return
    }

    this.isCrossfading = true
    const duration = this.crossfadeDurationSeconds
    const fromDeck = this.activeDeckId
    const toDeck = this.standbyDeck

    this.onCrossfadeStart?.(fromDeck, toDeck)

    try {
      await standbyEl.play()
    } catch (err) {
      this.onError?.(err instanceof Error ? err : new Error(String(err)))
      this.isCrossfading = false
      return
    }

    const now = this.graph.context.currentTime
    const fromGain = fromDeck === 'A' ? this.graph.deckAGain : this.graph.deckBGain
    const toGain = toDeck === 'A' ? this.graph.deckAGain : this.graph.deckBGain

    if (duration > 0) {
      const steps = 32
      const fadeOutCurve = new Float32Array(steps)
      const fadeInCurve = new Float32Array(steps)

      for (let i = 0; i < steps; i++) {
        const theta = (i / (steps - 1)) * (Math.PI / 2)
        fadeOutCurve[i] = Math.cos(theta)
        fadeInCurve[i] = Math.sin(theta)
      }

      fromGain.gain.cancelScheduledValues(now)
      toGain.gain.cancelScheduledValues(now)

      fromGain.gain.setValueCurveAtTime(fadeOutCurve, now, duration)
      toGain.gain.setValueCurveAtTime(fadeInCurve, now, duration)
    } else {
      fromGain.gain.cancelScheduledValues(now)
      toGain.gain.cancelScheduledValues(now)
      fromGain.gain.setTargetAtTime(0.0, now, 0.02)
      toGain.gain.setTargetAtTime(1.0, now, 0.02)
    }

    this.crossfadeTimer = window.setTimeout(
      () => {
        const oldActiveEl = this.getActiveElement()
        this.releaseMediaElement(oldActiveEl)
        if (fromDeck === 'A') {
          this.activeTrackDeckA = null
        } else {
          this.activeTrackDeckB = null
        }

        this.activeDeckId = toDeck
        this.isCrossfading = false
        this.isPrebufferingTriggered = false
        this.crossfadeTimer = null

        this.setDeckGain(fromDeck, 0.0)
        this.setDeckGain(toDeck, 1.0)

        this.onCrossfadeComplete?.(toDeck)
      },
      Math.max(10, duration * 1000),
    )
  }

  public destroy(): void {
    if (this.crossfadeTimer !== null) {
      window.clearTimeout(this.crossfadeTimer)
      this.crossfadeTimer = null
    }

    this.abortController.abort()

    this.releaseMediaElement(this.deckAElement)
    this.releaseMediaElement(this.deckBElement)

    if (this.deckASource) {
      this.deckASource.disconnect()
      this.deckASource = null
    }

    if (this.deckBSource) {
      this.deckBSource.disconnect()
      this.deckBSource = null
    }
  }

  private releaseMediaElement(el: HTMLAudioElement): void {
    try {
      el.pause()
      el.currentTime = 0
      el.removeAttribute('src')
      el.src = ''
      el.load()
    } catch {
      // Non-fatal
    }
  }

  private setupAudioElement(el: HTMLAudioElement, deckId: DeckId): void {
    el.crossOrigin = 'anonymous'
    el.preload = 'metadata'
    const signal = this.abortController.signal

    el.addEventListener(
      'timeupdate',
      () => {
        if (deckId !== this.activeDeckId && !this.isCrossfading) return
        const current = el.currentTime
        const duration = el.duration

        if (deckId === this.activeDeckId) {
          this.onTimeUpdate?.(current, Number.isFinite(duration) ? duration : 0)
          this.checkPrebufferThreshold(current, duration)
          this.checkCrossfadeThreshold(current, duration)
        }
      },
      { signal },
    )

    el.addEventListener(
      'progress',
      () => {
        if (deckId === this.activeDeckId && el.buffered.length > 0) {
          this.onBufferedUpdate?.(el.buffered.end(el.buffered.length - 1))
        }
      },
      { signal },
    )

    el.addEventListener(
      'ended',
      () => {
        if (deckId === this.activeDeckId) {
          this.handleTrackEnded(deckId)
        }
      },
      { signal },
    )

    el.addEventListener(
      'waiting',
      () => {
        if (deckId === this.activeDeckId) {
          this.setPlaybackState('BUFFERING')
        }
      },
      { signal },
    )

    el.addEventListener(
      'stalled',
      () => {
        if (deckId === this.activeDeckId && !el.paused && el.readyState < 3) {
          this.setPlaybackState('BUFFERING')
        }
      },
      { signal },
    )

    el.addEventListener(
      'playing',
      () => {
        if (deckId === this.activeDeckId) {
          this.setPlaybackState('PLAYING')
        }
      },
      { signal },
    )

    el.addEventListener(
      'pause',
      () => {
        if (deckId === this.activeDeckId && !this.isCrossfading) {
          this.setPlaybackState('PAUSED')
        }
      },
      { signal },
    )

    el.addEventListener(
      'canplay',
      () => {
        if (deckId === this.activeDeckId && this.playbackState === 'BUFFERING' && !el.paused) {
          this.setPlaybackState('PLAYING')
        }
      },
      { signal },
    )

    el.addEventListener(
      'error',
      () => {
        if (deckId === this.activeDeckId) {
          if (!el.src || el.src === window.location.href || !el.currentSrc) {
            return
          }
          this.setPlaybackState('ERROR')
          const mediaErr = el.error
          this.onError?.(
            new Error(mediaErr?.message ?? `Deck ${deckId} playback error: ${mediaErr?.code}`),
          )
        }
      },
      { signal },
    )
  }

  private attachAudioSources(): void {
    try {
      this.deckASource = this.graph.context.createMediaElementSource(this.deckAElement)
      this.deckASource.connect(this.graph.deckAGain)

      this.deckBSource = this.graph.context.createMediaElementSource(this.deckBElement)
      this.deckBSource.connect(this.graph.deckBGain)
    } catch (err) {
      this.onError?.(err instanceof Error ? err : new Error(String(err)))
    }
  }

  private getActiveElement(): HTMLAudioElement {
    return this.activeDeckId === 'A' ? this.deckAElement : this.deckBElement
  }

  private getStandbyElement(): HTMLAudioElement {
    return this.activeDeckId === 'A' ? this.deckBElement : this.deckAElement
  }

  private setDeckGain(deckId: DeckId, gain: number): void {
    const targetGain = deckId === 'A' ? this.graph.deckAGain : this.graph.deckBGain
    const now = this.graph.context.currentTime
    const clamped = Math.max(0.0, Math.min(1.0, gain))
    targetGain.gain.cancelScheduledValues(now)
    targetGain.gain.setTargetAtTime(clamped, now, 0.02)
  }

  private setPlaybackState(state: PlaybackState): void {
    if (this.playbackState !== state) {
      this.playbackState = state
      this.onPlaybackStateChange?.(state)
    }
  }

  private checkPrebufferThreshold(currentTime: number, duration: number): void {
    if (this.isPrebufferingTriggered || !Number.isFinite(duration) || duration <= 0) return

    const remainingSeconds = duration - currentTime
    const prebufferSeconds = this.prebufferWindowMs / 1000

    if (remainingSeconds <= prebufferSeconds) {
      this.isPrebufferingTriggered = true
      this.onPrebufferNeeded?.(this.standbyDeck)
    }
  }

  private checkCrossfadeThreshold(currentTime: number, duration: number): void {
    if (this.isCrossfading || !Number.isFinite(duration) || duration <= 0) return

    const remainingSeconds = duration - currentTime
    if (this.crossfadeDurationSeconds > 0 && remainingSeconds <= this.crossfadeDurationSeconds) {
      void this.crossfadeToStandby()
    }
  }

  private handleTrackEnded(deckId: DeckId): void {
    if (!this.isCrossfading) {
      this.onTrackEnded?.(deckId)
    }
  }
}
