import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import {
  Sparkles,
  Sliders,
  Wifi,
  Shield,
  Trash2,
  Info,
  Radio,
  ExternalLink,
  Check,
  Disc,
  Smartphone,
} from 'lucide-react';

interface SettingsScreenProps {
  onOpenEqualizer: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onOpenEqualizer }) => {
  const { playerStyle, setPlayerStyle, dataSaver, toggleDataSaver } = usePlayer();
  const [audioQuality, setAudioQuality] = useState<'standard' | 'high' | 'lossless'>('high');
  const [discordRpc, setDiscordRpc] = useState(true);
  const [cacheCleared, setCacheCleared] = useState(false);

  const handleClearCache = () => {
    setCacheCleared(true);
    setTimeout(() => setCacheCleared(false), 2000);
  };

  return (
    <div className="pb-36 pt-2 px-4 max-w-2xl mx-auto space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-outfit">
          Settings
        </h2>
        <p className="text-xs text-slate-400 mt-0.5">Customize audio and appearance</p>
      </div>

      {/* Appearance Section */}
      <div className="p-4 rounded-3xl bg-[#151a21] border border-white/5 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-red-400">
          Playback Appearance
        </h3>

        <div>
          <label className="text-sm font-semibold text-white block mb-2">
            Player Style
          </label>
          <div className="grid grid-cols-2 gap-2.5">
            <button
              onClick={() => setPlayerStyle('material')}
              className={`p-3 rounded-2xl border text-left transition-all ${
                playerStyle === 'material'
                  ? 'bg-red-500/15 border-red-500/40 text-white'
                  : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/10'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-bold text-sm">Material You</span>
                {playerStyle === 'material' && <Check className="w-4 h-4 text-red-400" />}
              </div>
              <p className="text-[11px] text-slate-400">
                Dynamic reactive album background with squiggly wave sliders
              </p>
            </button>

            <button
              onClick={() => setPlayerStyle('apple')}
              className={`p-3 rounded-2xl border text-left transition-all ${
                playerStyle === 'apple'
                  ? 'bg-red-500/15 border-red-500/40 text-white'
                  : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/10'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-bold text-sm">Apple Glass</span>
                {playerStyle === 'apple' && <Check className="w-4 h-4 text-red-400" />}
              </div>
              <p className="text-[11px] text-slate-400">
                Heavy frosted blur, iOS fluid card aesthetics and sleek sliders
              </p>
            </button>
          </div>
        </div>
      </div>

      {/* Audio Engine Section */}
      <div className="p-4 rounded-3xl bg-[#151a21] border border-white/5 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-red-400">
          Audio Engine & Quality
        </h3>

        {/* EQ Button */}
        <div
          onClick={onOpenEqualizer}
          className="flex items-center justify-between p-3 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 cursor-pointer transition-all"
        >
          <div className="flex items-center gap-3">
            <Sliders className="w-5 h-5 text-red-400" />
            <div>
              <p className="text-sm font-semibold text-white">Equalizer (Web Audio)</p>
              <p className="text-xs text-slate-400">Bass boost, Treble & custom acoustic curves</p>
            </div>
          </div>
          <span className="text-xs text-slate-400">Configure →</span>
        </div>

        {/* Quality select */}
        <div>
          <label className="text-sm font-semibold text-white block mb-2">
            Streaming Bitrate
          </label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 'standard', label: '128 kbps', sub: 'Data Saver' },
              { id: 'high', label: '256 kbps', sub: 'High Fidelity' },
              { id: 'lossless', label: 'FLAC', sub: 'Studio Lossless' },
            ].map((q) => (
              <button
                key={q.id}
                onClick={() => setAudioQuality(q.id as typeof audioQuality)}
                className={`p-2.5 rounded-2xl border text-center transition-all ${
                  audioQuality === q.id
                    ? 'bg-red-500/20 border-red-500/50 text-white font-bold'
                    : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/10'
                }`}
              >
                <span className="text-xs block">{q.label}</span>
                <span className="text-[10px] text-slate-400 block">{q.sub}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Data Saver Mode */}
        <div className="flex items-center justify-between pt-2">
          <div className="flex items-center gap-3">
            <Wifi className="w-5 h-5 text-slate-400" />
            <div>
              <p className="text-sm font-semibold text-white">Data Saver Mode</p>
              <p className="text-xs text-slate-400">Cache audio chunks and optimize images</p>
            </div>
          </div>

          <button
            onClick={toggleDataSaver}
            className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors ${
              dataSaver ? 'bg-red-600 justify-end' : 'bg-slate-700 justify-start'
            }`}
          >
            <span className="w-4 h-4 rounded-full bg-white shadow-md"></span>
          </button>
        </div>
      </div>

      {/* Integrations Section */}
      <div className="p-4 rounded-3xl bg-[#151a21] border border-white/5 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-red-400">
          Integrations & Storage
        </h3>

        {/* Discord RPC */}
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-semibold text-white">Discord Rich Presence</p>
            <p className="text-xs text-slate-400">Broadcast currently listening status</p>
          </div>
          <button
            onClick={() => setDiscordRpc(!discordRpc)}
            className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors ${
              discordRpc ? 'bg-indigo-600 justify-end' : 'bg-slate-700 justify-start'
            }`}
          >
            <span className="w-4 h-4 rounded-full bg-white shadow-md"></span>
          </button>
        </div>

        {/* Clear Storage */}
        <div className="flex items-center justify-between pt-2 border-t border-white/5">
          <div>
            <p className="text-sm font-semibold text-white">Cached Audio & Artwork</p>
            <p className="text-xs text-slate-400">Currently using 14.2 MB memory cache</p>
          </div>
          <button
            onClick={handleClearCache}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/5 hover:bg-rose-500/20 text-slate-300 hover:text-rose-400 border border-white/10 text-xs font-semibold transition-all"
          >
            {cacheCleared ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Trash2 className="w-3.5 h-3.5" />}
            <span>{cacheCleared ? 'Cleared!' : 'Clear Cache'}</span>
          </button>
        </div>
      </div>

      {/* About Section */}
      <div className="p-4 rounded-3xl bg-[#151a21] border border-white/5 space-y-2 text-center sm:text-left">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-red-600 to-rose-500 flex items-center justify-center text-white">
              <Radio className="w-4 h-4" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">Echo Music</h4>
              <p className="text-xs text-slate-400">Version 2.0.0</p>
            </div>
          </div>
          <span className="text-[11px] text-emerald-400 font-mono px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20">
            Ad-Free Engine
          </span>
        </div>
        <p className="text-xs text-slate-500 pt-2">
          Created with passion. Built for audiophiles with synced lyrics, offline downloads, and zero interruptions.
        </p>
      </div>
    </div>
  );
};
