import { useEffect, useRef, useState } from 'react'
import type { Track } from '../../types/domain.ts'
import { CanvasManager } from '../../services/canvas/CanvasManager.ts'
import { artwork } from '../../utils/artwork.ts'

interface CanvasVideoProps {
  track: Track | null
  className?: string
}

export function CanvasVideo({ track, className = '' }: CanvasVideoProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const [videoUrl, setVideoUrl] = useState<string | null>(null)
  const [isVideoReady, setIsVideoReady] = useState(false)
  const [hasError, setHasError] = useState(false)

  const coverArt =
    track?.artwork ||
    track?.thumbnailUrl ||
    track?.albumImage ||
    artwork(track?.title || 'echo-track', 800)

  useEffect(() => {
    const video = videoRef.current
    if (!track) {
      return () => {
        if (video) {
          video.pause()
          video.removeAttribute('src')
          video.load()
        }
      }
    }

    const abortController = new AbortController()

    CanvasManager.getInstance()
      .resolveCanvasVideo(track, abortController.signal)
      .then((result) => {
        if (!abortController.signal.aborted) {
          setVideoUrl(result?.videoUrl ?? null)
          setIsVideoReady(false)
          setHasError(false)
        }
      })
      .catch(() => {
        if (!abortController.signal.aborted) {
          setVideoUrl(null)
          setIsVideoReady(false)
        }
      })

    return () => {
      abortController.abort()
      if (video) {
        video.pause()
        video.removeAttribute('src')
        video.load()
      }
    }
  }, [track])

  useEffect(() => {
    const video = videoRef.current
    if (!video || !videoUrl) return

    setIsVideoReady(false)
    setHasError(false)

    const handleCanPlay = () => {
      setIsVideoReady(true)
      video.play().catch(() => {
        // Autoplay may be restricted by browser until user gesture
      })
    }

    const handleError = () => {
      setHasError(true)
      setIsVideoReady(false)
    }

    video.addEventListener('canplaythrough', handleCanPlay)
    video.addEventListener('error', handleError)

    video.src = videoUrl
    video.load()

    return () => {
      video.removeEventListener('canplaythrough', handleCanPlay)
      video.removeEventListener('error', handleError)
      video.pause()
      video.removeAttribute('src')
      video.load()
    }
  }, [videoUrl])

  return (
    <div
      className={`canvas-video-container pointer-events-none relative inset-0 h-full w-full overflow-hidden select-none ${className}`}
      aria-hidden="true"
    >
      {/* Fallback Artwork Layer with Ambient Blur */}
      <div
        className="canvas-artwork-layer absolute inset-0 bg-cover bg-center transition-opacity duration-700 ease-out"
        style={{
          backgroundImage: `url(${coverArt})`,
          transform: 'scale(1.15)',
          filter: 'blur(56px) saturate(160%) brightness(0.65)',
        }}
      />

      {/* Looping Canvas Video Layer */}
      {videoUrl && !hasError && (
        <video
          ref={videoRef}
          className={`canvas-video-element absolute inset-0 h-full w-full object-cover transition-opacity duration-400 ease-in-out ${
            isVideoReady ? 'opacity-90' : 'opacity-0'
          }`}
          style={{
            transform: 'scale(1.05)',
            filter: 'contrast(105%) brightness(0.85)',
          }}
          autoPlay
          muted
          loop
          playsInline
          preload="auto"
        />
      )}

      {/* Surface Depth Tint & Frosting Overlay */}
      <div
        className="canvas-tint-layer absolute inset-0"
        style={{
          background:
            'radial-gradient(circle at 50% 40%, rgba(11, 10, 15, 0.4) 0%, rgba(11, 10, 15, 0.85) 100%)',
        }}
      />
    </div>
  )
}
