import { useEffect, useRef } from 'react'
import { AudioEngine } from '../../engine/AudioEngine.ts'

interface VisualizerProps {
  isPlaying: boolean
  className?: string
  barCount?: number
  accentColor?: string
}

export function Visualizer({
  isPlaying,
  className = '',
  barCount = 48,
  accentColor = '#7c5cff',
}: VisualizerProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const engine = AudioEngine.getInstance()
    // Pre-allocated typed arrays to prevent heap allocations inside the 60 FPS RAF loop
    const frequencyData = new Uint8Array(128)
    const smoothedHeights = new Float32Array(barCount)

    let animationFrameId: number | null = null
    let isVisible = true

    // Check visibility via IntersectionObserver
    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry) {
          isVisible = entry.isIntersecting
        }
      },
      { threshold: 0.05 },
    )
    observer.observe(canvas)

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      const rect = canvas.getBoundingClientRect()
      const width = Math.floor(rect.width)
      const height = Math.floor(rect.height)

      if (width > 0 && height > 0) {
        canvas.width = width * dpr
        canvas.height = height * dpr
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      }
    }

    resize()
    const resizeObserver = new ResizeObserver(resize)
    resizeObserver.observe(canvas)

    // Attenuation & smoothing factor (linear interpolation)
    const decay = 0.88
    const attack = 0.65

    const render = () => {
      if (!isPlaying || !isVisible) {
        // Clear canvas smoothly when stopped
        const rect = canvas.getBoundingClientRect()
        ctx.clearRect(0, 0, rect.width, rect.height)
        animationFrameId = null
        return
      }

      const rect = canvas.getBoundingClientRect()
      const width = rect.width
      const height = rect.height

      ctx.clearRect(0, 0, width, height)

      // Fetch latest frequency byte data directly from AnalyserNode
      engine.getFrequencyData(frequencyData)

      const totalBins = frequencyData.length
      // Focus on lower to mid frequencies where musical energy lives
      const usableBins = Math.min(totalBins, 72)
      const step = usableBins / barCount

      const barWidth = Math.max(2, width / barCount - 3)
      const cornerRadius = Math.min(2, barWidth / 2)

      for (let i = 0; i < barCount; i++) {
        const binIndex = Math.floor(i * step)
        const rawValue = (frequencyData[binIndex] || 0) / 255.0
        const targetHeight = Math.pow(rawValue, 1.2) * (height - 4)

        // Smooth height transition to eliminate jitter during transient drops
        const currentHeight = smoothedHeights[i] ?? 0
        const newHeight =
          targetHeight > currentHeight
            ? currentHeight + (targetHeight - currentHeight) * attack
            : currentHeight * decay

        smoothedHeights[i] = newHeight

        const x = i * (barWidth + 3)
        const y = height - newHeight

        // Subtle gradient on each bar
        const gradient = ctx.createLinearGradient(0, y, 0, height)
        gradient.addColorStop(0, accentColor)
        gradient.addColorStop(1, 'rgba(124, 92, 255, 0.25)')

        ctx.fillStyle = gradient
        ctx.beginPath()
        if (typeof ctx.roundRect === 'function') {
          ctx.roundRect(x, Math.max(0, y), barWidth, Math.max(2, newHeight), [
            cornerRadius,
            cornerRadius,
            0,
            0,
          ])
        } else {
          ctx.rect(x, Math.max(0, y), barWidth, Math.max(2, newHeight))
        }
        ctx.fill()
      }

      animationFrameId = requestAnimationFrame(render)
    }

    if (isPlaying) {
      animationFrameId = requestAnimationFrame(render)
    }

    return () => {
      if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId)
      }
      observer.disconnect()
      resizeObserver.disconnect()
    }
  }, [isPlaying, barCount, accentColor])

  return (
    <canvas
      ref={canvasRef}
      className={`visualizer-canvas pointer-events-none block h-full w-full select-none ${className}`}
      aria-label="Real-time audio visualizer"
    />
  )
}
