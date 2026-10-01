import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Song } from '../types/music';
import { X, Plus, FolderPlus, Check } from 'lucide-react';

interface AddToPlaylistModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetSong?: Song | null;
}

export const AddToPlaylistModal: React.FC<AddToPlaylistModalProps> = ({
  isOpen,
  onClose,
  targetSong,
}) => {
  const { currentSong, playlists, addSongToPlaylist, createPlaylist } = usePlayer();
  const [newTitle, setNewTitle] = useState('');
  const [showCreateNew, setShowCreateNew] = useState(false);
  const [addedPlaylistId, setAddedPlaylistId] = useState<string | null>(null);

  if (!isOpen) return null;

  const songToAdd = targetSong || currentSong;
  if (!songToAdd) return null;

  const handleSelectPlaylist = (playlistId: string) => {
    addSongToPlaylist(playlistId, songToAdd);
    setAddedPlaylistId(playlistId);
    setTimeout(() => {
      setAddedPlaylistId(null);
      onClose();
    }, 900);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    const newId = createPlaylist(newTitle.trim(), 'Custom playlist');
    addSongToPlaylist(newId, songToAdd);
    setNewTitle('');
    setShowCreateNew(false);
    setAddedPlaylistId(newId);
    setTimeout(() => {
      setAddedPlaylistId(null);
      onClose();
    }, 900);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-sm rounded-3xl bg-[#151a21] border border-white/10 p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div>
            <h3 className="font-bold text-base text-white font-outfit">Add to Playlist</h3>
            <p className="text-xs text-slate-400 truncate max-w-[200px]">
              {songToAdd.title}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Create new playlist toggle */}
        {!showCreateNew ? (
          <button
            onClick={() => setShowCreateNew(true)}
            className="w-full my-3 flex items-center gap-2 px-4 py-3 rounded-2xl bg-red-600/10 border border-red-500/30 text-red-400 text-sm font-semibold hover:bg-red-600/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>New Playlist</span>
          </button>
        ) : (
          <form onSubmit={handleCreate} className="my-3 space-y-2">
            <input
              type="text"
              placeholder="Playlist name..."
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              autoFocus
              className="w-full px-3 py-2 text-sm rounded-xl bg-white/5 border border-white/20 text-white placeholder-slate-500 focus:outline-none focus:border-red-500"
            />
            <div className="flex gap-2">
              <button
                type="submit"
                className="flex-1 py-2 rounded-xl bg-red-600 text-white text-xs font-semibold hover:bg-red-500 transition-all"
              >
                Create & Add
              </button>
              <button
                type="button"
                onClick={() => setShowCreateNew(false)}
                className="px-3 py-2 rounded-xl bg-white/5 text-slate-400 text-xs font-semibold hover:bg-white/10 transition-all"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {/* Existing playlists */}
        <div className="mt-2 max-h-60 overflow-y-auto space-y-2">
          {playlists.map((pl) => {
            const hasSong = pl.songs.some((s) => s.id === songToAdd.id);
            const isJustAdded = addedPlaylistId === pl.id;

            return (
              <button
                key={pl.id}
                onClick={() => handleSelectPlaylist(pl.id)}
                className={`w-full flex items-center justify-between p-2.5 rounded-2xl border transition-all text-left ${
                  isJustAdded
                    ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400'
                    : 'bg-white/5 border-white/10 hover:bg-white/10 text-white'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <img
                    src={pl.coverUrl}
                    alt={pl.title}
                    className="w-10 h-10 rounded-xl object-cover flex-shrink-0"
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-semibold truncate">{pl.title}</p>
                    <p className="text-xs text-slate-400">{pl.songs.length} songs</p>
                  </div>
                </div>

                {isJustAdded ? (
                  <Check className="w-5 h-5 text-emerald-400" />
                ) : hasSong ? (
                  <span className="text-[11px] text-slate-500 font-medium">Added</span>
                ) : null}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
