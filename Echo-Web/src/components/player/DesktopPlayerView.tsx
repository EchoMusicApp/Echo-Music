import { useEffect, useState, useCallback } from 'react'
import {
  ChevronDown,
  Heart,
  Maximize2,
  Minimize2,
  Pause,
  Play,
  Repeat,
  Shuffle,
  SkipBack,
  SkipForward,
  Sliders,
  Volume2,
  VolumeX,
  Sparkles,
  Moon,
  Speaker,
  Share2,
  Keyboard,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAudioEngine } from '../../hooks/useAudioEngine.ts'
import { useEchoStore, getCachedTrack } from '../../stores/useEchoStore.ts'
import { formatDuration } from '../../utils/format.ts'
import { artwork } from '../../utils/artwork.ts'
import { CanvasVideo } from './CanvasVideo.tsx'
import { LyricsView } from './LyricsView.tsx'
import { Visualizer } from './Visualizer.tsx'
import { EqualizerSheet } from './EqualizerSheet.tsx'
import { MusicDnaDrawer } from './MusicDnaDrawer.tsx'
import { SleepTimerPopover } from './SleepTimerPopover.tsx'
import { AudioOutputPicker } from './AudioOutputPicker.tsx'
import { KeyboardShortcutsModal } from '../modals/KeyboardShortcutsModal.tsx'
import { shareTrack } from '../../services/sharing/playlistPortability.ts'

interface DesktopPlayerViewProps {
  isOpen: boolean
  onClose: () => void
  initialTab?: 'player' | 'lyrics'
}

export function DesktopPlayerView({ isOpen, onClose }: DesktopPlayerViewProps) {
  const {
    currentTrack: engineTrack,
    isPlaying,
    currentTime,
    duration,
    togglePlayback,
    next,
    previous,
    seekTo,
    setVolume,
    setMuted,
    setShuffle,
    setRepeatMode,
    setStereoWidth,
    setVolumeNormalization,
    setEqualizerBands,
    stereoWidth,
    volumeNormalization,
    equalizerGains,
    playbackRate,
    setPlaybackRate,
    sleepTimerRemaining,
    sleepTimerEndOfTrack,
    getAudioTelemetry,
  } = useAudioEngine()

  const currentTrackId = useEchoStore((state) => state.currentTrackId)
  const storeTrack = useEchoStore((state) => state.currentTrack) || getCachedTrack(currentTrackId)
  const currentTrack = engineTrack || storeTrack || null

  const preferences = useEchoStore((state) => state.preferences)
  const setPreference = useEchoStore((state) => state.setPreference)
  const setToast = useEchoStore((state) => state.setToast)
  const liked = useEchoStore((state) =>
    currentTrack ? state.likedTrackIds.includes(currentTrack.id) : false,
  )
  const toggleLike = useEchoStore((state) => state.toggleTrackLike)

  const [isEqOpen, setIsEqOpen] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [isDnaOpen, setIsDnaOpen] = useState(false)
  const [isSleepTimerOpen, setIsSleepTimerOpen] = useState(false)
  const [isOutputPickerOpen, setIsOutputPickerOpen] = useState(false)
  const [isShortcutsOpen, setIsShortcutsOpen] = useState(false)

  const telemetry = getAudioTelemetry()

  // Keyboard controls
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (!isOpen) return
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return
      }

      if (e.code === 'Space') {
        e.preventDefault()
        togglePlayback()
      } else if (e.code === 'ArrowRight') {
        e.preventDefault()
        seekTo(Math.min(duration, currentTime + 5))
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault()
        seekTo(Math.max(0, currentTime - 5))
      } else if (e.key === '?' || (e.shiftKey && e.key === '/')) {
        e.preventDefault()
        setIsShortcutsOpen((v) => !v)
      } else if (e.key === 'Escape') {
        if (isEqOpen) setIsEqOpen(false)
        else if (isDnaOpen) setIsDnaOpen(false)
        else if (isSleepTimerOpen) setIsSleepTimerOpen(false)
        else if (isOutputPickerOpen) setIsOutputPickerOpen(false)
        else if (isShortcutsOpen) setIsShortcutsOpen(false)
        else onClose()
      }
    },
    [
      isOpen,
      isEqOpen,
      isDnaOpen,
      isSleepTimerOpen,
      isOutputPickerOpen,
      isShortcutsOpen,
      togglePlayback,
      seekTo,
      duration,
      currentTime,
      onClose,
    ],
  )

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [handleKeyDown])

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement
        .requestFullscreen()
        .then(() => setIsFullscreen(true))
        .catch(() => {})
    } else {
      document
        .exitFullscreen()
        .then(() => setIsFullscreen(false))
        .catch(() => {})
    }
  }

  const handleShare = async () => {
    if (!currentTrack) return
    const status = await shareTrack(currentTrack)
    setToast(status === 'shared' ? 'Shared track' : 'Share link copied to clipboard')
  }

  const cyclePlaybackRate = () => {
    const rates = [1.0, 1.25, 1.5, 0.75]
    const curIdx = rates.indexOf(playbackRate)
    const nextRate = rates[(curIdx + 1) % rates.length]!
    setPlaybackRate(nextRate)
    setToast(`Speed: ${nextRate}x`)
  }

  if (!isOpen || !currentTrack) return null

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0
  const coverUrl =
    currentTrack.artwork ||
    currentTrack.thumbnailUrl ||
    currentTrack.albumImage ||
    artwork(currentTrack.title || 'echo-track', 800)

  const volumeVal = preferences.muted ? 0 : preferences.volume
  const isSleepActive =
    Boolean(sleepTimerRemaining && sleepTimerRemaining > 0) || Boolean(sleepTimerEndOfTrack)

  return (
    <div
      className="desktop-player-overlay fixed inset-0 z-50 flex h-screen w-screen flex-col overflow-hidden bg-[#0b0a0f] text-[#f4f1f7]"
      role="dialog"
      aria-label="Full-screen player experience"
    >
      {/* Background Layer: Looping Video / Artwork Backdrop */}
      <CanvasVideo track={currentTrack} className="absolute inset-0 z-0" />

      {/* Glass Frosting Surface Overlay */}
      <div
        className="glass-frosting-layer pointer-events-none absolute inset-0 z-1"
        style={{
          backdropFilter: 'blur(64px) saturate(180%)',
          WebkitBackdropFilter: 'blur(64px) saturate(180%)',
          backgroundColor: 'rgba(11, 10, 15, 0.55)',
        }}
      />

      {/* Main Content Layout Container */}
      <div className="relative z-10 flex h-full w-full flex-col">
        {/* Top Navigation Bar */}
        <header className="flex items-center justify-between border-b border-white/6 px-8 py-4">
          <button
            type="button"
            onClick={onClose}
            className="flex items-center gap-2 rounded-lg bg-white/4 px-3 py-1.5 text-xs font-medium text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
            title="Minimize player (Esc)"
          >
            <ChevronDown size={18} />
            <span>Minimize</span>
          </button>

          {/* Center: Playing context & Telemetry badge */}
          <div className="flex flex-col items-center">
            <div className="flex items-center gap-2">
              <span className="font-mono text-[11px] tracking-widest text-[#a59fad] uppercase">
                Playing from
              </span>
              <span className="rounded border border-[#7c5cff]/20 bg-[#7c5cff]/15 px-2 py-0.5 font-mono text-[10px] font-semibold text-[#b09cff]">
                {telemetry.codec} · {telemetry.sampleRate}
              </span>
            </div>
            <span className="mt-0.5 max-w-xs truncate text-xs font-medium text-[#f4f1f7]">
              {currentTrack.albumTitle || currentTrack.album?.title || 'Echo Music Catalog'}
            </span>
          </div>

          {/* Right Action Icons */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setIsDnaOpen(true)}
              className="flex items-center gap-1.5 rounded-lg bg-white/4 px-2.5 py-1.5 text-xs font-semibold text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
              title="Music DNA & lore"
            >
              <Sparkles size={15} className="text-[#b09cff]" />
              <span className="hidden sm:inline">DNA</span>
            </button>

            <button
              type="button"
              onClick={() => setIsOutputPickerOpen(true)}
              className="rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
              title="Select Audio Output Sink"
            >
              <Speaker size={18} />
            </button>

            <button
              type="button"
              onClick={() => setIsSleepTimerOpen(true)}
              className={`flex items-center gap-1 rounded-lg p-2 text-xs transition-colors ${
                isSleepActive
                  ? 'bg-[#7c5cff]/20 text-[#7c5cff]'
                  : 'bg-white/4 text-[#a59fad] hover:bg-white/8 hover:text-white'
              }`}
              title="Sleep Timer"
            >
              <Moon size={18} />
              {isSleepActive && (
                <span className="font-mono text-[11px] font-bold">
                  {sleepTimerEndOfTrack ? '1T' : formatDuration((sleepTimerRemaining || 0) * 1000)}
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={handleShare}
              className="rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
              title="Share Track via Song.link"
            >
              <Share2 size={18} />
            </button>

            <button
              type="button"
              onClick={() => setIsEqOpen(true)}
              className="rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
              title="Open DSP & 10-Band Equalizer"
            >
              <Sliders size={18} />
            </button>

            <button
              type="button"
              onClick={() => setIsShortcutsOpen(true)}
              className="hidden rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white md:block"
              title="Keyboard Shortcuts (?)"
            >
              <Keyboard size={18} />
            </button>

            <button
              type="button"
              onClick={toggleFullscreen}
              className="rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
              title={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
            >
              {isFullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
            </button>
          </div>
        </header>

        {/* Multi-Column Desktop Grid Layout */}
        <main className="grid flex-1 grid-cols-1 gap-8 overflow-hidden px-8 py-6 lg:grid-cols-12">
          {/* Left Column: Artwork, Metadata, 60 FPS Visualizer & Controls (5 Cols) */}
          <div className="custom-scrollbar mx-auto flex h-full w-full max-w-xl flex-col justify-between overflow-y-auto pr-2 lg:col-span-5">
            {/* Artwork Card */}
            <div className="group relative mx-auto aspect-square w-full max-w-[400px] overflow-hidden rounded-2xl border border-white/10 bg-[#16141e] shadow-2xl">
              <img
                src={coverUrl}
                alt={currentTrack.title}
                className="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-105"
                referrerPolicy="no-referrer"
                onError={(e) => {
                  const fallback = artwork(currentTrack.title || 'echo-track', 800)
                  if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
                }}
              />
            </div>

            {/* Track Info & Like Action */}
            <div className="mt-5 flex items-center justify-between gap-4">
              <div className="min-w-0">
                <h1 className="truncate text-2xl font-bold tracking-tight text-white md:text-3xl">
                  {currentTrack.title}
                </h1>
                <Link
                  to={`/artists/${currentTrack.artistId}`}
                  onClick={onClose}
                  className="mt-0.5 block truncate text-base font-medium text-[#a59fad] transition-colors hover:text-[#b09cff]"
                >
                  {currentTrack.artistName}
                </Link>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsDnaOpen(true)}
                  className="rounded-xl bg-white/4 p-2.5 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-[#b09cff]"
                  title="View Music DNA & Meaning"
                >
                  <Sparkles size={20} />
                </button>
                <button
                  type="button"
                  onClick={() => toggleLike(currentTrack)}
                  className={`rounded-xl p-3 transition-all ${
                    liked
                      ? 'bg-red-500/10 text-[#f87171]'
                      : 'bg-white/4 text-[#a59fad] hover:bg-white/8 hover:text-white'
                  }`}
                  aria-label={liked ? 'Unlike track' : 'Like track'}
                >
                  <Heart size={22} fill={liked ? 'currentColor' : 'none'} />
                </button>
              </div>
            </div>

            {/* 60 FPS Real-Time Frequency Visualizer */}
            <div className="my-3 h-14 w-full rounded-xl border border-white/5 bg-white/2 p-2">
              <Visualizer isPlaying={isPlaying} barCount={44} accentColor="#7c5cff" />
            </div>

            {/* Playback Progress Timeline */}
            <div className="flex w-full flex-col gap-1.5">
              <div className="group relative flex items-center">
                <input
                  type="range"
                  aria-label="Track playback position"
                  min={0}
                  max={duration > 0 ? duration : 100}
                  step={0.5}
                  value={currentTime}
                  onChange={(e) => seekTo(parseFloat(e.target.value))}
                  style={{ '--range-progress': `${progressPercent}%` } as React.CSSProperties}
                  className="h-1.5 w-full cursor-pointer appearance-none rounded-full bg-white/10 accent-[#7c5cff]"
                />
              </div>
              <div className="flex justify-between font-mono text-xs text-[#a59fad]">
                <span>{formatDuration(currentTime * 1000)}</span>
                <span>{formatDuration(duration * 1000)}</span>
              </div>
            </div>

            {/* Transport Playback Controls */}
            <div className="flex items-center justify-between py-2">
              <button
                type="button"
                onClick={() => {
                  const nextVal = !preferences.shuffle
                  setPreference('shuffle', nextVal)
                  setShuffle(nextVal)
                }}
                className={`rounded-lg p-2.5 transition-colors ${
                  preferences.shuffle
                    ? 'bg-[#7c5cff]/15 text-[#7c5cff]'
                    : 'text-[#a59fad] hover:text-white'
                }`}
                title="Toggle Shuffle"
              >
                <Shuffle size={18} />
              </button>

              <div className="flex items-center gap-4">
                <button
                  type="button"
                  onClick={previous}
                  className="p-3 text-[#f4f1f7] transition-colors hover:text-white"
                  title="Previous track"
                >
                  <SkipBack size={24} fill="currentColor" />
                </button>

                <button
                  type="button"
                  onClick={togglePlayback}
                  className="flex h-14 w-14 items-center justify-center rounded-full bg-[#7c5cff] text-[#0b0a0f] shadow-lg transition-transform hover:scale-105 hover:bg-[#6c48ff] active:scale-95"
                  title={isPlaying ? 'Pause' : 'Play'}
                >
                  {isPlaying ? (
                    <Pause size={24} fill="currentColor" />
                  ) : (
                    <Play size={24} fill="currentColor" className="ml-1" />
                  )}
                </button>

                <button
                  type="button"
                  onClick={next}
                  className="p-3 text-[#f4f1f7] transition-colors hover:text-white"
                  title="Next track"
                >
                  <SkipForward size={24} fill="currentColor" />
                </button>
              </div>

              <button
                type="button"
                onClick={() => {
                  const modes = ['off', 'all', 'one'] as const
                  const curIdx = modes.indexOf(preferences.repeat)
                  const nextMode = modes[(curIdx + 1) % modes.length]!
                  setPreference('repeat', nextMode)
                  setRepeatMode(nextMode === 'all' ? 'ALL' : nextMode === 'one' ? 'ONE' : 'OFF')
                }}
                className={`rounded-lg p-2.5 transition-colors ${
                  preferences.repeat !== 'off'
                    ? 'bg-[#7c5cff]/15 text-[#7c5cff]'
                    : 'text-[#a59fad] hover:text-white'
                }`}
                title={`Repeat: ${preferences.repeat}`}
              >
                <Repeat size={18} />
              </button>
            </div>

            {/* Bottom Row: Volume Slider, Playback Speed, & DSP Controls */}
            <div className="flex items-center justify-between border-t border-white/6 pt-2">
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => {
                    const nextMute = !preferences.muted
                    setPreference('muted', nextMute)
                    setMuted(nextMute)
                  }}
                  className="text-[#a59fad] transition-colors hover:text-white"
                >
                  {volumeVal === 0 ? <VolumeX size={18} /> : <Volume2 size={18} />}
                </button>
                <input
                  type="range"
                  aria-label="Volume"
                  min={0}
                  max={100}
                  value={volumeVal}
                  onChange={(e) => {
                    const val = Number(e.target.value)
                    setPreference('volume', val)
                    setPreference('muted', false)
                    setVolume(val / 100)
                    setMuted(false)
                  }}
                  className="h-1.5 w-28 cursor-pointer appearance-none rounded-full bg-white/10 accent-[#7c5cff]"
                />
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={cyclePlaybackRate}
                  className="rounded-lg bg-white/4 px-2.5 py-1 font-mono text-xs font-semibold text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
                  title="Change playback speed"
                >
                  {playbackRate}x
                </button>

                <button
                  type="button"
                  onClick={() => setIsEqOpen(true)}
                  className="flex items-center gap-1.5 rounded-lg bg-white/4 px-3 py-1.5 text-xs font-medium text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
                >
                  <Sliders size={14} />
                  <span>EQ / DSP</span>
                </button>
              </div>
            </div>
          </div>

          {/* Right Column: Synchronized Lyrics Stage (7 Cols) */}
          <div className="flex h-full flex-col overflow-hidden rounded-2xl border border-white/6 bg-black/25 lg:col-span-7">
            <LyricsView
              track={currentTrack}
              isPlaying={isPlaying}
              onSeek={seekTo}
              className="h-full"
            />
          </div>
        </main>
      </div>

      {/* Interactive DSP & 10-Band Equalizer Sheet */}
      <EqualizerSheet
        isOpen={isEqOpen}
        onClose={() => setIsEqOpen(false)}
        currentGains={
          equalizerGains.length === 10 ? equalizerGains : [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }
        stereoWidth={stereoWidth}
        volumeNormalization={volumeNormalization}
        onGainsChange={(gains) => setEqualizerBands(gains)}
        onStereoWidthChange={(w) => setStereoWidth(w)}
        onNormalizationChange={(n) => setVolumeNormalization(n)}
      />

      {/* Music DNA Drawer */}
      <MusicDnaDrawer track={currentTrack} isOpen={isDnaOpen} onClose={() => setIsDnaOpen(false)} />

      {/* Sleep Timer Popover */}
      <SleepTimerPopover isOpen={isSleepTimerOpen} onClose={() => setIsSleepTimerOpen(false)} />

      {/* Audio Output Sink Picker */}
      <AudioOutputPicker isOpen={isOutputPickerOpen} onClose={() => setIsOutputPickerOpen(false)} />

      {/* Keyboard Shortcuts Modal */}
      <KeyboardShortcutsModal isOpen={isShortcutsOpen} onClose={() => setIsShortcutsOpen(false)} />
    </div>
  )
}
