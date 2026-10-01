import React from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Playlist, Album, Song } from '../types/music';
import { ArrowLeft, Play, Shuffle, Heart, MoreVertical, Clock } from 'lucide-react';
import { formatTime } from '../utils/formatters';

interface PlaylistDetailScreenProps {
  item: Playlist | Album;
  type: 'playlist' | 'album';
  onBack: () => void;
  onOpenAddToPlaylist: (song: Song) => void;
}

export const PlaylistDetailScreen: React.FC<PlaylistDetailScreenProps> = ({
  item,
  type,
  onBack,
  onOpenAddToPlaylist,
}) => {
  const { playSong, toggleLike, likedSongIds, currentSong, isPlaying } = usePlayer();

  const totalDuration = item.songs.reduce((acc, s) => acc + s.duration, 0);

  const handlePlayAll = () => {
    if (item.songs.length > 0) {
      playSong(item.songs[0], item.songs);
    }
  };

  const handleShufflePlay = () => {
    if (item.songs.length > 0) {
      const shuffled = [...item.songs].sort(() => Math.random() - 0.5);
      playSong(shuffled[0], shuffled);
    }
  };

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-6 animate-fadeIn">
      {/* Back button */}
      <button
        onClick={onBack}
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 text-xs font-semibold text-slate-300 transition-all"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back</span>
      </button>

      {/* Hero Header */}
      <div className="flex flex-col sm:flex-row gap-5 items-center sm:items-end">
        <div className="w-44 h-44 sm:w-52 sm:h-52 rounded-3xl overflow-hidden shadow-2xl border border-white/10 flex-shrink-0">
          <img
            src={item.coverUrl}
            alt={item.title}
            className="w-full h-full object-cover"
          />
        </div>

        <div className="text-center sm:text-left space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-wider text-red-400">
            {type === 'album' ? 'Album' : 'Playlist'}
          </span>
          <h2 className="text-2xl sm:text-4xl font-extrabold text-white font-outfit">
            {item.title}
          </h2>
          {'description' in item && (
            <p className="text-xs text-slate-400 max-w-md">{item.description}</p>
          )}
          {'artist' in item && (
            <p className="text-sm font-semibold text-slate-300">{item.artist}</p>
          )}

          <div className="flex items-center justify-center sm:justify-start gap-3 text-xs text-slate-400 pt-1">
            <span>{item.songs.length} songs</span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <Clock className="w-3.5 h-3.5" />
              {formatTime(totalDuration)}
            </span>
          </div>
        </div>
      </div>

      {/* Action buttons */}
      <div className="flex items-center gap-3">
        <button
          onClick={handlePlayAll}
          className="flex items-center gap-2 px-6 py-3 rounded-full bg-red-600 hover:bg-red-500 active:scale-95 text-white font-bold text-sm shadow-xl shadow-red-600/30 transition-all"
        >
          <Play className="w-4 h-4 fill-white" />
          <span>Play All</span>
        </button>

        <button
          onClick={handleShufflePlay}
          className="flex items-center gap-2 px-5 py-3 rounded-full bg-white/10 hover:bg-white/20 active:scale-95 text-white font-semibold text-sm transition-all"
        >
          <Shuffle className="w-4 h-4" />
          <span>Shuffle</span>
        </button>
      </div>

      {/* Songs Table */}
      <div className="space-y-1.5 pt-2">
        {item.songs.map((song, idx) => {
          const isLiked = likedSongIds.includes(song.id);
          const isCurrent = currentSong?.id === song.id;

          return (
            <div
              key={song.id}
              onClick={() => playSong(song, item.songs)}
              className={`flex items-center justify-between p-2.5 rounded-2xl cursor-pointer transition-all ${
                isCurrent
                  ? 'bg-red-500/15 border border-red-500/30'
                  : 'bg-white/[0.03] hover:bg-white/[0.08] border border-transparent'
              }`}
            >
              <div className="flex items-center gap-3.5 min-w-0">
                <span className="w-5 text-center text-xs font-mono text-slate-500">
                  {idx + 1}
                </span>
                <img
                  src={song.coverUrl}
                  alt={song.title}
                  className="w-10 h-10 rounded-xl object-cover"
                />
                <div className="min-w-0">
                  <p
                    className={`text-sm font-semibold truncate ${
                      isCurrent ? 'text-red-400 font-bold' : 'text-white'
                    }`}
                  >
                    {song.title}
                  </p>
                  <p className="text-xs text-slate-400 truncate">{song.artist}</p>
                </div>
              </div>

              <div
                className="flex items-center gap-2 flex-shrink-0"
                onClick={(e) => e.stopPropagation()}
              >
                <span className="text-xs font-mono text-slate-500 mr-1">
                  {formatTime(song.duration)}
                </span>
                <button
                  onClick={() => toggleLike(song.id)}
                  className="p-2 text-slate-400 hover:text-red-500 transition-colors"
                >
                  <Heart className={`w-4 h-4 ${isLiked ? 'fill-red-500 text-red-500' : ''}`} />
                </button>
                <button
                  onClick={() => onOpenAddToPlaylist(song)}
                  className="p-2 text-slate-400 hover:text-white"
                >
                  <MoreVertical className="w-4 h-4" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
