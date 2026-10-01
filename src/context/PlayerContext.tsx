import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { Song, Playlist, PlayerStyle, RepeatMode, EqualizerSettings, LyricLine } from '../types/music';
import { SONGS, PLAYLISTS } from '../data/mockData';

interface PlayerContextType {
  currentSong: Song | null;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  isMuted: boolean;
  isShuffled: boolean;
  repeatMode: RepeatMode;
  queue: Song[];
  queueIndex: number;
  playerStyle: PlayerStyle;
  isPlayerOpen: boolean;
  history: Song[];
  likedSongIds: string[];
  downloadedSongIds: string[];
  playlists: Playlist[];
  equalizer: EqualizerSettings;
  sleepTimerMinutes: number | null;
  sleepTimerRemaining: number | null;
  currentLyricIndex: number;
  dataSaver: boolean;

  // Actions
  playSong: (song: Song, playlistQueue?: Song[]) => void;
  togglePlay: () => void;
  seek: (seconds: number) => void;
  nextTrack: () => void;
  prevTrack: () => void;
  setVolumeLevel: (val: number) => void;
  toggleMute: () => void;
  toggleShuffle: () => void;
  cycleRepeatMode: () => void;
  setPlayerStyle: (style: PlayerStyle) => void;
  setIsPlayerOpen: (open: boolean) => void;
  toggleLike: (songId: string) => void;
  toggleDownload: (songId: string) => void;
  addToQueue: (song: Song) => void;
  removeFromQueue: (index: number) => void;
  clearQueue: () => void;
  reorderQueue: (startIndex: number, endIndex: number) => void;
  createPlaylist: (title: string, description: string) => string;
  addSongToPlaylist: (playlistId: string, song: Song) => void;
  removeSongFromPlaylist: (playlistId: string, songId: string) => void;
  updateEqualizer: (eq: Partial<EqualizerSettings>) => void;
  setSleepTimer: (minutes: number | null) => void;
  toggleDataSaver: () => void;
  importSpotifyTracks: (playlistName: string, tracks: Song[]) => void;
}

const PlayerContext = createContext<PlayerContextType | null>(null);

export const PlayerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentSong, setCurrentSong] = useState<Song | null>(SONGS[0]);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(SONGS[0].duration);
  const [volume, setVolume] = useState<number>(0.85);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [isShuffled, setIsShuffled] = useState<boolean>(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>('all');
  const [queue, setQueue] = useState<Song[]>(SONGS);
  const [queueIndex, setQueueIndex] = useState<number>(0);
  const [playerStyle, setPlayerStyle] = useState<PlayerStyle>('material');
  const [isPlayerOpen, setIsPlayerOpen] = useState<boolean>(false);
  const [history, setHistory] = useState<Song[]>([SONGS[1], SONGS[2]]);
  const [likedSongIds, setLikedSongIds] = useState<string[]>(['song-1', 'song-2', 'song-4', 'song-7']);
  const [downloadedSongIds, setDownloadedSongIds] = useState<string[]>(['song-1', 'song-3', 'song-4']);
  const [playlists, setPlaylists] = useState<Playlist[]>(PLAYLISTS);
  const [dataSaver, setDataSaver] = useState<boolean>(false);

  // Equalizer
  const [equalizer, setEqualizer] = useState<EqualizerSettings>({
    enabled: true,
    bass: 2,
    mid: 0,
    treble: 3,
    preset: 'bass_boost',
  });

  // Sleep timer
  const [sleepTimerMinutes, setSleepTimerMinutesState] = useState<number | null>(null);
  const [sleepTimerRemaining, setSleepTimerRemaining] = useState<number | null>(null);

  // Audio elements & Web Audio API
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const sourceNodeRef = useRef<MediaElementAudioSourceNode | null>(null);
  const bassFilterRef = useRef<BiquadFilterNode | null>(null);
  const midFilterRef = useRef<BiquadFilterNode | null>(null);
  const trebleFilterRef = useRef<BiquadFilterNode | null>(null);
  const synthIntervalRef = useRef<number | null>(null);

  // Init audio element
  useEffect(() => {
    const audio = new Audio();
    audio.preload = 'metadata';
    audio.crossOrigin = 'anonymous';
    audioRef.current = audio;

    const onTimeUpdate = () => {
      if (audio.currentTime) {
        setCurrentTime(audio.currentTime);
      }
    };

    const onLoadedMetadata = () => {
      if (audio.duration && !isNaN(audio.duration) && audio.duration > 0) {
        setDuration(audio.duration);
      }
    };

    const onEnded = () => {
      handleTrackEnd();
    };

    const onError = () => {
      // If public CDN fails or CORS error, fall back gracefully to synthetic timer
      console.warn("Audio playback source error, using simulated playback timer");
      startSyntheticPlayback();
    };

    audio.addEventListener('timeupdate', onTimeUpdate);
    audio.addEventListener('loadedmetadata', onLoadedMetadata);
    audio.addEventListener('ended', onEnded);
    audio.addEventListener('error', onError);

    return () => {
      audio.removeEventListener('timeupdate', onTimeUpdate);
      audio.removeEventListener('loadedmetadata', onLoadedMetadata);
      audio.removeEventListener('ended', onEnded);
      audio.removeEventListener('error', onError);
      audio.pause();
    };
  }, []);

  // Web Audio Equalizer graph
  const setupWebAudio = useCallback(() => {
    if (!audioRef.current || audioContextRef.current) return;
    try {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      const ctx = new AudioCtx();
      audioContextRef.current = ctx;

      const source = ctx.createMediaElementSource(audioRef.current);
      sourceNodeRef.current = source;

      // Lowshelf for Bass
      const bass = ctx.createBiquadFilter();
      bass.type = 'lowshelf';
      bass.frequency.value = 250;
      bass.gain.value = equalizer.enabled ? equalizer.bass : 0;
      bassFilterRef.current = bass;

      // Peaking for Mid
      const mid = ctx.createBiquadFilter();
      mid.type = 'peaking';
      mid.frequency.value = 1500;
      mid.gain.value = equalizer.enabled ? equalizer.mid : 0;
      midFilterRef.current = mid;

      // Highshelf for Treble
      const treble = ctx.createBiquadFilter();
      treble.type = 'highshelf';
      treble.frequency.value = 6000;
      treble.gain.value = equalizer.enabled ? equalizer.treble : 0;
      trebleFilterRef.current = treble;

      source.connect(bass);
      bass.connect(mid);
      mid.connect(treble);
      treble.connect(ctx.destination);
    } catch (e) {
      console.warn("Web Audio not supported or failed to initialize", e);
    }
  }, [equalizer]);

  // Update EQ gains
  useEffect(() => {
    if (bassFilterRef.current) {
      bassFilterRef.current.gain.value = equalizer.enabled ? equalizer.bass : 0;
    }
    if (midFilterRef.current) {
      midFilterRef.current.gain.value = equalizer.enabled ? equalizer.mid : 0;
    }
    if (trebleFilterRef.current) {
      trebleFilterRef.current.gain.value = equalizer.enabled ? equalizer.treble : 0;
    }
  }, [equalizer]);

  // Sync volume with audio element
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = isMuted ? 0 : volume;
    }
  }, [volume, isMuted]);

  // Handle synthetic fallback for seamless preview
  const startSyntheticPlayback = () => {
    if (synthIntervalRef.current) clearInterval(synthIntervalRef.current);
    synthIntervalRef.current = window.setInterval(() => {
      setCurrentTime(prev => {
        if (currentSong && prev >= (currentSong.duration || 180)) {
          handleTrackEnd();
          return 0;
        }
        return prev + 1;
      });
    }, 1000);
  };

  const stopSyntheticPlayback = () => {
    if (synthIntervalRef.current) {
      clearInterval(synthIntervalRef.current);
      synthIntervalRef.current = null;
    }
  };

  // Sleep Timer countdown
  useEffect(() => {
    if (sleepTimerRemaining === null) return;
    if (sleepTimerRemaining <= 0) {
      setIsPlaying(false);
      if (audioRef.current) audioRef.current.pause();
      stopSyntheticPlayback();
      setSleepTimerRemaining(null);
      setSleepTimerMinutesState(null);
      return;
    }

    const timer = setInterval(() => {
      setSleepTimerRemaining(prev => (prev !== null && prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(timer);
  }, [sleepTimerRemaining]);

  const setSleepTimer = (minutes: number | null) => {
    setSleepTimerMinutesState(minutes);
    if (minutes === null) {
      setSleepTimerRemaining(null);
    } else {
      setSleepTimerRemaining(minutes * 60);
    }
  };

  // Play specific song
  const playSong = (song: Song, playlistQueue?: Song[]) => {
    setupWebAudio();
    if (audioContextRef.current && audioContextRef.current.state === 'suspended') {
      audioContextRef.current.resume();
    }

    setCurrentSong(song);
    setCurrentTime(0);
    setDuration(song.duration);

    if (playlistQueue && playlistQueue.length > 0) {
      setQueue(playlistQueue);
      const idx = playlistQueue.findIndex(s => s.id === song.id);
      setQueueIndex(idx !== -1 ? idx : 0);
    }

    // Add to history
    setHistory(prev => [song, ...prev.filter(s => s.id !== song.id)].slice(0, 30));

    if (audioRef.current) {
      audioRef.current.src = song.audioUrl;
      audioRef.current.currentTime = 0;
      audioRef.current.play().then(() => {
        setIsPlaying(true);
        stopSyntheticPlayback();
      }).catch((err) => {
        console.warn("Autoplay / audio stream fallback", err);
        setIsPlaying(true);
        startSyntheticPlayback();
      });
    } else {
      setIsPlaying(true);
      startSyntheticPlayback();
    }
  };

  const togglePlay = () => {
    if (!currentSong && queue.length > 0) {
      playSong(queue[0]);
      return;
    }

    setupWebAudio();
    if (audioContextRef.current && audioContextRef.current.state === 'suspended') {
      audioContextRef.current.resume();
    }

    if (isPlaying) {
      if (audioRef.current) audioRef.current.pause();
      stopSyntheticPlayback();
      setIsPlaying(false);
    } else {
      if (audioRef.current && audioRef.current.src) {
        audioRef.current.play().then(() => {
          setIsPlaying(true);
          stopSyntheticPlayback();
        }).catch(() => {
          setIsPlaying(true);
          startSyntheticPlayback();
        });
      } else if (currentSong) {
        playSong(currentSong);
      }
    }
  };

  const seek = (seconds: number) => {
    setCurrentTime(seconds);
    if (audioRef.current) {
      try {
        audioRef.current.currentTime = seconds;
      } catch (e) {
        // ignore
      }
    }
  };

  const handleTrackEnd = () => {
    if (repeatMode === 'one' && currentSong) {
      seek(0);
      if (audioRef.current) {
        audioRef.current.play();
      }
    } else {
      nextTrack();
    }
  };

  const nextTrack = () => {
    if (queue.length === 0) return;
    let nextIdx: number;
    if (isShuffled) {
      nextIdx = Math.floor(Math.random() * queue.length);
    } else {
      nextIdx = queueIndex + 1;
      if (nextIdx >= queue.length) {
        if (repeatMode === 'off') {
          setIsPlaying(false);
          return;
        }
        nextIdx = 0;
      }
    }
    setQueueIndex(nextIdx);
    playSong(queue[nextIdx]);
  };

  const prevTrack = () => {
    if (currentTime > 4) {
      seek(0);
      return;
    }
    if (queue.length === 0) return;
    let prevIdx = queueIndex - 1;
    if (prevIdx < 0) {
      prevIdx = queue.length - 1;
    }
    setQueueIndex(prevIdx);
    playSong(queue[prevIdx]);
  };

  const setVolumeLevel = (val: number) => {
    setVolume(val);
    if (val > 0) setIsMuted(false);
  };

  const toggleMute = () => {
    setIsMuted(prev => !prev);
  };

  const toggleShuffle = () => {
    setIsShuffled(prev => !prev);
  };

  const cycleRepeatMode = () => {
    setRepeatMode(prev => {
      if (prev === 'off') return 'all';
      if (prev === 'all') return 'one';
      return 'off';
    });
  };

  const toggleLike = (songId: string) => {
    setLikedSongIds(prev =>
      prev.includes(songId) ? prev.filter(id => id !== songId) : [...prev, songId]
    );
  };

  const toggleDownload = (songId: string) => {
    setDownloadedSongIds(prev =>
      prev.includes(songId) ? prev.filter(id => id !== songId) : [...prev, songId]
    );
  };

  const addToQueue = (song: Song) => {
    setQueue(prev => [...prev, song]);
  };

  const removeFromQueue = (index: number) => {
    setQueue(prev => prev.filter((_, i) => i !== index));
    if (index < queueIndex) {
      setQueueIndex(prev => prev - 1);
    }
  };

  const clearQueue = () => {
    if (currentSong) {
      setQueue([currentSong]);
      setQueueIndex(0);
    } else {
      setQueue([]);
      setQueueIndex(0);
    }
  };

  const reorderQueue = (startIndex: number, endIndex: number) => {
    setQueue(prev => {
      const next = [...prev];
      const [removed] = next.splice(startIndex, 1);
      next.splice(endIndex, 0, removed);
      return next;
    });
  };

  const createPlaylist = (title: string, description: string): string => {
    const newId = `pl-${Date.now()}`;
    const newPlaylist: Playlist = {
      id: newId,
      title,
      description,
      coverUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80',
      songs: [],
      isCustom: true,
      createdAt: new Date().toLocaleDateString()
    };
    setPlaylists(prev => [...prev, newPlaylist]);
    return newId;
  };

  const addSongToPlaylist = (playlistId: string, song: Song) => {
    setPlaylists(prev =>
      prev.map(pl => {
        if (pl.id === playlistId && !pl.songs.some(s => s.id === song.id)) {
          return { ...pl, songs: [...pl.songs, song] };
        }
        return pl;
      })
    );
  };

  const removeSongFromPlaylist = (playlistId: string, songId: string) => {
    setPlaylists(prev =>
      prev.map(pl => {
        if (pl.id === playlistId) {
          return { ...pl, songs: pl.songs.filter(s => s.id !== songId) };
        }
        return pl;
      })
    );
  };

  const updateEqualizer = (eq: Partial<EqualizerSettings>) => {
    setEqualizer(prev => ({ ...prev, ...eq }));
  };

  const toggleDataSaver = () => {
    setDataSaver(prev => !prev);
  };

  const importSpotifyTracks = (playlistName: string, tracks: Song[]) => {
    const newId = `pl-spotify-${Date.now()}`;
    const importedPl: Playlist = {
      id: newId,
      title: playlistName || 'Imported Spotify Playlist',
      description: `Imported via Spotify Sync (${tracks.length} tracks)`,
      coverUrl: tracks[0]?.coverUrl || 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80',
      songs: tracks,
      isCustom: true,
      createdAt: new Date().toLocaleDateString()
    };
    setPlaylists(prev => [...prev, importedPl]);
  };

  // Compute current active lyric index based on currentTime
  const currentLyricIndex = React.useMemo(() => {
    if (!currentSong?.lyrics || currentSong.lyrics.length === 0) return -1;
    const lyrics = currentSong.lyrics;
    let activeIdx = -1;
    for (let i = 0; i < lyrics.length; i++) {
      if (currentTime >= lyrics[i].time) {
        activeIdx = i;
      } else {
        break;
      }
    }
    return activeIdx;
  }, [currentSong, currentTime]);

  return (
    <PlayerContext.Provider
      value={{
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
        history,
        likedSongIds,
        downloadedSongIds,
        playlists,
        equalizer,
        sleepTimerMinutes,
        sleepTimerRemaining,
        currentLyricIndex,
        dataSaver,

        playSong,
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
        addToQueue,
        removeFromQueue,
        clearQueue,
        reorderQueue,
        createPlaylist,
        addSongToPlaylist,
        removeSongFromPlaylist,
        updateEqualizer,
        setSleepTimer,
        toggleDataSaver,
        importSpotifyTracks,
      }}
    >
      {children}
    </PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  const context = useContext(PlayerContext);
  if (!context) throw new Error('usePlayer must be used within PlayerProvider');
  return context;
};
