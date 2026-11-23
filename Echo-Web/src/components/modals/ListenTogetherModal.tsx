import { useState, useEffect, useRef } from 'react'
import { Copy, Check, Radio, Users, LogOut, X, Sparkles, Disc } from 'lucide-react'
import {
  listenTogether,
  type SynchronizedRoomState,
} from '../../services/listenTogether/listenTogetherClient'
import { useEchoStore } from '../../stores/useEchoStore'
import { useAudioEngine } from '../../hooks/useAudioEngine'
import { useModalFocusTrap } from '../../hooks/useModalFocusTrap'

interface ListenTogetherModalProps {
  isOpen: boolean
  onClose: () => void
}

export function ListenTogetherModal({ isOpen, onClose }: ListenTogetherModalProps) {
  const [roomState, setRoomState] = useState<SynchronizedRoomState | null>(() =>
    listenTogether.getState(),
  )
  const [joinCodeInput, setJoinCodeInput] = useState('')
  const [nickInput, setNickInput] = useState('Listener')
  const [isLoading, setIsLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [copiedLink, setCopiedLink] = useState(false)
  const dialogRef = useRef<HTMLDivElement>(null)
  useModalFocusTrap(dialogRef, onClose, isOpen)

  const setToast = useEchoStore((state) => state.setToast)
  const { currentTrack, isPlaying, currentTime, play, pause, seekTo } = useAudioEngine()

  useEffect(() => {
    const unsub = listenTogether.subscribe((state) => {
      setRoomState(state.code ? state : null)

      // If guest and host state changed, synchronize local player
      if (state.code && !state.isHost && state.track) {
        const localTrackId = currentTrack?.id
        const remoteTrackId = state.track.id

        if (localTrackId !== remoteTrackId) {
          play(state.track)
        }

        if (state.isPlaying !== isPlaying) {
          if (state.isPlaying) {
            play()
          } else {
            pause()
          }
        }

        // Latency drift adjustment
        if (Math.abs(currentTime - state.playbackPositionSeconds) > 2.5) {
          seekTo(state.playbackPositionSeconds)
        }
      }
    })
    return () => unsub()
  }, [currentTrack, isPlaying, currentTime, play, pause, seekTo])

  // If host, push local playback changes to listening room
  useEffect(() => {
    if (roomState?.isHost) {
      listenTogether.updateHostPlayback(currentTrack, isPlaying, currentTime)
    }
  }, [roomState?.isHost, currentTrack, isPlaying, currentTime])

  const handleCreateRoom = async () => {
    setIsLoading(true)
    setErrorMsg(null)
    try {
      const state = await listenTogether.createRoom(nickInput.trim() || 'Host')
      setRoomState(state)
      setToast(`Listening room ${state.code} created`)
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Failed to create room')
    } finally {
      setIsLoading(false)
    }
  }

  const handleJoinRoom = async () => {
    if (!joinCodeInput.trim()) {
      setErrorMsg('Please enter a 6-character room code')
      return
    }
    setIsLoading(true)
    setErrorMsg(null)
    try {
      const state = await listenTogether.joinRoom(joinCodeInput.trim(), nickInput.trim() || 'Guest')
      setRoomState(state)
      setToast(`Joined room ${state.code}`)
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Could not join room')
    } finally {
      setIsLoading(false)
    }
  }

  const handleLeaveRoom = () => {
    listenTogether.leaveRoom()
    setRoomState(null)
    setToast('Left listening room')
  }

  const handleCopyInvite = () => {
    if (!roomState?.code) return
    const inviteUrl = `${window.location.origin}/?listen=${roomState.code}`
    navigator.clipboard.writeText(inviteUrl)
    setCopiedLink(true)
    setToast('Room invite link copied to clipboard')
    setTimeout(() => setCopiedLink(false), 2000)
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md"
      role="dialog"
      aria-modal="true"
      aria-label="Listen Together synchronized room"
    >
      <div
        ref={dialogRef}
        className="relative w-full max-w-lg rounded-2xl border border-white/10 bg-[#16141e] p-6 text-[#f4f1f7] shadow-2xl"
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 rounded-lg bg-white/4 p-2 text-[#a59fad] transition-colors hover:bg-white/8 hover:text-white"
          title="Close (Esc)"
        >
          <X size={18} />
        </button>

        <div className="mb-4 flex items-center gap-2">
          <div className="rounded-xl bg-[#7c5cff]/15 p-2 text-[#b09cff]">
            <Radio size={20} />
          </div>
          <div>
            <h2 className="text-lg leading-tight font-bold text-white">Listen Together</h2>
            <p className="text-xs text-[#a59fad]">
              Synchronized group audio playback across devices.
            </p>
          </div>
        </div>

        {errorMsg && (
          <div className="mb-4 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-300">
            {errorMsg}
          </div>
        )}

        {roomState?.code ? (
          /* Active Room View */
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between rounded-xl border border-white/8 bg-white/4 p-4">
              <div>
                <span className="font-mono text-[11px] tracking-widest text-[#a59fad] uppercase">
                  Room Code
                </span>
                <div className="mt-0.5 font-mono text-2xl font-bold tracking-wider text-white">
                  {roomState.code}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleCopyInvite}
                  className="flex items-center gap-1.5 rounded-lg bg-[#7c5cff]/20 px-3 py-1.5 text-xs font-semibold text-[#b09cff] transition-colors hover:bg-[#7c5cff]/30"
                >
                  {copiedLink ? <Check size={14} /> : <Copy size={14} />}
                  <span>{copiedLink ? 'Copied' : 'Share Link'}</span>
                </button>
                <button
                  type="button"
                  onClick={handleLeaveRoom}
                  className="flex items-center gap-1 rounded-lg bg-red-500/15 px-3 py-1.5 text-xs font-semibold text-red-300 transition-colors hover:bg-red-500/25"
                  title="Leave Room"
                >
                  <LogOut size={14} />
                  <span>Leave</span>
                </button>
              </div>
            </div>

            {/* Currently Playing in Room */}
            <div className="flex items-center gap-3 rounded-xl border border-white/5 bg-white/2 p-3">
              <Disc size={20} className="shrink-0 animate-spin text-[#7c5cff]" />
              <div className="min-w-0 flex-1 text-xs">
                <span className="block text-[#a59fad]">Synchronizing:</span>
                <span className="block truncate font-medium text-white">
                  {roomState.track
                    ? `${roomState.track.title} - ${roomState.track.artistName}`
                    : 'Idle (Waiting for host to play)'}
                </span>
              </div>
              <span className="font-mono text-[11px] text-[#a59fad]">
                {roomState.isHost ? 'You are Host' : 'Synced as Guest'}
              </span>
            </div>

            {/* Participants Roster */}
            <div>
              <div className="mb-2 flex items-center justify-between text-xs font-medium text-[#a59fad]">
                <span className="flex items-center gap-1.5">
                  <Users size={14} />
                  <span>Participants ({roomState.members.length})</span>
                </span>
              </div>
              <div className="custom-scrollbar flex max-h-36 flex-col gap-1.5 overflow-y-auto pr-1">
                {roomState.members.map((member) => (
                  <div
                    key={member.id}
                    className="flex items-center justify-between rounded-lg border border-white/4 bg-white/3 px-3 py-2 text-xs"
                  >
                    <span className="flex items-center gap-1.5 font-medium text-white">
                      {member.name}
                      {member.isHost && (
                        <span className="py-0.2 rounded bg-[#7c5cff]/20 px-1.5 font-mono text-[10px] text-[#b09cff]">
                          HOST
                        </span>
                      )}
                    </span>
                    <span className="flex items-center gap-1 font-mono text-[10px] text-[#58ad83]">
                      <span className="h-1.5 w-1.5 rounded-full bg-[#58ad83]" />
                      <span>Connected</span>
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Event Logs */}
            {roomState.logs.length > 0 && (
              <div>
                <span className="mb-1 block font-mono text-[11px] tracking-wider text-[#a59fad] uppercase">
                  Room Activity
                </span>
                <div className="custom-scrollbar max-h-24 overflow-y-auto rounded-xl border border-white/4 bg-black/30 p-2.5 font-mono text-[11px] text-[#a59fad]">
                  {roomState.logs.slice(-5).map((log) => (
                    <div key={log.id} className="py-0.5">
                      {log.message}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          /* Create / Join Room Form */
          <div className="flex flex-col gap-5">
            <div>
              <label className="mb-1.5 block text-xs font-medium text-[#a59fad]">
                Your Display Nickname
              </label>
              <input
                type="text"
                value={nickInput}
                onChange={(e) => setNickInput(e.target.value)}
                placeholder="Listener"
                className="w-full rounded-xl border border-white/10 bg-white/4 px-3 py-2 text-sm text-white focus:border-[#7c5cff] focus:outline-none"
                maxLength={24}
              />
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {/* Create Card */}
              <div className="flex flex-col justify-between rounded-xl border border-white/8 bg-white/4 p-4">
                <div>
                  <h3 className="mb-1 flex items-center gap-1.5 text-sm font-bold text-white">
                    <Sparkles size={15} className="text-[#7c5cff]" />
                    <span>Host a Room</span>
                  </h3>
                  <p className="mb-4 text-xs text-[#a59fad]">
                    Generate a room code and invite friends to hear your queue in real time.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleCreateRoom}
                  disabled={isLoading}
                  className="w-full rounded-lg bg-[#7c5cff] px-3 py-2 text-xs font-semibold text-black transition-colors hover:bg-[#6c48ff] disabled:opacity-50"
                >
                  {isLoading ? 'Creating...' : 'Create Room'}
                </button>
              </div>

              {/* Join Card */}
              <div className="flex flex-col justify-between rounded-xl border border-white/8 bg-white/4 p-4">
                <div>
                  <h3 className="mb-1 flex items-center gap-1.5 text-sm font-bold text-white">
                    <Radio size={15} className="text-[#58ad83]" />
                    <span>Join Room</span>
                  </h3>
                  <p className="mb-2 text-xs text-[#a59fad]">
                    Enter a 6-character room code to join an active friend's session.
                  </p>
                  <input
                    type="text"
                    value={joinCodeInput}
                    onChange={(e) => setJoinCodeInput(e.target.value.toUpperCase())}
                    placeholder="E.g. ABC123"
                    className="mb-3 w-full rounded-lg border border-white/10 bg-black/40 px-3 py-1.5 text-center font-mono text-sm tracking-wider text-white uppercase focus:border-[#7c5cff] focus:outline-none"
                    maxLength={6}
                  />
                </div>
                <button
                  type="button"
                  onClick={handleJoinRoom}
                  disabled={isLoading || !joinCodeInput.trim()}
                  className="w-full rounded-lg bg-white/8 px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-white/12 disabled:opacity-50"
                >
                  {isLoading ? 'Joining...' : 'Join Room'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
