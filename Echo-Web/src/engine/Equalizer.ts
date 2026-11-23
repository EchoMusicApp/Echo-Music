import type { EqualizerBand, EqualizerPreset } from '../types/audio'

export const EQUALIZER_FREQUENCIES = [32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000] as const

export const EQUALIZER_PRESETS: Record<string, EqualizerPreset> = {
  flat: {
    name: 'Flat',
    gains: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  },
  bassBoost: {
    name: 'Bass Boost',
    gains: [5.0, 4.0, 3.0, 1.5, 0.5, 0, 0, 0, 0, 0],
  },
  trebleBoost: {
    name: 'Treble Boost',
    gains: [0, 0, 0, 0, 0, 1.0, 2.0, 3.5, 4.5, 5.0],
  },
  vocal: {
    name: 'Vocal',
    gains: [-2.0, -1.0, 0, 1.5, 3.0, 3.0, 2.0, 1.0, 0, -1.0],
  },
  acoustic: {
    name: 'Acoustic',
    gains: [3.5, 3.0, 2.0, 1.0, 1.5, 1.5, 2.0, 2.5, 3.0, 2.5],
  },
  electronic: {
    name: 'Electronic',
    gains: [4.5, 3.5, 1.5, 0, -1.0, 1.0, 2.0, 3.0, 4.0, 4.5],
  },
  rock: {
    name: 'Rock',
    gains: [4.5, 3.0, 1.5, 0.5, -0.5, -0.5, 1.0, 2.5, 3.5, 4.0],
  },
}

export class Equalizer {
  private readonly context: AudioContext
  public readonly inputNode: GainNode
  public readonly outputNode: GainNode
  private readonly filters: BiquadFilterNode[] = []
  private readonly defaultQ = 1.414

  constructor(context: AudioContext) {
    this.context = context
    this.inputNode = context.createGain()
    this.outputNode = context.createGain()

    let previousNode: AudioNode = this.inputNode
    const now = this.context.currentTime
    const nyquistLimit = Math.max(20, Math.min(20000, this.context.sampleRate / 2 - 1))

    EQUALIZER_FREQUENCIES.forEach((freq, index) => {
      const filter = this.context.createBiquadFilter()
      const clampedFreq = Math.max(20, Math.min(freq, nyquistLimit))
      filter.frequency.setValueAtTime(clampedFreq, now)

      if (index === 0) {
        filter.type = 'lowshelf'
      } else if (index === EQUALIZER_FREQUENCIES.length - 1) {
        filter.type = 'highshelf'
      } else {
        filter.type = 'peaking'
        filter.Q.setValueAtTime(Math.max(0.01, this.defaultQ), now)
      }

      filter.gain.setValueAtTime(0, now)
      previousNode.connect(filter)
      previousNode = filter
      this.filters.push(filter)
    })

    previousNode.connect(this.outputNode)
  }

  public setGain(bandIndex: number, gainDb: number): void {
    const filter = this.filters[bandIndex]
    if (!filter) return

    const clampedGain = Math.max(-15, Math.min(15, gainDb))
    const now = this.context.currentTime
    filter.gain.cancelScheduledValues(now)
    filter.gain.setTargetAtTime(clampedGain, now, 0.05)
  }

  public getGains(): number[] {
    return this.filters.map((filter) => filter.gain.value)
  }

  public getBands(): EqualizerBand[] {
    return this.filters.map((filter, index) => ({
      index,
      frequency: filter.frequency.value,
      gain: filter.gain.value,
      type: filter.type,
      q: filter.Q.value,
    }))
  }

  public setPreset(name: string): void {
    const preset = EQUALIZER_PRESETS[name]
    if (!preset) return
    this.setAllGains(preset.gains)
  }

  public setAllGains(gains: number[]): void {
    gains.forEach((gain, index) => {
      this.setGain(index, gain)
    })
  }

  public reset(): void {
    const flatGains = EQUALIZER_PRESETS['flat']?.gains ?? [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    this.setAllGains(flatGains)
  }

  public cleanup(): void {
    const now = this.context.currentTime
    this.inputNode.disconnect()
    for (const filter of this.filters) {
      filter.gain.cancelScheduledValues(now)
      filter.disconnect()
    }
    this.outputNode.disconnect()
  }
}
