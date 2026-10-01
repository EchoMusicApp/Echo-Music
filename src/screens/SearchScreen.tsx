import React, { useState, useMemo } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { SONGS, ARTISTS, ALBUMS } from '../data/mockData';
import { Song, Album, Artist } from '../types/music';
import { Search, X, Play, Heart, ListPlus, Music, Disc, User, Sparkles } from 'lucide-react';
import { formatTime } from '../utils/formatters';

interface SearchScreenProps {
  onSelectAlbum: (album: Album) => void;
  onSelectArtist: (artist: Artist) => void;
  onOpenAddToPlaylist: (song: Song) => void;
}

export const SearchScreen: React.FC<SearchScreenProps> = ({
  onSelectAlbum,
  onSelectArtist,
  onOpenAddToPlaylist,
}) => {
  const { playSong, addToQueue, toggleLike, likedSongIds, currentSong } = usePlayer();
  const [query, setQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'songs' | 'artists' | 'albums'>('all');

  const genres = [
    { name: 'Synthwave', color: 'from-purple-600 to-indigo-600', query: 'Synthwave' },
    { name: 'Lo-Fi Chill', color: 'from-cyan-600 to-blue-700', query: 'Lo-Fi' },
    { name: 'Electronic', color: 'from-rose-600 to-red-700', query: 'Electronic' },
    { name: 'Acoustic & Indie', color: 'from-amber-600 to-orange-700', query: 'Indie' },
    { name: 'Deep Space', color: 'from-blue-700 to-violet-800', query: 'Space' },
    { name: 'Nu-Disco', color: 'from-pink-600 to-rose-600', query: 'Nu-Disco' },
  ];

  const filteredSongs = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.toLowerCase();
    return SONGS.filter(
      (s) =>
        s.title.toLowerCase().includes(q) ||
        s.artist.toLowerCase().includes(q) ||
        s.album.toLowerCase().includes(q) ||
        (s.genre && s.genre.toLowerCase().includes(q))
    );
  }, [query]);

  const filteredAlbums = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.toLowerCase();
    return ALBUMS.filter(
      (a) => a.title.toLowerCase().includes(q) || a.artist.toLowerCase().includes(q)
    );
  }, [query]);

  const filteredArtists = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.toLowerCase();
    return ARTISTS.filter((art) => art.name.toLowerCase().includes(q));
  }, [query]);

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-6 animate-fadeIn">
      {/* Search Header */}
      <div>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-outfit mb-3">
          Search & Explore
        </h2>

        {/* Input Bar */}
        <div className="relative flex items-center">
          <Search className="absolute left-4 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="Search songs, artists, albums, or genres..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full pl-12 pr-10 py-3.5 rounded-2xl bg-[#151a21] border border-white/10 text-white placeholder-slate-400 focus:outline-none focus:border-red-500 shadow-lg text-sm transition-all"
          />
          {query && (
            <button
              onClick={() => setQuery('')}
              className="absolute right-3.5 p-1 rounded-full hover:bg-white/10 text-slate-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Filter Tabs if query is active */}
      {query.trim() && (
        <div className="flex gap-2">
          {(['all', 'songs', 'artists', 'albums'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold capitalize transition-all border ${
                activeFilter === filter
                  ? 'bg-red-600 text-white border-red-500 shadow-md shadow-red-600/30'
                  : 'bg-white/5 text-slate-300 border-white/10 hover:bg-white/10'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      )}

      {/* Results View */}
      {query.trim() ? (
        <div className="space-y-6">
          {/* Top Result Card if available */}
          {filteredSongs.length > 0 && activeFilter === 'all' && (
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Top Result
              </span>
              <div
                onClick={() => playSong(filteredSongs[0], filteredSongs)}
                className="p-4 rounded-3xl bg-gradient-to-r from-red-950/30 via-[#151a21] to-[#1e2430] border border-red-500/20 flex flex-col sm:flex-row sm:items-center justify-between gap-4 cursor-pointer hover:border-red-500/40 transition-all shadow-xl group"
              >
                <div className="flex items-center gap-4">
                  <img
                    src={filteredSongs[0].coverUrl}
                    alt={filteredSongs[0].title}
                    className="w-16 h-16 rounded-2xl object-cover shadow-lg group-hover:scale-105 transition-transform"
                  />
                  <div>
                    <h3 className="text-lg font-bold text-white font-outfit">
                      {filteredSongs[0].title}
                    </h3>
                    <p className="text-xs text-slate-300 mt-0.5">
                      Song • {filteredSongs[0].artist}
                    </p>
                    <span className="inline-block mt-2 px-2 py-0.5 rounded-md bg-red-500/20 text-red-400 text-[10px] font-semibold">
                      {filteredSongs[0].genre}
                    </span>
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    playSong(filteredSongs[0], filteredSongs);
                  }}
                  className="w-12 h-12 rounded-full bg-red-600 hover:bg-red-500 text-white flex items-center justify-center shadow-lg shadow-red-600/40 transition-all self-end sm:self-auto"
                >
                  <Play className="w-5 h-5 fill-white translate-x-0.5" />
                </button>
              </div>
            </div>
          )}

          {/* Songs Results */}
          {(activeFilter === 'all' || activeFilter === 'songs') && filteredSongs.length > 0 && (
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Songs ({filteredSongs.length})
              </span>
              <div className="space-y-1.5">
                {filteredSongs.map((song) => {
                  const isLiked = likedSongIds.includes(song.id);
                  const isCurrent = currentSong?.id === song.id;

                  return (
                    <div
                      key={song.id}
                      onClick={() => playSong(song, filteredSongs)}
                      className={`flex items-center justify-between p-2 rounded-2xl cursor-pointer transition-all ${
                        isCurrent
                          ? 'bg-red-500/15 border border-red-500/30'
                          : 'bg-white/[0.03] hover:bg-white/[0.08] border border-transparent'
                      }`}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <img
                          src={song.coverUrl}
                          alt={song.title}
                          className="w-11 h-11 rounded-xl object-cover shadow-sm flex-shrink-0"
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
                            {song.artist} • {song.album}
                          </p>
                        </div>
                      </div>

                      <div
                        className="flex items-center gap-1 sm:gap-2 flex-shrink-0"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <span className="text-xs font-mono text-slate-500 mr-1">
                          {formatTime(song.duration)}
                        </span>
                        <button
                          onClick={() => addToQueue(song)}
                          className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
                          title="Add to queue"
                        >
                          <ListPlus className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => toggleLike(song.id)}
                          className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-red-500 transition-colors"
                        >
                          <Heart
                            className={`w-4 h-4 ${isLiked ? 'fill-red-500 text-red-500' : ''}`}
                          />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Artists Results */}
          {(activeFilter === 'all' || activeFilter === 'artists') && filteredArtists.length > 0 && (
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Artists
              </span>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {filteredArtists.map((art) => (
                  <div
                    key={art.id}
                    onClick={() => onSelectArtist(art)}
                    className="flex items-center gap-3 p-3 rounded-2xl bg-[#151a21] border border-white/5 hover:border-white/15 cursor-pointer hover:bg-[#1a202c] transition-all"
                  >
                    <img
                      src={art.imageUrl}
                      alt={art.name}
                      className="w-12 h-12 rounded-full object-cover shadow-md"
                    />
                    <div className="min-w-0">
                      <p className="text-sm font-bold text-white truncate">{art.name}</p>
                      <p className="text-xs text-slate-400 truncate">
                        {art.monthlyListeners} listeners
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Albums Results */}
          {(activeFilter === 'all' || activeFilter === 'albums') && filteredAlbums.length > 0 && (
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Albums
              </span>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {filteredAlbums.map((alb) => (
                  <div
                    key={alb.id}
                    onClick={() => onSelectAlbum(alb)}
                    className="flex items-center gap-3 p-3 rounded-2xl bg-[#151a21] border border-white/5 hover:border-white/15 cursor-pointer hover:bg-[#1a202c] transition-all"
                  >
                    <img
                      src={alb.coverUrl}
                      alt={alb.title}
                      className="w-12 h-12 rounded-xl object-cover shadow-md"
                    />
                    <div className="min-w-0">
                      <p className="text-sm font-bold text-white truncate">{alb.title}</p>
                      <p className="text-xs text-slate-400 truncate">{alb.artist}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* No results */}
          {filteredSongs.length === 0 && filteredArtists.length === 0 && filteredAlbums.length === 0 && (
            <div className="text-center py-12">
              <Search className="w-12 h-12 text-slate-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-slate-300">No results found for "{query}"</p>
              <p className="text-xs text-slate-500 mt-1">
                Try searching for another artist, track, or music genre.
              </p>
            </div>
          )}
        </div>
      ) : (
        /* Default Browse Categories */
        <div>
          <h3 className="text-lg font-bold text-white font-outfit mb-3 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-amber-400" />
            <span>Browse Genres & Moods</span>
          </h3>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {genres.map((g) => (
              <div
                key={g.name}
                onClick={() => setQuery(g.query)}
                className={`h-24 p-3.5 rounded-2xl bg-gradient-to-br ${g.color} cursor-pointer shadow-lg hover:scale-[1.02] active:scale-95 transition-all flex flex-col justify-between`}
              >
                <span className="font-bold text-base text-white font-outfit">{g.name}</span>
                <span className="text-[10px] text-white/75 font-semibold uppercase tracking-wider">
                  Explore →
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
