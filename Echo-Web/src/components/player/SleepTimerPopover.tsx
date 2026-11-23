import { Moon, X, Clock, Check } from 'lucide-react'
import { useAudioEngine } from '../../hooks/useAudioEngine'
import { formatDuration } from '../../utils/format'

interface SleepTimerPopoverProps {
  isOpen: boolean
  onClose: () => void
}

export function SleepTimerPopover({ isOpen, onClose }: SleepTimerPopoverProps) {
  const { sleepTimerRemaining, sleepTimerEndOfTrack, startSleepTimer, cancelSleepTimer } =
    useAudioEngine()

  if (!isOpen) return null

  const isTimerActive =
    Boolean(sleepTimerRemaining && sleepTimerRemaining > 0) || Boolean(sleepTimerEndOfTrack)

  const handleSelect = (minsOrEnd: number | 'end_of_track') => {
    startSleepTimer(minsOrEnd)
    onClose()
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="Sleep timer settings"
    >
      <div className="relative w-full max-w-xs rounded-2xl border border-white/10 bg-[#16141e] p-5 text-[#f4f1f7] shadow-2xl">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="rounded-lg bg-[#7c5cff]/15 p-1.5 text-[#b09cff]">
              <Moon size={16} />
            </div>
            <h3 className="text-sm font-bold text-white">Sleep Timer</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-white/4 p-1.5 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
          >
            <X size={15} />
          </button>
        </div>

        {isTimerActive && (
          <div className="mb-4 flex items-center justify-between rounded-xl border border-[#7c5cff]/20 bg-[#7c5cff]/10 p-3">
            <div>
              <span className="block text-[11px] text-[#a59fad]">Active timer</span>
              <span className="font-mono text-sm font-bold text-white">
                {sleepTimerEndOfTrack
                  ? 'End of this song'
                  : formatDuration((sleepTimerRemaining || 0) * 1000)}
              </span>
            </div>
            <button
              type="button"
              onClick={() => {
                cancelSleepTimer()
                onClose()
              }}
              className="rounded-lg bg-red-500/20 px-2.5 py-1 text-xs font-medium text-red-300 transition-colors hover:bg-red-500/30"
            >
              Cancel
            </button>
          </div>
        )}

        <div className="flex flex-col gap-1.5">
          {[
            { label: '15 minutes', value: 15 },
            { label: '30 minutes', value: 30 },
            { label: '45 minutes', value: 45 },
            { label: '60 minutes', value: 60 },
          ].map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => handleSelect(option.value)}
              className="flex items-center justify-between rounded-xl px-3 py-2 text-left text-xs font-medium text-white transition-colors hover:bg-white/6"
            >
              <span className="flex items-center gap-2">
                <Clock size={14} className="text-[#a59fad]" />
                <span>{option.label}</span>
              </span>
              {sleepTimerRemaining && Math.round(sleepTimerRemaining / 60) === option.value && (
                <Check size={14} className="text-[#7c5cff]" />
              )}
            </button>
          ))}

          <button
            type="button"
            onClick={() => handleSelect('end_of_track')}
            className="mt-1 flex items-center justify-between rounded-xl border-t border-white/4 px-3 py-2 pt-2 text-left text-xs font-medium text-white transition-colors hover:bg-white/6"
          >
            <span className="flex items-center gap-2">
              <Moon size={14} className="text-[#a59fad]" />
              <span>End of current song</span>
            </span>
            {sleepTimerEndOfTrack && <Check size={14} className="text-[#7c5cff]" />}
          </button>
        </div>

        <p className="mt-3 border-t border-white/4 pt-3 text-[11px] leading-normal text-[#a59fad]/80">
          Audio fades out gently over the final 30 seconds before stopping playback.
        </p>
      </div>
    </div>
  )
}
