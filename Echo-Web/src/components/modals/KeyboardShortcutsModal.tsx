import { useRef } from 'react'
import { Keyboard, X } from 'lucide-react'
import { useModalFocusTrap } from '../../hooks/useModalFocusTrap'

interface KeyboardShortcutsModalProps {
  isOpen: boolean
  onClose: () => void
}

interface ShortcutItem {
  keys: string[]
  description: string
}

const shortcuts: ShortcutItem[] = [
  { keys: ['Space'], description: 'Play or pause playback' },
  { keys: ['←', '→'], description: 'Seek backward / forward 5 seconds' },
  { keys: ['Shift', '← / →'], description: 'Skip to previous or next song' },
  { keys: ['↑', '↓'], description: 'Increase or decrease player volume' },
  { keys: ['M'], description: 'Toggle mute' },
  { keys: ['L'], description: 'Like or unlike current track' },
  { keys: ['S'], description: 'Toggle shuffle mode' },
  { keys: ['R'], description: 'Cycle repeat mode (Off / All / One)' },
  { keys: ['F'], description: 'Toggle full-screen player view' },
  { keys: ['Esc'], description: 'Close modal or minimize full-screen player' },
  { keys: ['?'], description: 'Open this keyboard shortcuts reference' },
]

export function KeyboardShortcutsModal({ isOpen, onClose }: KeyboardShortcutsModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  useModalFocusTrap(dialogRef, onClose, isOpen)

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md"
      role="dialog"
      aria-modal="true"
      aria-label="Keyboard shortcuts"
    >
      <div
        ref={dialogRef}
        className="relative w-full max-w-md rounded-2xl border border-white/10 bg-[#16141e] p-6 text-[#f4f1f7] shadow-2xl"
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
          title="Close (Esc)"
        >
          <X size={18} />
        </button>

        <div className="mb-4 flex items-center gap-2.5">
          <div className="rounded-xl bg-[#7c5cff]/15 p-2 text-[#b09cff]">
            <Keyboard size={20} />
          </div>
          <div>
            <h2 className="text-lg leading-tight font-bold text-white">Keyboard Shortcuts</h2>
            <p className="text-xs text-[#a59fad]">
              Quick navigation for desktop and laptop keyboards.
            </p>
          </div>
        </div>

        <div className="custom-scrollbar flex max-h-80 flex-col gap-2 overflow-y-auto pr-1">
          {shortcuts.map((sc, idx) => (
            <div
              key={idx}
              className="flex items-center justify-between rounded-lg border border-white/4 bg-white/3 px-3 py-2 text-xs"
            >
              <span className="text-[#d8d4e2]">{sc.description}</span>
              <div className="flex items-center gap-1">
                {sc.keys.map((k, kIdx) => (
                  <kbd
                    key={kIdx}
                    className="rounded border border-white/15 bg-black/50 px-2 py-0.5 font-mono text-[11px] font-semibold text-white shadow-sm"
                  >
                    {k}
                  </kbd>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
