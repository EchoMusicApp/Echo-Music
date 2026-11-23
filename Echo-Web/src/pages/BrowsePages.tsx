import { useState, useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  ArrowRight,
  Clock3,
  DownloadCloud,
  Headphones,
  Heart,
  Pause,
  Play,
  RotateCcw,
  Search as SearchIcon,
  X,
  Sparkles,
  FileUp,
  Music2,
  Compass,
  Smartphone,
} from 'lucide-react'
import { useLatestRelease } from '../hooks/useLatestRelease.ts'
import type { Track } from '../types/domain.ts'
import {
  useCharts,
  useHomeFeed,
  useMoodsAndGenres,
  useTrending,
  useThemedHomeShelves,
  THEMED_HOME_CATEGORIES,
} from '../hooks/useHomeFeed.ts'
import {
  useSearch,
  useSearchSuggestions,
  getStoredRecentSearches,
  saveRecentSearch,
  removeRecentSearch,
  clearAllRecentSearches,
} from '../hooks/useSearch.ts'
import type { SearchFilterKey } from '../services/innertube/client.ts'
import { getCachedTrack, useEchoStore } from '../stores/useEchoStore.ts'
import { classNames } from '../utils/classNames.ts'
import { artwork } from '../utils/artwork.ts'
import {
  CreatePlaylistDialog,
  MediaCard,
  PageHeading,
  SectionHeading,
  TrackList,
} from '../components/MusicUI.tsx'
import {
  getQuickPicks,
  getForgottenFavorites,
  getHeavyRotation,
  getTopArtists,
  generateTastePlaylist,
  getCurrentTimeOfDay,
} from '../services/recommendations/localTasteEngine.ts'
import { resolveMissingTrack } from '../services/db/echoDatabase.ts'
import { parseM3UPlaylist, parseCSVPlaylist } from '../services/sharing/playlistPortability.ts'
import { LocalMusicSection } from '../components/library/LocalMusicSection.tsx'
import { SpotifyImportModal } from '../components/modals/SpotifyImportModal.tsx'
import { InnerTubeClient } from '../services/innertube/client.ts'

export function HomePage() {
  const release = useLatestRelease()
  const { data: themedShelves, isLoading, isError, error, refetch } = useThemedHomeShelves()
  const [activeCategory, setActiveCategory] = useState<
    'all' | 'bollywood' | 'hollywood' | 'romance' | 'soothing'
  >('all')
  const currentTrack = useEchoStore((state) => state.currentTrack)
  const playing = useEchoStore((state) => state.isPlaying)
  const togglePlayback = useEchoStore((state) => state.togglePlayback)
  const playTrack = useEchoStore((state) => state.playTrack)
  const playlists = useEchoStore((state) => state.playlists)
  const likedTrackIds = useEchoStore((state) => state.likedTrackIds)
  const recentItems = useEchoStore((state) => state.recentItems)
  const createPlaylist = useEchoStore((state) => state.createPlaylist)
  const addTrackToPlaylist = useEchoStore((state) => state.addTrackToPlaylist)
  const setToast = useEchoStore((state) => state.setToast)

  const [quickPicks, setQuickPicks] = useState<Track[]>([])
  const [forgottenFavs, setForgottenFavs] = useState<Track[]>([])
  const [heavyRotation, setHeavyRotation] = useState<Track[]>([])
  const [topArtists, setTopArtists] = useState<
    Array<{ id: string; name: string; count: number; artwork?: string | undefined }>
  >([])
  const [isGeneratingMix, setIsGeneratingMix] = useState(false)

  const timeOfDay = getCurrentTimeOfDay()
  const greeting =
    timeOfDay === 'morning'
      ? 'Good morning'
      : timeOfDay === 'afternoon'
        ? 'Good afternoon'
        : timeOfDay === 'evening'
          ? 'Good evening'
          : 'Late night listening'

  useEffect(() => {
    let cancelled = false
    Promise.all([
      getQuickPicks(likedTrackIds, 8),
      getForgottenFavorites(likedTrackIds, 8),
      getHeavyRotation(likedTrackIds, 8),
      getTopArtists(6),
    ])
      .then(([qp, ff, hr, ta]) => {
        if (!cancelled) {
          setQuickPicks(qp)
          setForgottenFavs(ff)
          setHeavyRotation(hr)
          setTopArtists(ta)
        }
      })
      .catch(() => {})

    return () => {
      cancelled = true
    }
  }, [likedTrackIds, recentItems.length])

  const handleGenerateTasteMix = async () => {
    setIsGeneratingMix(true)
    try {
      const catalogTracks = quickPicks
      const mix = await generateTastePlaylist(likedTrackIds, catalogTracks)
      const plId = createPlaylist(mix.title, mix.description)
      for (const t of mix.tracks) {
        addTrackToPlaylist(plId, t)
      }
      setToast(`Created "${mix.title}" with ${mix.tracks.length} songs`)
    } catch {
      setToast('Could not generate taste playlist')
    } finally {
      setIsGeneratingMix(false)
    }
  }

  const isBrandNewUser =
    likedTrackIds.length === 0 &&
    playlists.length === 0 &&
    recentItems.length === 0 &&
    quickPicks.length === 0

  const heroTrack = currentTrack || quickPicks[0]
  const visibleShelves =
    activeCategory === 'all' ? themedShelves : themedShelves?.filter((s) => s.id === activeCategory)

  return (
    <div className="page page--home">
      <header className="page-heading mb-4">
        <div>
          <h1>{greeting}</h1>
          <p className="text-xs text-[#a59fad]">
            Your listening instrument. Tracks stay in local memory, ready when you are.
          </p>
        </div>
        <a
          href={release.apkUrl}
          className="button button--quiet download-app-btn"
          download
          rel="noopener noreferrer"
          title={`Download Echo Music Android APK (${release.version})`}
        >
          <Smartphone size={15} className="text-[#ed5564]" />
          <span>Download Mobile App</span>
        </a>
      </header>

      {/* Category Filter Pills */}
      <div className="filter-pills mb-6" role="tablist" aria-label="Music categories">
        <button
          type="button"
          role="tab"
          aria-selected={activeCategory === 'all'}
          className={classNames('filter-pill', activeCategory === 'all' && 'is-active')}
          onClick={() => setActiveCategory('all')}
        >
          All
        </button>
        {THEMED_HOME_CATEGORIES.map((cat) => (
          <button
            key={cat.id}
            type="button"
            role="tab"
            aria-selected={activeCategory === cat.id}
            className={classNames('filter-pill', activeCategory === cat.id && 'is-active')}
            onClick={() => setActiveCategory(cat.id)}
          >
            {cat.id === 'bollywood'
              ? 'Bollywood'
              : cat.id === 'hollywood'
                ? 'Hollywood'
                : cat.id === 'romance'
                  ? 'Romance'
                  : 'Soothing'}
          </button>
        ))}
      </div>

      {/* Hero Track Card */}
      {heroTrack && (
        <section className="home-now" aria-labelledby="home-now-title">
          <img
            className="home-now__art"
            src={
              heroTrack.artwork ||
              heroTrack.thumbnailUrl ||
              artwork(heroTrack.title || 'echo-track', 480)
            }
            alt={`${heroTrack.title} artwork`}
            width={480}
            height={480}
            fetchPriority="high"
            referrerPolicy="no-referrer"
            onError={(e) => {
              const fallback = artwork(heroTrack.title || 'echo-track', 480)
              if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
            }}
          />
          <div className="home-now__copy">
            <p className="home-now__state">
              {currentTrack ? 'In your player' : 'Recommended pick'}
            </p>
            <h1 id="home-now-title">{heroTrack.title}</h1>
            <div className="home-now__links">
              <Link to={`/artists/${heroTrack.artistId}`}>{heroTrack.artistName}</Link>
              {heroTrack.albumTitle && (
                <Link to={`/albums/${heroTrack.albumId}`}>{heroTrack.albumTitle}</Link>
              )}
            </div>
            <div className="mt-3 flex items-center gap-3">
              <button
                className="button button--primary home-now__play"
                type="button"
                aria-label={
                  currentTrack && playing ? `Pause ${heroTrack.title}` : `Play ${heroTrack.title}`
                }
                onClick={() => {
                  if (currentTrack && currentTrack.id === heroTrack.id) {
                    togglePlayback()
                  } else {
                    playTrack(heroTrack)
                  }
                }}
              >
                {currentTrack && playing ? (
                  <Pause size={17} fill="currentColor" />
                ) : (
                  <Play size={17} fill="currentColor" />
                )}
                {currentTrack && playing ? 'Pause' : 'Play'}
              </button>

              <button
                type="button"
                onClick={handleGenerateTasteMix}
                disabled={isGeneratingMix}
                className="flex items-center gap-1.5 rounded-xl bg-white/6 px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-white/10"
                title="Generate Taste Mix playlist"
              >
                <Sparkles size={14} className="text-[#b09cff]" />
                <span>{isGeneratingMix ? 'Generating...' : 'Taste Mix'}</span>
              </button>
            </div>
          </div>
        </section>
      )}

      {/* New User Onboarding Discovery */}
      {isBrandNewUser && (
        <section className="page-section rounded-2xl border border-white/10 bg-white/4 p-6">
          <div className="mb-2 flex items-center gap-3 text-[#b09cff]">
            <Compass size={22} />
            <h2 className="text-base font-semibold text-white">Welcome to Echo</h2>
          </div>
          <p className="mb-4 max-w-xl text-xs text-[#a59fad]">
            Echo learns what you like entirely on this device, without tracking accounts. Start
            listening to any track, search for your favorite artists, or import playlists from
            Spotify.
          </p>
          <div className="flex flex-wrap gap-2">
            <Link to="/discover" className="button button--primary text-xs">
              <Compass size={14} /> Explore Charts & Genres
            </Link>
            <Link to="/search" className="button button--quiet text-xs">
              <SearchIcon size={14} /> Search Music
            </Link>
          </div>
        </section>
      )}

      {/* Jump Back In: Recently Played */}
      {recentItems.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Jump back in" to="/recently-played" />
          <div className="media-grid">
            {recentItems.slice(0, 6).map((item) => {
              const cardItem =
                item.kind === 'artist'
                  ? { id: item.id, name: item.title, image: item.image, thumbnailUrl: item.image }
                  : item.kind === 'album'
                    ? {
                        id: item.id,
                        title: item.title,
                        artistName: item.subtitle,
                        image: item.image,
                        thumbnailUrl: item.image,
                        type: 'album' as const,
                      }
                    : item.kind === 'playlist'
                      ? { id: item.id, title: item.title, image: item.image }
                      : {
                          id: item.id,
                          title: item.title,
                          artistName: item.subtitle,
                          artistId: `artist-${item.id}`,
                          albumId: `album-${item.id}`,
                          albumTitle: 'Single',
                          albumImage: item.image,
                          artwork: item.image,
                          thumbnailUrl: item.image,
                          durationMs: 0,
                          durationSeconds: 0,
                          explicit: false,
                          playCount: 0,
                        }
              return <MediaCard key={`${item.kind}:${item.id}`} kind={item.kind} item={cardItem} />
            })}
          </div>
        </section>
      )}

      {/* Heavy Rotation */}
      {heavyRotation.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Your Heavy Rotation" />
          <TrackList items={heavyRotation} />
        </section>
      )}

      {/* Quick Picks */}
      {quickPicks.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Quick Picks" />
          <TrackList items={quickPicks} />
        </section>
      )}

      {/* Top Artists Visited */}
      {topArtists.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Your Favourite Artists" />
          <div className="media-grid media-grid--artists">
            {topArtists.map((artist) => (
              <MediaCard
                key={artist.id}
                kind="artist"
                item={{
                  id: artist.id,
                  name: artist.name,
                  image: artist.artwork || '',
                  thumbnailUrl: artist.artwork || '',
                }}
              />
            ))}
          </div>
        </section>
      )}

      {/* Forgotten Favorites */}
      {forgottenFavs.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Forgotten Favorites" />
          <TrackList items={forgottenFavs} />
        </section>
      )}

      {/* User Playlists */}
      {playlists.length > 0 && (
        <section className="page-section">
          <SectionHeading title="Your playlists" to="/library" />
          <div className="quick-grid">
            {playlists.slice(0, 4).map((playlist) => (
              <Link className="quick-card" key={playlist.id} to={`/playlists/${playlist.id}`}>
                {playlist.image && (
                  <img
                    src={playlist.image}
                    alt=""
                    referrerPolicy="no-referrer"
                    onError={(e) => {
                      const fallback = artwork(playlist.title || 'echo-playlist', 80)
                      if (e.currentTarget.src !== fallback) e.currentTarget.src = fallback
                    }}
                  />
                )}
                <span>{playlist.title}</span>
                <span className="quick-card__arrow">
                  <ArrowRight size={16} />
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Remote Catalog Shelves Section */}
      {isLoading && (
        <div className="my-6" aria-busy="true" aria-label="Loading catalog shelves">
          <div className="skeleton-section">
            <div className="skeleton-title" />
            <div className="skeleton-grid">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="skeleton-card" />
              ))}
            </div>
          </div>
        </div>
      )}

      {isError && (
        <div className="my-4 rounded-xl border border-white/10 bg-white/4 p-5" role="alert">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-semibold text-white">Online catalog is unavailable</h3>
              <p className="mt-1 text-xs text-[#a59fad]">
                {error?.message ||
                  'Check your network connection. Your local music, history, and playlists remain active.'}
              </p>
            </div>
            <button
              className="button button--quiet text-xs"
              type="button"
              onClick={() => refetch()}
            >
              <RotateCcw size={14} /> Retry
            </button>
          </div>
        </div>
      )}

      {/* Themed Playlist Shelves (Bollywood, Hollywood, Romance, Soothing) */}
      {visibleShelves?.map((shelf) => (
        <section className="page-section" key={shelf.id}>
          <div className="mb-3 flex items-baseline justify-between">
            <div>
              <h2 className="text-base font-semibold text-white">{shelf.title}</h2>
              <p className="mt-0.5 text-xs text-[#a59fad]">{shelf.subtitle}</p>
            </div>
            {activeCategory === 'all' ? (
              <button
                type="button"
                className="text-xs font-medium text-[#b09cff] transition-colors hover:text-white"
                onClick={() => setActiveCategory(shelf.id)}
              >
                View all
              </button>
            ) : (
              <button
                type="button"
                className="text-xs font-medium text-[#b09cff] transition-colors hover:text-white"
                onClick={() => setActiveCategory('all')}
              >
                Show all categories
              </button>
            )}
          </div>
          <div className="media-grid">
            {shelf.items.map((item) => (
              <MediaCard key={item.id} kind="playlist" item={item} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}

export function DiscoverPage() {
  const [activeTab, setActiveTab] = useState<'curated' | 'charts' | 'moods' | 'trending'>('curated')
  const curated = useHomeFeed()
  const charts = useCharts()
  const moods = useMoodsAndGenres()
  const trending = useTrending()

  const currentQuery =
    activeTab === 'charts'
      ? charts
      : activeTab === 'moods'
        ? moods
        : activeTab === 'trending'
          ? trending
          : curated

  const { data: shelves, isLoading, isError, error, refetch } = currentQuery

  const discoverTabs = [
    { id: 'curated' as const, label: 'Curated' },
    { id: 'charts' as const, label: 'Top Charts' },
    { id: 'moods' as const, label: 'Moods & Genres' },
    { id: 'trending' as const, label: 'Trending' },
  ]

  return (
    <div className="page">
      <PageHeading
        title="Discover"
        description="Explore curated sets, seasonal arrivals, charts, and deep catalog selections."
      />

      <div className="filter-pills mb-6" role="tablist" aria-label="Discover catalog views">
        {discoverTabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={classNames('filter-pill', activeTab === tab.id && 'is-active')}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading && (
        <div className="skeleton-grid" aria-busy="true" aria-label={`Loading ${activeTab}`}>
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="skeleton-card" />
          ))}
        </div>
      )}

      {isError && (
        <div className="error-banner" role="alert">
          <h2>Could not load catalog</h2>
          <p>{error?.message || 'Check your internet connection or proxy server status.'}</p>
          <button className="button button--primary" type="button" onClick={() => refetch()}>
            <RotateCcw size={16} /> Retry
          </button>
        </div>
      )}

      {!isLoading &&
        !isError &&
        shelves &&
        shelves.length > 0 &&
        shelves.map((shelf, shelfIndex) => (
          <section className="page-section" key={`${shelf.title}-${shelfIndex}`}>
            <SectionHeading title={shelf.title} />
            <div className="media-grid">
              {shelf.items.map((item) => (
                <MediaCard
                  key={item.id}
                  kind={
                    'durationSeconds' in item
                      ? 'track'
                      : 'trackCount' in item
                        ? 'album'
                        : 'playlist'
                  }
                  item={item}
                />
              ))}
            </div>
          </section>
        ))}

      {!isLoading && !isError && (!shelves || shelves.length === 0) && (
        <div className="empty-state">
          <p>No catalog shelves found for this view.</p>
        </div>
      )}
    </div>
  )
}

const searchCategories: Array<{ id: SearchFilterKey; label: string }> = [
  { id: 'all', label: 'All' },
  { id: 'songs', label: 'Songs' },
  { id: 'albums', label: 'Albums' },
  { id: 'artists', label: 'Artists' },
  { id: 'playlists', label: 'Playlists' },
  { id: 'videos', label: 'Videos' },
  { id: 'podcasts', label: 'Podcasts' },
]

export function SearchPage() {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<SearchFilterKey>('all')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [activeSuggestion, setActiveSuggestion] = useState(-1)
  const [spotifyModalOpen, setSpotifyModalOpen] = useState(false)
  const [recentSearches, setRecentSearches] = useState<string[]>(() => getStoredRecentSearches())

  const { data: searchResults, isLoading, isError, error } = useSearch(query, category)
  const { data: suggestions = [] } = useSearchSuggestions(query)
  const playTrack = useEchoStore((state) => state.playTrack)
  const setToast = useEchoStore((state) => state.setToast)

  const handleSelectSearch = (term: string) => {
    setQuery(term)
    setShowSuggestions(false)
    const updated = saveRecentSearch(term)
    setRecentSearches(updated)
  }

  const hasQuery = query.trim().length > 0
  const tracks = searchResults?.tracks || []
  const albums = searchResults?.albums || []
  const artists = searchResults?.artists || []
  const playlists = searchResults?.playlists || []

  const totalResults = tracks.length + albums.length + artists.length + playlists.length

  // Omni-link detection
  const isYouTubeUrl =
    query.includes('youtube.com/watch') ||
    query.includes('youtu.be/') ||
    query.includes('music.youtube.com/watch')

  const detectedVideoId = isYouTubeUrl
    ? query.match(/[?&]v=([a-zA-Z0-9_-]{11})/)?.[1] ||
      query.match(/youtu\.be\/([a-zA-Z0-9_-]{11})/)?.[1]
    : null

  const detectedPlaylistMatch = query.match(/[?&]list=([a-zA-Z0-9_-]+)/)?.[1]
  const detectedPlaylistId = detectedPlaylistMatch
    ? detectedPlaylistMatch.startsWith('VL')
      ? detectedPlaylistMatch
      : `VL${detectedPlaylistMatch}`
    : null

  const detectedChannelId =
    query.match(/youtube\.com\/channel\/(UC[a-zA-Z0-9_-]+)/)?.[1] ||
    query.match(/music\.youtube\.com\/channel\/(UC[a-zA-Z0-9_-]+)/)?.[1] ||
    null

  const isSpotifyLink = query.includes('spotify.com')

  const handlePlayDirectVideo = () => {
    if (!detectedVideoId) return
    const directTrack: Track = {
      id: detectedVideoId,
      videoId: detectedVideoId,
      title: 'YouTube Direct Audio Stream',
      artistName: 'Echo Link Resolution',
      artistId: `artist_${detectedVideoId}`,
      albumId: `album_${detectedVideoId}`,
      albumTitle: 'Direct Audio',
      albumImage: `https://i.ytimg.com/vi/${detectedVideoId}/hqdefault.jpg`,
      artwork: `https://i.ytimg.com/vi/${detectedVideoId}/hqdefault.jpg`,
      thumbnailUrl: `https://i.ytimg.com/vi/${detectedVideoId}/hqdefault.jpg`,
      durationMs: 240000,
      durationSeconds: 240,
      explicit: false,
      playCount: 1,
    }
    playTrack(directTrack)
    setToast('Resolving and playing direct YouTube stream')
  }

  return (
    <div className="page">
      <PageHeading
        title="Search"
        description="Search millions of songs, albums, artists, or paste any YouTube or Spotify link."
      />

      <div className="search-field" style={{ position: 'relative' }}>
        <SearchIcon size={20} />
        <input
          id="global-search"
          type="search"
          autoComplete="off"
          placeholder="Search catalog or paste YouTube / Spotify URL..."
          value={query}
          onChange={(event) => {
            setQuery(event.target.value)
            setActiveSuggestion(-1)
            setShowSuggestions(true)
          }}
          onFocus={() => setShowSuggestions(true)}
          onBlur={() => window.setTimeout(() => setShowSuggestions(false), 120)}
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              setQuery('')
              setActiveSuggestion(-1)
              setShowSuggestions(false)
            } else if (event.key === 'ArrowDown' && suggestions.length > 0) {
              event.preventDefault()
              setActiveSuggestion((index) => (index + 1) % suggestions.length)
            } else if (event.key === 'ArrowUp' && suggestions.length > 0) {
              event.preventDefault()
              setActiveSuggestion((index) => (index <= 0 ? suggestions.length - 1 : index - 1))
            } else if (event.key === 'Enter') {
              if (activeSuggestion >= 0 && suggestions[activeSuggestion]) {
                event.preventDefault()
                handleSelectSearch(suggestions[activeSuggestion]!)
              } else if (query.trim()) {
                handleSelectSearch(query.trim())
              }
            }
          }}
          aria-expanded={showSuggestions && suggestions.length > 0}
          aria-haspopup="listbox"
          aria-activedescendant={
            activeSuggestion >= 0 ? `search-suggestion-${activeSuggestion}` : undefined
          }
          role="combobox"
          aria-controls="search-suggestions-list"
        />
        {query && (
          <button
            className="icon-button"
            type="button"
            aria-label="Clear search"
            onClick={() => {
              setQuery('')
              setShowSuggestions(false)
            }}
          >
            <X size={18} />
          </button>
        )}
        <kbd>ESC</kbd>

        {showSuggestions && suggestions.length > 0 && (
          <div
            className="search-suggestions-dropdown"
            role="listbox"
            id="search-suggestions-list"
            aria-label="Search suggestions"
            style={{
              position: 'absolute',
              top: '100%',
              left: 0,
              right: 0,
              marginTop: '0.375rem',
              background: 'var(--color-surface, #181524)',
              border: '1px solid var(--color-border, #2d2642)',
              borderRadius: '0.75rem',
              boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.5)',
              zIndex: 50,
              overflow: 'hidden',
            }}
          >
            {suggestions.map((sugg, index) => (
              <button
                key={sugg}
                type="button"
                id={`search-suggestion-${index}`}
                className="suggestion-item"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  width: '100%',
                  padding: '0.75rem 1rem',
                  border: 'none',
                  background:
                    index === activeSuggestion
                      ? 'var(--color-hover, rgba(255,255,255,0.08))'
                      : 'transparent',
                  color: 'inherit',
                  textAlign: 'left',
                  cursor: 'pointer',
                  fontSize: '0.9375rem',
                }}
                aria-selected={index === activeSuggestion}
                onMouseEnter={() => setActiveSuggestion(index)}
                onClick={() => {
                  handleSelectSearch(sugg)
                  setActiveSuggestion(-1)
                }}
              >
                <SearchIcon size={15} opacity={0.6} />
                <span>{sugg}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Direct Link Detection Banner */}
      {detectedVideoId && (
        <div className="my-2 flex items-center justify-between gap-4 rounded-xl border border-[#7c5cff]/30 bg-[#7c5cff]/15 p-4">
          <div className="flex items-center gap-3">
            <img
              src={`https://i.ytimg.com/vi/${detectedVideoId}/hqdefault.jpg`}
              alt=""
              className="h-12 w-12 rounded-lg object-cover"
            />
            <div>
              <span className="block text-xs font-semibold text-white">YouTube Link Detected</span>
              <span className="font-mono text-[11px] text-[#b09cff]">
                Video ID: {detectedVideoId}
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={handlePlayDirectVideo}
            className="flex items-center gap-1.5 rounded-xl bg-[#7c5cff] px-4 py-2 text-xs font-semibold text-black shadow-md transition-colors hover:bg-[#6c48ff]"
          >
            <Play size={14} fill="currentColor" />
            <span>Play Now</span>
          </button>
        </div>
      )}

      {detectedPlaylistId && (
        <div className="my-2 flex items-center justify-between gap-4 rounded-xl border border-[#7c5cff]/30 bg-[#7c5cff]/15 p-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-[#7c5cff]/20 p-2.5 text-[#7c5cff]">
              <Music2 size={20} />
            </div>
            <div>
              <span className="block text-xs font-semibold text-white">
                YouTube Playlist Link Detected
              </span>
              <span className="font-mono text-[11px] text-[#b09cff]">
                Playlist ID: {detectedPlaylistId}
              </span>
            </div>
          </div>
          <Link
            to={`/playlists/${detectedPlaylistId}`}
            className="flex items-center gap-1.5 rounded-xl bg-[#7c5cff] px-4 py-2 text-xs font-semibold text-black shadow-md transition-colors hover:bg-[#6c48ff]"
          >
            <span>Open Playlist</span>
          </Link>
        </div>
      )}

      {detectedChannelId && (
        <div className="my-2 flex items-center justify-between gap-4 rounded-xl border border-[#7c5cff]/30 bg-[#7c5cff]/15 p-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-[#7c5cff]/20 p-2.5 text-[#7c5cff]">
              <Music2 size={20} />
            </div>
            <div>
              <span className="block text-xs font-semibold text-white">
                YouTube Artist Link Detected
              </span>
              <span className="font-mono text-[11px] text-[#b09cff]">
                Channel ID: {detectedChannelId}
              </span>
            </div>
          </div>
          <Link
            to={`/artists/${detectedChannelId}`}
            className="flex items-center gap-1.5 rounded-xl bg-[#7c5cff] px-4 py-2 text-xs font-semibold text-black shadow-md transition-colors hover:bg-[#6c48ff]"
          >
            <span>View Artist</span>
          </Link>
        </div>
      )}

      {isSpotifyLink && (
        <div className="my-2 flex items-center justify-between gap-4 rounded-xl border border-[#58ad83]/30 bg-[#58ad83]/15 p-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-[#58ad83]/20 p-2.5 text-[#58ad83]">
              <Music2 size={20} />
            </div>
            <div>
              <span className="block text-xs font-semibold text-white">Spotify Link Detected</span>
              <span className="text-[11px] text-[#a59fad]">
                Import playlist directly into your Echo library
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setSpotifyModalOpen(true)}
            className="flex items-center gap-1.5 rounded-xl bg-[#58ad83] px-4 py-2 text-xs font-semibold text-black shadow-md transition-colors hover:bg-[#489972]"
          >
            <DownloadCloud size={14} />
            <span>Import with Echo</span>
          </button>
        </div>
      )}

      {hasQuery && (
        <div className="filter-pills" role="tablist" aria-label="Filter search results">
          {searchCategories.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={category === item.id}
              className={classNames('filter-pill', category === item.id && 'is-active')}
              onClick={() => {
                setCategory(item.id)
                setShowSuggestions(false)
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}

      {isLoading && hasQuery && (
        <div className="route-loading" role="status">
          <span className="route-loading__mark" />
          <span>Searching live catalog...</span>
        </div>
      )}

      {isError && hasQuery && (
        <div className="error-banner" role="alert">
          <h2>Search request failed</h2>
          <p>{error?.message || 'Check your internet connection or proxy server status.'}</p>
        </div>
      )}

      {!isLoading && hasQuery && totalResults === 0 && !isError && (
        <div className="search-empty">
          <SearchIcon size={25} />
          <h2>No matches found for “{query}”</h2>
          <p>Try searching for a different song, artist, or album title.</p>
        </div>
      )}

      {!hasQuery && (
        <div className="search-start">
          {recentSearches.length > 0 && (
            <section className="page-section mb-6">
              <SectionHeading
                title="Recent searches"
                action={
                  <button
                    type="button"
                    className="button button--quiet text-xs"
                    onClick={() => {
                      clearAllRecentSearches()
                      setRecentSearches([])
                    }}
                  >
                    Clear all
                  </button>
                }
              />
              <div className="flex flex-wrap gap-2">
                {recentSearches.map((term) => (
                  <div
                    key={term}
                    className="flex items-center gap-1 rounded-full bg-white/6 px-3 py-1.5 text-xs text-white transition-colors hover:bg-white/10"
                  >
                    <button
                      type="button"
                      className="cursor-pointer text-left hover:underline"
                      onClick={() => handleSelectSearch(term)}
                    >
                      <Clock3 size={13} className="mr-1.5 inline text-[#a59fad]" />
                      <span>{term}</span>
                    </button>
                    <button
                      type="button"
                      aria-label={`Remove ${term} from recent searches`}
                      className="ml-1 cursor-pointer rounded-full p-0.5 text-[#a59fad] hover:text-white"
                      onClick={(e) => {
                        e.stopPropagation()
                        const updated = removeRecentSearch(term)
                        setRecentSearches(updated)
                      }}
                    >
                      <X size={13} />
                    </button>
                  </div>
                ))}
              </div>
            </section>
          )}

          <section className="page-section">
            <SectionHeading title="Suggested searches" />
            <div className="search-suggestions">
              {['Radiohead', 'Billie Eilish', 'Kendrick Lamar', 'Daft Punk', 'Fleetwood Mac'].map(
                (suggestion) => (
                  <button
                    key={suggestion}
                    className="suggestion-chip"
                    type="button"
                    onClick={() => handleSelectSearch(suggestion)}
                  >
                    {suggestion}
                  </button>
                ),
              )}
            </div>
          </section>
        </div>
      )}

      {!isLoading && hasQuery && totalResults > 0 && (
        <>
          {tracks.length > 0 && (category === 'all' || category === 'songs') && (
            <section className="page-section">
              <SectionHeading
                title="Songs"
                action={
                  category === 'all' && tracks.length > 4 ? (
                    <button
                      className="button-link"
                      type="button"
                      onClick={() => setCategory('songs')}
                    >
                      See all {tracks.length}
                    </button>
                  ) : undefined
                }
              />
              <TrackList items={category === 'all' ? tracks.slice(0, 4) : tracks} />
            </section>
          )}

          {albums.length > 0 && (category === 'all' || category === 'albums') && (
            <section className="page-section">
              <SectionHeading
                title="Albums"
                action={
                  category === 'all' && albums.length > 4 ? (
                    <button
                      className="button-link"
                      type="button"
                      onClick={() => setCategory('albums')}
                    >
                      See all {albums.length}
                    </button>
                  ) : undefined
                }
              />
              <div className="media-grid">
                {(category === 'all' ? albums.slice(0, 4) : albums).map((album) => (
                  <MediaCard key={album.id} kind="album" item={album} />
                ))}
              </div>
            </section>
          )}

          {artists.length > 0 && (category === 'all' || category === 'artists') && (
            <section className="page-section">
              <SectionHeading
                title="Artists"
                action={
                  category === 'all' && artists.length > 4 ? (
                    <button
                      className="button-link"
                      type="button"
                      onClick={() => setCategory('artists')}
                    >
                      See all {artists.length}
                    </button>
                  ) : undefined
                }
              />
              <div className="media-grid">
                {(category === 'all' ? artists.slice(0, 4) : artists).map((artist) => (
                  <MediaCard key={artist.id} kind="artist" item={artist} />
                ))}
              </div>
            </section>
          )}

          {playlists.length > 0 && (category === 'all' || category === 'playlists') && (
            <section className="page-section">
              <SectionHeading
                title="Playlists"
                action={
                  category === 'all' && playlists.length > 4 ? (
                    <button
                      className="button-link"
                      type="button"
                      onClick={() => setCategory('playlists')}
                    >
                      See all {playlists.length}
                    </button>
                  ) : undefined
                }
              />
              <div className="media-grid">
                {(category === 'all' ? playlists.slice(0, 4) : playlists).map((playlist) => (
                  <MediaCard key={playlist.id} kind="playlist" item={playlist} />
                ))}
              </div>
            </section>
          )}
        </>
      )}

      <SpotifyImportModal isOpen={spotifyModalOpen} onClose={() => setSpotifyModalOpen(false)} />
    </div>
  )
}

const libraryTabs = ['All', 'Playlists', 'Liked', 'Local Music'] as const
type LibraryTab = (typeof libraryTabs)[number]

export function LibraryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')?.toLowerCase()
  const tab = libraryTabs.find((item) => item.toLowerCase() === requestedTab) ?? 'All'
  const preferences = useEchoStore((state) => state.preferences)
  const setPreference = useEchoStore((state) => state.setPreference)
  const [layout, setLayout] = useState<'grid' | 'list'>(preferences.lastPlayedLayout)
  const [filter, setFilter] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [spotifyModalOpen, setSpotifyModalOpen] = useState(false)

  const fileInputRef = useRef<HTMLInputElement>(null)
  const playlists = useEchoStore((state) => state.playlists)
  const likedTrackIds = useEchoStore((state) => state.likedTrackIds)
  const trackCache = useEchoStore((state) => state.trackCache)
  const cacheTrack = useEchoStore((state) => state.cacheTrack)
  const createPlaylist = useEchoStore((state) => state.createPlaylist)
  const addTrackToPlaylist = useEchoStore((state) => state.addTrackToPlaylist)
  const setToast = useEchoStore((state) => state.setToast)

  // Resolve any liked tracks missing from memory/cache
  useEffect(() => {
    if (tab !== 'All' && tab !== 'Liked') return
    const missing = likedTrackIds.filter((id) => !trackCache[id] && !getCachedTrack(id))
    if (missing.length === 0) return

    let cancelled = false
    void Promise.all(
      missing.map(async (id) => {
        const resolved = await resolveMissingTrack(id)
        if (resolved && !cancelled) {
          cacheTrack(resolved)
        }
      }),
    )

    return () => {
      cancelled = true
    }
  }, [tab, likedTrackIds, trackCache, cacheTrack])

  const selectTab = (nextTab: LibraryTab) => {
    const nextParams = new URLSearchParams(searchParams)
    if (nextTab === 'All') nextParams.delete('tab')
    else nextParams.set('tab', nextTab.toLowerCase())
    setSearchParams(nextParams)
  }

  const filteredPlaylists = playlists.filter((p) =>
    p.title.toLowerCase().includes(filter.toLowerCase()),
  )

  const likedTracks: Track[] = likedTrackIds
    .map((id) => trackCache[id] || getCachedTrack(id))
    .filter((t): t is Track => Boolean(t))

  const handleM3UOrCSVUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    const content = await file.text()
    let parsedItems: Array<{ title: string; artist: string }> = []
    if (file.name.endsWith('.m3u') || file.name.endsWith('.m3u8')) {
      parsedItems = parseM3UPlaylist(content)
    } else if (file.name.endsWith('.csv')) {
      parsedItems = parseCSVPlaylist(content)
    }

    if (parsedItems.length === 0) {
      setToast('Could not find songs in the uploaded file')
      return
    }

    const plTitle = file.name.replace(/\.[^/.]+$/, '')
    const plId = createPlaylist(plTitle, `Imported from ${file.name}`)
    let resolvedCount = 0

    for (let i = 0; i < Math.min(parsedItems.length, 30); i++) {
      const item = parsedItems[i]!
      try {
        const cat = await InnerTubeClient.search(`${item.title} ${item.artist}`, 'songs')
        if (cat.tracks && cat.tracks[0]) {
          addTrackToPlaylist(plId, cat.tracks[0])
          resolvedCount++
        }
      } catch {
        // Continue
      }
    }

    setToast(`Imported "${plTitle}" (${resolvedCount} tracks matched)`)
    if (e.target) e.target.value = ''
  }

  return (
    <div className="page">
      <PageHeading
        title="Your library"
        description="All your music. Everything you have saved, followed, and scanned."
        action={
          <div className="flex items-center gap-2">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleM3UOrCSVUpload}
              accept=".m3u,.m3u8,.csv"
              className="hidden"
            />
            <button
              className="button button--secondary"
              type="button"
              onClick={() => fileInputRef.current?.click()}
              title="Import M3U or CSV playlist"
            >
              <FileUp size={16} /> Import File
            </button>
            <button
              className="button button--secondary"
              type="button"
              onClick={() => setSpotifyModalOpen(true)}
              title="Import from Spotify"
            >
              <Music2 size={16} /> Spotify
            </button>
            <button
              className="button button--primary"
              type="button"
              onClick={() => setCreateOpen(true)}
            >
              <span className="plus-mark">+</span> New playlist
            </button>
          </div>
        }
      />

      <div className="library-toolbar">
        <div className="filter-pills" role="tablist" aria-label="Library category">
          {libraryTabs.map((item) => (
            <button
              type="button"
              role="tab"
              key={item}
              aria-selected={tab === item}
              className={classNames('filter-pill', tab === item && 'is-active')}
              onClick={() => selectTab(item)}
            >
              {item}
            </button>
          ))}
        </div>

        <div className="library-tools">
          <label className="sr-only" htmlFor="library-filter">
            Filter your library
          </label>
          <input
            id="library-filter"
            className="library-filter"
            placeholder="Filter"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          />
          <div className="layout-toggle" aria-label="Library layout">
            <button
              type="button"
              aria-label="Grid layout"
              aria-pressed={layout === 'grid'}
              onClick={() => {
                setLayout('grid')
                setPreference('lastPlayedLayout', 'grid')
              }}
              className={layout === 'grid' ? 'is-active' : ''}
            >
              Grid
            </button>
            <button
              type="button"
              aria-label="List layout"
              aria-pressed={layout === 'list'}
              onClick={() => {
                setLayout('list')
                setPreference('lastPlayedLayout', 'list')
              }}
              className={layout === 'list' ? 'is-active' : ''}
            >
              List
            </button>
          </div>
        </div>
      </div>

      {/* Tab: Local Music */}
      {tab === 'Local Music' && (
        <section className="page-section">
          <LocalMusicSection />
        </section>
      )}

      {(tab === 'All' || tab === 'Playlists') && (
        <section className="page-section">
          {tab === 'All' && <SectionHeading title="Playlists" />}
          <div
            className={classNames(
              'library-collections',
              layout === 'list' && 'library-collections--list',
            )}
          >
            {filteredPlaylists.map((item) => (
              <MediaCard
                key={item.id}
                kind="playlist"
                item={item}
                subtitle={`${item.trackIds.length} tracks · ${item.owner}`}
              />
            ))}
            <button
              className="library-create-card"
              type="button"
              onClick={() => setCreateOpen(true)}
            >
              <span>
                <span className="plus-mark">+</span>
              </span>
              <strong>Create a playlist</strong>
              <small>Put a few favorites together.</small>
            </button>
          </div>
        </section>
      )}

      {(tab === 'All' || tab === 'Liked') && likedTracks.length > 0 && (
        <section className="page-section">
          {tab === 'All' && <SectionHeading title="Liked songs" to="/liked" />}
          <TrackList items={likedTracks} />
        </section>
      )}

      {tab === 'All' && (
        <section className="page-section library-shortcuts">
          <SectionHeading title="Made for you" />
          <div className="shortcut-links">
            <Link to="/liked">
              <span className="shortcut-icon">
                <Heart size={20} />
              </span>
              <span>
                <strong>Liked Songs</strong>
                <small>{likedTrackIds.length} tracks in collection</small>
              </span>
              <ArrowRight size={17} />
            </Link>
            <Link to="/recently-played">
              <span className="shortcut-icon">
                <Clock3 size={20} />
              </span>
              <span>
                <strong>Recently Played</strong>
                <small>Pick up where you left off</small>
              </span>
              <ArrowRight size={17} />
            </Link>
          </div>
        </section>
      )}

      {tab === 'Playlists' && filteredPlaylists.length === 0 && (
        <div className="empty-state">
          <Headphones size={27} />
          <h3>No playlists yet</h3>
          <p>Create your first playlist to begin organizing your music.</p>
        </div>
      )}

      <CreatePlaylistDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <SpotifyImportModal isOpen={spotifyModalOpen} onClose={() => setSpotifyModalOpen(false)} />
    </div>
  )
}
