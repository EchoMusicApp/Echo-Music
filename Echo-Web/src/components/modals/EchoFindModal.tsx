import { useState, useEffect, useRef } from 'react'
import { Mic, RotateCcw, X, Volume2 } from 'lucide-react'
import { echoFind, type RecognitionResult } from '../../services/recognition/echoFindService'
import { useModalFocusTrap } from '../../hooks/useModalFocusTrap'

interface EchoFindModalProps {
  isOpen: boolean
  onClose: () => void
}

export function EchoFindModal({ isOpen, onClose }: EchoFindModalProps) {
  const [isListening, setIsListening] = useState(false)
  const [audioLevel, setAudioLevel] = useState(0)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const isMounted = useRef(true)
  const dialogRef = useRef<HTMLDivElement>(null)
  useModalFocusTrap(dialogRef, onClose, isOpen)

  const handleStartListening = async () => {
    setErrorMsg(null)
    setIsListening(true)
    setAudioLevel(0)

    try {
      const res: RecognitionResult = await echoFind.startListening((lvl) => {
        if (isMounted.current) {
          setAudioLevel(lvl)
        }
      }, 6000)

      if (isMounted.current) {
        setIsListening(false)
        if (!res.matched) {
          setErrorMsg(res.message || 'No matching music found.')
        }
      }
    } catch (err) {
      if (isMounted.current) {
        setIsListening(false)
        setErrorMsg(err instanceof Error ? err.message : 'Microphone access failed')
      }
    }
  }

  const handleClose = () => {
    echoFind.stopListening()
    setIsListening(false)
    setErrorMsg(null)
    setAudioLevel(0)
    onClose()
  }

  useEffect(() => {
    isMounted.current = true
    return () => {
      isMounted.current = false
      echoFind.stopListening()
    }
  }, [])

  useEffect(() => {
    if (!isOpen) return

    let active = true
    const timer = setTimeout(() => {
      if (active) {
        void handleStartListening()
      }
    }, 0)

    return () => {
      active = false
      clearTimeout(timer)
      echoFind.stopListening()
    }
  }, [isOpen])

  if (!isOpen) return null

  // Calculate radar pulse ring scale based on live microphone level
  const pulseScale = 1 + Math.min(0.6, audioLevel * 1.8)

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md"
      role="dialog"
      aria-modal="true"
      aria-label="Echo Find music recognition"
    >
      <div
        ref={dialogRef}
        className="relative w-full max-w-md rounded-2xl border border-white/10 bg-[#16141e] p-6 text-[#f4f1f7] shadow-2xl"
      >
        {/* Close Button */}
        <button
          type="button"
          onClick={handleClose}
          className="absolute top-4 right-4 rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
          title="Close (Esc)"
        >
          <X size={18} />
        </button>

        <div className="flex flex-col items-center text-center">
          <div className="mb-4 inline-flex items-center gap-1.5 rounded-full bg-[#7c5cff]/15 px-3 py-1 text-xs font-semibold tracking-wider text-[#b09cff] uppercase">
            <Volume2 size={13} />
            <span>Echo Find</span>
          </div>

          <h2 className="mb-1 text-xl font-bold text-white">
            {isListening ? 'Listening to audio...' : 'Music Recognition'}
          </h2>
          <p className="mb-8 max-w-xs text-xs text-[#a59fad]">
            {isListening
              ? 'Keep your device close to the audio source while Echo records a sample.'
              : errorMsg || 'Identify any song playing in your room or background.'}
          </p>

          {/* Radar Animation Stage */}
          {isListening && (
            <div className="relative mb-6 flex h-40 w-40 items-center justify-center">
              <div
                className="absolute inset-0 rounded-full border border-[#7c5cff]/20 bg-[#7c5cff]/10 transition-transform duration-75"
                style={{ transform: `scale(${pulseScale * 1.25})` }}
              />
              <div
                className="absolute inset-4 rounded-full border border-[#7c5cff]/40 bg-[#7c5cff]/20 transition-transform duration-75"
                style={{ transform: `scale(${pulseScale * 1.1})` }}
              />
              <div className="relative flex h-20 w-20 items-center justify-center rounded-full bg-[#7c5cff] text-black shadow-lg shadow-[#7c5cff]/30">
                <Mic size={32} className="animate-pulse" />
              </div>
            </div>
          )}

          {/* Error / No Match Notice */}
          {!isListening && errorMsg && (
            <div className="mb-6 w-full rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-left text-xs text-red-300">
              <p>{errorMsg}</p>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex w-full items-center justify-center gap-3">
            {isListening ? (
              <button
                type="button"
                onClick={() => {
                  echoFind.stopListening()
                  setIsListening(false)
                }}
                className="rounded-xl bg-white/8 px-5 py-2.5 text-xs font-semibold text-white transition-colors hover:bg-white/12"
              >
                Cancel
              </button>
            ) : (
              <button
                type="button"
                onClick={handleStartListening}
                className="flex items-center justify-center gap-2 rounded-xl bg-[#7c5cff] px-6 py-2.5 text-xs font-semibold text-black shadow-md transition-all hover:bg-[#6c48ff]"
              >
                <RotateCcw size={15} />
                <span>Listen Again</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
