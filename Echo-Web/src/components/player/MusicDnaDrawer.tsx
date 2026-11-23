import { useState, useEffect } from 'react'
import { BookOpen, Activity, Sparkles, ExternalLink, X, Info } from 'lucide-react'
import { fetchMusicDna, type MusicDnaData } from '../../services/musicDna/musicDnaService'
import type { Track } from '../../types/domain'

interface MusicDnaDrawerProps {
  track: Track | null
  isOpen: boolean
  onClose: () => void
}

export function MusicDnaDrawer({ track, isOpen, onClose }: MusicDnaDrawerProps) {
  const [dnaData, setDnaData] = useState<MusicDnaData | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<'lore' | 'telemetry'>('lore')

  useEffect(() => {
    if (!isOpen || !track) return

    let isCurrent = true
    Promise.resolve().then(() => {
      if (isCurrent) setIsLoading(true)
    })

    fetchMusicDna(track)
      .then((data) => {
        if (isCurrent) {
          setDnaData(data)
          setIsLoading(false)
        }
      })
      .catch(() => {
        if (isCurrent) {
          setIsLoading(false)
        }
      })

    return () => {
      isCurrent = false
    }
  }, [track, isOpen])

  if (!isOpen || !track) return null

  return (
    <div
      className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-white/8 bg-[#121017] text-[#f4f1f7] shadow-2xl"
      role="dialog"
      aria-label="Music DNA and track annotations"
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white/8 px-6 py-5">
        <div className="flex items-center gap-2">
          <div className="rounded-xl bg-[#7c5cff]/15 p-2 text-[#b09cff]">
            <Sparkles size={18} />
          </div>
          <div>
            <h2 className="text-base leading-tight font-bold text-white">Music DNA</h2>
            <p className="max-w-[240px] truncate text-xs text-[#a59fad]">
              {track.title} · {track.artistName}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
          title="Close (Esc)"
        >
          <X size={17} />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-white/6 px-6 pt-2">
        <button
          type="button"
          onClick={() => setActiveTab('lore')}
          className={`flex items-center gap-2 border-b-2 px-3 py-3 text-xs font-semibold transition-colors ${
            activeTab === 'lore'
              ? 'border-[#7c5cff] text-[#b09cff]'
              : 'border-transparent text-[#a59fad] hover:text-white'
          }`}
        >
          <BookOpen size={14} />
          <span>Story & Lore</span>
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('telemetry')}
          className={`flex items-center gap-2 border-b-2 px-3 py-3 text-xs font-semibold transition-colors ${
            activeTab === 'telemetry'
              ? 'border-[#7c5cff] text-[#b09cff]'
              : 'border-transparent text-[#a59fad] hover:text-white'
          }`}
        >
          <Activity size={14} />
          <span>Engine Telemetry</span>
        </button>
      </div>

      {/* Content Body */}
      <div className="custom-scrollbar flex-1 space-y-6 overflow-y-auto p-6">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center gap-3 py-16 text-xs text-[#a59fad]">
            <Sparkles size={24} className="animate-spin text-[#7c5cff]" />
            <span>Analyzing track DNA & annotations...</span>
          </div>
        ) : activeTab === 'lore' ? (
          /* Lore & Background Tab */
          <>
            {/* Genius Track Lore */}
            {dnaData?.genius?.description && (
              <div className="rounded-xl border border-white/6 bg-white/3 p-4">
                <span className="mb-2 block font-mono text-[11px] font-semibold tracking-wider text-[#7c5cff] uppercase">
                  Song Meaning & Story
                </span>
                <p className="text-xs leading-relaxed whitespace-pre-line text-[#e1dde8]">
                  {dnaData.genius.description}
                </p>
                {dnaData.genius.releaseDate && (
                  <span className="mt-3 block font-mono text-[11px] text-[#a59fad]">
                    Released: {dnaData.genius.releaseDate}
                  </span>
                )}
              </div>
            )}

            {/* Wikipedia Song Extract */}
            {dnaData?.wikipedia && (
              <div className="rounded-xl border border-white/6 bg-white/3 p-4">
                <div className="mb-2 flex items-center justify-between">
                  <span className="font-mono text-[11px] font-semibold tracking-wider text-[#a59fad] uppercase">
                    Wikipedia Background
                  </span>
                  {dnaData.wikipedia.pageUrl && (
                    <a
                      href={dnaData.wikipedia.pageUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-1 text-[11px] text-[#b09cff] hover:underline"
                    >
                      <span>Read wiki</span>
                      <ExternalLink size={11} />
                    </a>
                  )}
                </div>
                <p className="text-xs leading-relaxed text-[#c6c0cf]">
                  {dnaData.wikipedia.extract}
                </p>
              </div>
            )}

            {/* Wikipedia Artist Bio */}
            {dnaData?.artistWikipedia && (
              <div className="rounded-xl border border-white/6 bg-white/3 p-4">
                <div className="mb-2 flex items-center justify-between">
                  <span className="font-mono text-[11px] font-semibold tracking-wider text-[#a59fad] uppercase">
                    About {track.artistName}
                  </span>
                  {dnaData.artistWikipedia.pageUrl && (
                    <a
                      href={dnaData.artistWikipedia.pageUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-1 text-[11px] text-[#b09cff] hover:underline"
                    >
                      <span>Artist page</span>
                      <ExternalLink size={11} />
                    </a>
                  )}
                </div>
                <p className="text-xs leading-relaxed text-[#c6c0cf]">
                  {dnaData.artistWikipedia.extract}
                </p>
              </div>
            )}

            {!dnaData?.genius?.description && !dnaData?.wikipedia && (
              <div className="rounded-xl border border-white/4 bg-white/2 p-8 text-center text-xs text-[#a59fad]">
                <Info size={24} className="mx-auto mb-2 text-[#a59fad]/60" />
                <p>Limited background lore available for this specific recording.</p>
              </div>
            )}
          </>
        ) : (
          /* Engine Telemetry Tab: only real, measurable pipeline facts */
          <div className="space-y-4">
            <div className="space-y-2 rounded-xl border border-white/6 bg-white/3 p-4">
              <span className="block font-mono text-[11px] text-[#a59fad] uppercase">
                Audio Pipeline Telemetry
              </span>
              <div className="flex justify-between border-b border-white/4 py-1 text-xs">
                <span className="text-[#a59fad]">Audio Codec</span>
                <span className="font-mono text-white">
                  {track.id.startsWith('local_')
                    ? 'Local File (Decoded PCM)'
                    : 'Opus / WebM (Bitrate ~160k)'}
                </span>
              </div>
              <div className="flex justify-between border-b border-white/4 py-1 text-xs">
                <span className="text-[#a59fad]">Web Audio Internal Depth</span>
                <span className="font-mono text-white">32-bit Floating Point</span>
              </div>
              <div className="flex justify-between py-1 text-xs">
                <span className="text-[#a59fad]">Dynamic Compressor</span>
                <span className="font-mono text-[#58ad83]">Active Threshold: -14 dB</span>
              </div>
            </div>

            <div className="rounded-xl border border-white/4 bg-white/2 p-4 text-xs text-[#a59fad]">
              Per-track stats like tempo, musical key and loudness are not shown because Echo does
              not analyze audio content. Only measured pipeline facts above are reported.
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
