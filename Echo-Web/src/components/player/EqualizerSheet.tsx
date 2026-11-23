import { useState } from 'react'
import { Sliders, RotateCcw, Volume2, Sparkles, X } from 'lucide-react'
import { AudioEngine } from '../../engine/AudioEngine.ts'
import { EQUALIZER_FREQUENCIES, EQUALIZER_PRESETS } from '../../engine/Equalizer.ts'

interface EqualizerSheetProps {
  isOpen: boolean
  onClose: () => void
  currentGains: number[]
  stereoWidth: number
  volumeNormalization: boolean
  onGainsChange: (gains: number[]) => void
  onStereoWidthChange: (width: number) => void
  onNormalizationChange: (enabled: boolean) => void
}

const PRESET_LABELS: Record<string, string> = {
  flat: 'Flat',
  bassBoost: 'Bass Boost',
  trebleBoost: 'Treble Boost',
  vocal: 'Vocal',
  electronic: 'Electronic',
  rock: 'Rock',
  acoustic: 'Acoustic',
}

function formatFrequency(freq: number): string {
  if (freq >= 1000) {
    return `${freq / 1000}k`
  }
  return `${freq}`
}

export function EqualizerSheet({
  isOpen,
  onClose,
  currentGains,
  stereoWidth,
  volumeNormalization,
  onGainsChange,
  onStereoWidthChange,
  onNormalizationChange,
}: EqualizerSheetProps) {
  const [activePreset, setActivePreset] = useState<string>('custom')
  const engine = AudioEngine.getInstance()

  if (!isOpen) return null

  const handleBandChange = (index: number, value: number) => {
    const updated = [...currentGains]
    updated[index] = value
    setActivePreset('custom')
    onGainsChange(updated)
    engine.setEqualizerGain(index, value)
  }

  const handlePresetSelect = (presetKey: string) => {
    const preset = EQUALIZER_PRESETS[presetKey]
    if (!preset) return
    setActivePreset(presetKey)
    onGainsChange(preset.gains)
    engine.setEqualizerBands(preset.gains)
  }

  const handleReset = () => {
    setActivePreset('flat')
    engine.resetEqualizer()
    const flatGains = EQUALIZER_PRESETS['flat']?.gains ?? [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    onGainsChange(flatGains)
    onStereoWidthChange(1.0)
    engine.setStereoWidth(1.0)
  }

  return (
    <div
      className="equalizer-sheet-overlay fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{
        backgroundColor: 'rgba(11, 10, 15, 0.75)',
        backdropFilter: 'blur(16px)',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
      role="dialog"
      aria-label="Audio DSP & 10-Band Equalizer"
    >
      <div
        className="equalizer-sheet flex w-full max-w-2xl flex-col gap-6 rounded-xl border border-white/10 bg-[#14121a] p-6 text-[#f4f1f7] shadow-2xl"
        style={{
          boxShadow: '0 24px 60px rgba(0, 0, 0, 0.65)',
        }}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/8 pb-4">
          <div className="flex items-center gap-3">
            <span className="rounded-lg bg-[#7c5cff]/15 p-2 text-[#7c5cff]">
              <Sliders size={20} />
            </span>
            <div>
              <h2 className="text-lg font-semibold tracking-tight">Audio DSP & Equalizer</h2>
              <p className="text-xs text-[#a59fad]">
                10-Band Biquad Filtering & Mid/Side Spatial Matrix
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="flex items-center gap-1.5 rounded-md bg-white/6 px-3 py-1.5 text-xs font-medium text-[#a59fad] transition-colors hover:bg-white/10 hover:text-white"
              onClick={handleReset}
              title="Reset all EQ bands to flat"
            >
              <RotateCcw size={13} />
              Reset
            </button>
            <button
              type="button"
              className="rounded-md p-1.5 text-[#a59fad] transition-colors hover:bg-white/10 hover:text-white"
              onClick={onClose}
              aria-label="Close equalizer"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Presets Row */}
        <div>
          <label className="mb-2 block text-xs font-medium tracking-wider text-[#a59fad] uppercase">
            Presets
          </label>
          <div className="flex flex-wrap gap-2">
            {Object.keys(EQUALIZER_PRESETS).map((key) => {
              const isSelected = activePreset === key
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => handlePresetSelect(key)}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition-all ${
                    isSelected
                      ? 'bg-[#7c5cff] font-semibold text-[#0b0a0f] shadow-sm'
                      : 'bg-[#1e1b26] text-[#a59fad] hover:bg-[#282434] hover:text-white'
                  }`}
                >
                  {PRESET_LABELS[key] || key}
                </button>
              )
            })}
          </div>
        </div>

        {/* 10-Band Vertical Sliders */}
        <div className="rounded-lg border border-white/5 bg-[#181622] p-5">
          <div className="mb-3 flex items-center justify-between px-1 text-[11px] text-[#918a98]">
            <span>+15 dB</span>
            <span className="font-mono">0 dB (Flat)</span>
            <span>-15 dB</span>
          </div>

          <div className="grid grid-cols-10 items-center gap-2 text-center">
            {EQUALIZER_FREQUENCIES.map((freq, idx) => {
              const gain = currentGains[idx] ?? 0
              return (
                <div key={freq} className="flex flex-col items-center gap-2">
                  <span className="font-mono text-[10px] font-medium text-[#a59fad]">
                    {gain > 0 ? `+${gain.toFixed(1)}` : gain.toFixed(1)}
                  </span>
                  <div className="flex h-36 items-center justify-center">
                    <input
                      type="range"
                      aria-label={`${formatFrequency(freq)} Hz band`}
                      min={-15}
                      max={15}
                      step={0.5}
                      value={gain}
                      onChange={(e) => handleBandChange(idx, parseFloat(e.target.value))}
                      className="eq-vertical-slider"
                      style={{
                        writingMode: 'vertical-lr',
                        direction: 'rtl',
                        height: '136px',
                        width: '16px',
                        accentColor: '#7c5cff',
                        cursor: 'pointer',
                      }}
                    />
                  </div>
                  <span className="text-[11px] font-medium text-[#f4f1f7]">
                    {formatFrequency(freq)}
                  </span>
                </div>
              )
            })}
          </div>
        </div>

        {/* Spatial Stereo Width & Dynamics Normalization */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {/* Stereo Widener */}
          <div className="flex flex-col gap-2 rounded-lg border border-white/5 bg-[#181622] p-4">
            <div className="flex items-center justify-between text-xs">
              <span className="flex items-center gap-1.5 font-medium text-[#f4f1f7]">
                <Sparkles size={14} className="text-[#7c5cff]" />
                Spatial Stereo Width
              </span>
              <span className="font-mono text-[#a59fad]">
                {stereoWidth.toFixed(2)}x
                {stereoWidth === 0 ? ' (Mono)' : stereoWidth === 1.0 ? ' (Standard)' : ' (Wide)'}
              </span>
            </div>
            <input
              type="range"
              aria-label="Stereo Width"
              min={0.0}
              max={2.0}
              step={0.05}
              value={stereoWidth}
              onChange={(e) => {
                const val = parseFloat(e.target.value)
                onStereoWidthChange(val)
                engine.setStereoWidth(val)
              }}
              className="w-full cursor-pointer accent-[#7c5cff]"
            />
            <div className="flex justify-between text-[10px] text-[#918a98]">
              <span>0.0x (Mono)</span>
              <span>1.0x (Normal)</span>
              <span>2.0x (Ultra-Wide)</span>
            </div>
          </div>

          {/* Volume Normalization (Compressor) */}
          <div className="flex items-center justify-between rounded-lg border border-white/5 bg-[#181622] p-4">
            <div className="flex flex-col gap-0.5">
              <span className="flex items-center gap-1.5 text-xs font-medium text-[#f4f1f7]">
                <Volume2 size={14} className="text-[#50b68a]" />
                Dynamics Normalization
              </span>
              <span className="text-[11px] text-[#918a98]">
                Smooth loudness across different song masters
              </span>
            </div>
            <label className="relative inline-flex cursor-pointer items-center">
              <input
                type="checkbox"
                checked={volumeNormalization}
                onChange={(e) => {
                  const val = e.target.checked
                  onNormalizationChange(val)
                  engine.setVolumeNormalization(val)
                }}
                className="peer sr-only"
                aria-label="Toggle Volume Normalization"
              />
              <div className="peer h-6 w-11 rounded-full bg-[#2a2636] peer-checked:bg-[#7c5cff] peer-focus:outline-none after:absolute after:start-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all after:content-[''] peer-checked:after:translate-x-full peer-checked:after:border-white" />
            </label>
          </div>
        </div>
      </div>
    </div>
  )
}
