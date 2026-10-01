import { Song, Playlist, Album, Artist } from '../types/music';

// Synced lyrics samples
const midnightCityLyrics = [
  { time: 0, text: "(Intro - Synth Arpeggios)", translation: "Instrumental" },
  { time: 8, text: "Waiting in a car", translation: "In a car, quietly waiting" },
  { time: 13, text: "Waiting for a ride in the dark", translation: "Traveling through the night" },
  { time: 18, text: "The night city grows", translation: "Neon skyscrapers rise above" },
  { time: 24, text: "Look and see her eyes, they glow", translation: "Glowing across the skyline" },
  { time: 30, text: "Waiting in a car", translation: "Counting the city lights" },
  { time: 35, text: "Waiting for a ride in the dark", translation: "Shadows dance on asphalt" },
  { time: 42, text: "The city is my church", translation: "The sanctuary of beats" },
  { time: 48, text: "It wraps in the blinding twilight", translation: "Wrapped in luminous aura" },
  { time: 56, text: "Waiting in a car, waiting in a car...", translation: "Driving endless highways" },
  { time: 68, text: "(Saxophone & Synth drop)", translation: "Melodic climax" },
  { time: 85, text: "Looking at the stars tonight", translation: "Endless constellations" },
];

const starboyLyrics = [
  { time: 0, text: "(Upbeat electronic beat kicks in)", translation: "Intro beat" },
  { time: 5, text: "I'm tryna put you in the worst mood, ah", translation: "Setting the tempo" },
  { time: 10, text: "P1 cleaner than your church shoes, ah", translation: "Supercar shining clean" },
  { time: 15, text: "Milli point two just to hurt you, ah", translation: "Unapologetic success" },
  { time: 20, text: "All red Lamb' just to tease you, ah", translation: "Crimson roar of the engine" },
  { time: 25, text: "None of these toys on lease too, ah", translation: "Own everything with pride" },
  { time: 30, text: "Made your whole year in a week too, yah", translation: "Living at hyper speed" },
  { time: 35, text: "Main girl out of your league too, ah", translation: "High elevation" },
  { time: 40, text: "Look what you've done, I'm a motherfuckin' starboy", translation: "Chorus - Starboy" },
  { time: 47, text: "Every day a star is born, clap for 'em", translation: "Rise to the pinnacle" },
];

const goldenHourLyrics = [
  { time: 0, text: "(Soft piano chords arpeggiating)", translation: "Gentle piano intro" },
  { time: 6, text: "It was just two lovers", translation: "Two souls entangled" },
  { time: 12, text: "Sittin' in the car, listenin' to Blonde", translation: "Music filling the quiet air" },
  { time: 18, text: "Fallin' for each other", translation: "Helplessly sinking in love" },
  { time: 24, text: "Pink and orange skies, feelin' super childish", translation: "Sunset painted in warm hues" },
  { time: 30, text: "Donald Glover, no, you don't need no other", translation: "Serenity in this moment" },
  { time: 38, text: "You slow down time in a world that's movin' too fast", translation: "Chorus - Golden Hour" },
  { time: 48, text: "Glow in your eyes, everything is right", translation: "Shining brightly" },
];

// Audio files: High reliability royalty-free audio tracks from SoundHelix & CDN streams
export const SONGS: Song[] = [
  {
    id: 'song-1',
    title: 'Midnight Echoes',
    artist: 'Luna Horizon',
    album: 'Neon Reverie',
    duration: 198,
    coverUrl: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80',
    audioUrl: '/audio/song-1.mp3',
    lyrics: midnightCityLyrics,
    colorHex: '#8b5cf6',
    isLiked: true,
    isDownloaded: true,
    plays: 1420000,
    genre: 'Synthwave'
  },
  {
    id: 'song-2',
    title: 'Blinding Pulse',
    artist: 'Aero Daft',
    album: 'Hyperdrive Deluxe',
    duration: 224,
    coverUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80',
    audioUrl: '/audio/song-2.mp3',
    lyrics: starboyLyrics,
    colorHex: '#ef4444',
    isLiked: true,
    isDownloaded: false,
    plays: 2890000,
    genre: 'Electronic'
  },
  {
    id: 'song-3',
    title: 'Golden Sunset Serenade',
    artist: 'Kaelen Wood',
    album: 'Solstice Memories',
    duration: 215,
    coverUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80',
    audioUrl: '/audio/song-3.mp3',
    lyrics: goldenHourLyrics,
    colorHex: '#f59e0b',
    isLiked: false,
    isDownloaded: true,
    plays: 950000,
    genre: 'Acoustic / Indie'
  },
  {
    id: 'song-4',
    title: 'Rain on Tokyo Street',
    artist: 'Kenji Sato',
    album: 'Midnight in Shibuya',
    duration: 260,
    coverUrl: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=600&auto=format&fit=crop&q=80',
    audioUrl: '/audio/song-4.mp3',
    lyrics: [
      { time: 0, text: "(Raindrops hitting umbrella, distant city ambient)", translation: "Intro" },
      { time: 10, text: "Walking down the neon alleyway", translation: "Under Shibuya lights" },
      { time: 22, text: "Steam rising from the ramen stand", translation: "Warmth against the cold" },
      { time: 35, text: "Memories linger like the mist", translation: "Past whispers" },
      { time: 48, text: "Until the morning sun returns", translation: "Until sunrise" },
    ],
    colorHex: '#06b6d4',
    isLiked: true,
    isDownloaded: true,
    plays: 640000,
    genre: 'Lo-Fi Chill'
  },
  {
    id: 'song-5',
    title: 'After Hours Velocity',
    artist: 'The Vibe Collective',
    album: 'Metropolis Dreams',
    duration: 185,
    coverUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80',
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',
    lyrics: [
      { time: 0, text: "(Fast driving bassline)", translation: "Groove" },
      { time: 12, text: "Pedal to the metal, night won't wait", translation: "Accelerating fast" },
      { time: 25, text: "Reflections in rearview mirror fade away", translation: "Leaving it all behind" },
      { time: 40, text: "Just keep dancing through the storm", translation: "Rhythm never stops" },
    ],
    colorHex: '#ec4899',
    isLiked: false,
    isDownloaded: false,
    plays: 1100000,
    genre: 'Nu-Disco'
  },
  {
    id: 'song-6',
    title: 'Echoes of the Canyon',
    artist: 'Sierra Valley',
    album: 'Wilderness Whispers',
    duration: 242,
    coverUrl: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=600&auto=format&fit=crop&q=80',
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',
    lyrics: [
      { time: 0, text: "(Wind blowing through pine needles)", translation: "Nature Intro" },
      { time: 15, text: "Standing at the edge of quiet cliffs", translation: "Looking over canyon" },
      { time: 30, text: "I call your name and hear the sound return", translation: "Echoes resonate" },
      { time: 50, text: "Like an old guitar on summer breeze", translation: "Sweet harmony" },
    ],
    colorHex: '#10b981',
    isLiked: false,
    isDownloaded: false,
    plays: 420000,
    genre: 'Folk / Ambient'
  },
  {
    id: 'song-7',
    title: 'Cyberpunk Odyssey',
    artist: 'Luna Horizon',
    album: 'Neon Reverie',
    duration: 210,
    coverUrl: 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80',
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',
    lyrics: [
      { time: 0, text: "(Digital glitch beats)", translation: "Syncing systems" },
      { time: 14, text: "Data streams running through our veins", translation: "High bandwidth soul" },
      { time: 28, text: "In 2077 we found love again", translation: "Future romance" },
      { time: 45, text: "Overclock the audio, feel the power", translation: "Full volume" },
    ],
    colorHex: '#a855f7',
    isLiked: true,
    isDownloaded: false,
    plays: 3400000,
    genre: 'Synthwave'
  },
  {
    id: 'song-8',
    title: 'Celestial Drift',
    artist: 'Astral Travelers',
    album: 'Deep Space Broadcast',
    duration: 275,
    coverUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80',
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',
    lyrics: [
      { time: 0, text: "(Cosmic ambient frequencies)", translation: "Orbital drift" },
      { time: 20, text: "Floating past Saturn's icy rings", translation: "Weightless serenity" },
      { time: 40, text: "Signal from Earth fading into stars", translation: "Silent universe" },
    ],
    colorHex: '#3b82f6',
    isLiked: false,
    isDownloaded: true,
    plays: 510000,
    genre: 'Ambient Space'
  }
];

export const PLAYLISTS: Playlist[] = [
  {
    id: 'pl-favorites',
    title: 'Liked Songs',
    description: 'All your favorite songs gathered in one place',
    coverUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[0], SONGS[1], SONGS[3], SONGS[6]],
    isCustom: false
  },
  {
    id: 'pl-late-night',
    title: 'Late Night Chill & Drive',
    description: 'Smooth synths and lowkey beats for peaceful drives and late night coding',
    coverUrl: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[0], SONGS[3], SONGS[4], SONGS[7]],
    isCustom: false
  },
  {
    id: 'pl-workout',
    title: 'High Octane Workout',
    description: 'Pure adrenaline and heavy hitting rhythms to smash your personal records',
    coverUrl: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[1], SONGS[4], SONGS[6]],
    isCustom: false
  },
  {
    id: 'pl-focus',
    title: 'Deep Focus & Flow',
    description: 'Instrumental soundscapes engineered to eliminate distractions',
    coverUrl: 'https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[2], SONGS[3], SONGS[5], SONGS[7]],
    isCustom: false
  }
];

export const ALBUMS: Album[] = [
  {
    id: 'alb-1',
    title: 'Neon Reverie',
    artist: 'Luna Horizon',
    year: 2024,
    coverUrl: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[0], SONGS[6]]
  },
  {
    id: 'alb-2',
    title: 'Hyperdrive Deluxe',
    artist: 'Aero Daft',
    year: 2023,
    coverUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[1]]
  },
  {
    id: 'alb-3',
    title: 'Midnight in Shibuya',
    artist: 'Kenji Sato',
    year: 2024,
    coverUrl: 'https://images.unsplash.com/photo-1503945438517-f65904a52ce6?w=600&auto=format&fit=crop&q=80',
    songs: [SONGS[3]]
  }
];

export const ARTISTS: Artist[] = [
  {
    id: 'art-1',
    name: 'Luna Horizon',
    bio: 'Pioneering retro-futuristic soundscapes with modern analog synth mastery and dreamy vocals.',
    imageUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop&q=80',
    monthlyListeners: '4.8M',
    topSongs: [SONGS[0], SONGS[6]]
  },
  {
    id: 'art-2',
    name: 'Aero Daft',
    bio: 'Electronic powerhouse duo bridging French house, electro funk, and futuristic synth beats.',
    imageUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&auto=format&fit=crop&q=80',
    monthlyListeners: '7.2M',
    topSongs: [SONGS[1]]
  },
  {
    id: 'art-3',
    name: 'Kenji Sato',
    bio: 'Tokyo-based producer renowned for organic lo-fi textures and rainy night city jazz.',
    imageUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=600&auto=format&fit=crop&q=80',
    monthlyListeners: '2.1M',
    topSongs: [SONGS[3]]
  }
];
