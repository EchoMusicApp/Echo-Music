import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { SONGS, ALBUMS, PLAYLISTS } from '../data/mockData';
import { Song, Album, Playlist } from '../types/music';
import {
  Play,
  Heart,
  MoreVertical,
  Flame,
  Sparkles,
  Zap,
  Coffee,
  Dumbbell,
  Brain,
  PartyPopper,
  Radio,
  Clock,
} from 'lucide-react';

interface HomeScreenProps {
  onSelectPlaylist: (playlist: Playlist) => void;
  onSelectAlbum: (album: Album) => void;
  onOpenAddToPlaylist: (song: Song) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  onSelectPlaylist,
  onSelectAlbum,
  onOpenAddToPlaylist,
}) => {
  const { playSong, currentSong, isPlaying, togglePlay, toggleLike, likedSongIds } = usePlayer();
  const [selectedMood, setSelectedMood] = useState<string>('all');

  const moodChips = [
    { id: 'all', label: 'All', icon: Sparkles },
    { id: 'energize', label: 'Energize', icon: Zap },
    { id: 'relax', label: 'Relax', icon: Coffee },
    { id: 'workout', label: 'Workout', icon: Dumbbell },
    { id: 'focus', label: 'Focus', icon: Brain },
    { id: 'party', label: 'Party', icon: PartyPopper },
  ];

  // Quick picks: 6 tracks for the Speed Dial
  const speedDialTracks = SONGS.slice(0, 6);

  // Filtered recommendations based on mood chip
  const displayedSongs =
    selectedMood === 'all'
      ? SONGS
      : selectedMood === 'workout' || selectedMood === 'energize'
      ? SONGS.filter((s) => s.genre?.includes('Electronic') || s.genre?.includes('Synthwave'))
      : selectedMood === 'relax' || selectedMood === 'focus'
      ? SONGS.filter((s) => s.genre?.includes('Chill') || s.genre?.includes('Indie') || s.genre?.includes('Space'))
      : SONGS;

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-8 animate-fadeIn">
      {/* Dynamic Header Greeting */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <span className="text-xs font-semibold text-red-400 uppercase tracking-widest flex items-center gap-1.5">
            <Radio className="w-3.5 h-3.5 animate-pulse" />
            Echo Dynamic Feed
          </span>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-outfit mt-0.5">
            Good evening, Listener
          </h2>
        </div>

        {/* Ambient indicator */}
        <div className="self-start sm:self-auto flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/10 text-xs text-slate-300">
          <Clock className="w-3.5 h-3.5 text-amber-400" />
          <span>Late Night Synth Mix</span>
        </div>
      </div>

      {/* Mood Chips Row */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none no-scrollbar">
        {moodChips.map((chip) => {
          const Icon = chip.icon;
          const isSelected = selectedMood === chip.id;
          return (
            <button
              key={chip.id}
              onClick={() => setSelectedMood(chip.id)}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all duration-200 border ${
                isSelected
                  ? 'bg-red-600 text-white border-red-500 shadow-md shadow-red-600/30'
                  : 'bg-white/5 text-slate-300 border-white/10 hover:bg-white/10 hover:text-white'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{chip.label}</span>
            </button>
          );
        })}
      </div>

      {/* Speed Dial / Quick Picks Grid (Echo's 2x3 prominent grid) */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-lg font-bold text-white font-outfit flex items-center gap-2">
            <Flame className="w-4 h-4 text-red-500" />
            <span>Speed Dial & Quick Picks</span>
          </h3>
          <span className="text-xs text-slate-400">Instant play</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {speedDialTracks.map((song) => {
            const isThisPlaying = currentSong?.id === song.id && isPlaying;
            return (
              <div
                key={song.id}
                onClick={() => playSong(song, SONGS)}
                className="group relative flex items-center gap-3 p-2 rounded-2xl bg-[#151a21] hover:bg-[#1e2430] border border-white/5 hover:border-white/15 transition-all cursor-pointer shadow-md overflow-hidden"
              >
                <div className="relative w-12 h-12 rounded-xl overflow-hidden flex-shrink-0">
                  <img
                    src={song.coverUrl}
                    alt={song.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute inset-0 bg-black/30 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <Play className="w-5 h-5 fill-white text-white" />
                  </div>
                </div>

                <div className="min-w-0 flex-1">
                  <h4 className="text-sm font-semibold text-white truncate group-hover:text-red-400 transition-colors">
                    {song.title}
                  </h4>
                  <p className="text-xs text-slate-400 truncate">{song.artist}</p>
                </div>

                {isThisPlaying && (
                  <div className="flex gap-0.5 items-end h-3 pr-2">
                    <span className="w-1 bg-red-500 rounded-full animate-bounce [animation-duration:600ms]"></span>
                    <span className="w-1 bg-red-500 rounded-full animate-bounce [animation-duration:800ms]"></span>
                    <span className="w-1 bg-red-500 rounded-full animate-bounce [animation-duration:500ms]"></span>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Featured Playlists Carousel */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-lg font-bold text-white font-outfit">Made For You</h3>
          <span className="text-xs text-slate-400 font-medium">Auto-Generated</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {PLAYLISTS.map((pl) => (
            <div
              key={pl.id}
              onClick={() => onSelectPlaylist(pl)}
              className="group cursor-pointer rounded-2xl bg-[#151a21] border border-white/5 hover:border-white/15 p-3 transition-all hover:bg-[#1a202c]"
            >
              <div className="relative aspect-square w-full rounded-xl overflow-hidden mb-2.5 shadow-md">
                <img
                  src={pl.coverUrl}
                  alt={pl.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (pl.songs.length > 0) playSong(pl.songs[0], pl.songs);
                  }}
                  className="absolute bottom-2 right-2 w-9 h-9 rounded-full bg-red-600 hover:bg-red-500 text-white flex items-center justify-center shadow-lg opacity-0 group-hover:opacity-100 transition-all translate-y-2 group-hover:translate-y-0"
                >
                  <Play className="w-4 h-4 fill-white translate-x-0.5" />
                </button>
              </div>

              <h4 className="text-sm font-bold text-white truncate">{pl.title}</h4>
              <p className="text-xs text-slate-400 line-clamp-1 mt-0.5">{pl.description}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Recommended Albums */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-lg font-bold text-white font-outfit">Top Albums</h3>
          <span className="text-xs text-slate-400">Trending releases</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {ALBUMS.map((alb) => (
            <div
              key={alb.id}
              onClick={() => onSelectAlbum(alb)}
              className="group cursor-pointer rounded-2xl bg-[#151a21] border border-white/5 hover:border-white/15 p-3 transition-all hover:bg-[#1a202c] flex items-center gap-3"
            >
              <img
                src={alb.coverUrl}
                alt={alb.title}
                className="w-14 h-14 rounded-xl object-cover flex-shrink-0 shadow-md group-hover:scale-105 transition-transform"
              />
              <div className="min-w-0">
                <h4 className="text-sm font-bold text-white truncate group-hover:text-red-400 transition-colors">
                  {alb.title}
                </h4>
                <p className="text-xs text-slate-400 truncate">{alb.artist}</p>
                <span className="text-[10px] text-slate-500 font-mono mt-0.5 block">
                  {alb.year} • {alb.songs.length} Tracks
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Songs Stream */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-lg font-bold text-white font-outfit">Discover Fresh Tracks</h3>
          <span className="text-xs text-red-400 font-semibold">{displayedSongs.length} Tracks</span>
        </div>

        <div className="space-y-1.5">
          {displayedSongs.map((song, idx) => {
            const isLiked = likedSongIds.includes(song.id);
            const isCurrent = currentSong?.id === song.id;

            return (
              <div
                key={song.id}
                onClick={() => playSong(song, displayedSongs)}
                className={`group flex items-center justify-between p-2 rounded-2xl transition-all cursor-pointer ${
                  isCurrent
                    ? 'bg-red-500/15 border border-red-500/30'
                    : 'bg-white/[0.03] hover:bg-white/[0.08] border border-transparent'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className="w-5 text-center text-xs font-mono text-slate-500">
                    {idx + 1}
                  </span>
                  <img
                    src={song.coverUrl}
                    alt={song.title}
                    className="w-11 h-11 rounded-xl object-cover flex-shrink-0 shadow-sm"
                  />
                  <div className="min-w-0">
                    <p
                      className={`text-sm font-semibold truncate ${
                        isCurrent ? 'text-red-400 font-bold' : 'text-white'
                      }`}
                    >
                      {song.title}
                    </p>
                    <p className="text-xs text-slate-400 truncate">
                      {song.artist} • <span className="text-slate-500">{song.album}</span>
                    </p>
                  </div>
                </div>

                <div
                  className="flex items-center gap-1 sm:gap-2 flex-shrink-0"
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    onClick={() => toggleLike(song.id)}
                    className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-red-500 transition-colors"
                  >
                    <Heart
                      className={`w-4 h-4 ${isLiked ? 'fill-red-500 text-red-500' : ''}`}
                    />
                  </button>

                  <button
                    onClick={() => onOpenAddToPlaylist(song)}
                    className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
                    title="Add to Playlist"
                  >
                    <MoreVertical className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
