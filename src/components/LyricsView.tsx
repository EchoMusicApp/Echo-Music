import React, { useEffect, useRef } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Mic2, Languages } from 'lucide-react';

export const LyricsView: React.FC = () => {
  const { currentSong, currentTime, seek, currentLyricIndex } = usePlayer();
  const [showTranslation, setShowTranslation] = React.useState<boolean>(true);
  const activeLineRef = useRef<HTMLDivElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  // Auto scroll to active lyric line smoothly
  useEffect(() => {
    if (activeLineRef.current && containerRef.current) {
      activeLineRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [currentLyricIndex]);

  if (!currentSong?.lyrics || currentSong.lyrics.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center px-4">
        <Mic2 className="w-12 h-12 text-slate-500 mb-3 animate-pulse" />
        <h3 className="text-base font-semibold text-slate-300">No lyrics available</h3>
        <p className="text-xs text-slate-500 mt-1 max-w-xs">
          Synced lyrics couldn't be found for this track.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full max-h-[60vh] select-none">
      {/* Translation toggle header */}
      <div className="flex items-center justify-between pb-3 mb-2 border-b border-white/10 px-2">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
          <Mic2 className="w-3.5 h-3.5 text-red-400" />
          Synchronized Lyrics
        </span>
        <button
          onClick={() => setShowTranslation(prev => !prev)}
          className={`flex items-center gap-1 text-xs px-2.5 py-1 rounded-full border transition-all ${
            showTranslation
              ? 'bg-red-500/20 text-red-400 border-red-500/30 font-medium'
              : 'bg-white/5 text-slate-400 border-white/10'
          }`}
          title="Toggle Translations / Romanization"
        >
          <Languages className="w-3.5 h-3.5" />
          <span>{showTranslation ? 'Translations On' : 'Original Only'}</span>
        </button>
      </div>

      {/* Lyrics Scrollable Lines */}
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto space-y-4 px-2 py-6 scroll-smooth"
      >
        {currentSong.lyrics.map((line, idx) => {
          const isActive = idx === currentLyricIndex;
          const isPast = idx < currentLyricIndex;

          return (
            <div
              key={idx}
              ref={isActive ? activeLineRef : null}
              onClick={() => seek(line.time)}
              className={`p-3 rounded-2xl cursor-pointer transition-all duration-300 group ${
                isActive
                  ? 'bg-white/10 text-white font-bold scale-[1.03] shadow-lg border border-white/15'
                  : isPast
                  ? 'text-slate-400 hover:text-slate-200'
                  : 'text-slate-500 hover:text-slate-300'
              }`}
            >
              <p
                className={`text-lg sm:text-xl transition-all ${
                  isActive
                    ? 'text-transparent bg-clip-text bg-gradient-to-r from-white via-rose-100 to-red-200 font-extrabold'
                    : ''
                }`}
              >
                {line.text}
              </p>
              {showTranslation && line.translation && (
                <p
                  className={`text-xs mt-1 transition-opacity ${
                    isActive ? 'text-red-300 font-medium' : 'text-slate-500'
                  }`}
                >
                  {line.translation}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
