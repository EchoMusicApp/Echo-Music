import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { SONGS } from '../data/mockData';
import { Playlist, Song } from '../types/music';
import {
  Heart,
  Download,
  History,
  ListMusic,
  Plus,
  Play,
  BarChart2,
  FolderSync,
  MoreVertical,
  Flame,
  CheckCircle2,
} from 'lucide-react';
import { formatTime } from '../utils/formatters';

interface LibraryScreenProps {
  onSelectPlaylist: (playlist: Playlist) => void;
  onOpenSpotifyImport: () => void;
  onOpenCreatePlaylist: () => void;
  onOpenAddToPlaylist: (song: Song) => void;
}

export const LibraryScreen: React.FC<LibraryScreenProps> = ({
  onSelectPlaylist,
  onOpenSpotifyImport,
  onOpenCreatePlaylist,
  onOpenAddToPlaylist,
}) => {
  const {
    playlists,
    likedSongIds,
    downloadedSongIds,
    history,
    playSong,
    toggleLike,
    currentSong,
  } = usePlayer();

  const [activeTab, setActiveTab] = useState<'playlists' | 'liked' | 'downloads' | 'history' | 'stats'>('playlists');

  const likedSongs = SONGS.filter((s) => likedSongIds.includes(s.id));
  const downloadedSongs = SONGS.filter((s) => downloadedSongIds.includes(s.id));

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-outfit">
            Your Library
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            {playlists.length} playlists • {likedSongs.length} favorites
          </p>
        </div>

        <button
          onClick={onOpenCreatePlaylist}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-red-600 hover:bg-red-500 text-white text-xs font-semibold shadow-md shadow-red-600/30 transition-all active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>New Playlist</span>
        </button>
      </div>

      {/* Spotify Import Callout Banner */}
      <div
        onClick={onOpenSpotifyImport}
        className="p-3.5 rounded-2xl bg-gradient-to-r from-emerald-950/40 via-[#151a21] to-[#1e2430] border border-emerald-500/25 flex items-center justify-between cursor-pointer hover:border-emerald-500/50 transition-all group shadow-lg"
      >
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center flex-shrink-0">
            <FolderSync className="w-5 h-5 group-hover:rotate-180 transition-transform duration-500" />
          </div>
          <div>
            <h4 className="text-sm font-bold text-white flex items-center gap-1.5">
              <span>Import from Spotify</span>
              <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-mono">
                Fast Sync
              </span>
            </h4>
            <p className="text-xs text-slate-400">
              Transfer playlists and liked songs in seconds
            </p>
          </div>
        </div>
        <span className="text-xs font-semibold text-emerald-400 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 group-hover:bg-emerald-500/20 transition-all">
          Import →
        </span>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none no-scrollbar">
        {[
          { id: 'playlists', label: 'Playlists', icon: ListMusic },
          { id: 'liked', label: `Liked (${likedSongs.length})`, icon: Heart },
          { id: 'downloads', label: `Offline (${downloadedSongs.length})`, icon: Download },
          { id: 'history', label: 'History', icon: History },
          { id: 'stats', label: 'Stats', icon: BarChart2 },
        ].map((tab) => {
          const Icon = tab.icon;
          const isSelected = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as typeof activeTab)}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all border ${
                isSelected
                  ? 'bg-white text-slate-900 border-white shadow-md'
                  : 'bg-white/5 text-slate-300 border-white/10 hover:bg-white/10'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab: Playlists */}
      {activeTab === 'playlists' && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {playlists.map((pl) => (
            <div
              key={pl.id}
              onClick={() => onSelectPlaylist(pl)}
              className="group cursor-pointer rounded-2xl bg-[#151a21] border border-white/5 hover:border-white/15 p-3 transition-all hover:bg-[#1a202c]"
            >
              <div className="relative aspect-square w-full rounded-xl overflow-hidden mb-2.5 shadow-md">
                <img
                  src={pl.coverUrl}
                  alt={pl.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
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
              <p className="text-xs text-slate-400 mt-0.5">{pl.songs.length} tracks</p>
            </div>
          ))}
        </div>
      )}

      {/* Tab: Liked Songs */}
      {activeTab === 'liked' && (
        <div className="space-y-1.5">
          {likedSongs.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <Heart className="w-10 h-10 mx-auto text-slate-600 mb-2" />
              <p className="text-sm">No favorite tracks yet.</p>
            </div>
          ) : (
            likedSongs.map((song) => (
              <div
                key={song.id}
                onClick={() => playSong(song, likedSongs)}
                className="flex items-center justify-between p-2 rounded-2xl bg-white/[0.03] hover:bg-white/[0.08] cursor-pointer transition-all"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <img
                    src={song.coverUrl}
                    alt={song.title}
                    className="w-11 h-11 rounded-xl object-cover"
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-white truncate">{song.title}</p>
                    <p className="text-xs text-slate-400 truncate">{song.artist}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                  <button
                    onClick={() => toggleLike(song.id)}
                    className="p-2 text-red-500 hover:scale-110 transition-transform"
                  >
                    <Heart className="w-4 h-4 fill-red-500" />
                  </button>
                  <button
                    onClick={() => onOpenAddToPlaylist(song)}
                    className="p-2 text-slate-400 hover:text-white"
                  >
                    <MoreVertical className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Tab: Downloaded / Offline Tracks */}
      {activeTab === 'downloads' && (
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-xs text-emerald-400 pb-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>Ready for offline playback without network connection</span>
          </div>

          {downloadedSongs.map((song) => (
            <div
              key={song.id}
              onClick={() => playSong(song, downloadedSongs)}
              className="flex items-center justify-between p-2 rounded-2xl bg-white/[0.03] hover:bg-white/[0.08] cursor-pointer transition-all"
            >
              <div className="flex items-center gap-3 min-w-0">
                <img
                  src={song.coverUrl}
                  alt={song.title}
                  className="w-11 h-11 rounded-xl object-cover"
                />
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-white truncate">{song.title}</p>
                  <p className="text-xs text-slate-400 truncate">{song.artist}</p>
                </div>
              </div>

              <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-mono">
                  Offline
                </span>
                <button
                  onClick={() => onOpenAddToPlaylist(song)}
                  className="p-2 text-slate-400 hover:text-white"
                >
                  <MoreVertical className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tab: History */}
      {activeTab === 'history' && (
        <div className="space-y-1.5">
          {history.length === 0 ? (
            <p className="text-xs text-slate-500 py-6 text-center">No listening history yet.</p>
          ) : (
            history.map((song, idx) => (
              <div
                key={`${song.id}-${idx}`}
                onClick={() => playSong(song, history)}
                className="flex items-center justify-between p-2 rounded-2xl bg-white/[0.03] hover:bg-white/[0.08] cursor-pointer transition-all"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <img
                    src={song.coverUrl}
                    alt={song.title}
                    className="w-11 h-11 rounded-xl object-cover"
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-white truncate">{song.title}</p>
                    <p className="text-xs text-slate-400 truncate">{song.artist}</p>
                  </div>
                </div>
                <span className="text-xs font-mono text-slate-500">{formatTime(song.duration)}</span>
              </div>
            ))
          )}
        </div>
      )}

      {/* Tab: Listening Stats */}
      {activeTab === 'stats' && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div className="p-4 rounded-2xl bg-[#151a21] border border-white/5">
              <span className="text-xs text-slate-400 block">Total Listening</span>
              <p className="text-2xl font-bold font-outfit text-white mt-1">42.8 hrs</p>
              <span className="text-[11px] text-emerald-400 font-medium">+14% this month</span>
            </div>

            <div className="p-4 rounded-2xl bg-[#151a21] border border-white/5">
              <span className="text-xs text-slate-400 block">Top Artist</span>
              <p className="text-2xl font-bold font-outfit text-red-400 mt-1 truncate">
                Luna Horizon
              </p>
              <span className="text-[11px] text-slate-400 font-medium">18 hours streamed</span>
            </div>

            <div className="p-4 rounded-2xl bg-[#151a21] border border-white/5 col-span-2 sm:col-span-1">
              <span className="text-xs text-slate-400 block">Unique Songs</span>
              <p className="text-2xl font-bold font-outfit text-purple-400 mt-1">146</p>
              <span className="text-[11px] text-slate-400 font-medium">9 playlists created</span>
            </div>
          </div>

          {/* Expressive Genre Distribution Bars */}
          <div className="p-5 rounded-3xl bg-[#151a21] border border-white/5 space-y-3">
            <h4 className="text-sm font-bold text-white">Top Genres</h4>

            <div>
              <div className="flex justify-between text-xs mb-1 font-medium">
                <span className="text-slate-300">Synthwave & Electro</span>
                <span className="text-purple-400">45%</span>
              </div>
              <div className="h-2 rounded-full bg-white/10 overflow-hidden">
                <div className="h-full bg-purple-500 rounded-full" style={{ width: '45%' }} />
              </div>
            </div>

            <div>
              <div className="flex justify-between text-xs mb-1 font-medium">
                <span className="text-slate-300">Lo-Fi Chill & Beats</span>
                <span className="text-cyan-400">28%</span>
              </div>
              <div className="h-2 rounded-full bg-white/10 overflow-hidden">
                <div className="h-full bg-cyan-500 rounded-full" style={{ width: '28%' }} />
              </div>
            </div>

            <div>
              <div className="flex justify-between text-xs mb-1 font-medium">
                <span className="text-slate-300">Acoustic & Indie</span>
                <span className="text-amber-400">18%</span>
              </div>
              <div className="h-2 rounded-full bg-white/10 overflow-hidden">
                <div className="h-full bg-amber-500 rounded-full" style={{ width: '18%' }} />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
