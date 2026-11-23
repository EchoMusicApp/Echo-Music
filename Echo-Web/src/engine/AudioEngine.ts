import type {
  AudioEngineConfig,
  AudioEngineEvent,
  AudioEngineEventMap,
  AudioEngineState,
  DeckId,
  PlaybackState,
  QueueSnapshot,
  RepeatMode,
  SponsorSegment,
  Track,
} from '../types/audio'
import type { StreamResolutionResult } from '../types/innertube'
import { DeckController } from './DeckController'
import { MediaSession } from './MediaSession'
import { QueueManager } from './QueueManager'
import { SponsorBlock } from './SponsorBlock'
import { WebAudioGraph } from './WebAudioGraph'
import { db } from '../services/db/echoDatabase'
import { useEchoStore } from '../stores/useEchoStore'
import { recordListeningEvent } from '../services/recommendations/localTasteEngine'
import { updateLastFmNowPlaying, scrobbleLastFmTrack } from '../services/lastfm/lastfmService'
import { updateDiscordPresence, clearDiscordPresence } from '../services/discord/discordService'

export class AudioEngine {
  private static instance: AudioEngine | null = null

  private readonly graph: WebAudioGraph
  private readonly deckController: DeckController
  private readonly queueManager: QueueManager
  private readonly sponsorBlock: SponsorBlock
  private readonly mediaSession: MediaSession

  private listeners = new Set<(state: AudioEngineState) => void>()
  private eventHandlers = new Map<AudioEngineEvent, Set<(...args: unknown[]) => void>>()

  private volumeLevel = 0.8
  private isMuted = false
  private autoRadioEnabled = true

  // LRU-bounded stream URL cache and registry of created blob object URLs, which are revoked
  // when evicted so IndexedDB blobs are not pinned in memory.
  private streamCache = new Map<string, string>()
  private static readonly STREAM_CACHE_MAX = 200
  private objectUrls = new Set<string>()
  private isFetchingRadioContinuation = false
  private currentLoadRequestId = 0

  private userInteractionCleanup: (() => void) | null = null

  private sleepTimerInterval: number | null = null
  private sleepTimerEndsAt: number | null = null
  private sleepTimerFadeActive = false
  private sleepTimerEndOfTrack = false
  private playbackRate = 1.0
  private lastStreamFormat: StreamResolutionResult['format'] | null = null
  private consecutiveErrorCount = 0
  private lastPodcastSaveMs = 0
  private lastFmScrobbled = false
  private loudnessMeasured = false

  private constructor(config: Partial<AudioEngineConfig> = {}) {
    this.graph = new WebAudioGraph({
      volumeNormalization: config.volumeNormalization ?? false,
      stereoWidth: config.stereoWidth ?? 1.0,
    })

    this.deckController = new DeckController(this.graph, {
      crossfadeDuration: config.crossfadeDuration ?? 3.0,
      prebufferWindowMs: config.prebufferWindowMs ?? 15000,
    })

    this.queueManager = new QueueManager()

    const sponsorBlockOptions: { enabled?: boolean; categories?: string[] } = {}
    if (typeof config.sponsorBlockEnabled === 'boolean') {
      sponsorBlockOptions.enabled = config.sponsorBlockEnabled
    }
    if (config.sponsorBlockCategories) {
      sponsorBlockOptions.categories = config.sponsorBlockCategories
    }
    this.sponsorBlock = new SponsorBlock(sponsorBlockOptions)

    this.mediaSession = new MediaSession({
      onPlay: () => void this.play(),
      onPause: () => this.pause(),
      onPreviousTrack: () => void this.previous(),
      onNextTrack: () => void this.next(),
      onSeekTo: (seconds) => this.seek(seconds),
      onSeekBackward: (offset) => this.seek(Math.max(0, this.getCurrentTime() - offset)),
      onSeekForward: (offset) =>
        this.seek(Math.min(this.deckController.getDuration(), this.getCurrentTime() + offset)),
    })

    this.autoRadioEnabled = config.autoRadio ?? true

    this.wireSubsystems()
    this.setupUserInteractionListener()
  }

  public static getInstance(config?: Partial<AudioEngineConfig>): AudioEngine {
    if (!AudioEngine.instance) {
      AudioEngine.instance = new AudioEngine(config)
    }
    return AudioEngine.instance
  }

  public getState(): AudioEngineState {
    const detailed = this.queueManager.getDetailedQueue()
    return {
      playbackState: this.deckController.state,
      currentTrack: this.queueManager.currentTrack,
      currentTime: this.deckController.getCurrentTime(),
      duration: this.deckController.getDuration(),
      bufferedTime: this.deckController.getBufferedTime(),
      volume: this.volumeLevel,
      muted: this.isMuted,
      repeatMode: this.queueManager.repeatMode,
      isShuffle: this.queueManager.isShuffle,
      activeDeck: this.deckController.activeDeck,
      upNext: this.queueManager.upNext,
      sourceQueue: this.queueManager.sourceQueue,
      sourceUpcoming: detailed.sourceUpcoming,
      history: this.queueManager.history,
      radioQueue: this.queueManager.radioQueue,
      equalizerGains: this.graph.equalizer.getGains(),
      stereoWidth: this.graph.stereoWidener.getWidth(),
      volumeNormalization: this.graph.getVolumeNormalization(),
      crossfadeDuration: this.deckController.crossfadeDuration,
      playbackRate: this.playbackRate,
      sleepTimerRemaining: this.getSleepTimerRemaining(),
      sleepTimerEndOfTrack: this.sleepTimerEndOfTrack,
    }
  }

  public subscribe(listener: (state: AudioEngineState) => void): () => void {
    this.listeners.add(listener)
    listener(this.getState())
    return () => {
      this.listeners.delete(listener)
    }
  }

  public on<K extends AudioEngineEvent>(event: K, handler: AudioEngineEventMap[K]): () => void {
    let handlers = this.eventHandlers.get(event)
    if (!handlers) {
      handlers = new Set()
      this.eventHandlers.set(event, handlers)
    }
    const genericHandler = handler as unknown as (...args: unknown[]) => void
    handlers.add(genericHandler)
    return () => {
      handlers?.delete(genericHandler)
    }
  }

  public async play(track?: Track): Promise<void> {
    if (track) {
      this.queueManager.playTrackDirectly(track)
      await this.loadAndPlayTrack(track)
      return
    }

    const current = this.queueManager.currentTrack
    if (!current) {
      const nextResult = this.queueManager.next()
      if (nextResult.track) {
        await this.loadAndPlayTrack(nextResult.track)
      }
      return
    }

    await this.deckController.play()
    this.emitState()
  }

  public pause(): void {
    this.deckController.pause()
    this.mediaSession.updatePlaybackState('paused')
    this.emitState()
  }

  public async togglePlayback(): Promise<void> {
    if (this.deckController.state === 'PLAYING') {
      this.pause()
    } else {
      await this.play()
    }
  }

  public async next(): Promise<void> {
    const { track, shouldStop } = this.queueManager.next()
    this.emit('queueChange')

    if (!track || shouldStop) {
      this.pause()
      this.emitState()
      return
    }

    await this.loadAndPlayTrack(track)
    this.checkAutoRadio()
  }

  public async previous(): Promise<void> {
    const currentTime = this.deckController.getCurrentTime()
    const { track, seekToZero } = this.queueManager.previous(currentTime)
    this.emit('queueChange')

    if (seekToZero) {
      this.seek(0)
      return
    }

    if (track) {
      await this.loadAndPlayTrack(track)
    }
  }

  public getCurrentTime(): number {
    return this.deckController.getCurrentTime()
  }

  public seek(seconds: number): void {
    this.deckController.seek(seconds)
    this.mediaSession.updatePositionState(this.deckController.getDuration(), seconds)
    this.emitState()
  }

  public seekTo(positionSeconds: number): void {
    this.seek(positionSeconds)
  }

  public playNext(track: Track): void {
    this.queueManager.playNext(track)
    this.emit('queueChange')
    this.emitState()
  }

  public addToQueue(track: Track): void {
    this.queueManager.addToQueue(track)
    this.emit('queueChange')
    this.emitState()
  }

  public removeFromQueue(index: number): void {
    this.queueManager.removeUpNext(index)
    this.emit('queueChange')
    this.emitState()
  }

  public moveQueueItem(from: number, to: number): void {
    this.queueManager.moveUpNext(from, to)
    this.emit('queueChange')
    this.emitState()
  }

  public clearQueue(): void {
    this.queueManager.clearUpNext()
    this.emit('queueChange')
    this.emitState()
  }

  public removeSourceQueueItem(index: number): void {
    this.queueManager.removeSourceQueueItem(index)
    this.emit('queueChange')
    this.emitState()
  }

  public moveSourceQueueItem(from: number, to: number): void {
    this.queueManager.moveSourceQueueItem(from, to)
    this.emit('queueChange')
    this.emitState()
  }

  public clearSourceUpcoming(): void {
    this.queueManager.clearSourceUpcoming()
    this.emit('queueChange')
    this.emitState()
  }

  public clearEntireQueue(): void {
    this.queueManager.clearEntireQueue()
    this.emit('queueChange')
    this.emitState()
  }

  public getDetailedQueue() {
    return this.queueManager.getDetailedQueue()
  }

  public setQueue(tracks: Track[], startIndex = 0): void {
    const track = this.queueManager.setSourceQueue(tracks, startIndex)
    this.emit('queueChange')
    if (track) {
      void this.loadAndPlayTrack(track)
    }
  }

  public playQueue(tracks: Track[], startIndex = 0): void {
    this.setQueue(tracks, startIndex)
  }

  public setVolume(volume: number): void {
    this.volumeLevel = Math.max(0, Math.min(1, volume))
    this.graph.setMasterVolume(this.volumeLevel, this.isMuted)
    this.emit('volumeChange', this.volumeLevel, this.isMuted)
    this.emitState()
  }

  public setMuted(muted: boolean): void {
    this.isMuted = muted
    this.graph.setMasterVolume(this.volumeLevel, this.isMuted)
    this.emit('volumeChange', this.volumeLevel, this.isMuted)
    this.emitState()
  }

  public setShuffle(enabled: boolean): void {
    this.queueManager.setShuffle(enabled)
    this.mediaSession.updateShuffleActive(enabled)
    this.emit('queueChange')
    this.emitState()
  }

  public setRepeatMode(mode: RepeatMode): void {
    this.queueManager.setRepeatMode(mode)
    this.mediaSession.updateRepeatMode(mode.toLowerCase() as 'off' | 'all' | 'one')
    this.emit('queueChange')
    this.emitState()
  }

  public setCrossfadeDuration(seconds: number): void {
    this.deckController.setCrossfadeDuration(seconds)
    this.emitState()
  }

  public setVolumeNormalization(enabled: boolean): void {
    this.graph.setVolumeNormalization(enabled)
    this.emitState()
  }

  public setStereoWidth(width: number): void {
    this.graph.stereoWidener.setWidth(width)
    this.emitState()
  }

  public setEqualizerGain(bandIndex: number, gainDb: number): void {
    this.graph.equalizer.setGain(bandIndex, gainDb)
    this.emitState()
  }

  public setEqualizerBands(gains: number[]): void {
    this.graph.equalizer.setAllGains(gains)
    this.emitState()
  }

  public setEqualizerPreset(presetName: string): void {
    this.graph.equalizer.setPreset(presetName)
    this.emitState()
  }

  public resetEqualizer(): void {
    this.graph.equalizer.reset()
    this.emitState()
  }

  public startSleepTimer(minutesOrOption: number | 'end_of_track'): void {
    this.cancelSleepTimer()
    if (minutesOrOption === 'end_of_track') {
      this.sleepTimerEndOfTrack = true
      this.emitState()
      return
    }

    const durationMs = minutesOrOption * 60 * 1000
    this.sleepTimerEndsAt = Date.now() + durationMs

    this.sleepTimerInterval = window.setInterval(() => {
      if (!this.sleepTimerEndsAt) return
      const remaining = Math.max(0, this.sleepTimerEndsAt - Date.now())

      // Fade out audio gradually in the final 30 seconds
      if (remaining <= 30000 && !this.sleepTimerFadeActive) {
        this.sleepTimerFadeActive = true
        let step = 0
        const steps = 30
        const startVol = this.volumeLevel
        const fadeInterval = window.setInterval(() => {
          step++
          const curVol = Math.max(0, startVol * (1 - step / steps))
          this.graph.setMasterVolume(curVol, this.isMuted)
          if (step >= steps) {
            window.clearInterval(fadeInterval)
          }
        }, 1000)
      }

      if (remaining <= 0) {
        this.pause()
        this.cancelSleepTimer()
        this.graph.setMasterVolume(this.volumeLevel, this.isMuted)
      } else {
        this.emitState()
      }
    }, 1000)

    this.emitState()
  }

  public cancelSleepTimer(): void {
    if (this.sleepTimerInterval !== null) {
      window.clearInterval(this.sleepTimerInterval)
      this.sleepTimerInterval = null
    }
    this.sleepTimerEndsAt = null
    this.sleepTimerFadeActive = false
    this.sleepTimerEndOfTrack = false
    this.graph.setMasterVolume(this.volumeLevel, this.isMuted)
    this.emitState()
  }

  public getSleepTimerRemaining(): number | null {
    if (!this.sleepTimerEndsAt) return null
    return Math.max(0, Math.round((this.sleepTimerEndsAt - Date.now()) / 1000))
  }

  public setPlaybackRate(rate: number): void {
    this.playbackRate = Math.max(0.5, Math.min(2.0, rate))
    this.deckController.setPlaybackRate(this.playbackRate)
    this.emitState()
  }

  public getPlaybackRate(): number {
    return this.playbackRate
  }

  public async setAudioOutput(sinkId: string): Promise<boolean> {
    return await this.deckController.setAudioSink(sinkId)
  }

  public getAudioTelemetry(): {
    codec: string
    bitrate: string
    sampleRate: string
    bitDepth: string
  } {
    const current = this.queueManager.currentTrack
    const ctx = this.graph.context
    const sampleRateKhz = (ctx.sampleRate / 1000).toFixed(1)
    const isLocal = current?.id.startsWith('local_')

    // Streamed tracks report what the server actually resolved; nothing is overstated.
    const isOpus = (this.lastStreamFormat?.mimeType ?? '').toLowerCase().includes('opus')
    const codec = isLocal ? 'Local Audio' : isOpus ? 'Opus (WebM)' : 'AAC (MP4)'
    const bitrateKbps = this.lastStreamFormat
      ? Math.round(this.lastStreamFormat.bitrate / 1000)
      : null
    const bitrate = isLocal ? 'Source bitrate' : bitrateKbps ? `~${bitrateKbps} kbps` : 'Unresolved'
    const bitDepth = '32-bit Float DSP'

    return {
      codec,
      bitrate,
      sampleRate: `${sampleRateKhz} kHz`,
      bitDepth,
    }
  }

  public snapshotQueue(): QueueSnapshot {
    const time = this.deckController.getCurrentTime()
    return this.queueManager.snapshotQueue(time)
  }

  public async restoreQueue(snapshot: QueueSnapshot, resume = true): Promise<void> {
    const track = this.queueManager.restoreQueue(snapshot)
    this.emit('queueChange')

    if (track) {
      await this.loadAndPlayTrack(track, resume)
      if (snapshot.currentTime > 0) {
        this.seek(snapshot.currentTime)
      }
    }
  }

  public setPauseAtEndOfItems(enable: boolean): void {
    this.queueManager.setPauseAtEndOfItems(enable)
  }

  public getVisualizerData(): Uint8Array<ArrayBuffer> {
    return this.graph.getFrequencyData()
  }

  public getFrequencyData(outputArray?: Uint8Array<ArrayBuffer>): Uint8Array<ArrayBuffer> {
    return this.graph.getFrequencyData(outputArray)
  }

  public getBeatEnergy(): number {
    return this.graph.getBeatEnergy()
  }

  public destroy(): void {
    if (this.userInteractionCleanup) {
      this.userInteractionCleanup()
      this.userInteractionCleanup = null
    }

    this.sponsorBlock.destroy()
    this.mediaSession.destroy()
    this.deckController.destroy()
    this.graph.destroy()
    this.listeners.clear()
    this.eventHandlers.clear()
    AudioEngine.instance = null
  }

  private wireSubsystems(): void {
    this.sponsorBlock.attachPlayer(
      () => this.deckController.getCurrentTime(),
      (seconds) => this.seek(seconds),
    )

    this.sponsorBlock.onSegmentSkipped = (seg: SponsorSegment) => {
      this.emit('segmentSkip', seg)
    }

    this.deckController.onPlaybackStateChange = (state: PlaybackState) => {
      this.emit('playbackStateChange', state)
      if (state === 'PLAYING') {
        this.mediaSession.updatePlaybackState('playing')
      } else if (state === 'PAUSED' || state === 'IDLE' || state === 'ENDED') {
        this.mediaSession.updatePlaybackState('paused')
      }
      this.emitState()

      const current = this.queueManager.currentTrack
      const prefs = useEchoStore.getState().preferences
      if (current && prefs.discordRpc) {
        if (state === 'PLAYING') {
          void updateDiscordPresence(current, true, this.deckController.getCurrentTime())
        } else if (state === 'PAUSED') {
          void updateDiscordPresence(current, false, this.deckController.getCurrentTime())
        }
      }
    }

    this.deckController.onTimeUpdate = (current, duration) => {
      this.emit('timeUpdate', current, duration)
      this.mediaSession.updatePositionState(duration, current)
      this.emitState()

      const currentTrack = this.queueManager.currentTrack
      if (!currentTrack) return
      const prefs = useEchoStore.getState().preferences

      // True Loudness normalization measurement (first 5s)
      if (prefs.volumeNormalization && !this.loudnessMeasured && current > 0.5 && current < 5.0) {
        const rms = this.graph.calculateRMS()
        if (rms > 0.005) {
          this.loudnessMeasured = true
          this.graph.setTrackLoudnessTarget(currentTrack.id, rms)
        }
      }

      // Silence skipping (leading silence under 3s)
      if (prefs.skipSilence && current < 2.5 && this.graph.isSilent()) {
        this.seek(Math.min(current + 0.5, 3.0))
      }

      // Last.fm Scrobble rule: play 50% or 240 seconds, whichever comes first
      if (
        !this.lastFmScrobbled &&
        prefs.lastfmScrobbling &&
        prefs.lastfmSession?.sessionKey &&
        duration > 30 &&
        (current >= 240 || (duration > 0 && current / duration >= 0.5))
      ) {
        this.lastFmScrobbled = true
        void scrobbleLastFmTrack(currentTrack, prefs.lastfmSession.sessionKey)
      }

      // Podcast progress persistence (throttled 5s)
      const now = Date.now()
      if (now - this.lastPodcastSaveMs >= 5000 && duration > 0) {
        this.lastPodcastSaveMs = now
        const isPodcast = Boolean(
          (currentTrack.durationSeconds && currentTrack.durationSeconds > 600) ||
          currentTrack.albumTitle?.toLowerCase().includes('podcast'),
        )
        if (isPodcast) {
          const episodeId = currentTrack.videoId || currentTrack.id
          const posMs = Math.round(current * 1000)
          const durMs = Math.round(duration * 1000)
          void db.podcastProgress
            .put({
              episodeId,
              showTitle: currentTrack.artistName || 'Podcast',
              episodeTitle: currentTrack.title || 'Episode',
              positionMs: posMs,
              durationMs: durMs,
              completed: durMs > 0 && posMs / durMs >= 0.95,
              dismissed: false,
              updatedAt: now,
            })
            .catch(() => {})
        }
      }
    }

    this.deckController.onBufferedUpdate = (buffered) => {
      this.emit('bufferedUpdate', buffered)
    }

    this.deckController.onTrackEnded = () => {
      this.consecutiveErrorCount = 0
      const current = this.queueManager.currentTrack
      if (current) {
        void recordListeningEvent({
          track: current,
          durationPlayedMs: Math.round(this.deckController.getCurrentTime() * 1000),
          trackDurationMs:
            Math.round(this.deckController.getDuration() * 1000) ||
            (current.durationSeconds ? current.durationSeconds * 1000 : 180000),
          completed: true,
          skipped: false,
        })

        const episodeId = current.videoId || current.id
        if (episodeId) {
          void db.podcastProgress
            .update(episodeId, {
              completed: true,
              updatedAt: Date.now(),
            })
            .catch(() => {})
        }
      }

      const prefs = useEchoStore.getState().preferences
      if (prefs.discordRpc) {
        void clearDiscordPresence()
      }

      if (this.sleepTimerEndOfTrack) {
        this.pause()
        this.cancelSleepTimer()
        return
      }

      void this.next()
    }

    this.deckController.onPrebufferNeeded = () => {
      void this.handlePrebuffer()
    }

    this.deckController.onCrossfadeStart = (_fromDeck: DeckId, toDeck: DeckId) => {
      this.queueManager.setActiveDeck(toDeck)
      const nextTrack = this.deckController.getStandbyTrack()
      if (nextTrack) {
        this.mediaSession.updateTrack(nextTrack)
        this.emit('trackChange', nextTrack)
      }
      this.emit('deckSwap', toDeck)
      this.emitState()
    }

    this.deckController.onCrossfadeComplete = (newActiveDeck: DeckId) => {
      this.queueManager.setActiveDeck(newActiveDeck)
      this.emit('deckSwap', newActiveDeck)
      this.emitState()
    }

    this.deckController.onError = (error) => {
      const current = this.queueManager.currentTrack
      if (current) {
        this.streamCache.delete(current.id)
        current.streamUrl = undefined
      }
      this.emit('error', error)
      this.emitState()

      // One retry with a fresh resolve; a second failure skips to the next track so a dead
      // source cannot stall the queue.
      this.consecutiveErrorCount++
      if (this.consecutiveErrorCount >= 2) {
        this.consecutiveErrorCount = 0
        void this.next()
      } else if (current) {
        void this.loadAndPlayTrack(current)
      }
    }
  }

  private async loadAndPlayTrack(track: Track, autoPlay = true): Promise<void> {
    const requestId = ++this.currentLoadRequestId
    this.mediaSession.updateTrack(track)
    this.emit('trackChange', track)

    const streamUrl = await this.resolveStreamUrl(track)
    if (requestId !== this.currentLoadRequestId) {
      return
    }

    await this.deckController.loadActive(track, streamUrl, autoPlay)
    if (requestId !== this.currentLoadRequestId) {
      return
    }

    this.consecutiveErrorCount = 0
    this.lastFmScrobbled = false
    this.loudnessMeasured = false

    this.queueManager.setActiveDeck(this.deckController.activeDeck)

    const prefs = useEchoStore.getState().preferences

    // Last.fm Now Playing update
    if (prefs.lastfmScrobbling && prefs.lastfmSession?.sessionKey) {
      void updateLastFmNowPlaying(track, prefs.lastfmSession.sessionKey)
    }

    // Discord Rich Presence update
    if (prefs.discordRpc) {
      void updateDiscordPresence(track, autoPlay, 0)
    }

    // Podcast position resume check
    const episodeId = track.videoId || track.id
    if (episodeId) {
      try {
        const progress = await db.podcastProgress.get(episodeId)
        if (
          progress &&
          !progress.completed &&
          progress.positionMs > 5000 &&
          progress.positionMs / progress.durationMs < 0.95
        ) {
          this.seek(progress.positionMs / 1000)
        }
      } catch {
        // Non-fatal
      }
    }

    if (track.sponsorBlockSegments && track.sponsorBlockSegments.length > 0) {
      this.sponsorBlock.setManualSegments(
        track.sponsorBlockSegments.map((s) => ({
          category: 'music_offtopic',
          start: s.start,
          end: s.end,
        })),
      )
    } else {
      const vid = track.videoId || track.id
      if (vid && /^[a-zA-Z0-9_-]{11}$/.test(vid)) {
        void this.sponsorBlock.loadSegmentsForVideo(vid)
      }
    }

    this.emitState()
  }

  private async handlePrebuffer(): Promise<void> {
    const nextTrack = this.queueManager.peekNext()
    if (!nextTrack) {
      this.checkAutoRadio()
      return
    }

    try {
      const streamUrl = await this.resolveStreamUrl(nextTrack)
      this.deckController.prebufferStandby(nextTrack, streamUrl)
    } catch {
      // Prebuffer failure is non-fatal
    }
  }

  private createObjectUrl(blob: Blob): string {
    const url = URL.createObjectURL(blob)
    this.objectUrls.add(url)
    // Keep the registry bounded: revoke the oldest URL if it grows past twice the stream
    // cache cap (a URL may still be attached to the active deck after its cache entry goes).
    if (this.objectUrls.size > AudioEngine.STREAM_CACHE_MAX * 2) {
      for (const old of this.objectUrls) {
        this.objectUrls.delete(old)
        URL.revokeObjectURL(old)
        break
      }
    }
    return url
  }

  private isUrlExpired(url: string): boolean {
    try {
      const match = /[?&]expire=(\d+)/i.exec(url)
      if (match?.[1]) {
        const expireSeconds = Number.parseInt(match[1], 10)
        if (Number.isFinite(expireSeconds) && Date.now() / 1000 >= expireSeconds - 30) {
          return true
        }
      }
    } catch {
      // Ignore URL parsing errors
    }
    return false
  }

  private async resolveStreamUrl(track: Track): Promise<string> {
    if (track.streamUrl && !this.isUrlExpired(track.streamUrl)) {
      return track.streamUrl
    }

    // Check offline storage: local imported files
    if (track.id.startsWith('local_')) {
      const localRec = await db.localFiles.get(track.id)
      if (localRec?.audioBlob) {
        const objectUrl = this.createObjectUrl(localRec.audioBlob)
        track.streamUrl = objectUrl
        return objectUrl
      }
    }

    const cached = this.streamCache.get(track.id)
    if (cached && !this.isUrlExpired(cached)) {
      // LRU refresh
      this.streamCache.delete(track.id)
      this.streamCache.set(track.id, cached)
      track.streamUrl = cached
      return cached
    }

    const videoId = track.videoId || track.id
    if (videoId && /^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
      try {
        const prefs = useEchoStore.getState().preferences
        // Data saver forces the smallest stream regardless of the chosen quality tier.
        const quality = prefs.dataSaver ? 'low' : prefs.audioQuality
        const res = await fetch(
          `/api/stream/resolve?videoId=${encodeURIComponent(videoId)}&quality=${encodeURIComponent(quality)}`,
        )
        if (res.ok) {
          const data = (await res.json()) as StreamResolutionResult
          if (data.streamUrl) {
            this.lastStreamFormat = data.format
            this.streamCache.set(track.id, data.streamUrl)
            if (this.streamCache.size > AudioEngine.STREAM_CACHE_MAX) {
              const oldest = this.streamCache.keys().next().value
              if (oldest !== undefined) this.streamCache.delete(oldest)
            }
            track.streamUrl = data.streamUrl
            if (data.durationMs && !track.durationMs) {
              track.durationMs = data.durationMs
            }
            return data.streamUrl
          }
        }
      } catch (err) {
        throw new Error(
          `Failed to resolve live audio stream for "${track.title}": ${err instanceof Error ? err.message : String(err)}`,
        )
      }
    }

    throw new Error(
      `Could not resolve stream URL for track "${track.title}" (ID: ${track.id}). Network stream resolution is required.`,
    )
  }

  private checkAutoRadio(): void {
    if (!this.autoRadioEnabled || this.isFetchingRadioContinuation) return

    const current = this.queueManager.currentTrack
    if (!current) return

    const upNext = this.queueManager.upNext
    const radioQueue = this.queueManager.radioQueue

    if (upNext.length === 0 && radioQueue.length < 3) {
      void this.fetchRadioContinuation(current.videoId || current.id)
    }
  }

  private async fetchRadioContinuation(videoId: string): Promise<void> {
    if (this.isFetchingRadioContinuation || !videoId) return
    this.isFetchingRadioContinuation = true

    try {
      const res = await fetch(`/api/radio/continuation?videoId=${encodeURIComponent(videoId)}`)
      if (res.ok) {
        const data = (await res.json()) as { tracks: Track[] }
        if (Array.isArray(data.tracks) && data.tracks.length > 0) {
          this.queueManager.addRadioTracks(data.tracks)
          this.emit('queueChange')
          this.emitState()
        }
      }
    } catch {
      // Radio continuation fetch failures are non-fatal
    } finally {
      this.isFetchingRadioContinuation = false
    }
  }

  private setupUserInteractionListener(): void {
    const resumeContext = () => {
      void this.graph.resume()
      if (this.deckController.state === 'REQUIRES_USER_INTERACTION') {
        void this.deckController.play()
      }
    }

    window.addEventListener('pointerdown', resumeContext, { passive: true })
    window.addEventListener('keydown', resumeContext, { passive: true })

    this.userInteractionCleanup = () => {
      window.removeEventListener('pointerdown', resumeContext)
      window.removeEventListener('keydown', resumeContext)
    }
  }

  private emit<K extends AudioEngineEvent>(
    event: K,
    ...args: Parameters<AudioEngineEventMap[K]>
  ): void {
    const handlers = this.eventHandlers.get(event)
    if (handlers) {
      for (const handler of handlers) {
        handler(...args)
      }
    }
  }

  private emitState(): void {
    const state = this.getState()
    for (const listener of this.listeners) {
      listener(state)
    }
  }
}
