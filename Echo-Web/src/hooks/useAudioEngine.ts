import { useEffect, useState } from 'react'
import { AudioEngine } from '../engine/AudioEngine.ts'
import type { AudioEngineState, RepeatMode, Track } from '../types/audio.ts'
import { getCachedTrack, useEchoStore } from '../stores/useEchoStore.ts'

export function useAudioEngine() {
  const engine = AudioEngine.getInstance()
  const [state, setState] = useState<AudioEngineState>(() => engine.getState())

  useEffect(() => {
    const unsubscribe = engine.subscribe((updatedState) => {
      setState(updatedState)
    })
    return () => {
      unsubscribe()
    }
  }, [engine])

  return {
    engine,
    state,
    isPlaying: state.playbackState === 'PLAYING',
    isBuffering: state.playbackState === 'BUFFERING',
    currentTrack: state.currentTrack,
    currentTime: state.currentTime,
    duration: state.duration,
    bufferedTime: state.bufferedTime,
    volume: state.volume,
    muted: state.muted,
    repeatMode: state.repeatMode,
    isShuffle: state.isShuffle,
    activeDeck: state.activeDeck,
    upNext: state.upNext,
    sourceQueue: state.sourceQueue,
    sourceUpcoming: state.sourceUpcoming,
    history: state.history,
    radioQueue: state.radioQueue,
    stereoWidth: state.stereoWidth,
    volumeNormalization: state.volumeNormalization,
    crossfadeDuration: state.crossfadeDuration,
    equalizerGains: state.equalizerGains,
    playbackRate: state.playbackRate ?? 1.0,
    sleepTimerRemaining: state.sleepTimerRemaining,
    sleepTimerEndOfTrack: state.sleepTimerEndOfTrack,

    play: (track?: Track) => engine.play(track),
    playQueue: (tracks: Track[], startIndex?: number) => engine.playQueue(tracks, startIndex),
    pause: () => engine.pause(),
    togglePlayback: () => engine.togglePlayback(),
    next: () => engine.next(),
    previous: () => engine.previous(),
    seek: (seconds: number) => engine.seek(seconds),
    seekTo: (seconds: number) => engine.seekTo(seconds),
    getCurrentTime: () => engine.getCurrentTime(),
    playNext: (track: Track) => engine.playNext(track),
    addToQueue: (track: Track) => engine.addToQueue(track),
    removeFromQueue: (index: number) => engine.removeFromQueue(index),
    moveQueueItem: (from: number, to: number) => engine.moveQueueItem(from, to),
    removeSourceQueueItem: (index: number) => engine.removeSourceQueueItem(index),
    moveSourceQueueItem: (from: number, to: number) => engine.moveSourceQueueItem(from, to),
    clearQueue: () => engine.clearQueue(),
    clearSourceUpcoming: () => engine.clearSourceUpcoming(),
    clearEntireQueue: () => engine.clearEntireQueue(),
    getDetailedQueue: () => engine.getDetailedQueue(),
    setVolume: (volume: number) => engine.setVolume(volume),
    setMuted: (muted: boolean) => engine.setMuted(muted),
    setShuffle: (shuffle: boolean) => engine.setShuffle(shuffle),
    setRepeatMode: (mode: RepeatMode) => engine.setRepeatMode(mode),
    setCrossfadeDuration: (seconds: number) => engine.setCrossfadeDuration(seconds),
    setVolumeNormalization: (enabled: boolean) => engine.setVolumeNormalization(enabled),
    setStereoWidth: (width: number) => engine.setStereoWidth(width),
    setEqualizerGain: (band: number, gainDb: number) => engine.setEqualizerGain(band, gainDb),
    setEqualizerBands: (gains: number[]) => engine.setEqualizerBands(gains),
    setEqualizerPreset: (name: string) => engine.setEqualizerPreset(name),
    resetEqualizer: () => engine.resetEqualizer(),
    snapshotQueue: () => engine.snapshotQueue(),
    restoreQueue: (snapshot: Parameters<typeof engine.restoreQueue>[0], resume?: boolean) =>
      engine.restoreQueue(snapshot, resume),
    setPauseAtEndOfItems: (enable: boolean) => engine.setPauseAtEndOfItems(enable),
    getVisualizerData: () => engine.getVisualizerData(),
    getFrequencyData: (outputArray?: Uint8Array<ArrayBuffer>) =>
      engine.getFrequencyData(outputArray),
    getBeatEnergy: () => engine.getBeatEnergy(),
    startSleepTimer: (minutesOrOption: number | 'end_of_track') =>
      engine.startSleepTimer(minutesOrOption),
    cancelSleepTimer: () => engine.cancelSleepTimer(),
    setPlaybackRate: (rate: number) => engine.setPlaybackRate(rate),
    setAudioOutput: (sinkId: string) => engine.setAudioOutput(sinkId),
    getAudioTelemetry: () => engine.getAudioTelemetry(),
  }
}

export function bridgeAudioEngineWithStore(): () => void {
  const engine = AudioEngine.getInstance()
  const echoStore = useEchoStore.getState()

  const initialRepeat =
    echoStore.preferences.repeat === 'all'
      ? 'ALL'
      : echoStore.preferences.repeat === 'one'
        ? 'ONE'
        : 'OFF'

  engine.setRepeatMode(initialRepeat)
  engine.setShuffle(echoStore.preferences.shuffle)
  engine.setVolume(echoStore.preferences.volume / 100)
  engine.setMuted(echoStore.preferences.muted)
  engine.setVolumeNormalization(echoStore.preferences.volumeNormalization)

  const startingTrack = echoStore.currentTrack || getCachedTrack(echoStore.currentTrackId)
  if (startingTrack && !engine.getState().currentTrack) {
    const queueTracks = echoStore.queueIds
      .map((id) => getCachedTrack(id))
      .filter((t): t is Track => Boolean(t))
    engine.setQueue([startingTrack, ...queueTracks], 0)
  }

  const unsubscribeEngine = engine.subscribe((engineState) => {
    const currentStore = useEchoStore.getState()

    if (engineState.currentTrack && engineState.currentTrack.id !== currentStore.currentTrackId) {
      useEchoStore.setState({
        currentTrack: engineState.currentTrack,
        currentTrackId: engineState.currentTrack.id,
      })
    }

    const isPlaying = engineState.playbackState === 'PLAYING'
    if (isPlaying !== currentStore.isPlaying) {
      useEchoStore.setState({ isPlaying })
    }

    const upNextIds = engineState.upNext.map((t) => t.id)
    if (
      upNextIds.length !== currentStore.queueIds.length ||
      upNextIds.some((id, idx) => id !== currentStore.queueIds[idx])
    ) {
      useEchoStore.setState({ queueIds: upNextIds })
    }
  })

  return () => {
    unsubscribeEngine()
  }
}
