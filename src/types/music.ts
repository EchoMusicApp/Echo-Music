export interface LyricLine {
  time: number; // in seconds
  text: string;
  translation?: string;
}

export interface Song {
  id: string;
  title: string;
  artist: string;
  album: string;
  duration: number; // seconds
  coverUrl: string;
  audioUrl: string;
  lyrics?: LyricLine[];
  colorHex?: string;
  isLiked?: boolean;
  isDownloaded?: boolean;
  plays?: number;
  genre?: string;
}

export interface Playlist {
  id: string;
  title: string;
  description: string;
  coverUrl: string;
  songs: Song[];
  isCustom?: boolean;
  createdAt?: string;
}

export interface Album {
  id: string;
  title: string;
  artist: string;
  year: number;
  coverUrl: string;
  songs: Song[];
}

export interface Artist {
  id: string;
  name: string;
  bio: string;
  imageUrl: string;
  monthlyListeners: string;
  topSongs: Song[];
}

export type PlayerStyle = 'material' | 'apple';

export type RepeatMode = 'off' | 'all' | 'one';

export interface EqualizerSettings {
  enabled: boolean;
  bass: number; // -10 to +10 dB
  mid: number;  // -10 to +10 dB
  treble: number; // -10 to +10 dB
  preset: 'flat' | 'bass_boost' | 'electronic' | 'rock' | 'vocal' | 'custom';
}
