import { useEffect, useRef, useState, useCallback } from 'react'
import { Languages, Loader2, Sparkles, AlertCircle, Plus, Minus } from 'lucide-react'
import type { Track } from '../../types/domain.ts'
import type { LyricLine, LyricWord, SynchronizedLyrics } from '../../types/lyrics.ts'
import { db } from '../../services/db/echoDatabase.ts'
import { LyricsOrchestrator } from '../../services/lyrics/LyricsOrchestrator.ts'
import { TranslationClient } from '../../services/ai/TranslationClient.ts'
import { useModalFocusTrap } from '../../hooks/useModalFocusTrap.ts'
import { AudioEngine } from '../../engine/AudioEngine.ts'

interface LyricsViewProps {
  track: Track | null
  isPlaying: boolean
  className?: string
  onSeek?: (seconds: number) => void
}

export function LyricsView({ track, isPlaying, className = '', onSeek }: LyricsViewProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const lineRefs = useRef<Array<HTMLDivElement | null>>([])

  const [lyricsData, setLyricsData] = useState<SynchronizedLyrics | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [activeLineIndex, setActiveLineIndex] = useState<number>(-1)
  const [currentTimeMs, setCurrentTimeMs] = useState<number>(0)
  // Sync offset (ms) shifts when lyric lines are matched against playback; persisted per track.
  const [syncOffsetMs, setSyncOffsetMs] = useState<number>(0)

  // AI Translation State
  const [isTranslating, setIsTranslating] = useState(false)
  const [translationError, setTranslationError] = useState<string | null>(null)
  const [isTranslated, setIsTranslated] = useState(false)
  const [showConfigModal, setShowConfigModal] = useState(false)

  // Manual scroll override (pauses auto-scroll for 3.5s)
  const isUserScrollingRef = useRef(false)
  const scrollTimeoutRef = useRef<number | null>(null)
  const [isManualScrollActive, setIsManualScrollActive] = useState(false)

  // Load the saved sync offset for this track
  useEffect(() => {
    if (!track) return
    const trackId = track.videoId || track.id
    void db.syncOffsets.get(trackId).then((record) => {
      setSyncOffsetMs(record?.offsetMs ?? 0)
    })
  }, [track])

  const adjustSyncOffset = (deltaMs: number) => {
    const next = syncOffsetMs + deltaMs
    setSyncOffsetMs(next)
    if (track) {
      void db.syncOffsets.put({
        trackId: track.videoId || track.id,
        offsetMs: next,
        updatedAt: Date.now(),
      })
    }
  }

  // Fetch lyrics whenever track changes
  useEffect(() => {
    if (!track) return

    const abortController = new AbortController()

    LyricsOrchestrator.getInstance()
      .getLyrics(track, abortController.signal)
      .then((lyrics) => {
        if (!abortController.signal.aborted) {
          setLyricsData(lyrics)
          setIsLoading(false)
          setIsTranslated(false)
          setTranslationError(null)
        }
      })
      .catch(() => {
        if (!abortController.signal.aborted) {
          setLyricsData(null)
          setIsLoading(false)
        }
      })

    return () => {
      abortController.abort()
    }
  }, [track])

  // Microsecond Synchronization: RAF Loop polling audioEngine.getCurrentTime()
  useEffect(() => {
    let rafId: number | null = null
    const engine = AudioEngine.getInstance()

    const syncTick = () => {
      const currentSeconds = engine.getCurrentTime()
      const ms = Math.max(0, currentSeconds * 1000)
      setCurrentTimeMs(ms)

      if (lyricsData?.lines && lyricsData.lines.length > 0) {
        const lines = lyricsData.lines
        let matchedIdx = -1

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i]!
          const nextLine = lines[i + 1]
          const lineEnd =
            line.endTimeMs ?? (nextLine ? nextLine.startTimeMs : line.startTimeMs + 4000)

          if (ms >= line.startTimeMs + syncOffsetMs && ms < lineEnd + syncOffsetMs) {
            matchedIdx = i
            break
          } else if (ms >= line.startTimeMs + syncOffsetMs) {
            matchedIdx = i
          }
        }

        if (matchedIdx !== -1) {
          setActiveLineIndex(matchedIdx)
        }
      }

      if (isPlaying) {
        rafId = requestAnimationFrame(syncTick)
      }
    }

    if (isPlaying) {
      rafId = requestAnimationFrame(syncTick)
    } else {
      // One update when paused
      syncTick()
    }

    return () => {
      if (rafId !== null) {
        cancelAnimationFrame(rafId)
      }
    }
  }, [isPlaying, lyricsData, syncOffsetMs])
  // Auto-scrolling to active line with momentum and manual scroll pause
  const scrollToActiveLine = useCallback(
    (force = false) => {
      if ((!force && isUserScrollingRef.current) || activeLineIndex < 0) return
      const targetElement = lineRefs.current[activeLineIndex]
      const container = containerRef.current
      if (!targetElement || !container) return

      const containerRect = container.getBoundingClientRect()
      const targetRect = targetElement.getBoundingClientRect()

      const offsetTop =
        targetRect.top - containerRect.top + container.scrollTop - containerRect.height * 0.4

      container.scrollTo({
        top: Math.max(0, offsetTop),
        behavior: 'smooth',
      })
    },
    [activeLineIndex],
  )

  useEffect(() => {
    scrollToActiveLine()
  }, [activeLineIndex, scrollToActiveLine])

  // Detect manual user scrolling or touch interaction
  const handleUserScroll = () => {
    isUserScrollingRef.current = true
    setIsManualScrollActive(true)

    if (scrollTimeoutRef.current !== null) {
      window.clearTimeout(scrollTimeoutRef.current)
    }

    // Pause auto-scrolling for 3.5 seconds before resuming
    scrollTimeoutRef.current = window.setTimeout(() => {
      isUserScrollingRef.current = false
      setIsManualScrollActive(false)
      scrollToActiveLine(true)
    }, 3500)
  }

  // Handle click on line to seek
  const handleLineClick = (startTimeMs: number) => {
    const seconds = startTimeMs / 1000
    if (onSeek) {
      onSeek(seconds)
    } else {
      AudioEngine.getInstance().seekTo(seconds)
    }
  }

  // AI Translation Handler
  const handleTranslate = async () => {
    if (!lyricsData || lyricsData.lines.length === 0) return

    const client = TranslationClient.getInstance()
    if (!client.hasConfiguredKey()) {
      setShowConfigModal(true)
      return
    }

    setIsTranslating(true)
    setTranslationError(null)

    try {
      const translated = await client.translateLyrics(lyricsData.lines)
      setLyricsData((prev) => (prev ? { ...prev, lines: translated } : null))
      setIsTranslated(true)
    } catch (err) {
      setTranslationError(err instanceof Error ? err.message : String(err))
    } finally {
      setIsTranslating(false)
    }
  }

  const renderWord = (word: LyricWord, wordIdx: number) => {
    const isPast = currentTimeMs >= word.endTimeMs
    const isCurrent = currentTimeMs >= word.startTimeMs && currentTimeMs < word.endTimeMs
    const progress = isCurrent
      ? Math.max(
          0,
          Math.min(1, (currentTimeMs - word.startTimeMs) / (word.endTimeMs - word.startTimeMs)),
        )
      : isPast
        ? 1
        : 0

    return (
      <span
        key={wordIdx}
        className="lyric-word relative mr-1.5 inline-block transition-all duration-75"
        style={{
          color: isPast ? '#ffffff' : isCurrent ? '#ffffff' : 'rgba(244, 241, 247, 0.45)',
          textShadow: isCurrent
            ? '0 0 16px rgba(124, 92, 255, 0.8), 0 0 32px rgba(124, 92, 255, 0.4)'
            : 'none',
        }}
      >
        {isCurrent && (
          <span
            className="absolute inset-0 overflow-hidden font-bold text-[#ffffff]"
            style={{
              clipPath: `inset(0 ${(1 - progress) * 100}% 0 0)`,
              textShadow: '0 0 12px #7c5cff',
            }}
          >
            {word.text}
          </span>
        )}
        {word.text}
      </span>
    )
  }

  const renderLine = (line: LyricLine, idx: number) => {
    const isActive = idx === activeLineIndex
    const isPast = idx < activeLineIndex

    return (
      <div
        key={idx}
        ref={(el) => {
          lineRefs.current[idx] = el
        }}
        onClick={() => handleLineClick(line.startTimeMs)}
        className={`lyric-line group cursor-pointer rounded-xl px-4 py-3.5 transition-all duration-300 select-none ${
          isActive
            ? 'lyric-line--active origin-left scale-[1.02] bg-white/6'
            : isPast
              ? 'lyric-line--past opacity-55 hover:opacity-85'
              : 'lyric-line--upcoming opacity-35 hover:opacity-75'
        }`}
      >
        <p
          className={`text-xl font-semibold tracking-tight transition-colors duration-200 md:text-2xl ${
            isActive ? 'text-white' : 'text-[#f4f1f7]'
          }`}
        >
          {line.words && line.words.length > 0 && isActive
            ? line.words.map((w, wIdx) => renderWord(w, wIdx))
            : line.text}
        </p>

        {line.translatedText && (
          <p className="lyric-translation mt-1.5 text-sm font-medium tracking-normal text-[#b09cff] opacity-90 md:text-base">
            {line.translatedText}
          </p>
        )}
      </div>
    )
  }

  return (
    <div
      className={`lyrics-stage-container relative flex h-full flex-col overflow-hidden ${className}`}
    >
      {/* Header Bar */}
      <div className="lyrics-stage-header z-10 flex items-center justify-between border-b border-white/6 bg-transparent px-6 py-4">
        <div className="flex items-center gap-3">
          <span className="font-mono text-xs tracking-widest text-[#a59fad] uppercase">
            Lyrics Stage
          </span>
          {lyricsData?.provider && (
            <span className="rounded border border-white/10 bg-white/8 px-2 py-0.5 font-mono text-[10px] text-[#b09cff] uppercase">
              {lyricsData.provider}
              {lyricsData.isWordSynced ? ' (Karaoke)' : lyricsData.isSynced ? ' (Synced)' : ''}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {lyricsData?.isSynced !== false && (
            <div
              className="flex items-center gap-1 rounded-lg bg-white/8 px-1.5 py-1 text-[#e7e4ed]"
              title="Adjust lyric sync timing (persisted per track)"
            >
              <button
                type="button"
                className="rounded p-1 transition-all hover:bg-white/12"
                onClick={() => adjustSyncOffset(-500)}
                aria-label="Shift lyrics 0.5 seconds earlier"
              >
                <Minus size={12} />
              </button>
              <span className="w-12 text-center font-mono text-[10px]" aria-live="polite">
                {syncOffsetMs >= 0 ? '+' : ''}
                {(syncOffsetMs / 1000).toFixed(1)}s
              </span>
              <button
                type="button"
                className="rounded p-1 transition-all hover:bg-white/12"
                onClick={() => adjustSyncOffset(500)}
                aria-label="Shift lyrics 0.5 seconds later"
              >
                <Plus size={12} />
              </button>
            </div>
          )}
          {lyricsData && (
            <button
              type="button"
              onClick={handleTranslate}
              disabled={isTranslating}
              className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
                isTranslated
                  ? 'bg-[#7c5cff] font-semibold text-[#0b0a0f]'
                  : 'bg-white/8 text-[#f4f1f7] hover:bg-white/12'
              }`}
              title="AI Translate Lyrics"
            >
              {isTranslating ? (
                <Loader2 size={13} className="animate-spin" />
              ) : (
                <Languages size={13} />
              )}
              {isTranslating ? 'Translating...' : isTranslated ? 'Translated' : 'AI Translate'}
            </button>
          )}

          <button
            type="button"
            onClick={() => setShowConfigModal(true)}
            className="rounded-lg p-1.5 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
            title="Configure AI Translation Keys"
            aria-label="Lyrics AI settings"
          >
            <Sparkles size={16} />
          </button>
        </div>
      </div>

      {/* Manual Scroll Override Snap Banner */}
      {isManualScrollActive && (
        <div className="absolute top-16 left-1/2 z-20 -translate-x-1/2">
          <button
            type="button"
            onClick={() => {
              isUserScrollingRef.current = false
              setIsManualScrollActive(false)
              scrollToActiveLine(true)
            }}
            className="rounded-full bg-[#7c5cff] px-3 py-1 text-xs font-semibold text-[#0b0a0f] shadow-lg transition-transform hover:scale-105"
          >
            Jump to current lyric
          </button>
        </div>
      )}

      {/* Translation Error Banner */}
      {translationError && (
        <div className="mx-6 mt-3 flex items-center gap-2 rounded-lg border border-red-500/30 bg-red-500/15 px-3 py-2 text-xs text-red-200">
          <AlertCircle size={14} />
          <span className="truncate">{translationError}</span>
        </div>
      )}

      {/* Lyrics Scrollable Viewport */}
      <div
        ref={containerRef}
        onScroll={handleUserScroll}
        onTouchMove={handleUserScroll}
        onWheel={handleUserScroll}
        className="lyrics-stage-viewport custom-scrollbar flex-1 overflow-y-auto scroll-smooth px-6 py-12"
        style={{
          maskImage:
            'linear-gradient(to bottom, transparent 0%, black 12%, black 88%, transparent 100%)',
          WebkitMaskImage:
            'linear-gradient(to bottom, transparent 0%, black 12%, black 88%, transparent 100%)',
        }}
      >
        {isLoading ? (
          <div className="flex h-full flex-col items-center justify-center gap-3 text-[#a59fad]">
            <Loader2 size={28} className="animate-spin text-[#7c5cff]" />
            <p className="text-sm">Fetching lyrics cascade...</p>
          </div>
        ) : lyricsData && lyricsData.lines.length > 0 ? (
          <div className="mx-auto flex max-w-2xl flex-col gap-2 py-8">
            {lyricsData.lines.map((line, idx) => renderLine(line, idx))}
          </div>
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-2 p-8 text-center text-[#918a98]">
            <p className="text-base font-medium">
              No synchronized lyrics available for this track.
            </p>
            <p className="text-xs text-[#a59fad]">
              Enjoy the music or try searching for another version.
            </p>
          </div>
        )}
      </div>

      {/* AI Key Configuration Modal */}
      {showConfigModal && (
        <AIConfigModal
          onClose={() => setShowConfigModal(false)}
          onSaved={() => {
            setShowConfigModal(false)
            if (lyricsData && !isTranslated) {
              void handleTranslate()
            }
          }}
        />
      )}
    </div>
  )
}

function AIConfigModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const client = TranslationClient.getInstance()
  const initial = client.getConfig()
  const dialogRef = useRef<HTMLDivElement>(null)
  useModalFocusTrap(dialogRef, onClose)

  const [provider, setProvider] = useState(initial.provider)
  const [openRouterKey, setOpenRouterKey] = useState(initial.openRouterKey || '')
  const [openRouterModel, setOpenRouterModel] = useState(
    initial.openRouterModel || 'google/gemini-2.0-flash-001',
  )
  const [deepLKey, setDeepLKey] = useState(initial.deepLKey || '')
  const [mistralKey, setMistralKey] = useState(initial.mistralKey || '')
  const [openAiBaseUrl, setOpenAiBaseUrl] = useState(initial.openAiBaseUrl || '')
  const [openAiKey, setOpenAiKey] = useState(initial.openAiKey || '')
  const [openAiModel, setOpenAiModel] = useState(initial.openAiModel || 'gpt-4o-mini')
  const [anthropicKey, setAnthropicKey] = useState(initial.anthropicKey || '')
  const [anthropicModel, setAnthropicModel] = useState(initial.anthropicModel || 'claude-haiku-4-5')
  const [geminiKey, setGeminiKey] = useState(initial.geminiKey || '')
  const [geminiModel, setGeminiModel] = useState(initial.geminiModel || 'gemini-2.0-flash')
  const [targetLanguage, setTargetLanguage] = useState(initial.targetLanguage || 'English')

  const handleSave = () => {
    client.saveConfig({
      provider,
      openRouterKey,
      openRouterModel,
      deepLKey,
      mistralKey,
      openAiBaseUrl,
      openAiKey,
      openAiModel,
      anthropicKey,
      anthropicModel,
      geminiKey,
      geminiModel,
      targetLanguage,
    })
    onSaved()
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        ref={dialogRef}
        className="flex w-full max-w-md flex-col gap-4 rounded-xl border border-white/10 bg-[#14121a] p-6 text-[#f4f1f7] shadow-2xl"
      >
        <h3 className="text-base font-semibold">AI Lyrics Translation Settings</h3>
        <p className="text-xs text-[#a59fad]">
          Keys are stored locally in your browser (<code className="text-xs">localStorage</code>)
          and never proxied through our server.
        </p>

        <div className="flex flex-col gap-1.5">
          <label className="text-xs font-medium text-[#a59fad]">Provider</label>
          <div className="grid grid-cols-3 gap-2">
            {(
              [
                'openrouter',
                'deepl',
                'mistral',
                'openai-compatible',
                'anthropic',
                'gemini',
              ] as const
            ).map((p) => (
              <button
                key={p}
                type="button"
                onClick={() => setProvider(p)}
                className={`rounded-lg border py-1.5 text-xs font-medium capitalize ${
                  provider === p
                    ? 'border-[#7c5cff] bg-[#7c5cff] font-semibold text-[#0b0a0f]'
                    : 'border-white/8 bg-white/4 text-[#a59fad] hover:text-white'
                }`}
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        {provider === 'openrouter' && (
          <>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">OpenRouter API Key</label>
              <input
                type="password"
                placeholder="sk-or-v1-..."
                value={openRouterKey}
                onChange={(e) => setOpenRouterKey(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Model</label>
              <input
                type="text"
                placeholder="google/gemini-2.0-flash-001"
                value={openRouterModel}
                onChange={(e) => setOpenRouterModel(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
          </>
        )}

        {provider === 'deepl' && (
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#a59fad]">DeepL API Key</label>
            <input
              type="password"
              placeholder="Your DeepL API Auth Key"
              value={deepLKey}
              onChange={(e) => setDeepLKey(e.target.value)}
              className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
            />
          </div>
        )}

        {provider === 'mistral' && (
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-[#a59fad]">Mistral API Key</label>
            <input
              type="password"
              placeholder="Your Mistral API Key"
              value={mistralKey}
              onChange={(e) => setMistralKey(e.target.value)}
              className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
            />
          </div>
        )}

        {provider === 'openai-compatible' && (
          <>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Base URL</label>
              <input
                type="text"
                placeholder="https://api.openai.com/v1"
                value={openAiBaseUrl}
                onChange={(e) => setOpenAiBaseUrl(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">API Key</label>
              <input
                type="password"
                placeholder="sk-..."
                value={openAiKey}
                onChange={(e) => setOpenAiKey(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Model</label>
              <input
                type="text"
                placeholder="gpt-4o-mini"
                value={openAiModel}
                onChange={(e) => setOpenAiModel(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
          </>
        )}

        {provider === 'anthropic' && (
          <>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Anthropic API Key</label>
              <input
                type="password"
                placeholder="sk-ant-..."
                value={anthropicKey}
                onChange={(e) => setAnthropicKey(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Model</label>
              <input
                type="text"
                placeholder="claude-haiku-4-5"
                value={anthropicModel}
                onChange={(e) => setAnthropicModel(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
          </>
        )}

        {provider === 'gemini' && (
          <>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Gemini API Key</label>
              <input
                type="password"
                placeholder="AIza..."
                value={geminiKey}
                onChange={(e) => setGeminiKey(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-[#a59fad]">Model</label>
              <input
                type="text"
                placeholder="gemini-2.0-flash"
                value={geminiModel}
                onChange={(e) => setGeminiModel(e.target.value)}
                className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
              />
            </div>
          </>
        )}

        <div className="flex flex-col gap-1.5">
          <label className="text-xs font-medium text-[#a59fad]">Target Language</label>
          <input
            type="text"
            placeholder="e.g. English, Spanish, Japanese, German"
            value={targetLanguage}
            onChange={(e) => setTargetLanguage(e.target.value)}
            className="rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-xs focus:border-[#7c5cff] focus:outline-none"
          />
        </div>

        <div className="flex justify-end gap-2 border-t border-white/8 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-white/6 px-4 py-2 text-xs font-medium text-[#a59fad] transition-colors hover:bg-white/10"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSave}
            className="rounded-lg bg-[#7c5cff] px-4 py-2 text-xs font-semibold text-[#0b0a0f] transition-colors hover:bg-[#6c48ff]"
          >
            Save Settings
          </button>
        </div>
      </div>
    </div>
  )
}
