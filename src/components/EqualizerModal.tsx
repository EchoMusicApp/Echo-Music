import React from 'react';
import { usePlayer } from '../context/PlayerContext';
import { X, Sliders, Check, Volume2 } from 'lucide-react';
import { EqualizerSettings } from '../types/music';

interface EqualizerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const EqualizerModal: React.FC<EqualizerModalProps> = ({ isOpen, onClose }) => {
  const { equalizer, updateEqualizer } = usePlayer();

  if (!isOpen) return null;

  const presets: { id: EqualizerSettings['preset']; label: string; bass: number; mid: number; treble: number }[] = [
    { id: 'flat', label: 'Flat', bass: 0, mid: 0, treble: 0 },
    { id: 'bass_boost', label: 'Bass Booster', bass: 6, mid: 1, treble: 2 },
    { id: 'electronic', label: 'Electronic', bass: 5, mid: -1, treble: 5 },
    { id: 'rock', label: 'Rock', bass: 4, mid: 3, treble: 4 },
    { id: 'vocal', label: 'Vocal Boost', bass: -2, mid: 5, treble: 3 },
  ];

  const applyPreset = (preset: typeof presets[0]) => {
    updateEqualizer({
      preset: preset.id,
      bass: preset.bass,
      mid: preset.mid,
      treble: preset.treble,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-sm rounded-3xl bg-[#151a21] border border-white/10 p-6 shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-red-500/20 text-red-400">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base text-white font-outfit">Equalizer</h3>
              <p className="text-xs text-slate-400">Real-time Web Audio Filter</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Master Switch */}
        <div className="flex items-center justify-between py-3 my-2 px-3 rounded-2xl bg-white/5 border border-white/10">
          <span className="text-sm font-semibold text-slate-200">Enable Equalizer</span>
          <button
            onClick={() => updateEqualizer({ enabled: !equalizer.enabled })}
            className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors ${
              equalizer.enabled ? 'bg-red-600 justify-end' : 'bg-slate-700 justify-start'
            }`}
          >
            <span className="w-4 h-4 rounded-full bg-white shadow-md"></span>
          </button>
        </div>

        {/* Presets */}
        <div className="mt-3">
          <label className="text-xs font-semibold uppercase tracking-wider text-slate-400 block mb-2">
            Sound Presets
          </label>
          <div className="grid grid-cols-3 gap-2">
            {presets.map((p) => {
              const isSelected = equalizer.preset === p.id;
              return (
                <button
                  key={p.id}
                  onClick={() => applyPreset(p)}
                  disabled={!equalizer.enabled}
                  className={`text-xs py-2 px-1.5 rounded-xl border font-medium transition-all truncate text-center ${
                    isSelected
                      ? 'bg-red-500/20 border-red-500/50 text-red-400'
                      : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'
                  } ${!equalizer.enabled ? 'opacity-40 cursor-not-allowed' : ''}`}
                >
                  {p.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Sliders for Bass, Mid, Treble */}
        <div className="mt-6 space-y-4">
          {/* Bass */}
          <div>
            <div className="flex justify-between items-center text-xs mb-1">
              <span className="font-semibold text-slate-300">Bass (Low Shelf 250Hz)</span>
              <span className="font-mono text-red-400 font-bold">
                {equalizer.bass > 0 ? `+${equalizer.bass}` : equalizer.bass} dB
              </span>
            </div>
            <input
              type="range"
              min="-10"
              max="10"
              step="1"
              disabled={!equalizer.enabled}
              value={equalizer.bass}
              onChange={(e) =>
                updateEqualizer({ bass: parseInt(e.target.value, 10), preset: 'custom' })
              }
              className={`w-full h-2 rounded-lg bg-white/15 accent-red-500 cursor-pointer ${
                !equalizer.enabled ? 'opacity-40 cursor-not-allowed' : ''
              }`}
            />
          </div>

          {/* Mid */}
          <div>
            <div className="flex justify-between items-center text-xs mb-1">
              <span className="font-semibold text-slate-300">Mid (Peaking 1.5kHz)</span>
              <span className="font-mono text-red-400 font-bold">
                {equalizer.mid > 0 ? `+${equalizer.mid}` : equalizer.mid} dB
              </span>
            </div>
            <input
              type="range"
              min="-10"
              max="10"
              step="1"
              disabled={!equalizer.enabled}
              value={equalizer.mid}
              onChange={(e) =>
                updateEqualizer({ mid: parseInt(e.target.value, 10), preset: 'custom' })
              }
              className={`w-full h-2 rounded-lg bg-white/15 accent-red-500 cursor-pointer ${
                !equalizer.enabled ? 'opacity-40 cursor-not-allowed' : ''
              }`}
            />
          </div>

          {/* Treble */}
          <div>
            <div className="flex justify-between items-center text-xs mb-1">
              <span className="font-semibold text-slate-300">Treble (High Shelf 6kHz)</span>
              <span className="font-mono text-red-400 font-bold">
                {equalizer.treble > 0 ? `+${equalizer.treble}` : equalizer.treble} dB
              </span>
            </div>
            <input
              type="range"
              min="-10"
              max="10"
              step="1"
              disabled={!equalizer.enabled}
              value={equalizer.treble}
              onChange={(e) =>
                updateEqualizer({ treble: parseInt(e.target.value, 10), preset: 'custom' })
              }
              className={`w-full h-2 rounded-lg bg-white/15 accent-red-500 cursor-pointer ${
                !equalizer.enabled ? 'opacity-40 cursor-not-allowed' : ''
              }`}
            />
          </div>
        </div>

        {/* Done button */}
        <button
          onClick={onClose}
          className="mt-6 w-full py-3 rounded-2xl bg-gradient-to-r from-red-600 to-rose-600 text-white font-semibold shadow-lg shadow-red-600/30 hover:from-red-500 hover:to-rose-500 active:scale-[0.98] transition-all text-sm"
        >
          Save & Apply
        </button>
      </div>
    </div>
  );
};
