import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { LyricsView } from './LyricsView';
import { formatTime } from '../utils/formatters';
import {
  ChevronDown,
  Heart,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  ListMusic,
  Mic2,
  Sliders,
  Volume2,
  VolumeX,
  Timer,
  Download,
  Share2,
  Trash2,
  Sparkles,
  Cast,
  Check,
} from 'lucide-react';

interface FullPlayerProps {
  onOpenEqualizer: () => void;
  onOpenSleepTimer: () => void;
  onOpenAddToPlaylist: () => void;
}

export const FullPlayer: React.FC<FullPlayerProps> = ({
  onOpenEqualizer,
  onOpenSleepTimer,
  onOpenAddToPlaylist,
}) => {
  const {
    currentSong,
    isPlaying,
    currentTime,
    duration,
    volume,
    isMuted,
    isShuffled,
    repeatMode,
    queue,
    queueIndex,
    playerStyle,
    isPlayerOpen,
    likedSongIds,
    downloadedSongIds,
    sleepTimerRemaining,
    togglePlay,
    seek,
    nextTrack,
    prevTrack,
    setVolumeLevel,
    toggleMute,
    toggleShuffle,
    cycleRepeatMode,
    setPlayerStyle,
    setIsPlayerOpen,
    toggleLike,
    toggleDownload,
    playSong,
    removeFromQueue,
    clearQueue,
  } = usePlayer();

  const [activeTab, setActiveTab] = useState<'player' | 'lyrics' | 'queue'>('player');
  const [showShareNotification, setShowShareNotification] = useState(false);

  if (!isPlayerOpen || !currentSong) return null;

  const isLiked = likedSongIds.includes(currentSong.id);
  const isDownloaded = downloadedSongIds.includes(currentSong.id);
  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(`Listening to ${currentSong.title} by ${currentSong.artist} on Echo Music!`);
      setShowShareNotification(true);
      setTimeout(() => setShowShareNotification(false), 2500);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black flex flex-col justify-between">
      {/* Background with Ambient Glow */}
      <div
        className={`absolute inset-0 transition-all duration-700 pointer-events-none ${
          playerStyle === 'apple'
            ? 'scale-125 filter blur-3xl opacity-40'
            : 'opacity-25 filter blur-2xl'
        }`}
        style={{
          backgroundImage: `url(${currentSong.coverUrl})`,
          backgroundPosition: 'center',
          backgroundSize: 'cover',
        }}
      />
      <div className="absolute inset-0 bg-black/60 backdrop-blur-2xl" />

      {/* Share Toast */}
      {showShareNotification && (
        <div className="absolute top-16 left-1/2 -translate-x-1/2 z-50 flex items-center gap-2 px-4 py-2 rounded-full bg-white/20 backdrop-blur-md border border-white/20 text-white text-xs font-medium shadow-2xl">
          <Check className="w-3.5 h-3.5 text-emerald-400" />
          <span>Track link copied to clipboard!</span>
        </div>
      )}

      {/* Header bar */}
      <div className="relative z-10 flex items-center justify-between px-5 pt-4 pb-2">
        <button
          onClick={() => setIsPlayerOpen(false)}
          className="p-2.5 rounded-full bg-white/10 hover:bg-white/20 active:scale-95 transition-all text-white"
          title="Minimize Player"
        >
          <ChevronDown className="w-5 h-5" />
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400">
            Playing From Library
          </span>
          <span className="text-xs font-semibold text-white/90 max-w-[180px] truncate">
            {currentSong.album}
          </span>
        </div>

        {/* Style Selector / Options */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setPlayerStyle(playerStyle === 'material' ? 'apple' : 'material')}
            className="flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-white/10 hover:bg-white/20 border border-white/15 text-slate-200 transition-all"
            title="Toggle Material You / Apple Style"
          >
            <Sparkles className="w-3 h-3 text-amber-400" />
            <span className="hidden sm:inline">Theme:</span>
            <span className="capitalize">{playerStyle}</span>
          </button>
        </div>
      </div>

      {/* Main Body */}
      <div className="relative z-10 flex-1 px-6 py-2 overflow-y-auto max-w-md mx-auto w-full flex flex-col justify-center">
        {activeTab === 'lyrics' ? (
          <LyricsView />
        ) : activeTab === 'queue' ? (
          /* Queue View */
          <div className="flex flex-col h-full max-h-[65vh]">
            <div className="flex items-center justify-between pb-3 mb-2 border-b border-white/10">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                <ListMusic className="w-3.5 h-3.5 text-red-400" />
                Up Next ({queue.length} Tracks)
              </span>
              <button
                onClick={clearQueue}
                className="text-xs text-rose-400 hover:text-rose-300 transition-colors flex items-center gap-1"
              >
                <Trash2 className="w-3 h-3" />
                Clear
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-2 pr-1">
              {queue.map((song, idx) => {
                const isCurrent = idx === queueIndex;
                return (
                  <div
                    key={`${song.id}-${idx}`}
                    onClick={() => playSong(song)}
                    className={`flex items-center justify-between p-2 rounded-xl transition-all cursor-pointer ${
                      isCurrent
                        ? 'bg-red-500/20 border border-red-500/30'
                        : 'bg-white/5 hover:bg-white/10 border border-transparent'
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <img
                        src={song.coverUrl}
                        alt={song.title}
                        className="w-10 h-10 rounded-lg object-cover flex-shrink-0"
                      />
                      <div className="min-w-0">
                        <p
                          className={`text-sm font-semibold truncate ${
                            isCurrent ? 'text-red-400' : 'text-white'
                          }`}
                        >
                          {song.title}
                        </p>
                        <p className="text-xs text-slate-400 truncate">
                          {song.artist}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 flex-shrink-0" onClick={e => e.stopPropagation()}>
                      <span className="text-xs text-slate-500 font-mono">
                        {formatTime(song.duration)}
                      </span>
                      {queue.length > 1 && (
                        <button
                          onClick={() => removeFromQueue(idx)}
                          className="p-1.5 rounded-full hover:bg-white/10 text-slate-400 hover:text-rose-400 transition-colors"
                          title="Remove from queue"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          /* Standard Now Playing View */
          <div className="flex flex-col items-center">
            {/* Artwork */}
            <div
              className={`relative aspect-square w-full max-w-[290px] sm:max-w-[320px] rounded-3xl overflow-hidden shadow-2xl transition-all duration-500 ${
                playerStyle === 'apple'
                  ? 'border border-white/20 shadow-rose-950/40 ring-1 ring-white/10'
                  : 'shadow-red-600/20'
              }`}
            >
              <img
                src={currentSong.coverUrl}
                alt={currentSong.title}
                className={`w-full h-full object-cover transition-transform duration-700 ${
                  isPlaying ? 'scale-100' : 'scale-95 opacity-90'
                }`}
              />
            </div>

            {/* Song title & Artist */}
            <div className="w-full flex items-center justify-between mt-6 px-1">
              <div className="min-w-0 pr-4">
                <h2 className="text-xl sm:text-2xl font-bold text-white truncate font-outfit">
                  {currentSong.title}
                </h2>
                <p className="text-sm sm:text-base text-slate-300 truncate mt-0.5">
                  {currentSong.artist}
                </p>
              </div>

              <div className="flex items-center gap-1.5 flex-shrink-0">
                <button
                  onClick={() => toggleDownload(currentSong.id)}
                  className={`p-2.5 rounded-full transition-colors ${
                    isDownloaded
                      ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      : 'hover:bg-white/10 text-slate-400'
                  }`}
                  title={isDownloaded ? 'Downloaded Offline' : 'Download for Offline'}
                >
                  <Download className="w-5 h-5" />
                </button>

                <button
                  onClick={() => toggleLike(currentSong.id)}
                  className="p-2.5 rounded-full hover:bg-white/10 transition-colors"
                  title="Favorite"
                >
                  <Heart
                    className={`w-6 h-6 transition-all duration-200 active:scale-125 ${
                      isLiked ? 'fill-red-500 text-red-500' : 'text-slate-400'
                    }`}
                  />
                </button>
              </div>
            </div>

            {/* Progress Slider */}
            <div className="w-full mt-5 px-1">
              <div className="relative group">
                <input
                  type="range"
                  min="0"
                  max={duration || 100}
                  value={currentTime}
                  onChange={(e) => seek(parseFloat(e.target.value))}
                  className="w-full h-2 rounded-lg bg-white/15 cursor-pointer accent-red-500 transition-all"
                />
                <div
                  className="absolute top-0 left-0 h-2 bg-gradient-to-r from-red-500 to-rose-400 rounded-lg pointer-events-none"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>

              <div className="flex justify-between items-center text-xs font-mono text-slate-400 mt-2">
                <span>{formatTime(currentTime)}</span>
                <span>{formatTime(duration)}</span>
              </div>
            </div>

            {/* Playback Controls */}
            <div className="w-full flex items-center justify-between mt-4 px-2">
              <button
                onClick={toggleShuffle}
                className={`p-2 rounded-full transition-colors ${
                  isShuffled ? 'text-red-400 bg-red-500/10' : 'text-slate-400 hover:text-white'
                }`}
                title="Shuffle"
              >
                <Shuffle className="w-5 h-5" />
              </button>

              <button
                onClick={prevTrack}
                className="p-3 rounded-full hover:bg-white/10 text-white transition-all active:scale-90"
                title="Previous Track"
              >
                <SkipBack className="w-7 h-7" />
              </button>

              <button
                onClick={togglePlay}
                className="w-16 h-16 rounded-full bg-gradient-to-tr from-red-600 to-rose-500 hover:from-red-500 hover:to-rose-400 active:scale-95 text-white flex items-center justify-center shadow-xl shadow-red-600/40 transition-all"
                title={isPlaying ? 'Pause' : 'Play'}
              >
                {isPlaying ? (
                  <Pause className="w-8 h-8 fill-white" />
                ) : (
                  <Play className="w-8 h-8 fill-white translate-x-0.5" />
                )}
              </button>

              <button
                onClick={nextTrack}
                className="p-3 rounded-full hover:bg-white/10 text-white transition-all active:scale-90"
                title="Next Track"
              >
                <SkipForward className="w-7 h-7" />
              </button>

              <button
                onClick={cycleRepeatMode}
                className={`p-2 rounded-full transition-colors ${
                  repeatMode !== 'off' ? 'text-red-400 bg-red-500/10' : 'text-slate-400 hover:text-white'
                }`}
                title={`Repeat: ${repeatMode}`}
              >
                {repeatMode === 'one' ? (
                  <Repeat1 className="w-5 h-5" />
                ) : (
                  <Repeat className="w-5 h-5" />
                )}
              </button>
            </div>

            {/* Volume slider (in Apple style / or expanded) */}
            <div className="w-full flex items-center gap-3 mt-4 px-4 py-2 rounded-2xl bg-white/5 border border-white/10">
              <button
                onClick={toggleMute}
                className="text-slate-400 hover:text-white transition-colors"
                title={isMuted ? 'Unmute' : 'Mute'}
              >
                {isMuted || volume === 0 ? (
                  <VolumeX className="w-4 h-4 text-rose-400" />
                ) : (
                  <Volume2 className="w-4 h-4" />
                )}
              </button>
              <input
                type="range"
                min="0"
                max="1"
                step="0.01"
                value={isMuted ? 0 : volume}
                onChange={(e) => setVolumeLevel(parseFloat(e.target.value))}
                className="w-full h-1.5 rounded-lg bg-white/20 accent-white cursor-pointer"
              />
              <span className="text-[11px] font-mono text-slate-400 w-8 text-right">
                {Math.round((isMuted ? 0 : volume) * 100)}%
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Bottom Sub-nav / Quick Toggles */}
      <div className="relative z-10 px-6 py-4 border-t border-white/10 flex items-center justify-between max-w-md mx-auto w-full">
        {/* Toggle between Player, Lyrics, and Queue */}
        <div className="flex items-center gap-1 bg-white/10 p-1 rounded-full border border-white/10">
          <button
            onClick={() => setActiveTab('player')}
            className={`px-3 py-1 text-xs font-semibold rounded-full transition-all ${
              activeTab === 'player'
                ? 'bg-red-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Player
          </button>
          <button
            onClick={() => setActiveTab('lyrics')}
            className={`flex items-center gap-1 px-3 py-1 text-xs font-semibold rounded-full transition-all ${
              activeTab === 'lyrics'
                ? 'bg-red-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Mic2 className="w-3 h-3" />
            <span>Lyrics</span>
          </button>
          <button
            onClick={() => setActiveTab('queue')}
            className={`flex items-center gap-1 px-3 py-1 text-xs font-semibold rounded-full transition-all ${
              activeTab === 'queue'
                ? 'bg-red-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <ListMusic className="w-3 h-3" />
            <span>Queue</span>
          </button>
        </div>

        {/* Secondary feature buttons: Equalizer, Sleep Timer, Playlist, Share */}
        <div className="flex items-center gap-1 sm:gap-2">
          <button
            onClick={onOpenEqualizer}
            className="p-2 rounded-full hover:bg-white/10 text-slate-300 hover:text-white transition-colors"
            title="Audio Equalizer (Web Audio)"
          >
            <Sliders className="w-4 h-4" />
          </button>

          <button
            onClick={onOpenSleepTimer}
            className={`p-2 rounded-full transition-colors relative ${
              sleepTimerRemaining !== null ? 'text-amber-400 bg-amber-500/10' : 'text-slate-300 hover:text-white'
            }`}
            title="Sleep Timer"
          >
            <Timer className="w-4 h-4" />
            {sleepTimerRemaining !== null && (
              <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-amber-400 animate-ping"></span>
            )}
          </button>

          <button
            onClick={onOpenAddToPlaylist}
            className="p-2 rounded-full hover:bg-white/10 text-slate-300 hover:text-white transition-colors"
            title="Add to Playlist"
          >
            <Cast className="w-4 h-4" />
          </button>

          <button
            onClick={handleShare}
            className="p-2 rounded-full hover:bg-white/10 text-slate-300 hover:text-white transition-colors"
            title="Share Song"
          >
            <Share2 className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
