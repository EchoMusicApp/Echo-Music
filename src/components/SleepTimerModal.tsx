import React from 'react';
import { usePlayer } from '../context/PlayerContext';
import { X, Timer, Moon, Check } from 'lucide-react';
import { formatTime } from '../utils/formatters';

interface SleepTimerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SleepTimerModal: React.FC<SleepTimerModalProps> = ({ isOpen, onClose }) => {
  const { sleepTimerMinutes, sleepTimerRemaining, setSleepTimer } = usePlayer();

  if (!isOpen) return null;

  const options = [
    { minutes: 15, label: '15 Minutes' },
    { minutes: 30, label: '30 Minutes' },
    { minutes: 45, label: '45 Minutes' },
    { minutes: 60, label: '1 Hour' },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-sm rounded-3xl bg-[#151a21] border border-white/10 p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400">
              <Moon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base text-white font-outfit">Sleep Timer</h3>
              <p className="text-xs text-slate-400">Stop playback automatically</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {sleepTimerRemaining !== null && (
          <div className="my-4 p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-center">
            <span className="text-xs font-semibold text-amber-300 uppercase tracking-wider block">
              Time Remaining
            </span>
            <span className="text-2xl font-bold font-mono text-amber-400 mt-1 block">
              {formatTime(sleepTimerRemaining)}
            </span>
          </div>
        )}

        <div className="space-y-2 mt-4">
          {options.map((opt) => {
            const isSelected = sleepTimerMinutes === opt.minutes;
            return (
              <button
                key={opt.minutes}
                onClick={() => {
                  setSleepTimer(opt.minutes);
                  onClose();
                }}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-2xl border text-sm font-semibold transition-all ${
                  isSelected
                    ? 'bg-amber-500/20 border-amber-500/40 text-amber-400'
                    : 'bg-white/5 border-white/10 text-slate-200 hover:bg-white/10'
                }`}
              >
                <span>{opt.label}</span>
                {isSelected && <Check className="w-4 h-4" />}
              </button>
            );
          })}

          {sleepTimerRemaining !== null && (
            <button
              onClick={() => {
                setSleepTimer(null);
                onClose();
              }}
              className="w-full py-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 text-rose-400 text-sm font-semibold hover:bg-rose-500/20 transition-all mt-2"
            >
              Turn Off Sleep Timer
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
