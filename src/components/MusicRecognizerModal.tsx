import React, { useState, useEffect } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { SONGS } from '../data/mockData';
import { Song } from '../types/music';
import { X, Mic, Radio, Play, Heart, Check, Sparkles } from 'lucide-react';

interface MusicRecognizerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const MusicRecognizerModal: React.FC<MusicRecognizerModalProps> = ({ isOpen, onClose }) => {
  const { playSong, toggleLike, likedSongIds } = usePlayer();
  const [state, setState] = useState<'listening' | 'identifying' | 'matched' | 'failed'>('listening');
  const [matchedSong, setMatchedSong] = useState<Song | null>(null);

  useEffect(() => {
    if (isOpen) {
      setState('listening');
      setMatchedSong(null);

      const t1 = setTimeout(() => {
        setState('identifying');
      }, 2500);

      const t2 = setTimeout(() => {
        // Pick a random song from catalog as the recognized track
        const pick = SONGS[Math.floor(Math.random() * SONGS.length)];
        setMatchedSong(pick);
        setState('matched');
      }, 4200);

      return () => {
        clearTimeout(t1);
        clearTimeout(t2);
      };
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const isLiked = matchedSong ? likedSongIds.includes(matchedSong.id) : false;

  const handlePlayRecognized = () => {
    if (matchedSong) {
      playSong(matchedSong);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-sm rounded-3xl bg-[#151a21] border border-white/10 p-6 shadow-2xl text-center relative overflow-hidden">
        {/* Glow */}
        <div className="absolute -top-10 -left-10 w-40 h-40 bg-red-600/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-10 -right-10 w-40 h-40 bg-purple-600/20 rounded-full blur-3xl pointer-events-none" />

        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <span className="text-[10px] uppercase font-bold tracking-widest text-red-400 block mb-1">
          Echo SoundID
        </span>
        <h3 className="font-bold text-lg text-white font-outfit mb-6">Music Recognizer</h3>

        {state === 'listening' || state === 'identifying' ? (
          <div className="flex flex-col items-center justify-center py-6">
            {/* Animated Mic Radar */}
            <div className="relative flex items-center justify-center mb-6">
              <span className="absolute w-28 h-28 rounded-full bg-red-500/20 animate-ping"></span>
              <span className="absolute w-20 h-20 rounded-full bg-rose-500/30 animate-pulse"></span>
              <div className="relative w-16 h-16 rounded-full bg-gradient-to-tr from-red-600 to-rose-500 text-white flex items-center justify-center shadow-xl shadow-red-500/40">
                <Mic className="w-7 h-7" />
              </div>
            </div>

            <p className="text-sm font-semibold text-white">
              {state === 'listening' ? 'Listening to environment...' : 'Computing audio fingerprint...'}
            </p>
            <p className="text-xs text-slate-400 mt-1 max-w-xs">
              Powered by VibraFP acoustic recognition engine.
            </p>
          </div>
        ) : matchedSong ? (
          <div className="flex flex-col items-center animate-fadeIn">
            <div className="relative w-28 h-28 rounded-2xl overflow-hidden shadow-xl mb-4 border border-white/20">
              <img
                src={matchedSong.coverUrl}
                alt={matchedSong.title}
                className="w-full h-full object-cover"
              />
              <span className="absolute top-2 right-2 px-1.5 py-0.5 rounded-md bg-emerald-500/90 text-white text-[9px] font-bold">
                100% Match
              </span>
            </div>

            <h4 className="text-lg font-bold text-white font-outfit truncate max-w-xs">
              {matchedSong.title}
            </h4>
            <p className="text-xs text-slate-300 mb-5">{matchedSong.artist}</p>

            <div className="flex gap-2 w-full">
              <button
                onClick={handlePlayRecognized}
                className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-red-600 to-rose-500 hover:from-red-500 hover:to-rose-400 text-white text-xs font-bold shadow-lg shadow-red-600/30 flex items-center justify-center gap-2 active:scale-95 transition-all"
              >
                <Play className="w-4 h-4 fill-white" />
                <span>Play Now</span>
              </button>

              <button
                onClick={() => toggleLike(matchedSong.id)}
                className={`p-3 rounded-2xl border transition-all ${
                  isLiked
                    ? 'bg-red-500/20 border-red-500/40 text-red-500'
                    : 'bg-white/5 border-white/15 text-slate-300 hover:bg-white/10'
                }`}
                title="Save to Favorites"
              >
                <Heart className={`w-5 h-5 ${isLiked ? 'fill-red-500' : ''}`} />
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
};
