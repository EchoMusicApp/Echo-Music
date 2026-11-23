import { Equalizer } from './Equalizer'
import { StereoWidener } from './StereoWidener'

export interface WebAudioGraphOptions {
  volumeNormalization?: boolean
  stereoWidth?: number
  fftSize?: number
  smoothingTimeConstant?: number
}

export class WebAudioGraph {
  public readonly context: AudioContext
  public readonly deckAGain: GainNode
  public readonly deckBGain: GainNode
  public readonly masterDeckMixer: GainNode

  public readonly equalizer: Equalizer
  public readonly stereoWidener: StereoWidener

  public readonly compressor: DynamicsCompressorNode
  private readonly compressorInput: GainNode
  private readonly compressorWetGain: GainNode
  private readonly compressorDryGain: GainNode
  private readonly compressorOutput: GainNode

  public readonly masterGain: GainNode
  public readonly loudnessGain: GainNode
  public readonly analyser: AnalyserNode

  private readonly frequencyDataBuffer: Uint8Array<ArrayBuffer>
  private readonly timeDomainBuffer: Uint8Array<ArrayBuffer>
  private readonly beatEnergyBuffer: Uint8Array<ArrayBuffer>
  private readonly trackGainMap = new Map<string, number>()

  private isNormalized = false
  private isDestroyed = false

  constructor(options: WebAudioGraphOptions = {}) {
    const AudioContextClass =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    this.context = new AudioContextClass()

    const now = this.context.currentTime

    this.deckAGain = this.context.createGain()
    this.deckBGain = this.context.createGain()
    this.masterDeckMixer = this.context.createGain()

    this.deckAGain.gain.setValueAtTime(1.0, now)
    this.deckBGain.gain.setValueAtTime(0.0, now)

    this.deckAGain.connect(this.masterDeckMixer)
    this.deckBGain.connect(this.masterDeckMixer)

    this.equalizer = new Equalizer(this.context)
    this.stereoWidener = new StereoWidener(this.context)
    if (typeof options.stereoWidth === 'number') {
      this.stereoWidener.setWidth(options.stereoWidth)
    }

    this.masterDeckMixer.connect(this.equalizer.inputNode)
    this.equalizer.outputNode.connect(this.stereoWidener.inputNode)

    this.compressorInput = this.context.createGain()
    this.compressorOutput = this.context.createGain()
    this.compressorWetGain = this.context.createGain()
    this.compressorDryGain = this.context.createGain()

    this.compressor = this.context.createDynamicsCompressor()
    this.compressor.threshold.setValueAtTime(-14, now)
    this.compressor.knee.setValueAtTime(12, now)
    this.compressor.ratio.setValueAtTime(4, now)
    this.compressor.attack.setValueAtTime(0.003, now)
    this.compressor.release.setValueAtTime(0.25, now)

    this.stereoWidener.outputNode.connect(this.compressorInput)

    this.compressorInput.connect(this.compressor)
    this.compressor.connect(this.compressorWetGain)
    this.compressorWetGain.connect(this.compressorOutput)

    this.compressorInput.connect(this.compressorDryGain)
    this.compressorDryGain.connect(this.compressorOutput)

    this.setVolumeNormalization(options.volumeNormalization ?? false)

    this.loudnessGain = this.context.createGain()
    this.loudnessGain.gain.setValueAtTime(1.0, now)
    this.compressorOutput.connect(this.loudnessGain)

    this.masterGain = this.context.createGain()
    this.masterGain.gain.setValueAtTime(1.0, now)
    this.loudnessGain.connect(this.masterGain)

    this.analyser = this.context.createAnalyser()
    this.analyser.fftSize = options.fftSize ?? 512
    this.analyser.smoothingTimeConstant = options.smoothingTimeConstant ?? 0.8

    this.masterGain.connect(this.analyser)
    this.analyser.connect(this.context.destination)

    this.frequencyDataBuffer = new Uint8Array(this.analyser.frequencyBinCount)
    this.timeDomainBuffer = new Uint8Array(this.analyser.frequencyBinCount)
    this.beatEnergyBuffer = new Uint8Array(this.analyser.frequencyBinCount)
  }

  public setVolumeNormalization(enable: boolean): void {
    this.isNormalized = enable
    const now = this.context.currentTime
    this.compressorDryGain.gain.cancelScheduledValues(now)
    this.compressorWetGain.gain.cancelScheduledValues(now)
    if (enable) {
      this.compressorDryGain.gain.setTargetAtTime(0.0, now, 0.05)
      this.compressorWetGain.gain.setTargetAtTime(1.0, now, 0.05)
    } else {
      this.compressorDryGain.gain.setTargetAtTime(1.0, now, 0.05)
      this.compressorWetGain.gain.setTargetAtTime(0.0, now, 0.05)
    }
  }

  public getVolumeNormalization(): boolean {
    return this.isNormalized
  }

  public setTrackLoudnessTarget(trackId: string, currentRms: number): void {
    if (!this.isNormalized) {
      this.loudnessGain.gain.setValueAtTime(1.0, this.context.currentTime)
      return
    }
    // Target RMS around ~0.10 (-20 dBFS, matching roughly -14 LUFS standard)
    const TARGET_RMS = 0.1
    let gain = this.trackGainMap.get(trackId)
    if (gain === undefined && currentRms > 0.001) {
      // Clamp compensation between 0.5 (-6dB) and 2.0 (+6dB) to avoid distortion
      gain = Math.max(0.5, Math.min(2.0, TARGET_RMS / currentRms))
      this.trackGainMap.set(trackId, gain)
    }
    const effectiveGain = gain ?? 1.0
    this.loudnessGain.gain.setTargetAtTime(effectiveGain, this.context.currentTime, 0.1)
  }

  public calculateRMS(): number {
    const buffer = new Float32Array(this.analyser.frequencyBinCount)
    this.analyser.getFloatTimeDomainData(buffer)
    let sum = 0
    for (let i = 0; i < buffer.length; i++) {
      const val = buffer[i] ?? 0
      sum += val * val
    }
    return Math.sqrt(sum / buffer.length)
  }

  public isSilent(): boolean {
    return this.calculateRMS() < 0.005
  }

  public setMasterVolume(volume: number, muted = false): void {
    const target = muted ? 0.0 : Math.max(0.0, Math.min(1.0, volume))
    const now = this.context.currentTime
    this.masterGain.gain.cancelScheduledValues(now)
    this.masterGain.gain.setTargetAtTime(target, now, 0.02)
  }

  public getFrequencyData(outputArray?: Uint8Array<ArrayBuffer>): Uint8Array<ArrayBuffer> {
    const data = outputArray ?? this.frequencyDataBuffer
    this.analyser.getByteFrequencyData(data)
    return data
  }

  public getTimeDomainData(outputArray?: Uint8Array<ArrayBuffer>): Uint8Array<ArrayBuffer> {
    const data = outputArray ?? this.timeDomainBuffer
    this.analyser.getByteTimeDomainData(data)
    return data
  }

  public getBeatEnergy(): number {
    this.analyser.getByteFrequencyData(this.beatEnergyBuffer)

    const binCount = Math.min(16, this.beatEnergyBuffer.length)
    if (binCount === 0) return 0

    let sum = 0
    for (let i = 0; i < binCount; i++) {
      const val = this.beatEnergyBuffer[i] ?? 0
      sum += val * val
    }

    const rms = Math.sqrt(sum / binCount)
    return Math.min(1.0, rms / 255)
  }

  public async resume(): Promise<void> {
    if (this.context.state === 'suspended') {
      await this.context.resume()
    }
  }

  public destroy(): void {
    if (this.isDestroyed) return
    this.isDestroyed = true

    const now = this.context.currentTime
    this.deckAGain.gain.cancelScheduledValues(now)
    this.deckBGain.gain.cancelScheduledValues(now)
    this.masterDeckMixer.gain.cancelScheduledValues(now)
    this.compressorDryGain.gain.cancelScheduledValues(now)
    this.compressorWetGain.gain.cancelScheduledValues(now)
    this.compressorInput.gain.cancelScheduledValues(now)
    this.compressorOutput.gain.cancelScheduledValues(now)
    this.masterGain.gain.cancelScheduledValues(now)

    this.deckAGain.disconnect()
    this.deckBGain.disconnect()
    this.masterDeckMixer.disconnect()

    this.equalizer.cleanup()
    this.stereoWidener.cleanup()

    this.compressorInput.disconnect()
    this.compressor.disconnect()
    this.compressorWetGain.disconnect()
    this.compressorDryGain.disconnect()
    this.compressorOutput.disconnect()

    this.masterGain.disconnect()
    this.analyser.disconnect()

    if (this.context.state !== 'closed') {
      void this.context.close()
    }
  }
}
