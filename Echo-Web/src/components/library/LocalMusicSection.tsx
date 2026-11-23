import { useState, useEffect, useRef } from 'react'
import { FolderPlus, HardDrive, Play, Plus, Trash2, Music } from 'lucide-react'
import { getAllLocalTracks, importLocalFiles } from '../../services/local/localMusicScanner'
import { db } from '../../services/db/echoDatabase'
import { useEchoStore } from '../../stores/useEchoStore'
import { useAudioEngine } from '../../hooks/useAudioEngine'
import { formatDuration } from '../../utils/format'
import type { Track } from '../../types/domain'

export function LocalMusicSection() {
  const [localTracks, setLocalTracks] = useState<Track[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [scanProgress, setScanProgress] = useState<{
    current: number
    total: number
    name: string
  } | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)
  const dirInputRef = useRef<HTMLInputElement>(null)

  const { play, playQueue, addToQueue } = useAudioEngine()
  const setToast = useEchoStore((state) => state.setToast)

  const loadTracks = async () => {
    try {
      const tracks = await getAllLocalTracks()
      setLocalTracks(tracks)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getAllLocalTracks()
      .then((tracks) => {
        if (active) {
          setLocalTracks(tracks)
          setIsLoading(false)
        }
      })
      .catch(() => {
        if (active) {
          setIsLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [])

  const handleFilesSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files || files.length === 0) return

    setScanProgress({ current: 0, total: files.length, name: 'Reading metadata...' })
    try {
      await importLocalFiles(files, (cur, total, name) => {
        setScanProgress({ current: cur, total, name })
      })
      setToast(`Scanned and added ${files.length} local tracks`)
      await loadTracks()
    } catch (err) {
      setToast(err instanceof Error ? err.message : 'Error importing local files')
    } finally {
      setScanProgress(null)
      if (e.target) e.target.value = ''
    }
  }

  const handleDeleteLocalTrack = async (id: string, title: string) => {
    await db.localFiles.delete(id)
    setToast(`Removed "${title}" from local library`)
    await loadTracks()
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Top Banner & Scan Actions */}
      <div className="flex flex-col items-start justify-between gap-4 rounded-2xl border border-white/8 bg-[#16141e] p-6 sm:flex-row sm:items-center">
        <div className="flex items-center gap-3.5">
          <div className="rounded-xl bg-[#7c5cff]/15 p-3 text-[#b09cff]">
            <HardDrive size={24} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">Local Device Music</h2>
            <p className="text-xs text-[#a59fad]">
              Play locally stored audio files with full DSP, synchronized lyrics, and offline
              capability.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFilesSelected}
            multiple
            accept="audio/*,.mp3,.flac,.wav,.m4a,.ogg,.webm"
            className="hidden"
          />
          <input
            type="file"
            ref={dirInputRef}
            onChange={handleFilesSelected}
            // @ts-expect-error webkitdirectory is standard for folder uploads in browser
            webkitdirectory=""
            className="hidden"
          />

          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center gap-1.5 rounded-xl bg-white/6 px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-white/10"
          >
            <Plus size={15} />
            <span>Add Files</span>
          </button>

          <button
            type="button"
            onClick={() => dirInputRef.current?.click()}
            className="flex items-center gap-1.5 rounded-xl bg-[#7c5cff] px-4 py-2 text-xs font-semibold text-black shadow-md transition-colors hover:bg-[#6c48ff]"
          >
            <FolderPlus size={15} />
            <span>Scan Folder</span>
          </button>
        </div>
      </div>

      {/* Progress Card during scanning */}
      {scanProgress && (
        <div className="flex flex-col gap-2 rounded-xl border border-white/8 bg-white/4 p-4">
          <div className="flex justify-between text-xs text-[#a59fad]">
            <span className="max-w-sm truncate">Scanning: {scanProgress.name}</span>
            <span className="font-mono">
              {scanProgress.current} / {scanProgress.total}
            </span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
            <div
              className="h-full bg-[#7c5cff] transition-all"
              style={{
                width: `${Math.round((scanProgress.current / Math.max(1, scanProgress.total)) * 100)}%`,
              }}
            />
          </div>
        </div>
      )}

      {/* Track List or Empty State */}
      {localTracks.length === 0 && !isLoading ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-white/5 bg-white/2 p-12 text-center">
          <div className="mb-4 rounded-2xl bg-white/4 p-4 text-[#a59fad]">
            <Music size={32} />
          </div>
          <h3 className="mb-1 text-base font-bold text-white">No Local Music Added</h3>
          <p className="mb-6 max-w-sm text-xs text-[#a59fad]">
            Import individual audio files or an entire folder from your device storage. They will be
            saved to your browser database.
          </p>
          <button
            type="button"
            onClick={() => dirInputRef.current?.click()}
            className="flex items-center gap-2 rounded-xl bg-[#7c5cff] px-5 py-2.5 text-xs font-semibold text-black shadow-md transition-all hover:bg-[#6c48ff]"
          >
            <FolderPlus size={16} />
            <span>Select Music Folder</span>
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <div className="mb-1 flex items-center justify-between px-2">
            <span className="text-xs font-medium text-[#a59fad]">
              {localTracks.length} locally stored track{localTracks.length === 1 ? '' : 's'}
            </span>
            <button
              type="button"
              onClick={() => playQueue(localTracks, 0)}
              className="flex items-center gap-1.5 text-xs font-semibold text-[#b09cff] hover:underline"
            >
              <Play size={13} fill="currentColor" />
              <span>Play All</span>
            </button>
          </div>

          <div className="overflow-hidden rounded-xl border border-white/6 bg-[#121017]">
            {localTracks.map((track, idx) => (
              <div
                key={track.id}
                className="group flex items-center justify-between border-b border-white/4 px-4 py-3 transition-colors last:border-0 hover:bg-white/4"
              >
                <div className="flex min-w-0 items-center gap-3.5">
                  <span className="w-6 text-center font-mono text-xs text-[#a59fad] group-hover:hidden">
                    {idx + 1}
                  </span>
                  <button
                    type="button"
                    onClick={() => play(track)}
                    className="hidden h-6 w-6 items-center justify-center rounded-full bg-[#7c5cff] text-black transition-transform group-hover:flex hover:scale-105"
                    title="Play track"
                  >
                    <Play size={12} fill="currentColor" className="ml-0.5" />
                  </button>

                  <div className="min-w-0">
                    <span className="block truncate text-sm font-medium text-white">
                      {track.title}
                    </span>
                    <span className="block truncate text-xs text-[#a59fad]">
                      {track.artistName} · {track.albumTitle}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs text-[#a59fad]">
                    {formatDuration((track.durationSeconds || 0) * 1000)}
                  </span>
                  <button
                    type="button"
                    onClick={() => addToQueue(track)}
                    className="rounded-lg bg-white/2 p-1.5 text-[#a59fad] transition-colors hover:bg-white/6 hover:text-white"
                    title="Add to Up Next"
                  >
                    <Plus size={15} />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDeleteLocalTrack(track.id, track.title)}
                    className="rounded-lg bg-white/2 p-1.5 text-[#a59fad] transition-colors hover:bg-white/6 hover:text-red-400"
                    title="Remove from Local Library"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
