import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { SONGS } from '../data/mockData';
import { X, Check, ArrowRight, Music, RefreshCw, AlertCircle } from 'lucide-react';

interface SpotifyImportModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SpotifyImportModal: React.FC<SpotifyImportModalProps> = ({ isOpen, onClose }) => {
  const { importSpotifyTracks } = usePlayer();
  const [spotifyUrl, setSpotifyUrl] = useState('');
  const [isImporting, setIsImporting] = useState(false);
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const [matchedTracksCount, setMatchedTracksCount] = useState<number>(0);

  if (!isOpen) return null;

  const handleImport = (e: React.FormEvent) => {
    e.preventDefault();
    if (!spotifyUrl.trim()) return;

    setIsImporting(true);
    setImportStatus('Connecting to Spotify metadata provider...');

    setTimeout(() => {
      setImportStatus('Fetching track list and matching with Echo audio catalog...');
      setTimeout(() => {
        // Select matching songs from library
        const importedSongs = SONGS.slice(0, 5);
        setMatchedTracksCount(importedSongs.length);
        importSpotifyTracks('Spotify My Top Hits 2024', importedSongs);
        setIsImporting(false);
        setImportStatus('success');
      }, 1200);
    }, 1000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-sm rounded-3xl bg-[#151a21] border border-white/10 p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <Music className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base text-white font-outfit">Spotify Import</h3>
              <p className="text-xs text-slate-400">Migrate your playlists seamlessly</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {importStatus === 'success' ? (
          <div className="py-6 text-center">
            <div className="w-12 h-12 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto mb-3">
              <Check className="w-6 h-6" />
            </div>
            <h4 className="text-base font-bold text-white">Import Complete!</h4>
            <p className="text-xs text-slate-400 mt-1">
              Successfully matched {matchedTracksCount} songs. Added to your Library playlists.
            </p>
            <button
              onClick={onClose}
              className="mt-5 w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs transition-all shadow-md shadow-emerald-600/30"
            >
              View in Library
            </button>
          </div>
        ) : (
          <form onSubmit={handleImport} className="mt-4 space-y-4">
            <div>
              <label className="text-xs font-semibold text-slate-300 block mb-1.5">
                Spotify Playlist Link or URI
              </label>
              <input
                type="text"
                placeholder="https://open.spotify.com/playlist/..."
                value={spotifyUrl}
                onChange={(e) => setSpotifyUrl(e.target.value)}
                className="w-full px-3.5 py-2.5 text-xs rounded-xl bg-white/5 border border-white/15 text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
              />
              <p className="text-[11px] text-slate-500 mt-1">
                Tip: Or enter artist / album name to automatically scrape & match.
              </p>
            </div>

            {isImporting && (
              <div className="flex items-center gap-2 p-3 rounded-xl bg-white/5 border border-white/10 text-xs text-emerald-400 animate-pulse">
                <RefreshCw className="w-4 h-4 animate-spin flex-shrink-0" />
                <span>{importStatus}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={isImporting || !spotifyUrl.trim()}
              className="w-full py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-500 active:scale-95 disabled:opacity-50 text-white text-xs font-bold shadow-lg shadow-emerald-600/30 transition-all flex items-center justify-center gap-2"
            >
              <span>Start Fast Sync</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
