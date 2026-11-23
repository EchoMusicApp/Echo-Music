import type { Track } from '../../types/domain'

export interface RecognitionResult {
  matched: boolean
  confidence?: number
  track?: Track
  message?: string
}

export class EchoFindService {
  private mediaStream: MediaStream | null = null
  private audioContext: AudioContext | null = null
  private analyser: AnalyserNode | null = null
  private mediaRecorder: MediaRecorder | null = null
  private isListening = false

  public async startListening(
    onAudioLevel?: (level: number) => void,
    durationMs = 6000,
  ): Promise<RecognitionResult> {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw new Error('Microphone access is not supported by your browser')
    }

    this.isListening = true
    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: false,
        noiseSuppression: false,
        autoGainControl: true,
      },
    })

    // Setup real-time audio analyser for UI pulsing feedback
    const AudioContextClass =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    this.audioContext = new AudioContextClass()
    const sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream)
    this.analyser = this.audioContext.createAnalyser()
    this.analyser.fftSize = 256
    sourceNode.connect(this.analyser)

    const dataArray = new Uint8Array(this.analyser.frequencyBinCount)
    let animationFrameId: number | null = null

    const checkAudioLevel = () => {
      if (!this.isListening || !this.analyser) return
      this.analyser.getByteFrequencyData(dataArray)
      let sum = 0
      for (let i = 0; i < dataArray.length; i++) {
        sum += dataArray[i]!
      }
      const avg = sum / dataArray.length / 255
      onAudioLevel?.(avg)
      animationFrameId = requestAnimationFrame(checkAudioLevel)
    }

    if (onAudioLevel) {
      animationFrameId = requestAnimationFrame(checkAudioLevel)
    }

    // Record audio slice
    const recordedChunks: Blob[] = []
    const mimeType = MediaRecorder.isTypeSupported('audio/webm')
      ? 'audio/webm'
      : MediaRecorder.isTypeSupported('audio/mp4')
        ? 'audio/mp4'
        : 'audio/wav'

    this.mediaRecorder = new MediaRecorder(this.mediaStream, { mimeType })
    this.mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        recordedChunks.push(event.data)
      }
    }

    this.mediaRecorder.start(250)

    // Wait for listening duration
    await new Promise((resolve) => setTimeout(resolve, durationMs))

    // Stop recording and cleanup mic
    if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId)
    }
    this.stopListening()

    const audioBlob = new Blob(recordedChunks, { type: mimeType })
    if (audioBlob.size < 1000) {
      return {
        matched: false,
        message: 'Could not record sufficient audio. Please try again.',
      }
    }

    // Send audio to recognition backend
    const formData = new FormData()
    formData.append('audio', audioBlob, 'echo_recording.webm')

    try {
      const res = await fetch('/api/recognition/identify', {
        method: 'POST',
        body: formData,
      })

      if (!res.ok) {
        throw new Error(`Recognition failed with HTTP ${res.status}`)
      }

      const result = (await res.json()) as RecognitionResult
      return result
    } catch (err) {
      return {
        matched: false,
        message: err instanceof Error ? err.message : 'Unable to connect to recognition service',
      }
    }
  }

  public stopListening(): void {
    this.isListening = false

    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      try {
        this.mediaRecorder.stop()
      } catch {
        // Ignore
      }
      this.mediaRecorder = null
    }

    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop())
      this.mediaStream = null
    }

    if (this.audioContext && this.audioContext.state !== 'closed') {
      try {
        this.audioContext.close()
      } catch {
        // Ignore
      }
      this.audioContext = null
    }

    this.analyser = null
  }
}

export const echoFind = new EchoFindService()
