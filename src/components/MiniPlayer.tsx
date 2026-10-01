import React from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Play, Pause, SkipForward, Heart } from 'lucide-react';

export const MiniPlayer: React.FC = () => {
  const {
    currentSong,
    isPlaying,
    currentTime,
    duration,
    togglePlay,
    nextTrack,
    toggleLike,
    likedSongIds,
    setIsPlayerOpen,
  } = usePlayer();

  if (!currentSong) return null;

  const isLiked = likedSongIds.includes(currentSong.id);
  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;
  const ambientColor = currentSong.colorHex || '#fa243c';

  return (
    <div className="fixed bottom-20 sm:bottom-22 left-3 right-3 sm:max-w-md mx-auto z-20">
      <div
        onClick={() => setIsPlayerOpen(true)}
        className="group relative flex items-center justify-between p-2.5 rounded-3xl apple-liquid-dock cursor-pointer overflow-hidden transition-all duration-300 active:scale-[0.98] shadow-2xl"
      >
        {/* Dynamic ambient color glow */}
        <div
          className="absolute -inset-1 opacity-30 blur-2xl pointer-events-none transition-all duration-700"
          style={{ backgroundColor: ambientColor }}
        />

        {/* Specular top light rim */}
        <div className="absolute top-0 left-6 right-6 h-[1px] bg-gradient-to-r from-transparent via-white/50 to-transparent pointer-events-none" />

        {/* Progress Bar Line */}
        <div className="absolute bottom-0 left-3 right-3 h-[2.5px] bg-white/10 rounded-full overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-300"
            style={{
              width: `${progressPercent}%`,
              background: `linear-gradient(90deg, ${ambientColor}, #ffffff)`,
            }}
          />
        </div>

        {/* Song Info */}
        <div className="flex items-center gap-3 min-w-0 pr-2 relative z-10">
          <div className="relative w-11 h-11 rounded-2xl overflow-hidden flex-shrink-0 shadow-lg border border-white/20">
            <img
              src={currentSong.coverUrl}
              alt={currentSong.title}
              className={`w-full h-full object-cover transition-transform duration-500 ${
                isPlaying ? 'scale-105' : 'scale-100'
              }`}
            />
            {isPlaying && (
              <div className="absolute inset-0 bg-black/25 flex items-center justify-center">
                <div className="flex gap-0.5 items-end h-3">
                  <span className="w-1 bg-white rounded-full animate-bounce [animation-duration:600ms]"></span>
                  <span className="w-1 bg-white rounded-full animate-bounce [animation-duration:800ms]"></span>
                  <span className="w-1 bg-white rounded-full animate-bounce [animation-duration:500ms]"></span>
                </div>
              </div>
            )}
          </div>

          <div className="min-w-0">
            <h4 className="text-sm font-bold text-white truncate group-hover:text-rose-300 transition-colors font-outfit">
              {currentSong.title}
            </h4>
            <p className="text-xs text-slate-300/80 truncate font-medium">
              {currentSong.artist}
            </p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-1 sm:gap-2 flex-shrink-0 relative z-10" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => toggleLike(currentSong.id)}
            className="p-2 rounded-full hover:bg-white/15 text-slate-300 hover:text-white transition-all active:scale-90"
            title="Like"
          >
            <Heart
              className={`w-5 h-5 transition-transform active:scale-125 ${
                isLiked ? 'fill-[#fa243c] text-[#fa243c] drop-shadow-[0_0_8px_rgba(250,36,60,0.6)]' : ''
              }`}
            />
          </button>

          <button
            onClick={togglePlay}
            className="p-2.5 rounded-full text-white active:scale-90 transition-all shadow-lg overflow-hidden relative group/btn"
            style={{
              background: `linear-gradient(135deg, ${ambientColor} 0%, #fa243c 100%)`,
              boxShadow: `0 4px 15px ${ambientColor}60`,
            }}
            title={isPlaying ? 'Pause' : 'Play'}
          >
            <div className="absolute inset-0 bg-gradient-to-b from-white/30 to-transparent pointer-events-none" />
            {isPlaying ? (
              <Pause className="w-4 h-4 fill-white relative z-10" />
            ) : (
              <Play className="w-4 h-4 fill-white translate-x-0.5 relative z-10" />
            )}
          </button>

          <button
            onClick={nextTrack}
            className="p-2 rounded-full hover:bg-white/15 text-slate-300 hover:text-white transition-all active:scale-90"
            title="Next Track"
          >
            <SkipForward className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
