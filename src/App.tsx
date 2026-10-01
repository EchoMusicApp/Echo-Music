import React, { useState } from 'react';
import { PlayerProvider, usePlayer } from './context/PlayerContext';
import { Navigation } from './components/Navigation';
import { MiniPlayer } from './components/MiniPlayer';
import { FullPlayer } from './components/FullPlayer';
import { EqualizerModal } from './components/EqualizerModal';
import { SleepTimerModal } from './components/SleepTimerModal';
import { AddToPlaylistModal } from './components/AddToPlaylistModal';
import { ListenTogetherModal } from './components/ListenTogetherModal';
import { SpotifyImportModal } from './components/SpotifyImportModal';
import { MusicRecognizerModal } from './components/MusicRecognizerModal';
import { HomeScreen } from './screens/HomeScreen';
import { SearchScreen } from './screens/SearchScreen';
import { LibraryScreen } from './screens/LibraryScreen';
import { PlaylistDetailScreen } from './screens/PlaylistDetailScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { Playlist, Album, Song } from './types/music';

const AppContent: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<string>('home');
  const [selectedDetail, setSelectedDetail] = useState<{
    item: Playlist | Album;
    type: 'playlist' | 'album';
  } | null>(null);

  // Modals state
  const [isEqOpen, setIsEqOpen] = useState(false);
  const [isSleepOpen, setIsSleepOpen] = useState(false);
  const [isAddToPlaylistOpen, setIsAddToPlaylistOpen] = useState(false);
  const [targetSongForPlaylist, setTargetSongForPlaylist] = useState<Song | null>(null);
  const [isJamOpen, setIsJamOpen] = useState(false);
  const [isSpotifyImportOpen, setIsSpotifyImportOpen] = useState(false);
  const [isRecognizerOpen, setIsRecognizerOpen] = useState(false);

  const handleOpenAddToPlaylist = (song?: Song) => {
    setTargetSongForPlaylist(song || null);
    setIsAddToPlaylistOpen(true);
  };

  const handleSelectPlaylist = (playlist: Playlist) => {
    setSelectedDetail({ item: playlist, type: 'playlist' });
  };

  const handleSelectAlbum = (album: Album) => {
    setSelectedDetail({ item: album, type: 'album' });
  };

  const handleTabChange = (tab: string) => {
    if (tab === 'jam') {
      setIsJamOpen(true);
      return;
    }
    setSelectedDetail(null);
    setCurrentTab(tab);
  };

  return (
    <div className="min-h-screen bg-[#0c0f14] text-slate-100 flex flex-col relative font-sans">
      {/* Top Header */}
      <Navigation
        currentTab={selectedDetail ? 'detail' : currentTab}
        onTabChange={handleTabChange}
        onOpenRecognizer={() => setIsRecognizerOpen(true)}
        onOpenSettings={() => {
          setSelectedDetail(null);
          setCurrentTab('settings');
        }}
      />

      {/* Main View Area */}
      <main className="flex-1 w-full max-w-5xl mx-auto pt-2">
        {selectedDetail ? (
          <PlaylistDetailScreen
            item={selectedDetail.item}
            type={selectedDetail.type}
            onBack={() => setSelectedDetail(null)}
            onOpenAddToPlaylist={handleOpenAddToPlaylist}
          />
        ) : currentTab === 'home' ? (
          <HomeScreen
            onSelectPlaylist={handleSelectPlaylist}
            onSelectAlbum={handleSelectAlbum}
            onOpenAddToPlaylist={handleOpenAddToPlaylist}
          />
        ) : currentTab === 'search' ? (
          <SearchScreen
            onSelectAlbum={handleSelectAlbum}
            onSelectArtist={() => {}}
            onOpenAddToPlaylist={handleOpenAddToPlaylist}
          />
        ) : currentTab === 'library' ? (
          <LibraryScreen
            onSelectPlaylist={handleSelectPlaylist}
            onOpenSpotifyImport={() => setIsSpotifyImportOpen(true)}
            onOpenCreatePlaylist={() => handleOpenAddToPlaylist()}
            onOpenAddToPlaylist={handleOpenAddToPlaylist}
          />
        ) : currentTab === 'settings' ? (
          <SettingsScreen onOpenEqualizer={() => setIsEqOpen(true)} />
        ) : null}
      </main>

      {/* Floating Mini Player (visible always when a track is active) */}
      <MiniPlayer />

      {/* Full Player Modal */}
      <FullPlayer
        onOpenEqualizer={() => setIsEqOpen(true)}
        onOpenSleepTimer={() => setIsSleepOpen(true)}
        onOpenAddToPlaylist={() => handleOpenAddToPlaylist()}
      />

      {/* Feature Modals */}
      <EqualizerModal isOpen={isEqOpen} onClose={() => setIsEqOpen(false)} />
      <SleepTimerModal isOpen={isSleepOpen} onClose={() => setIsSleepOpen(false)} />
      <AddToPlaylistModal
        isOpen={isAddToPlaylistOpen}
        onClose={() => setIsAddToPlaylistOpen(false)}
        targetSong={targetSongForPlaylist}
      />
      <ListenTogetherModal isOpen={isJamOpen} onClose={() => setIsJamOpen(false)} />
      <SpotifyImportModal
        isOpen={isSpotifyImportOpen}
        onClose={() => setIsSpotifyImportOpen(false)}
      />
      <MusicRecognizerModal
        isOpen={isRecognizerOpen}
        onClose={() => setIsRecognizerOpen(false)}
      />
    </div>
  );
};

export function App() {
  return (
    <PlayerProvider>
      <AppContent />
    </PlayerProvider>
  );
}

export default App;
