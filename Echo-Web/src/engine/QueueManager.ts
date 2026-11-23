import type { DeckId, QueueSnapshot, RepeatMode, Track } from '../types/audio.ts'

function cloneTrack(track: Track): Track {
  return {
    ...track,
    ...(track.artists
      ? {
          artists: track.artists.map((a) => ({
            ...a,
            ...(a.genres ? { genres: [...a.genres] } : {}),
          })),
        }
      : {}),
    ...(track.album ? { album: { ...track.album } } : {}),
    ...(track.sponsorBlockSegments
      ? { sponsorBlockSegments: track.sponsorBlockSegments.map((s) => ({ ...s })) }
      : {}),
  }
}

export type QueueListType = 'upNext' | 'sourceQueue'

export interface DetailedQueueState {
  currentTrack: Track | null
  upNext: Track[]
  sourceUpcoming: Track[]
  history: Track[]
  radioQueue: Track[]
  totalUpcomingCount: number
}

export class QueueManager {
  private historyList: Track[] = []
  private activeTrack: Track | null = null
  private activeTrackSource: 'upNext' | 'sourceQueue' | 'radio' | 'direct' = 'direct'
  private upNextList: Track[] = []
  private sourceQueueList: Track[] = []
  private shuffledList: Track[] = []
  private radioQueueList: Track[] = []

  private currentSourceIndex = -1
  private repeatModeSetting: RepeatMode = 'OFF'
  private shuffleEnabled = false
  private pauseAtEndOfItemsSetting = false
  private currentActiveDeck: DeckId = 'A'

  constructor(initialTracks: Track[] = [], startIndex = 0) {
    if (initialTracks.length > 0) {
      this.setSourceQueue(initialTracks, startIndex)
    }
  }

  public get history(): Track[] {
    return this.historyList.map(cloneTrack)
  }

  public get currentTrack(): Track | null {
    return this.activeTrack ? cloneTrack(this.activeTrack) : null
  }

  public get upNext(): Track[] {
    return this.upNextList.map(cloneTrack)
  }

  public get sourceQueue(): Track[] {
    return (this.shuffleEnabled ? this.shuffledList : this.sourceQueueList).map(cloneTrack)
  }

  public get radioQueue(): Track[] {
    return this.radioQueueList.map(cloneTrack)
  }

  public get repeatMode(): RepeatMode {
    return this.repeatModeSetting
  }

  public get isShuffle(): boolean {
    return this.shuffleEnabled
  }

  public get pauseAtEndOfItems(): boolean {
    return this.pauseAtEndOfItemsSetting
  }

  public setPauseAtEndOfItems(enable: boolean): void {
    this.pauseAtEndOfItemsSetting = enable
  }

  public setActiveDeck(deck: DeckId): void {
    this.currentActiveDeck = deck
  }

  public getDetailedQueue(): DetailedQueueState {
    const effectiveSource = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList
    const sourceUpcoming =
      this.currentSourceIndex >= 0 && this.currentSourceIndex < effectiveSource.length - 1
        ? effectiveSource.slice(this.currentSourceIndex + 1).map(cloneTrack)
        : []

    return {
      currentTrack: this.currentTrack,
      upNext: this.upNext,
      sourceUpcoming,
      history: this.history,
      radioQueue: this.radioQueue,
      totalUpcomingCount: this.upNextList.length + sourceUpcoming.length,
    }
  }

  public setSourceQueue(tracks: Track[], startIndex = 0): Track | null {
    this.sourceQueueList = tracks.map(cloneTrack)
    const clampedIndex = Math.max(0, Math.min(startIndex, Math.max(0, tracks.length - 1)))
    this.currentSourceIndex = tracks.length > 0 ? clampedIndex : -1
    this.activeTrack = tracks[clampedIndex] ? cloneTrack(tracks[clampedIndex]) : null
    this.activeTrackSource = 'sourceQueue'

    if (this.shuffleEnabled && this.activeTrack) {
      this.computeShuffledQueue(this.activeTrack)
    } else {
      this.shuffledList = []
    }

    return this.activeTrack
  }

  public playTrackDirectly(track: Track): void {
    if (this.activeTrack && this.activeTrack.id !== track.id) {
      if (!this.pauseAtEndOfItemsSetting) {
        this.historyList.push(this.activeTrack)
      }
    }
    this.activeTrack = cloneTrack(track)
    this.activeTrackSource = 'direct'

    const existingIndex = this.sourceQueueList.findIndex((item) => item.id === track.id)
    if (existingIndex !== -1) {
      this.currentSourceIndex = existingIndex
      this.activeTrackSource = 'sourceQueue'
      if (this.shuffleEnabled) {
        this.computeShuffledQueue(track)
      }
    } else {
      // Start a single-track source context
      this.sourceQueueList = [cloneTrack(track)]
      this.currentSourceIndex = 0
      this.shuffledList = []
    }
  }

  public playNext(track: Track): void {
    const cloned = cloneTrack(track)
    this.upNextList = this.upNextList.filter((item) => item.id !== cloned.id)
    this.upNextList.unshift(cloned)
  }

  public addToQueue(track: Track): void {
    const cloned = cloneTrack(track)
    this.upNextList = this.upNextList.filter((item) => item.id !== cloned.id)
    this.upNextList.push(cloned)
  }

  public removeUpNext(index: number): Track | null {
    if (index < 0 || index >= this.upNextList.length) return null
    const [removed] = this.upNextList.splice(index, 1)
    return removed ?? null
  }

  public moveUpNext(from: number, to: number): void {
    if (from < 0 || from >= this.upNextList.length || to < 0 || to >= this.upNextList.length) {
      return
    }
    const [item] = this.upNextList.splice(from, 1)
    if (item) {
      this.upNextList.splice(to, 0, item)
    }
  }

  public removeSourceQueueItem(relativeIndex: number): Track | null {
    const effective = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList
    const absoluteIndex = this.currentSourceIndex + 1 + relativeIndex
    if (absoluteIndex < 0 || absoluteIndex >= effective.length) return null

    const [removed] = effective.splice(absoluteIndex, 1)
    if (this.shuffleEnabled && removed) {
      const origIndex = this.sourceQueueList.findIndex((t) => t.id === removed.id)
      if (origIndex !== -1) {
        this.sourceQueueList.splice(origIndex, 1)
      }
    }
    return removed ?? null
  }

  public moveSourceQueueItem(fromRelative: number, toRelative: number): void {
    const effective = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList
    const fromAbs = this.currentSourceIndex + 1 + fromRelative
    const toAbs = this.currentSourceIndex + 1 + toRelative

    if (
      fromAbs <= this.currentSourceIndex ||
      fromAbs >= effective.length ||
      toAbs <= this.currentSourceIndex ||
      toAbs >= effective.length
    ) {
      return
    }

    const [item] = effective.splice(fromAbs, 1)
    if (item) {
      effective.splice(toAbs, 0, item)
    }
  }

  public clearUpNext(): void {
    this.upNextList = []
  }

  public clearSourceUpcoming(): void {
    if (this.shuffleEnabled) {
      this.shuffledList = this.shuffledList.slice(0, this.currentSourceIndex + 1)
    }
    this.sourceQueueList = this.sourceQueueList.slice(0, this.currentSourceIndex + 1)
  }

  public clearEntireQueue(): void {
    this.upNextList = []
    this.clearSourceUpcoming()
    this.radioQueueList = []
  }

  public addRadioTracks(tracks: Track[]): void {
    this.radioQueueList.push(...tracks.map(cloneTrack))
  }

  public clearRadioQueue(): void {
    this.radioQueueList = []
  }

  public setRepeatMode(mode: RepeatMode): void {
    this.repeatModeSetting = mode
  }

  public setShuffle(enabled: boolean): void {
    if (this.shuffleEnabled === enabled) return
    this.shuffleEnabled = enabled

    if (enabled) {
      if (this.activeTrack) {
        this.computeShuffledQueue(this.activeTrack)
      } else if (this.sourceQueueList.length > 0) {
        this.shuffledList = this.fisherYatesShuffle(this.sourceQueueList)
        this.currentSourceIndex = 0
      }
    } else {
      if (this.activeTrack) {
        const originalIndex = this.sourceQueueList.findIndex(
          (track) => track.id === this.activeTrack?.id,
        )
        if (originalIndex !== -1) {
          this.currentSourceIndex = originalIndex
        }
      }
      this.shuffledList = []
    }
  }

  public peekNext(): Track | null {
    if (this.repeatModeSetting === 'ONE' && this.activeTrack) {
      return this.activeTrack
    }

    if (this.upNextList.length > 0) {
      return this.upNextList[0] ?? null
    }

    const effectiveList = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList
    if (effectiveList.length === 0) {
      return this.radioQueueList[0] ?? null
    }

    const nextIndex = this.currentSourceIndex + 1
    if (nextIndex < effectiveList.length) {
      return effectiveList[nextIndex] ?? null
    }

    if (this.repeatModeSetting === 'ALL' && effectiveList.length > 0) {
      return effectiveList[0] ?? null
    }

    if (this.radioQueueList.length > 0) {
      return this.radioQueueList[0] ?? null
    }

    return null
  }

  public next(): { track: Track | null; shouldStop: boolean } {
    if (this.pauseAtEndOfItemsSetting) {
      return { track: this.activeTrack, shouldStop: true }
    }

    if (this.repeatModeSetting === 'ONE' && this.activeTrack) {
      return { track: this.activeTrack, shouldStop: false }
    }

    // 1. Up Next items have highest priority
    if (this.upNextList.length > 0) {
      if (this.activeTrack && !this.pauseAtEndOfItemsSetting) {
        this.historyList.push(this.activeTrack)
      }
      const nextFromUpNext = this.upNextList.shift()!
      this.activeTrack = nextFromUpNext
      this.activeTrackSource = 'upNext'
      return { track: nextFromUpNext, shouldStop: false }
    }

    // 2. Source queue
    const effectiveList = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList

    if (effectiveList.length > 0) {
      const nextIndex = this.currentSourceIndex + 1
      if (nextIndex < effectiveList.length) {
        if (this.activeTrack && !this.pauseAtEndOfItemsSetting) {
          this.historyList.push(this.activeTrack)
        }
        this.currentSourceIndex = nextIndex
        this.activeTrack = effectiveList[nextIndex] ?? null
        this.activeTrackSource = 'sourceQueue'
        return { track: this.activeTrack, shouldStop: false }
      }

      if (this.repeatModeSetting === 'ALL') {
        if (this.activeTrack && !this.pauseAtEndOfItemsSetting) {
          this.historyList.push(this.activeTrack)
        }
        this.currentSourceIndex = 0
        this.activeTrack = effectiveList[0] ?? null
        this.activeTrackSource = 'sourceQueue'
        return { track: this.activeTrack, shouldStop: false }
      }
    }

    // 3. Radio queue fallback
    if (this.radioQueueList.length > 0) {
      if (this.activeTrack && !this.pauseAtEndOfItemsSetting) {
        this.historyList.push(this.activeTrack)
      }
      const nextFromRadio = this.radioQueueList.shift()!
      this.activeTrack = nextFromRadio
      this.activeTrackSource = 'radio'
      return { track: nextFromRadio, shouldStop: false }
    }

    // Queue exhausted with repeat OFF and no radio: retain final track metadata in place
    return { track: null, shouldStop: true }
  }

  public previous(currentTimeSeconds: number): { track: Track | null; seekToZero: boolean } {
    if (currentTimeSeconds >= 3.0 || this.historyList.length === 0) {
      return { track: this.activeTrack, seekToZero: true }
    }

    const previousTrack = this.historyList.pop()!

    // Only return to upNext if the track was playing from upNext
    if (this.activeTrack && this.activeTrackSource === 'upNext') {
      this.upNextList.unshift(this.activeTrack)
    }

    this.activeTrack = previousTrack

    const effectiveList = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList
    const matchedIndex = effectiveList.findIndex((item) => item.id === previousTrack.id)
    if (matchedIndex !== -1) {
      this.currentSourceIndex = matchedIndex
      this.activeTrackSource = 'sourceQueue'
    } else {
      this.activeTrackSource = 'direct'
    }

    return { track: previousTrack, seekToZero: false }
  }

  public snapshotQueue(currentTimeSeconds = 0): QueueSnapshot {
    return {
      id: `snapshot_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      timestamp: Date.now(),
      history: this.historyList.map(cloneTrack),
      currentTrack: this.activeTrack ? cloneTrack(this.activeTrack) : null,
      upNext: this.upNextList.map(cloneTrack),
      sourceQueue: this.sourceQueueList.map(cloneTrack),
      radioQueue: this.radioQueueList.map(cloneTrack),
      shuffledSourceQueue: this.shuffledList.map(cloneTrack),
      currentTime: currentTimeSeconds,
      activeDeck: this.currentActiveDeck,
      repeatMode: this.repeatModeSetting,
      isShuffle: this.shuffleEnabled,
      pauseAtEndOfItems: this.pauseAtEndOfItemsSetting,
    }
  }

  public restoreQueue(snapshot: QueueSnapshot): Track | null {
    this.historyList = snapshot.history.map(cloneTrack)
    this.activeTrack = snapshot.currentTrack ? cloneTrack(snapshot.currentTrack) : null
    this.upNextList = snapshot.upNext.map(cloneTrack)
    this.sourceQueueList = snapshot.sourceQueue.map(cloneTrack)
    this.radioQueueList = snapshot.radioQueue.map(cloneTrack)
    this.shuffledList = snapshot.shuffledSourceQueue.map(cloneTrack)
    this.currentActiveDeck = snapshot.activeDeck
    this.repeatModeSetting = snapshot.repeatMode
    this.shuffleEnabled = snapshot.isShuffle
    this.pauseAtEndOfItemsSetting = snapshot.pauseAtEndOfItems

    if (this.activeTrack) {
      const list = this.shuffleEnabled ? this.shuffledList : this.sourceQueueList
      const index = list.findIndex((track) => track.id === this.activeTrack?.id)
      this.currentSourceIndex = index !== -1 ? index : 0
      this.activeTrackSource = index !== -1 ? 'sourceQueue' : 'direct'
    } else {
      this.currentSourceIndex = -1
    }

    return this.activeTrack
  }

  private computeShuffledQueue(pinnedTrack: Track): void {
    const idx =
      this.currentSourceIndex >= 0 && this.currentSourceIndex < this.sourceQueueList.length
        ? this.currentSourceIndex
        : this.sourceQueueList.findIndex((t) => t.id === pinnedTrack.id)

    const remaining =
      idx !== -1
        ? [...this.sourceQueueList.slice(0, idx), ...this.sourceQueueList.slice(idx + 1)]
        : this.sourceQueueList.filter((t) => t.id !== pinnedTrack.id)

    const shuffledRemaining = this.fisherYatesShuffle(remaining)
    this.shuffledList = [pinnedTrack, ...shuffledRemaining]
    this.currentSourceIndex = 0
  }

  private fisherYatesShuffle(array: readonly Track[]): Track[] {
    const result = [...array]
    for (let i = result.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      const temp = result[i]!
      result[i] = result[j]!
      result[j] = temp
    }
    return result
  }
}
